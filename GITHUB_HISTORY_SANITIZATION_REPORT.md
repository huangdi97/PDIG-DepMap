# GITHUB_HISTORY_SANITIZATION_REPORT.md

> Git 历史净化报告（`git-filter-repo`）——首次 push 前的第 2 阶段门禁。
> 生成时间：2026-09-18（Asia/Shanghai）
> 关联：`GITHUB_SECRET_PRIVACY_AUDIT.md`（A2）、`TAG_REWRITE_MAP.md`、`GITHUB_PUBLICATION_REPORT.md`

---

## 0. 结论

| Gate | 结果 |
| --- | --- |
| `GITHUB_HISTORY_SANITIZED` | **PASS** |
| `SECRET_IN_HISTORY` | **NO** |
| `PRIVATE_KEY_IN_HISTORY` | **NO** |
| `TOKEN_IN_HISTORY` | **NO** |
| `RAW_FINANCIAL_DATA_IN_HISTORY` | **NO** |
| `PERSONAL_ABSOLUTE_PATH_IN_HISTORY` | **NO** |

净化后**全部 127 个可达 commit、8 个 ref** 均已重写；规则字面量残留 **0/69**，通用敏感检测器命中 **0**。
`SANITIZED_CANONICAL_HEAD`：净化后规范提交 = 首次 push 时 `main` 与 `feat/mvp03-living-graph` 的 tip，
其 SHA 记录于 `GITHUB_PUBLICATION_REPORT.md`。

> 说明：净化提交链为 `ef035c6`（路径清理 + 签名外置）→ `fee803d`（commit SHA 重校正 + 标签映射）
> → **单个文档封印提交**（本报告 + 密钥隐私审计刷新 + `docs/NATIVE_MIGRATION.md` + README 文档索引）。
> 本报告自身也在提交链内，**不在此硬编码最终 SHA**（文件无法包含自身 SHA，硬编码必然滞后一拍）；
> 以 `GITHUB_PUBLICATION_REPORT.md` 记录的 push tip 为准。

> 一个**既有缺陷**与本轮净化无关，已单列（§7）：`fixtures/import/` 28 个文件中 12 个的 sha256 与
> `CONFORMANCE_MANIFEST.json` 记录不一致，导致 `fixtureIntegrity` 子项 FAIL。证据表明净化未改动任何
> fixture 字节（见 §7），不属净化回归，不在本轮擅自修数。

---

## 1. 备份（净化前，离线，不上传）

| 位置（本机，路径不入公开记录） | 大小 | SHA256 | `git bundle verify` |
| --- | --- | --- | --- |
| `<offline-backup-A>\PDIG-DepMap-pre-sanitize.bundle` | 2,138,727 B | `FDBFF86536E2181A01C190CC87F974DA6B25A6DF3AD3A4F0D9315149DB521EF0` | OK |
| `<offline-backup-B>\PDIG-DepMap-presanitize.bundle` | 2,138,743 B | `EF3BCB02D0A18EA424480DBA687597B8A1D6DD61C9123878917C27CD10E54573` | OK |

两份 bundle 均为 `--all` 全量、**9 个 ref、完整历史**（`The bundle records a complete history`），
存放于工作区外与版本库外，**未上传 GitHub**、**未放入项目目录**、**未进入发布产物**。

> 两份备份分别位于**另一磁盘的专用备份目录**与**仓库父目录下的 `pdig-backup/`**，
> 此处以占位符记录是为了不再向历史引入机器绝对路径；校验一律以 **SHA256** 为唯一依据。

---

## 2. 工具与执行方式

- 工具：**`git-filter-repo` 2.47.0**（经 pip 装入托管 venv），**未使用 `filter-branch`**，未使用自制改写脚本。
- 实际调用（在独立镜像裸仓库内执行，非直接改写工作区）：

```
python git_filter_repo.py \
  --force \
  --no-gc \
  --replace-refs delete-no-add \
  --replace-text <仓库外规则文件>
```

- 规则文件置于**仓库之外**（`<sanitize-workdir>/replace-rules.txt`），不进入任何提交。

### 2.1 规则集（69 条）

| 类别 | 条数 | 替换目标 |
| --- | --- | --- |
| 非生产测试签名口令 | **1** | `"<REDACTED_NONPROD_TEST_SECRET>"`（带引号的上下文精确匹配，避免误伤同名 keystore 文件名 `pdig-nonprod.jks`） |
| 机器绝对路径 | **68** | `%USERPROFILE%`、`<repo>`、`<repo_parent>`、`<DEVECO_HOME>`、`<ANDROID_SDK_ROOT>`、`<ANDROID_STUDIO_HOME>`、`<GRADLE_HOME>`、`<DEPMAP_TOOLS_HOME>`、`<HUAWEI_HOME>`、`<TOOLS_ROOT>`、`<ASCII_BUILD_ROOT>`、`%WINDIR%`、`%PROGRAMFILES%` |

