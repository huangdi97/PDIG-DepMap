# GITHUB_PUBLICATION_REPORT.md

> GitHub 仓库发布报告（PART A 收口）。
> 生成时间：2026-09-18（Asia/Shanghai）
> 关联：`GITHUB_SECRET_PRIVACY_AUDIT.md`、`GITHUB_HISTORY_SANITIZATION_REPORT.md`、
> `GITHUB_REPOSITORY_SIZE_AUDIT.md`、`TAG_REWRITE_MAP.md`、`LICENSE_DECISION.md`

---

## 0. 结论

| 项 | 结果 |
| --- | --- |
| 仓库 | `huangdi97/PDIG-DepMap`（**PRIVATE**，非空） |
| 默认分支 | `main` |
| 首次 push | **已完成**，且**先完成历史净化**（`GITHUB_HISTORY_SANITIZED = PASS`） |
| force push | **未使用**（全程快进） |
| 发布 Release | **无**（本轮不发布 v1.0 或任何 Release） |
| `GITHUB_CI` | **PASS**（第三次 `35303432883`、第四次 `35306907095`、第五次 `35314181367` 均全绿；前两次 FAIL 的原因见 §6，均已修复且复验。**注意**：五次全部为人工 `workflow_dispatch` —— 本仓库 `push` 事件从未触发过 CI，见 §6.5） |

---

## 1. 首次推送内容

`SANITIZED_CANONICAL_HEAD`：首次 push 前的规范提交为 `443bd7e9941ccb334003f992f2c51465b5e5e7bf`。
（`GITHUB_HISTORY_SANITIZATION_REPORT.md` 自身在提交链内，无法包含自身 SHA，故由其委托本文件记录。）

首次 push（`--tags`，全部为 `[new ...]`，无 force）：

| ref | 首次 push SHA | 说明 |
| --- | --- | --- |
| `refs/heads/main` | `443bd7e9…` | 默认分支 = 净化后规范提交 |
| `refs/heads/feat/mvp03-living-graph` | `443bd7e9…` | 与 main 同提交 |
| `refs/heads/feat/mvp02-global-source` | `2ad62ca7…` | MVP02 分支（main 的祖先） |
| `refs/heads/engineering/baseline-v1` | `b89ad8f3…` | Engineering Baseline V1（main 的祖先） |
| `refs/heads/master` | `ac511f6d…` | 早期分支（所有分支的祖先） |
| `refs/tags/v0.2.0-mvp02` | `9e28aa15…` | MVP02 冻结标签 |
| `refs/tags/v0.3.0-mvp03` | `714d52ee…` | MVP03 冻结标签 |
| `refs/tags/v0.3.0-uniapp-reference` | `f5f61f93…`（peel → `7bc0ed32…`） | Legacy 行为 Oracle，**注记标签** |

**未推送**：任何临时 / 备份 ref（`refs/replace/*`、filter-repo 备份 ref 已在净化时 `delete-no-add`）、
两份离线 bundle（仅存于本机，见净化报告 §1）。

推送后两次修复提交（均为快进，非 force）：

| 提交区间 | 内容 |
| --- | --- |
| `443bd7e9..d9e5319` | §5 的两项仓库缺陷修复 + Android CI job 修正 |
| `d9e5319..0b38bd40` | §6.3 清单根因修复 + §6.4 CI 门控补强 + 本报告入库 |
| `0b38bd40..42b59a1` | 报告更新（记录第三次 CI 全绿） |
| `42b59a1..2c9f71c` | PART B：`HARMONY_ARGON2_FEASIBILITY.md` + 状态文档同步 |
| `2c9f71c..3e3b005` | PART B：Harmony `DEPMAP_CONTAINER_V1`（JCS / AAD / AES-256-GCM）+ 编译门取证 + `HARMONY_CONTAINER_V1_POC.md` |

当前推送 tip：`3e3b005d626b6154fe8bd40eef71528df8603d2c`（`main` 与 `feat/mvp03-living-graph` 同提交）。
（`GITHUB_HISTORY_SANITIZATION_REPORT.md` 不硬编码自身 SHA，`SANITIZED_CANONICAL_HEAD` 仍为首次 push 的
`443bd7e9…`；本节的 tip 为推送链末端，二者语义不同，不可混用。）

