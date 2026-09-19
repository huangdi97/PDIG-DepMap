# GIT_OPS_INCIDENT_AND_RULES.md — 2026-09-18/19 Git 事故与运维规则

> 本文是**工程运维文档**，记录一次真实事故（对象库损坏→恢复→非 force 快进推送），
> 并固化由此产生的规则。任何在本仓库处理分叉 / 推进 main 的人都应先读 §3。

事故对象：`feat/mvp03-living-graph`（本轮工作：Harmony ArkTS parser 移植，
canonical 65 → 85/91）。
结果：**无文件内容丢失**，最终以非 force 的 fast-forward 推送到远端。

---

## 1. 现象链（按发生顺序）

| # | 现象 | 直接证据 |
| --- | --- | --- |
| 1 | `git commit` 输出正常、commit 对象已建、reflog 已写，**但分支 ref 不动** | `git log --oneline -1` 仍是旧 commit；`git cat-file -t <新sha>` 存在 |
| 2 | `git update-ref` 返回 0，同样不生效；关掉沙箱重试也无效 | `git rev-parse HEAD` 不变 |
| 3 | 分支 ref **只存在于 `packed-refs`**，`.git/refs/heads/feat/` 目录不存在 | `git show-ref` 出得来；`ls .git/refs/heads/` 里没有 `feat/` |
| 4 | `git push` 之后 `refs/remotes/origin/*` **变空**（fetch 重写 packed-refs 时丢 remote-tracking） | `git rev-parse origin/feat/...` 失败；`ls -R .git/refs/remotes` 空 |
| 5 | `git rebase --onto` 被 120s 超时 SIGTERM 杀死，留下空的 `.git/rebase-merge/` | `ls .git/rebase-merge` 为空目录 |
| 6 | rebase 触发的 gc 把「因 ref 混乱而显得不可达」的对象 prune 掉 → **对象库损坏** | `pack has 42 unresolved deltas`；`Could not read <sha>`；本地独有提交的树**永久丢失** |
| 7 | 协商式 `git fetch` 无法修复：delta base 已缺，协商必然失败 | `git fetch --no-tags origin <branch>` 报 unresolved deltas 后中止 |
| 8 | `git commit` 本身也会触发 gc，二次损坏 | commit 被超时中断后再查，对象又少一批 |

根因一句话：**分支 ref 写在 packed-refs 里、而创建嵌套 loose ref 目录的操作在本环境被拦截**
（现象 1–4），加上**长事务 git 命令被超时杀死后由 gc 剪掉"看似不可达"的对象**（现象 5–8）。
两者叠加：ref 不动 → 用 rebase 救 → rebase 被杀 → 对象损坏。

---

## 2. 恢复步骤（已实操验证，按顺序）

0. **`tar` 备份工作区**（`--exclude=.git`）。工作区文件自始至终完好，这一步是安全帽。
1. `git clone --mirror <remote>` 一份到独立目录。
2. **直接 `cp -n` 镜像的 `objects/pack/*` 到 `.git/objects/pack/`**
   —— 不走 `git fetch`：协商会因为 delta base 缺失而失败。
3. `mkdir -p .git/refs/heads/feat` 后手写分支 ref
   （`git rev-parse <sha> > .git/refs/heads/feat/mvp03-living-graph`）；
   远端 tracking ref 同理手写进 `.git/refs/remotes/origin/...`。
4. **重建索引**：`rm -f .git/index && git add -A`。
   只 `git add -A` 不够 —— stat 信息未变时它会跳过 blob 写入，树对象依旧缺失。
5. **plumbing 建提交**：
   `T=$(git write-tree)` → `git commit-tree "$T" -p <远端尖端sha> -F -`（父显式设为远端尖端）。
6. **校验重建结果**：`git diff-tree -r --stat <远端尖端> "$T"`，
   差异必须**恰好等于本轮改动**（本轮：26 files / +3661 / −497；6 新增 1 删除 19 修改）。
   这是"重建的树就是我写的那份"的唯一证据，不能省。
7. 非 force push：父子关系正确，`git push origin <branch>` 走 fast-forward。

---

## 3. 规则（本仓库在本环境下必须遵守）

### R1 — 禁止用 `rebase` 处理分叉

`rebase` 是长事务：被超时杀死后触发的 gc 会剪掉"因 ref 混乱显得不可达"的对象，
造成**不可恢复的**本地独有提交丢失（本次已发生）。

分叉的正确处理策略，二选一：

- **A（推荐）**：`remote tip + 当前 worktree 树 → plumbing commit with explicit parent`
  ```
  git fetch origin
  T=$(git write-tree)                       # 需要时先 rm -f .git/index && git add -A
  NEW=$(git commit-tree "$T" -p <origin/分支尖端> -F -)
  git rev-parse "$NEW" > .git/refs/heads/<分支>
  ```
  再用 `git diff-tree` 校验（步骤 6）。