- 每条路径规则覆盖 **4 种分隔符变体**（`/`、`\`、`\\`、`\\\\`），并按长度降序排列，避免短规则抢占。
- 口令提取采用**赋值上下文 + 词边界**约束：命中 `storePassword` / `keyAlias` / `keyPassword` 的
  `key = "value"` 形式，且排除文件名后缀。历史中实际存在**唯一口令字面量**（12 字符），三处赋值共用；
  另一条 22 字符候选经上下文复核为 UI `placeholder` 文本误报（命中 `restorePassword` 子串），已排除。

---

## 3. 本环境的 4 个异常与处置（均已实测，非推测）

| # | 现象 | 证据 | 处置 |
| --- | --- | --- | --- |
| 1 | `git-filter-repo` 未重定向 stdin 时被 SIGTERM 中断，仓库停在**半写状态**（refs 已更新、对象缺失） | 首次运行 SIGTERM + 空输出；随后 `show-ref` 报 `bad ref`，`count-objects` 显示 packs=0 | 一律 `< /dev/null` 并后台执行；中断后**重建镜像**而非在半写状态续跑 |
| 2 | `git gc` 在本环境**清空对象库** | 在 E: 盘镜像执行 `git gc --prune=now` 后 `count: 0 / in-pack: 0 / packs: 0`，并报 `unable to update info/refs` | 使用 `--no-gc` 跳过 `reflog expire` + `gc`；代价是旧对象仍留在本地（不可达，不推送），反而增强可恢复性 |
| 3 | 含 `/` 的分支名**嵌套松散引用创建失败** | filter-repo 的 `ref-map` 给出正确映射，但 `refs/heads/feat/*`、`refs/heads/engineering/*` 未被创建/被删除，回落至 packed-refs 旧值 | 依据 `ref-map` 用 `fix_refs.mjs` 逐条补齐松散引用文件；8/8 ref 校验通过 |
| 4 | filter-repo 收尾的 `git reset --hard` **解析到未更新的旧 ref**，导致索引/工作区停在旧树 | 收尾日志 `HEAD is now at 5a27a7c`（旧 SHA） | 不用 `reset --hard`：改用 `git read-tree HEAD` + `git checkout-index -f -a` 对齐索引与工作区；`git status` 转 CLEAN |

附带影响（已修复）：filter-repo 会移除 `origin` → 已重新添加；
`core.autocrlf=true` 下 `checkout-index` 给 `fixtures/import/csv-crlf.csv` 引入 CRLF → 已按 blob 字节还原。

---

## 4. 重写结果

| ref | 净化前 | 净化后 |
| --- | --- | --- |
| `refs/heads/engineering/baseline-v1` | `def93896500b97f5c718a46946cef6d9e37a564a` | `b89ad8f3caa3faf8b9a630c1ef6fe10300f9bd18` |
| `refs/heads/feat/mvp02-global-source` | `482e545db17c1f2fa9101dd1fb62104d3f51855f` | `2ad62ca7b46247c4e2b91bf506340ccda141c9a0` |
| `refs/heads/feat/mvp03-living-graph` | `5a27a7c75881201de9b2da18607a52e8431802a6` | `ef035c6826bd5c1e32a81fc5d09643caf8e285f9` |
| `refs/heads/main` | `c8d5111c41f528fbbd46fd25956654365e8203b4` | `df9e14ba7a2de176fc932817b116bfe2bde53d8f` |
| `refs/heads/master` | `75e2725e57fac052bdad984fb266848f3769048f` | `ac511f6d1899747119666d3bb12a5b6a21b1b75b` |
| `refs/tags/v0.2.0-mvp02` | `1d0d1f6fb01137e7322e6d2e99d1b768a46d7e10` | `9e28aa1504ec693c3f8208e67cfd2f07b6cf76ea` |
| `refs/tags/v0.3.0-mvp03` | `21945e6620a6dcea2d6dac48163a42118d3af7b1` | `714d52ee47d50bf7c0687e2c7fd16f7c99ef8847` |
| `refs/tags/v0.3.0-uniapp-reference` | `8ff65fb31f96741a290d7426157ca64f756b99d7` | `f5f61f93d1a6633592f48b48d3d1fd432c46def3` |

- 可达 commit 数：**126**（与净化前一致；无 commit 被剪除，commit-map 中 pruned 条目 = 0）。
- 逐 ref 语义校验：**8/8 PRESERVED**（分支 author/committer/message 一致；标注标签 tagger/date/message 一致，
  仅 object 指向改变）。详见 `TAG_REWRITE_MAP.md`。
- Legacy 行为 Oracle 标签 `v0.3.0-uniapp-reference` 已重指向净化后等价提交；
  `CONFORMANCE_MANIFEST.json` 的 `oracle.commit` 已同步为 `7bc0ed323ea82ce98139acd14eabd040a1ea111e`，
  与该标签目标**一致**（已校验）。

---

## 5. 净化后全历史复扫

- 扫描面：**仅可达对象**（`rev-list --objects --all`）。
  因使用 `--no-gc`，净化前旧对象仍留在库中但已不可达，不计入"历史"判定。
- 样本量与命中数的**当时实测值**见 §9.1；**最终 push 时的终值**记录在
  `GITHUB_PUBLICATION_REPORT.md`（本报告自身在提交链内，无法给出自指的最终计数）。
- 规则残留检查为**字节级**比对（`Buffer.indexOf`），避免含中文 UTF-8 规则的假阴性。

| 检查项 | 命中 |
| --- | --- |
| 规则字面量残留（69 条） | **0** |
| `PRIVATE_KEY`（PEM/OpenSSH/PGP 私钥块） | 0 |
| `TOKEN`（ghp_/github_pat_/AKIA/xox*/AIza/sk-/Bearer） | 0 |
| `RAW_FINANCIAL`（Luhn 有效 13–19 位数字 / IBAN） | 0 |
| `PERSONAL_ABS_PATH`（`X:\Users\<name>`、`/Users/<name>`、`/home/<name>`） | 0 |
| `MACHINE_ABS_PATH`（机器绝对路径：Windows 盘符路径 / Unix 家目录之外的本机目录） | 0 |