---

## 2. 推送前门禁（全部 PASS）

| Gate | 结果 |
| --- | --- |
| `SECRET_IN_HISTORY` | **NO** |
| `PRIVATE_KEY_IN_HISTORY` | **NO** |
| `TOKEN_IN_HISTORY` | **NO** |
| `RAW_FINANCIAL_DATA_IN_HISTORY` | **NO** |
| `PERSONAL_ABSOLUTE_PATH_IN_HISTORY` | **NO** |
| 规则字面量残留（69 条） | **0** |
| 通用敏感检测器（私钥 / 令牌 / Luhn / 个人路径 / 机器路径） | **0 / 0 / 0 / 0 / 0** |
| 可达 commit 中"净化前 SHA" | **0** |
| ref 与 `ref-map` 一致性 | **8 / 8 OK** |
| `git fsck --full` | 仅 2 个 dangling 对象（净化前残留，**不可达**，不进远端） |
| `git status --short -uall` | CLEAN |
| `git diff --check` | 无输出 |

净化复扫样本量（可达对象）：**1,259 个 blob**。

---

## 3. 一致性重新封印（净化后，未改变运行时语义）

| Gate | 结果 |
| --- | --- |
| `CODEGEN GATE`（spec → 三端 generated） | **PASS** |
| 一致性用例 | **91 / 91 ok** |
| import fixture 完整性 | **28 / 28 ok**（修正 12 条清单哈希后，见 §6.3） |
| `ORACLE SELFCHECK`（冻结 TS oracle 逐字节复现） | **PASS（91 cases）** |
| Android `:conformance:run` | **pass=91 / fail=0 / total=91** |
| Android `:core:test --rerun-tasks` | **71 PASSED / 0 FAILED**（6 个测试类） |
| Harmony / iOS | **NOT_RUN**（诚实口径，未虚报） |

---

## 4. 仓库形态

| 项 | 值 |
| --- | --- |
| 受控文件数 | **725**（HEAD `d9e5319`） |
| 受控文件总字节 | ≈ 4.87 MB |
| 最大单文件 | ≈ 587 KB（无 > 1 MB 文件） |
| Git LFS | **未使用**（无需） |
| License | **TBD**（决定过程见 `LICENSE_DECISION.md`） |
| `SECURITY.md` | 已生效（GitHub 已识别 security policy） |

GitHub 文档入口：`docs/ARCHITECTURE.md`、`docs/SECURITY_MODEL.md`、`docs/CONFORMANCE.md`、
`docs/DEPMAP_FORMAT.md`、`docs/NATIVE_MIGRATION.md`；README 已建文档索引。

---

## 5. 首次 CI 暴露并已修复的两个仓库缺陷

首次 CI（`35300849483`）在**远真环境**暴露了两个本地不可见的仓库缺陷（均已修复并二次推送）：

| 缺陷 | 表现 | 根因 | 处置 |
| --- | --- | --- | --- |
| **fixtures 缺失** | CI 侧 `cases: 85/91`、`ORACLE SELFCHECK: FAIL (6 drifted)` | `.gitignore` 的 `coverage/` 规则匹配**任意层级**同名目录，把一致性用例目录 `fixtures/coverage/`（6 个 coverage 用例）一并排除 → checkout 后文件缺失 | 增加例外 `!fixtures/coverage/`；6 个用例入库 |
| **fixture 字节被归一化** | 同一文件 Windows 与 Linux 算出不同 sha256（本地 16/28 vs CI 15/28） | `.gitattributes` 的 `* text=auto eol=lf` 把 `fixtures/import/csv-crlf.csv` 的 **CRLF（被测语义本身）** 归一化成 LF | 对 `fixtures/import/*`、`fixtures/coverage/*` 加 `-text`，按原始字节入库 |

