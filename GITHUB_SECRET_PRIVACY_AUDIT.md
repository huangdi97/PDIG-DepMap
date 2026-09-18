# GITHUB_SECRET_PRIVACY_AUDIT.md

> GitHub 发布前 Secret / 隐私审计（A2）。
> 生成时间：2026-09-17（Asia/Shanghai）
> 审计范围：**当前工作树 + 完整 Git 历史**（不是只扫 HEAD）。

---

## 0. 一句话结论

> **无任何生产凭据、私钥、API token、真实个人金融数据进入仓库。**
> 但存在**两类低危、已确认非生产的字面量**曾进入 Git 历史（非生产测试签名字面量、含本机用户名的绝对路径）。
> 因此：**`SECRET_IN_HISTORY = YES`（低危类）**，按 A2 门禁要求**不擅自 push**，处置决策见 §6。

---

## 1. 审计方法

### 1.1 扫描面

| 扫描面 | 规模 | 说明 |
| --- | --- | --- |
| 工作树受控文件 | **706** | `git ls-files` |
| 工作树文本文件（实际内容扫描） | **680** | 排除 `node_modules/`、二进制 |
| Git 历史 commit | **124** | `git rev-list --all` |
| Git 历史唯一 blob | **1,181** | `git rev-list --objects --all` |
| 历史 blob 实际内容扫描 | **1,163** | 文本扩展名 + ≤ 2 MB；**100% 解析成功** |

> 历史扫描使用 `git cat-file --batch` 的**二进制**通道解码（避免因 UTF-8 解码导致长度错位与漏检），
> 解析计数已做自校验（选中 1,163 / 实际解析 1,163）。

### 1.2 规则集（14 条内容规则 + 文件名规则 + PII 启发式）

**凭据类**
`PRIVATE_KEY_BLOCK`（PEM/OpenSSH/PGP 私钥块）· `AWS_ACCESS_KEY`（`AKIA…`）·
`OPENAI_KEY`（`sk-…`）· `GITHUB_TOKEN`（`gh[pousr]_…`）· `SLACK_TOKEN`（`xox[baprs]-…`）·
`GOOGLE_API_KEY`（`AIza…`）· `JWT` · `BEARER_LITERAL` ·
`GENERIC_SECRET_ASSIGN`（api_key / access_token / client_secret / secret_key 赋值）·
`SIGNING_PASSWORD`（storePassword / keyPassword / keystorePassword 赋值）·
`PRIVATE_KEY_FILE_MARKER`（PuTTY / ssh-rsa 私钥）