> 历史中残留的 `C:\Users\<user>` / `X:\Users\<name>` 共 9 处，经上下文核对**全部为文档占位符**
> （`<user>` / `<name>` / `<you>`），非真实个人路径，予以保留（它们本身是规避指引的一部分）。

---

## 6. 一致性重新封印（净化后回归）

在 `fee803d` 上重跑，确认历史重写**未改变运行时语义**：

| Gate | 结果 |
| --- | --- |
| `CODEGEN GATE`（spec → 三端 generated） | **PASS** |
| `FIXTURE INTEGRITY`（91 用例 sha256 vs manifest） | **91/91 ok** |
| `ORACLE SELFCHECK`（冻结 TS oracle 逐字节复现） | **PASS（91 cases）** |
| Android `:conformance:run` | **pass=91 / fail=0 / total=91** |
| Android `:core:test --rerun-tasks` | **71 PASSED / 0 FAILED**（6 个测试类） |
| Harmony / iOS | **NOT_RUN**（诚实口径，未虚报） |

---

## 7. 既有缺陷（非净化回归，单列跟踪）

| 项 | 状态 |
| --- | --- |
| `fixtures/import/` 28 个文件中 **12 个**的 sha256 与 `CONFORMANCE_MANIFEST.json` 记录不一致 | 导致 `fixtureIntegrity` 子项 FAIL（91 个用例本身全 ok） |

**判定依据（三方哈希比对）**：对抽样的 `normal-wechat.csv`、`utf8-bom.csv`、`gbk.csv`、`refund.csv`、
`dup-jan-aug.csv`、`ofx-basic.qfx`，其 **工作区文件 = 当前 HEAD blob = 净化前历史 blob**，三者哈希完全相同；
且这些文件与清单同在提交 `97a0348`（2026-09-16）落地，无"文件更新而清单未更新"的时间差。
→ **净化未改动 fixture 字节**，该不一致在净化前即已存在。

**处置**：本轮**不擅自改清单数值**（改数会掩盖真实差异，且违反"不得把不确定伪装成已处理"）。
建议以生成器 `core/scripts/generate-conformance.ts` 重新生成清单、逐项核对后再封印，作为独立任务跟踪。

---

## 8. 关于本地残留旧对象

- 因使用 `--no-gc`，净化前的旧 commit/blob 仍以**不可达对象**形式留在本地 `.git` 中。
- 这是**有意为之**：本地可恢复性更强；而 `git push` 只传输**可达对象**，旧对象不会进入远端。
- 若日后需要彻底清理，须先确认本环境 `git gc` 可用性（见 §3 异常 2），否则不得执行。