修复后复验：`WORKTREE_FIXTURES=119 / TRACKED=119 / UNTRACKED=0`；
`csv-crlf.csv` 的 **工作树字节 == blob 字节**，本地与 CI 口径统一为 **16/28**。

同时修正 CI 的 Android job：`:core` 与 `:conformance` 都是**纯 Kotlin JVM 模块**（硬约束：不得引入
Android Context / Compose / SQLite），本不需要 Android SDK；原 `android-actions/setup-android@v3`
步骤在 runner 上以 `sdkmanager --licenses exit 1` 失败。改为 **JDK 21（与 `jvmToolchain(21)` 对齐）+
`--configure-on-demand`**，被门禁的两个模块照常在 runner 上实跑。

---

## 6. `GITHUB_CI`

| 运行 | 触发 | 结果 |
| --- | --- | --- |
| `35300849483` | `workflow_dispatch`（main @ `443bd7e9`） | **FAIL**（两个 job 均失败） |
| `35301936345` | `workflow_dispatch`（main @ `d9e53190`，§5 修复后） | **FAIL**（Android job **PASS**；Canonical job 仅剩 `fixtureIntegrity`） |
| `35303432883` | `workflow_dispatch`（main @ `0b38bd40`，§6.3 + §6.4 后） | **PASS**（两个 job 全绿） |
| `35306907095` | `workflow_dispatch`（main @ `3e3b005`，Harmony 容器实现入库后） | **PASS**（两个 job 全绿） |
| `35314181367` | `workflow_dispatch`（main @ `c2c217e`，Argon2 vendoring + NAPI 桥 + 编译可达性 Gate 入库后） | **PASS**（两个 job 全绿） |

### 6.5 `35314181367` 附带发现：`push` 触发从未生效（值得单独记账）

第四次运行之后，本轮把 4 个提交（`7cb33ca` → `c2c217e`）推上 `main`。
**推送后 GitHub 并没有产生 `push` 事件的运行** —— 经核查：

```
gh api repos/huangdi97/PDIG-DepMap/actions/workflows/361028409/runs
→ total_count = 4，四个全部是 workflow_dispatch
```

也就是说：`ci.yml` 声明了 `on: push: branches: ["**"]`，
但**本仓库历史上从未有任何一次 `push` 事件真正触发过 CI**，
全部四次绿灯都是人工 `workflow_dispatch` 的结果。

同批核查已排除「配置问题」：

| 核查项 | 结果 |
| --- | --- |
| 远端工作流文件与本地一致 | `git rev-parse HEAD:.github/workflows/ci.yml` = `69f7685`，远端 API 同值 |
| Actions 开关 | `enabled=true`，`allowed_actions=all` |
| 工作流状态 | `active`（workflow id `361028409`） |

**结论与处置**：触发未生效的原因尚未定因（非配置、非禁用）。
本轮按「不把不确定伪装成必须处理」的原则**不改 CI 触发语义**（改 `on: push` 属于
扩大范围，且当前 `push` 并未被用来保证任何结论），改为**显式 dispatch 并记录运行 ID**，
使「CI 是否真的跑过」始终有据可查。此项记为待观察，不阻塞 N3。

> 副作用提醒（对本项目的实际影响）：由于 `push` 不触发，
> **不能把「推上去了」当成「CI 会跑」**。任何依赖 CI 结论的 gate，
> 都必须由运行 ID 佐证，而不是由提交已推送佐证。


### 6.1 首次运行的两个失败（性质不同，均已分别处置）

1. **Canonical job — `fixtureIntegrity` FAIL**：`fixtures/import` 28 个文件中 **12 个**的 sha256 与
   `CONFORMANCE_MANIFEST.json` 记录不一致 → `run.mjs` 以 exit 1 结束。
   - 该缺陷**在净化前即存在**，已由三方哈希比对证明与净化无关（详见净化报告 §7）。
   - 叠加 §5 的两项缺陷后首次 CI 表现为 `cases: 85/91`；§5 修复后该部分回到 **91/91 + oracle PASS**。
   - 完整定因与修复见 §6.3。
