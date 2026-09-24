# GITHUB_PREPUBLICATION_BASELINE.md

> GitHub 首次发布前基线（A0）。
> 生成时间：2026-09-17（Asia/Shanghai）
> 原则：**不重新初始化 Git**。当前 `.git`、commit history、branch、tags、Legacy freeze 历史
> 与 Android Native migration 历史全部保留。

---

## 0. 一句话结论

> 当前工作树 **CLEAN**（0 modified / 0 staged / 0 untracked），706 个文件受控，
> **无 remote**，124 个 commit 可达，3 个 tag 完整。
> 仓库对象库极小（`.git` = **4.22 MB**），**无需 Git LFS**。
> 因此本轮 A1「处理当前工作树」的收口动作只有一个：把 1 个误入库的 agent memory 文件移出版本控制。

---

## 1. A0 命令实测输出

| 命令                                       | 实测结果                                                             |
| ------------------------------------------ | -------------------------------------------------------------------- |
| `git rev-parse --show-toplevel`            | `<repo>`                                                             |
| `git rev-parse HEAD`                       | `3c24ced03f7152887c88418d3ebca2c41eba61b3`                           |
| `git branch --show-current`                | `feat/mvp03-living-graph`                                            |
| `git status --short -uall`                 | **0 行（CLEAN）**                                                    |
| `git diff --check`                         | 无输出（PASS）                                                       |
| `git remote -v`                            | **空（无 remote）**                                                  |
| `git ls-files \| wc -l`                    | **706**                                                              |
| `git ls-files --others --exclude-standard` | **0**                                                                |
| `git diff --name-only` / `--cached`        | **0 / 0**                                                            |
| `git rev-list --count HEAD`                | **124**                                                              |
| `git rev-list --all`                       | **124**（4 个分支无分叉外提交）                                      |
| `git count-objects -v`                     | count 1173 / size 2894 KB；in-pack 1324 / size-pack 1212 KB；packs 2 |

### 1.1 HEAD 附近历史（最近 20 条，节选）

```
3c24ced (HEAD -> feat/mvp03-living-graph) feat(harmony): enter N3 - freeze Android native core and build Harmony Stage Model project
6053f3c chore(generated): track Harmony/iOS CanonicalEnums codegen outputs and legacy freeze README
bc2eeb8 docs(android): align Gate G4/G21 with the final 51/51 and 41/41 counts
4e74d1f docs(android): do not hard-code HEAD sha in the closure report
65758ab docs(android): D-16 closure - final E2E v4 run id, Git closure record, stale-report banner
17449e8 fix(android): keep Import/Restore file workflows alive across the lock round-trip (D-16)
...
7bc0ed3 (tag-reachable) chore(build): ignore the agent workspace memory directory
```

首个 commit（root）：`3f1425d772e34609c1f042768595c04c8bc9386c`

### 1.2 Tags（全部保留，不删除、不移动）

| Tag                       | 类型                 | 指向对象   | 用途                                                      |
| ------------------------- | -------------------- | ---------- | --------------------------------------------------------- |
| `v0.2.0-mvp02`            | lightweight → commit | `9e28aa15` | MVP02 Global Source Abstraction 冻结                      |
| `v0.3.0-mvp03`            | lightweight → commit | `714d52ee` | MVP03 Living Graph & Change Safety 冻结                   |
| `v0.3.0-uniapp-reference` | **annotated tag**    | `8ff65fb3` | Legacy uni-app x **Behavior Oracle** 冻结（**必须保留**） |

### 1.3 Branches

| Branch                     | HEAD      | 说明                                     |
| -------------------------- | --------- | ---------------------------------------- |
| `feat/mvp03-living-graph`  | `3c24ced` | **当前分支，唯一完整历史**（124 commit） |
| `engineering/baseline-v1`  | `b89ad8f` | Engineering Baseline V1 历史遗留分支     |
| `feat/mvp02-global-source` | `2ad62ca` | MVP02 历史分支                           |
| `master`                   | `ac511f6` | 早期历史分支（已被后续分支取代）         |

**判定**：`feat/mvp03-living-graph` 是唯一包含全部工作（Legacy freeze + Android Native + Harmony N3 起点）的分支。
按 A15 建议，**把当前 HEAD 建为 `main` 并保留原分支**，不做 rename、不做 merge、不删除历史分支。

---

## 2. 工作树状态判定（A1）

### 2.1 总体

| 类别                      | 数量              | 判定    |
| ------------------------- | ----------------- | ------- |
| tracked                   | **706**           | —       |
| modified                  | 0                 | CLEAN   |
| staged                    | 0                 | CLEAN   |
| untracked                 | **0**             | CLEAN   |
| ignored（磁盘上，不入库） | **34,634** 条路径 | 见 §2.3 |

### 2.2 正式资产入库现状（逐目录复核）