**隐私类**
`ABS_WIN_USER_PATH`（`X:\Users\<name>\`）· `ABS_UNIX_HOME_PATH` · `EMAIL`
**PII 启发式**：13–19 位数字序列 + **Luhn 校验**（银行卡号候选）

**文件名规则（历史全量路径 942 条）**
`*.jks` / `*.keystore` / `*.p12` / `*.pfx` / `*.p8` / `*.der` / `*.pem` / `*.key` /
`.env*` / `local.properties` / `secrets/` / `credentials/` / `*.depmap` / `*.sqlite*` /
`*.db` / `id_rsa` / `id_ed25519` / `.npmrc` / `.git-credentials` / `signing/`

---

## 2. 扫描结果总览

| 规则 | 工作树命中 | 历史命中 | 判定 |
| --- | --- | --- | --- |
| `PRIVATE_KEY_BLOCK` | 0 | **0** | ✅ |
| `AWS_ACCESS_KEY` | 0 | **0** | ✅ |
| `OPENAI_KEY` | 0 | **0** | ✅ |
| `GITHUB_TOKEN` | 0 | **0** | ✅ |
| `SLACK_TOKEN` | 0 | **0** | ✅ |
| `GOOGLE_API_KEY` | 0 | **0** | ✅ |
| `JWT` / `BEARER_LITERAL` | 0 | **0** | ✅ |
| `GENERIC_SECRET_ASSIGN` | 0 | **0** | ✅ |
| `PRIVATE_KEY_FILE_MARKER` | 0 | **0** | ✅ |
| **`SIGNING_PASSWORD`** | **3** | **6**（2 个唯一位置） | ⚠ **F-01** |
| **`ABS_WIN_USER_PATH`** | **10**（8 个文件） | **19**（12 个唯一位置） | ⚠ **F-02** |
| `ABS_UNIX_HOME_PATH` | 0 | 0 | ✅ |
| `EMAIL` | 1 | 0 | ✅ 误报（`example.com`） |
| Luhn 有效 PAN 候选 | 1 | — | ✅ 误报（npm 版本串） |
| **风险文件名（历史全量）** | — | **0** | ✅ |

---

## 3. 发现项（Findings）

### F-01 — 非生产测试签名字面量 · 严重度 **LOW**

| 项 | 值 |
| --- | --- |
| 位置 | `android/app/build.gradle.kts` L33 / L34 / L35 |
| 内容性质 | `storePassword` / `keyAlias` / `keyPassword` 三个字面量（各 12 字符） |
| 首次进入历史 | `c70919c`（feat(android): Compose app…），最近一次 `17449e8` |
| 对应密钥库 | `local_private/build-chain/pdig-nonprod.jks` |
| 密钥库是否入库 | **否**（`git ls-files --error-unmatch` 返回 "did not match any file(s) known to git"；`local_private/` 已 gitignore） |
| 生效条件 | 仅当显式传入 `-PpdigNonProdSigning=true` 时才套用；默认 release **保持未签名** |
| 源码自述 | 文件内注释明确写明「⚠️ 非生产测试签名（NON_PRODUCTION_TEST_SIGNING）… **不是** production signing」 |

**判定**：这是**本地一次性非生产测试密钥库**的口令，**密钥库本体不在仓库内**，
且默认构建路径不使用它。攻击者仅凭口令无密钥库文件**不可利用**。
与 `ANDROID_PRODUCTION_RELEASE_READY = BLOCKED_BY_PRODUCTION_SIGNING`（B4：无生产 keystore）一致。

**处置建议（未执行）**：将三个值外置为 Gradle property / 环境变量。
**本轮不修改**：Android 已进入 `CORE_FROZEN / MAINTENANCE_ONLY`，改动 `build.gradle.kts`
会触碰已冻结的构建链，风险大于收益；且该值本身不具生产价值。

### F-02 — 文档中含本机用户名的绝对路径 · 严重度 **LOW（隐私卫生）**

| 文件 | 位置 |
| --- | --- |
| `ANDROID_CORE_JVM_TEST_REPORT.md` | L23 / L24 / L123 |
| `ANDROID_GIT_SCOPE_AUDIT.md` | L170 |
| `ANDROID_N1_N2_FINAL_CLOSURE_REPORT_V2.md` | L43 / L54 |
| `RELEASE_CANDIDATE_MANIFEST.md` | L97 |
| `docs/ANDROID_BUILD_REPORT.md` | L34 / L108 |
| `docs/HARMONY_BUILD_REPORT.md` | L109 / L126 |
| `docs/HARMONY_TOOLCHAIN_AUDIT.md` | L153 |

共 **12 个唯一位置**（部分文件在多个历史版本中重复出现，故历史命中 19 次）。

**判定**：泄露的是**开发机 OS 用户名与目录布局**，不是凭据、不是他人个人信息。
属于公开前应清理的隐私卫生问题。

**处置建议（未执行）**：公开前用 `<user>` 占位替换。
**本轮不修改**：这些文件是**已冻结的工程取证报告**，就地改写会污染证据链；
且首次发布为 PRIVATE，不构成对外泄露。

### F-03 — 已入库的 Agent 会话记录 · 严重度 **LOW**

`.workbuddy-ai/memory/2026-09-12.md` 曾入库（`.gitignore` 后续才加 `.workbuddy-ai/`）。
内容性质为 Agent 工作记录，**非产品资产**。
**处置：本轮执行 `git rm --cached`**（文件保留在磁盘），符合 A1「agent session / 本地 agent memory 默认不入库」。

---

## 4. 已排除的误报（逐项核实，不虚报也不隐瞒）

| 命中 | 实际内容 | 判定 |
| --- | --- | --- |
| `PASSWORD_ASSIGN` × 9（测试与 codegen） | `core/scripts/generate-conformance.ts`、`core/tests/crypto/container-mutation.test.ts`、Android `BackupExportRegressionTest.kt`、`platforms/ios/.../DepmapContainerV1Tests.swift`、`docs/IOS_RELEASE_HANDOFF.md` 中的口令 | **Canonical Golden Vector 固定口令**，三端互操作契约的一部分；synthetic，非凭据 |
| `EMAIL` × 1 | `CANONICAL_DESIGN.md` L407，域名 `example.com` | 文档占位符 |
| Luhn 有效序列 × 1 | `BLOCKERS.md` L51–52：npm 包版本串 `5.24.2026081015-1930` 等 | 版本号，非卡号 |

---

## 5. 专项核验

| 检查项 | 结果 |
| --- | --- |
| `*.jks` / `*.keystore` / `*.p12` / `*.p8` / `*.pfx` 入库 | **0**（磁盘上唯一 keystore 在 `local_private/build-chain/`，未入库） |
| `.env` / `local.properties` 入库 | **0** |
| `*.depmap` / `*.db` / `*.sqlite*` 入库 | **0** |
| 私钥 / 证书私钥入库 | **0** |
| 数据库备份 / runtime dump 入库 | **0**（`local_private/` 已忽略） |
| logcat / adb dump 入库 | **0**（`*.log` 已忽略） |
| 真实账单 / 真实金融流水入库 | **0**（真实数据只放 `local_private/`，`real_bills/` 已忽略） |
| 受控二进制资产 | 17 个，全部为 **应用图标 / 启动图 / `gradle-wrapper.jar` / Legacy AAR**，**无截图、无真实界面内容** |
| `fixtures/` 数据性质 | 113 个数据文件，其中 32 个显式带 synthetic/demo/样例标记；**无 Luhn 有效 PAN** |
| `fixtures/import/ofx-basic.qfx` 长数字串 | OFX `<ACCTID>`/`<FITID>` 风格 synthetic 账户标识，**非银行卡号** |
| 截图/私人数据 | 受控 `.png` 仅 11 个且全为图标/启动图；`*/screenshots/evidence/` 已忽略 |

---

## 6. 门禁结论

| Gate | 结果 |
| --- | --- |
| `GITHUB_SECRET_SCAN` | **PASS**（无生产凭据 / 私钥 / token / API key） |
| `GITHUB_PRIVACY_SCAN` | **PASS_WITH_LOW_FINDINGS**（F-02 本机路径、F-03 agent 记录） |
| **`SECRET_IN_HISTORY`** | **YES**（类别：**非生产测试签名字面量 F-01** + **本机用户名路径 F-02**；**无生产凭据**） |

### 6.1 处置决策（按 A2 要求，本轮**停止**，不擅自 push）

A2 规定：「如果任何 secret 曾经进入 Git 历史……不得把含 secret 的历史推到 GitHub」。
F-01 / F-02 已确认**非生产、不可单独利用**，但仍属"曾进入历史的字面量"，
因此**首次 push 需用户明确授权**。可选路径：

| 方案 | 动作 | 代价 |
| --- | --- | --- |
| **A（推荐）** | 接受已记录暴露，push 到 **PRIVATE** 仓库；公开前再做历史净化 | 零代价；公开前必须回来处理 |
| **B** | 用 `git filter-repo` / BFG 净化 F-01/F-02 | **重写全部 124 个 commit 与 3 个 tag**，需完整备份；与本工作区 loose-ref 故障叠加风险高 |
| **C** | 轮换非生产测试 keystore（重新生成 + 新口令），使历史字面量失效 | 需重新生成 `pdig-nonprod.jks`；口令仍需入库或外置，不能彻底消除 |

> 本轮**未执行** B / C，也未 push。见 `GITHUB_PUBLICATION_REPORT.md` §GITHUB_INITIAL_PUSH。

---

## 7. 净化后复扫（2026-09-18 追加，覆盖 §6 的历史类门禁）

依据用户决策「先净化 Git 历史，再首次 push」，已用 `git-filter-repo` 2.47.0 完成全历史字面量净化，
并对**可达历史**（126 commit / 1,197 blob）做字节级复扫。详见 `GITHUB_HISTORY_SANITIZATION_REPORT.md`。

| Gate | 净化前 | 净化后 |
| --- | --- | --- |
| `SECRET_IN_HISTORY` | **YES**（F-01 非生产测试签名字面量） | **NO** |
| `PRIVATE_KEY_IN_HISTORY` | NO | **NO** |
| `TOKEN_IN_HISTORY` | NO | **NO** |
| `RAW_FINANCIAL_DATA_IN_HISTORY` | NO | **NO** |
| `PERSONAL_ABSOLUTE_PATH_IN_HISTORY`（F-02 本机用户名路径） | **YES** | **NO** |

- F-01：三处非生产签名赋值的口令字面量已替换为 `<REDACTED_NONPROD_TEST_SECRET>`；
  同时 `android/app/build.gradle.kts` 已改为环境变量 / Gradle property 查找，缺失时显式 fail-closed。
- F-02：40 个受控文件 197 处机器绝对路径在工作树层已替换为占位符，历史层由 filter-repo 全量替换。
- F-03（受控 agent 记录）已在 A1 阶段移出索引。
- 规则字面量残留 **0/69**；通用敏感检测器（私钥 / 令牌 / Luhn 卡号 / 个人路径 / 机器路径）命中 **0**。
- 历史中保留的 `C:\Users\<user>` / `X:\Users\<name>` 共 9 处为文档占位符，非真实个人路径。

**结论**：A2 门禁的历史类阻塞项已解除，`GITHUB_HISTORY_SANITIZED = PASS`，可执行首次 push。