2. **Android job — `Setup Android SDK` 失败**：`sdkmanager --licenses` exit 1（CI 环境问题）。
   - 已在 §5 中修正（去掉不必要的 SDK 安装步骤）。

### 6.2 第二次运行（修复后）：验证 §5 两项修复确实生效

| job | 结论 | 关键读数 |
| --- | --- | --- |
| Android core（JVM tests + conformance） | **SUCCESS**（2m22s） | JDK 21 + `--configure-on-demand`；`:core:test`、`:conformance:run` 在 runner 上实跑 |
| Canonical（codegen / fixtures / oracle） | **FAIL** | `cases: 91/91 ok`、`imports: 16/28 ok`、`ORACLE SELFCHECK: PASS`、`codegen PASS`、`fixtureIntegrity FAIL` |

结论：§5 的两项修复**均生效且可验证** —— coverage 用例回到 91/91 且冻结 oracle 逐字节复现 PASS
（证明 `.gitignore` 例外正确）；Android SDK 步骤移除后该 job 转绿（证明 `:core`/`:conformance` 确为纯 JVM 模块）。
Canonical job 的唯一红灯即 §6.3 的清单缺陷。

### 6.3 `fixtures/import` 12/28 的定因与修复（本轮完成）

此前仅作出方向性判断（"问题在清单侧、不在字节侧"）。本轮用 **blob 级取证**补齐机制层面根因：

**取证**（`.workbuddy/blob_cr_probe.mjs`、`.workbuddy/fixture_diag.mjs`）：

1. 12 个文件在 HEAD 的 blob 与其**引入提交 `97a0348`** 的 blob **完全相同**，且**均不含 CR 字节**。
2. 对照组 `csv-cr-only.csv` 经历同一次历史净化后**仍保留 CR** —— 证明净化不会剥离 CR，
   排除"历史净化改写字节"这一假设。
3. 对 12 个文件逐一验证：`sha256(CRLF化(当前字节))` **精确等于**清单原值（12/12 全中），
   排除"内容本身不同"这一假设。

**根因**：**仓库字节从未漂移**；清单记录的是生成机在 `core.autocrlf=true` 下**检出态的 CRLF 假象**。
一致性运行器 `run.mjs` 直接哈希工作树字节（`readFileSync`，不做归一化），
因此该清单在 Windows 上"看似正确"、在 Linux CI 上**必然**失配 —— 这是首次 CI 才暴露它的原因。

**处置：修正清单，而非篡改字节**（改动 = 12 行哈希替换 + 1 行 `$note`）：

- 新值取自 **git blob 本体**（canonical object），并逐文件断言 `blob 字节 == 工作树字节`；
- 若任一文件的失配**不能**由"LF↔CRLF"解释，脚本立即中止，不落盘；
- `oracle.commit = 7bc0ed32…` 原样保留（行为 Oracle 冻结语义未动）；
- 不重排键、不重格式化、不动 `fixtures[]`、不降低门禁、不加 `continue-on-error`。

> 注：清单再生的"合规路径"本是生成器 `core/scripts/generate-conformance.ts`，但实测它会同时重写
> `oracle.commit`，破坏 Oracle 冻结语义，故本轮采用**外科手术式修正**而非整体再生。

**复验**（`node tools/conformance/run.mjs`）：
`cases 91/91`、`imports 28/28`、`ORACLE SELFCHECK PASS`、`platform android PASS`、**`VERDICT: PASS`**。

### 6.4 CI 门控补强：把恒为 `NOT_RUN` 的 Android 平台校验变成真门禁

`conformance/reports/` 被 `.gitignore` 排除，而 Android 与 Canonical 是两个独立 job ——
因此 CI 的 Canonical job **永远看不到** Android 报告，只能对 android 平台报 `NOT_RUN`。
这虽然诚实，却意味着 CI **从未真正校验**「Android 实现逐用例复现 canonical」这一核心不变量。

处置：`canonical` 通过 `needs: android-core` 串行依赖 Android job，并在 `android-core` 成功时
下载其 `android.json` 后再执行 `run.mjs`：