- **B**：直接 fresh clone 远端，把改动在新 clone 里重新提交后推送。

### R2 — 推进 main 只走 fast-forward，且以 fresh clone 复验为准

main 推进前：
`git merge-base --is-ancestor origin/main origin/<branch>` 必须 exit 0，
然后 `git merge --ff-only`。禁止 merge commit / rebase / force push。

当工作区发生过对象库级事故时，额外做 **REMOTE FRESH CLONE REPRODUCTION**：
在全新 ASCII-only 目录 `git clone` → checkout 目标分支 →
`git fsck --full` 无损坏 → 在该 clone 里重跑全部 portable gates 与本机 Harmony 门禁 →
全部通过后才推进 main。旧工作区在事故后只作为**已完成工作的参考**，
不再执行 rebase / gc / prune / `reset --hard` / `clean -fd` / force push。

### R3 — 分支 / 远端 ref 可能"看得见但写不进"

`git commit` 显示成功 ≠ 分支推进了。每次 commit / push 后用
`git rev-parse HEAD` 与 `git ls-remote origin refs/heads/<branch>` **双向核对**。
`git update-ref` 在本环境可能返回 0 却无效；需要时手写 ref 文件（先 `mkdir -p` 嵌套目录）。

### R4 — 别在损坏的对象库上反复尝试

一旦出现 `unresolved deltas` / `Could not read`，停止 `fetch` / `commit` / `gc`，
直接进入 §2 的镜像回填流程。每次失败尝试都可能再剪掉一批对象。

---

## 4. CI 口径（永久，四条独立账）

| 账 | 值 | 产生位置 |
| --- | --- | --- |
| `GITHUB_PORTABLE_CI` | PASS / FAIL | `.github/workflows/ci.yml` 全部 job |
| `HARMONY_HOST_CONFORMANCE_LOCAL` | PASS 89/89（canonical 85/91） | 本机 DevEco hvigor，`tools/harmony/run-conformance-host.mjs` |
| `HARMONY_COMPILE_REACHABILITY_LOCAL` | PASS（A/B/C/D） | 本机 clean assembleHap，`tools/harmony/check-compiled-reachability.mjs --build` |
| `HARMONY_GITHUB_HOSTED_NATIVE_BUILD` | **NOT_AVAILABLE** | hosted runner 无 DevEco / HarmonyOS SDK / hvigor |
| `HARMONY_DEVICE_RUNTIME` | **NOT_RUN** | 无设备运行时 |

禁止：把 `GITHUB_PORTABLE_CI = PASS` 写成「Harmony native verification PASS」。
也禁止：因为 hosted runner 没有 DevEco，就永远阻止 main integration ——
Harmony 的本地证据按次记录于 `WORK_STATUS.md` 与 `HARMONY_REMAINING_6_AUDIT.md`，
main 推进以「远端 portable CI 绿 + 本地 Harmony 门禁绿」为准。

---

## 5. 顺带修正：workflow 的 push 触发

`on: push: branches: ["**"]` 在本仓库**从未触发过一次运行** ——
`gh api repos/<repo>/actions/runs` 的全部历史运行都是 `workflow_dispatch`
（含对 main 与 feat 分支的多次真实 push）。
"看起来配了自动验证、实际一次没跑"比没配更危险。

已改为显式列举：`push: [main, feat/mvp03-living-graph]`、`pull_request: [main]`。

**但改完之后 push 依然没有触发 —— 必须如实记下，不能让"改过"冒充"修好"：**

- 2026-09-19 02:28:34Z 的 `PushEvent`（`refs/heads/feat/mvp03-living-graph`，
  含本次 workflow 改动）在 `repos/<repo>/events` 里**确实存在**；
- 而 `repos/<repo>/actions/runs?event=push` 的 `total_count` 仍为 **0**；
- 该 commit 的 check-runs 为 **0**。

即：GitHub 收到了 push，但没有为它启动任何运行。分支过滤器不是原因
（`["**"]` 与显式列举都不触发）。已从 API 侧排除的项：
`actions/permissions` = `enabled:true, allowed_actions:all`；
workflow 本身解析正常（同一份文件的 `workflow_dispatch` 能跑）；
commit message 无 `[skip ci]`；仓库非 fork、未归档、main 为默认分支。

**因此当前生效的规则是**：在出现第一条 `event=push` 的运行之前，
远端验证一律手工触发（`gh workflow run CI --ref <branch>`）并用 API 核对
`head_sha`；不得因为"配了 push 触发"就假定它已经跑过。

仍未查明的部分需在 GitHub 网页端核对（API 不暴露）：
Settings → Actions → General 的 Actions permissions / Allow actions、
以及是否存在组织级策略限制。