---

## 9. 补记：push 前复核发现的分支漏改（2026-09-18 第二轮）

### 9.1 缺陷：`refs/heads/feat/mvp02-global-source` 未重指向（已修复）

**发现**：步骤 11 完整性校验时复核 `.git/filter-repo/ref-map`，发现该分支仍指向**净化前**
`482e545db17c1f2fa9101dd1fb62104d3f51855f`，而非 ref-map 记录的净化后
`2ad62ca7b46247c4e2b91bf506340ccda141c9a0`。

**后果（严重）**：该分支使 **25 个净化前 commit 重新变为可达**（可达 commit 152 而非 127）。
若在此状态下 push，未净化历史会被推送到远端——§5 首轮"1,197 blob"的复扫即在此缺陷下完成，
覆盖面不完整，已按修正后的可达集重跑。

**根因**：本仓库存在已知故障——`.git/refs/heads/**` 下的 **loose ref 会被外部进程回收**
（该现象在仓库自身历史记录 `.workbuddy-ai/memory/2026-09-12.md` 中已有记载，规避方式为把 ref
固化进 `.git/packed-refs`）。`git update-ref` 两次调用均**返回 exit 0 但 ref 未变**（静默失败），
而该分支的 ref 只存在于 `packed-refs`，故未被更新。

**修正**：直接改写 `.git/packed-refs` 中该行为 `2ad62ca7…`（严格按 ref-map 的 `old → new` 值，
写入前断言旧值等于 `482e545d…` 以防误改）。

**复验（修正后全量重跑；以下为当时的实测快照，最终 push 终值见 `GITHUB_PUBLICATION_REPORT.md`）**：

| 项 | 结果 |
| --- | --- |
| ref 与 ref-map 一致性 | **8/8 OK**（`feat/mvp03-living-graph` 为 `ef035c68` 的后代 `fee803d9`，判 OK） |
| 可达 commit 数 | **127**（= 126 个重写 commit + 1 个 SHA 重校正封印 commit） |
| 可达 commit 中"净化前 SHA" | **0** |
| 可达 blob 数 | **1,246** |
| 规则字面量残留（69 条） | **0** |
| 通用敏感检测器（5 类） | **0** |
| `RESCAN_GATE` | **PASS** |

> 方法论修正：**本环境不可信 `git update-ref` 的返回值**，任何 ref 变更后必须以 `git show-ref` 复核。

### 9.2 push 范围与分支拓扑（祖先矩阵实测）

| 分支 | commit 数 | 与其他分支关系 |
| --- | --- | --- |
| `master` | 18 | 所有分支的祖先 |
| `feat/mvp02-global-source` | 25 | `main` / `mvp03` 的祖先 |
| `engineering/baseline-v1` | 39 | `main` / `mvp03` 的祖先 |
| `main` | 125 | `feat/mvp03-living-graph` 的祖先（`merge-base = main`，纯快进关系） |
| `feat/mvp03-living-graph` | 127 | 当前 HEAD = 净化后规范提交 |

→ 四条历史分支**全部是 `feat/mvp03-living-graph` 的祖先**，push 任一子集都不会丢失历史。
本轮 push 集合：`main`、`feat/mvp03-living-graph`、`feat/mvp02-global-source`、
`engineering/baseline-v1`、`master` + 3 个正式 tag；**不含**任何临时/备份 ref（`refs/replace/*`、
filter-repo 备份 ref 均未保留）。

### 9.3 已知作用域限制（诚实口径，不掩盖）

**commit SHA 重校正只作用于当前树，不作用于历史提交内的文档文本。**

- `remap_sha.mjs` 按工作树受控文件重指向，覆盖 **48 文件 / 188 处**（长 SHA 14、短 SHA 174）。
- 历史提交中文档正文引用的 commit SHA 仍为**净化前值**（例：`.workbuddy-ai/memory/2026-09-12.md`
  中 `d992de4` / `c2afd54` / `7bd9991` / `df10945` 四处短 SHA）。
- 判定：属**文档可追溯性瑕疵**，不属密钥 / 隐私 / 个人路径门禁项（已由 §5 复扫证明无敏感残留）。
- 处置：本轮**不再二次重写历史**——二次重写会使 §1 两份 bundle 与 §4 全部映射失效，风险大于收益。
  如需彻底消除，应以 `commit-map` 生成 `--replace-text` 规则再跑一次 `git filter-repo`，作为独立任务跟踪。