| 目录                       | tracked                          | 判定                                                                                   |
| -------------------------- | -------------------------------- | -------------------------------------------------------------------------------------- |
| `spec/`                    | 14                               | ✅ Canonical Spec 全部入库                                                             |
| `fixtures/`                | 113                              | ✅ 全部入库（synthetic fixture，见隐私审计）                                           |
| `conformance/`             | 1（`CONFORMANCE_MANIFEST.json`） | ✅ 入库                                                                                |
| `tools/`                   | 5                                | ✅ codegen / conformance / freeze / harmony 工具入库                                   |
| `android/`                 | 72                               | ✅ 原生源码入库；`build/`、`.gradle/`、wrapper jar 之外均无遗漏                        |
| `harmony/`                 | 19                               | ✅ Stage Model 工程 + ArkTS Domain 入库                                                |
| `ios/`                     | 1                                | ⚠ **只有 codegen 产物**（`CanonicalEnums.swift`）；N4 `BLOCKED_BY_MACOS`，符合当前阶段 |
| `legacy/`                  | 1（`README.md`）                 | ✅ Legacy 定位声明入库                                                                 |
| `docs/`                    | 99                               | ✅ 入库                                                                                |
| `app/`（Legacy uni-app x） | 71                               | ✅ Behavior Oracle 入库                                                                |
| `core/`                    | 141                              | ✅ Legacy Core / Oracle 入库（`node_modules/` 已忽略）                                 |
| `platforms/`               | 42                               | ✅ Legacy 三端工程入库                                                                 |
| `store/`                   | 5                                | ✅ 商店材料入库                                                                        |
| 根目录状态文件             | 117                              | ✅ 入库                                                                                |

### 2.3 被忽略内容的主要类别（磁盘占用）

| 类别                       | 路径数 | 磁盘   | 为什么忽略                             |
| -------------------------- | ------ | ------ | -------------------------------------- |
| Harmony 依赖               | 22,821 | —      | `oh_modules/`（构建期依赖）            |
| Core Node 依赖             | 7,501  | 134 MB | `node_modules/`                        |
| Android 构建产物           | 3,007  | 428 MB | `build/`、`.gradle/`                   |
| `local_private/`           | 814    | 106 MB | 本地运行证据、APK/AAB、非生产 keystore |
| `platforms/` 构建残留      | 235    | —      | Legacy 构建输出                        |
| `.tmp_audit/`              | 192    | 242 MB | Agent 临时工具与探测输出               |
| `.workbuddy/`              | 9      | —      | Agent 工作区（已忽略）                 |
| `.pi/`                     | 1      | —      | Agent 会话草稿（已忽略）               |
| `fixtures/`、`tools/` 残留 | 8      | —      | 本地运行证据                           |

### 2.4 A1 唯一收口动作

| 项                                   | 现状                                                           | 处置                                                                                                        |
| ------------------------------------ | -------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------- |
| `.workbuddy-ai/memory/2026-09-12.md` | **已入库**（历史遗留，`.gitignore` 后来才加 `.workbuddy-ai/`） | **`git rm --cached`** 移出版本控制，文件保留在磁盘。符合 A1「agent session / 本地 agent memory 默认不入库」 |

> 说明：`.workbuddy/`、` .pi/`、`.tmp_audit/` 当前**均未入库**（`.gitignore` 已覆盖），无需动作。
> `local_private/README.md` 是唯一有意入库的 `local_private` 文件（说明该目录用途），保留。

---

## 3. Remote 现状

```
git remote -v   →   (empty)
```

**无任何 remote**，因此 A14 中「origin 已存在则不要覆盖」的风险不存在，可直接建立 `origin`。

---

## 4. 本工作区特有的 Git 故障（继承记录，务必遵守）

上一轮已记录并复现：**`git commit` 会成功创建对象但 HEAD 不推进**
（`.git/refs/heads/**` 的 loose ref 被外部进程回收）。

规避方式（本轮沿用，不使用任何被禁止命令）：

```
git add <paths> && git write-tree
git commit-tree <tree> -p <parent> -m "<msg>"      # 显式建链
# 写入 .git/packed-refs（完整 40 位 SHA），packed-refs 稳定
```

每次提交后必须复查 `git rev-parse HEAD` 与 `git status --short -uall`，**不只看 `git commit` 的返回码**。

**禁止命令（本轮全程遵守）**：`rm -rf .git`、`git init`、`git reset --hard`、`git clean -fd`、
`git restore .`、force push、删除测试制造 PASS。

---

## 5. 基线结论

| 检查         | 结果                                                 |
| ------------ | ---------------------------------------------------- |
| 历史完整性   | ✅ 124 commit / 4 branch / 3 tag 全部保留            |
| 工作树       | ✅ CLEAN                                             |
| 正式资产入库 | ✅ 全部入库（唯一例外见 §2.4）                       |
| Remote       | 无（可直接建立 `origin`）                            |
| 体积风险     | ✅ `.git` 4.22 MB，最大入库文件 573 KB，**无需 LFS** |

**GIT_WORKTREE_READY = PASS**（在 §2.4 的 `git rm --cached` 执行并提交后成立）
