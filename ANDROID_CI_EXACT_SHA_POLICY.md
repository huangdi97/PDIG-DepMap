# ANDROID_CI_EXACT_SHA_POLICY.md

> 生成时间：2026-09-22（ANDROID_CANONICAL_FREEZE → Production/Reality Closure 轮）
> 对应契约：Goal §6「建立可审计 exact-SHA policy：workflow_dispatch + commit SHA + run URL + 全部 job success；禁止写『CI green』而不写明 commit」。
> 结论：**`PUSH_TRIGGER = PASS`**（证据见 §3）。

---

## 1. Policy（必须遵守）

在任何报告、验收、Gate 结论中引用 CI 结果时，**必须同时提供下列 4 项**，缺一不可：

```text
1. workflow_dispatch（或 push）触发
2. 被测 commit 的完整 SHA（head_sha）
3. run URL / run ID（https://github.com/huangdi97/PDIG-DepMap/actions/runs/<id>）
4. 该 run 的全部 job 的最终结论（success / failure）
```

**禁止**：只写「CI green」/「CI passed」而不写明被测 commit——那无法区分是哪个 commit 的绿。

### 1.1 写报告的最小格式示例

```text
CI: push@89b653a13f3dd96f6ed4acc579128b218c9b7c22
run https://github.com/huangdi97/PDIG-DepMap/actions/runs/35700579040
jobs: Android app=success · Android core=success · Canonical=success · Harmony static=success
```

### 1.2 什么不算有效 CI 证据

- 只给 run ID 不给 SHA，或只给 SHA 不给 run URL；
- 用 `workflow_dispatch` 的绿冒充**任意 commit** 的 push CI 绿（dispatch 只证明它 dispatch 的那个 SHA）；
- `continue-on-error` / 全 skip 的「绿」；
- 只列出部分成功 job 而隐藏失败 job。

---

## 2. push 触发根因调查（真实原因）

### 2.1 历史问题

- 过去 `.github/workflows/ci.yml` 使用 `branches: ["**"]`，在本仓库**从未触发过一次 event=push 的运行**
  （全部 14 条早期运行都是 workflow_dispatch）。「看起来配了自动验证、实际一次没跑」是比没配更危险的假象。
- 2026-09-19 已改为**显式列举分支**：
  ```yaml
  on:
    push:
      branches: [main, feat/mvp03-living-graph]
    pull_request:
      branches: [main]
    workflow_dispatch:
  ```
  并新增 `ios.yml`。

### 2.2 当前验证结果（2026-09-22 实测，`gh api` 证据）

- `repos/huangdi97/PDIG-DepMap/actions/runs?event=push` → **total_count = 44**（存在 event=push 运行）。
- 最近一条针对基准 HEAD 的 **event=push** 运行：
  - **CI** run `35700579040`：`head_sha = 89b653a13f3dd96f6ed4acc579128b218c9b7c22`、
    branch `feat/mvp03-living-graph`、event `push`、status `completed`、conclusion **success**，
    4 个 job 全部 success：
    `Android app (JVM tests + assembleDebug)` / `Android core (JVM tests + conformance)` /
    `Canonical (codegen / fixtures / oracle)` / `Harmony static (no SDK, no device)`。
    URL: https://github.com/huangdi97/PDIG-DepMap/actions/runs/35700579040
  - **iOS** run `35700579087`：同 SHA `89b653a…`，event `push`，conclusion **success**，
    job `iOS core (Swift build + canonical conformance)` success。
    URL: https://github.com/huangdi97/PDIG-DepMap/actions/runs/35700579087

### 2.3 根因结论

```
PUSH_TRIGGER = PASS
原因：branches 显式列举修正后，push 已能正确触发 workflow（44 条 event=push 运行，最新 2 条
      均针对基准 HEAD 89b653a 且全绿）。历史「push 不触发」的根因是 branches: ["**"] 在本仓库
      不生效（GitHub 对跨分支通配符的语义），不是 repo 级 Actions policy / permissions / 外部设置。
证据：actions/runs?event=push (total_count=44) + 上述两条 run 的 head_sha/event/jobs 明细（gh api）。
```

> 若未来 push 触发再次失效，优先复查：工作流文件是否存在于 default branch、branches 过滤是否
> 显式覆盖目标分支、Actions 是否在 repo 级别被限制、concurrency 是否取消、skip-ci 提交信息模式。

---

## 3. 本轮 exact-SHA 证据（契约 §6 / §22）

| 项 | 值 |
|----|----|
| Push CI（基准 HEAD） | run 35700579040（CI）+ 35700579087（iOS），head_sha = `89b653a13f3dd96f6ed4acc579128b218c9b7c22`，event = push，all jobs success ✅ |
| 本地回归（同基准 HEAD 复跑） | `:core:test` 71/71 · `:app:testDebugUnitTest` 9/9 · `:conformance:run` 91/91 · connectedDebugAndroidTest **59/59**（emulator-5554，2026-09-22 实跑）· assembleDebug / assembleRelease / bundleRelease SUCCESSFUL |
| 禁止项 | 不得把本表 push CI 绿写成「Harmony native verification PASS」；不得把 dispatch 绿写成任意 commit 的 push 绿 |

---

## 4. 状态
## 3.5 2026-09-22 main@3f466f8 push CI 状态（账户计费限制，非代码缺陷）

- 本轮文档 commit `3f466f8` push 后，CI run `35728513967`（event=push）触发：
  - Android app / Android core / Harmony static 3 个 job **success** ✅
  - iOS run `35728513716` **success** ✅
  - Canonical job **未启动**（annotations：`The job was not started because recent account payments have
    failed or your spending limit needs to be increased`）→ **GitHub 账户计费/配额限制**
- 重跑 `gh run rerun --failed` 后 Canonical 仍被同一账单限制拦截（非代码/配置缺陷）。
- **基准 HEAD 89b653a 的 push CI 全绿不受影响**（run 35700579040 CI + 35700579087 iOS，all jobs success）。
- 结论：`PUSH_TRIGGER` 仍然 PASS（push 事件正确触发 workflow）；Canonical job 的调度被
  **EXTERNAL_REPO_ACCOUNT_BILLING** 阻塞 —— 属于外部 blocker，见 BLOCKERS.md E-10。
- 解除：用户在 GitHub → Settings → Billing & plans 处理账单/提升 spending limit 后重跑。