- 上传/下载改为**目录级**（`conformance/reports/`），保证落在 `conformance/reports/android.json`；
- `if: always()` 保证 Android 失败时 Canonical 仍独立如实上报，不被 "skipped" 掩盖；
- Android 失败时不下载报告 → 该平台仍如实报 `NOT_RUN`（`NOT_RUN ≠ PASS`）。

### 6.5 第三次运行（根因修复 + 门控补强后）：首次全绿

| job | 结论 | 耗时 |
| --- | --- | --- |
| Android core（JVM tests + conformance） | **SUCCESS** | 2m33s |
| Canonical（codegen / fixtures / oracle） | **SUCCESS** | 14s |

Canonical job 在远真 runner 上的实际读数：

```
cases:   91/91 ok
imports: 28/28 ok                      ← 修复前为 16/28
ORACLE SELFCHECK: PASS (91 cases reproduce exactly)
android  PASS  91 pass / 0 fail / 91 total   ← §6.4 之前在 CI 中恒为 NOT_RUN
harmony  NOT_RUN  (conformance/reports/harmony.json absent)
ios      NOT_RUN  (conformance/reports/ios.json absent)

codegen          : PASS
fixtureIntegrity : PASS
oracleSelfcheck  : PASS
platform android : PASS
VERDICT: PASS
```

两点确认：

1. §6.3 的清单修正在 **Linux runner** 上同样成立（`imports: 28/28`），证明新哈希取自 canonical blob 字节、
   而不是又一次"本机恰好正确"。
2. §6.4 的门控补强生效：CI 侧 `platform android` 由恒 `NOT_RUN` 变为 **PASS 91/91**，
   即 CI 现在真正校验「Android 实现逐用例复现 canonical」这一核心不变量。

### 6.6 第四次运行（Harmony 容器实现入库后）：仍然全绿

`3e3b005` 引入了 Harmony 的 `crypto/` 模块与两个新工具，但**未触碰** canonical 链路
（spec / codegen / fixtures / oracle / Android）。因此这次运行的价值是"回归确认"，
而不是新能力证明 —— Harmony 侧的能力证据在 `HARMONY_CONTAINER_V1_POC.md`，
且明确不含"CI 已验证 Harmony"。

| job | 结论 | 耗时 |
| --- | --- | --- |
| Android core（JVM tests + conformance） | **SUCCESS** | 2m19s |
| Canonical（codegen / fixtures / oracle） | **SUCCESS** | 19s |

```
CODEGEN GATE: PASS
cases:   91/91 ok
imports: 28/28 ok
ORACLE SELFCHECK: PASS (91 cases reproduce exactly)
platform android : PASS
platform harmony : NOT_RUN (no conformance report produced by this platform yet)
VERDICT: PASS
```

### 6.7 诚实口径

- Harmony / iOS 在 CI 中为 `NOT_RUN`，未声明为 PASS。
- Android 模拟器 E2E 未建立（hosted runner 无硬件加速），未声明为 PASS。
- `:app:assembleDebug` 未纳入 CI（未在 hosted runner 上验证）。

---

## 7. 已知限制与后续任务

| 项 | 状态 | 后续 |
| --- | --- | --- |
| `fixtures/import` 12/28 sha256 与清单不一致 | **FIXED**（本轮，见 §6.3） | 根因为 `core.autocrlf=true` 检出态假象；已按 canonical blob 字节修正清单。后续若用生成器再生清单，须先解决"生成器会重写 `oracle.commit`"的问题 |
| 历史提交内文档正文引用的 commit SHA 仍为净化前值 | **ACCEPTED** | 属文档可追溯性瑕疵，非密钥 / 隐私门禁项；彻底消除需二次 `filter-repo`，风险大于收益（净化报告 §9.3） |
| License | **TBD** | 见 `LICENSE_DECISION.md`；公开前必须定稿 |
| 公开（public）发布 | **未执行** | 需先定 License、复核 §7 全部 OPEN 项 |
| Harmony N3 / iOS N4 | N3 ACTIVE / N4 NOT_STARTED | 见 `docs/NATIVE_MIGRATION.md` |
