# WEB_VNEXT_FINAL_REPORT

**分支**：`feat/web-vnext`（PR #2） · **起始 HEAD**：`bc24f6689a44bd7c125eadeb980064b54ceaa237`
**日期**：2026-10-30 · **执行环境**：Windows + 本地 Node v22.23.4（未入 PATH）

> 本报告只写**实际跑过**的东西。没有跑的一律写 `NOT_RUN`，包括视觉回归基线与人眼验收。

## 1. 本轮改变了什么

### 1.1 Shared Truth 收敛（android 分支 → web 分支）

`feat/android-ui-vnext-translation` 相对 `main` 在 `core/ spec/ fixtures/ conformance/ tools/` 上有 140 文件增量（Canonical v4：identity-profile、maintenance-profile、region-fact、impact-scope、plan-analysis/rebase、action-verification 等）。本轮把这些**平台中立 Truth** 并入 web 分支（不搬 Android UI / Compose / 测试工地 / APK）：

- `core/`、`fixtures/`、`conformance/`、`spec/{domain,schema,proposals,errors,migration,security,roadmap}`、`tools/codegen/`
- codegen 重新生成 8 个平台 Generated 文件（`node tools/codegen/generate.mjs`），随后 `--check` 通过

### 1.2 修掉的三个真实 Core 缺陷（不是 UI 便利）

| 缺陷                                                                                                                         | 影响                                                                   | 修法                                                                       |
| ---------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------- | -------------------------------------------------------------------------- |
| `Relation` 联合类型缺少 v4 已冻结的 `authenticates` / `controls`                                                             | v4 关系在 TS 层被拒绝，手机号变更（认证/恢复）无法建模                 | 按 `spec/domain/domain.json` + Schema V4 CHECK 补齐                        |
| `replace_phone_number` 模板构建 4 个动作但**未持久化**（`create()` 时 actions=[]，返回内存合并对象）                         | `Action DAG` / `Make-Before-Break` / `claimUnassignedImpacts` 全部失效 | 改为 `plans.updateActions()` 落库后再返回                                  |
| 三处测试与 v4 语义不一致（PRB-012 用 v3 schema 跑 v4 关系；payload 逐字节等价忽略“本地库 schema 元数据”；INV-16 未走验证门） | Core 门禁红                                                            | 显式 helper + 更强断言（新增 `core/tests/helpers/payload-equivalence.ts`） |

### 1.3 Web 从单文件 Preview 升级为 Product Candidate

- `web/` 成为源码真源（Vite + TypeScript strict + React），`website/app/` 降为构建产物（`npm run build:web`，含 `sw-manifest.json` + `BUILD_SHA256SUMS.txt`）。
- Domain Worker = 唯一 Truth 权威：`BrowserSqliteDriver`（`@sqlite.org/sqlite-wasm`，`:memory:`，同步 API）+ Core Repository/Service/Kernel。UI 只发 typed RPC。
- 加密：`web/src/crypto/depmap-web.ts`（hash-wasm Argon2id v19 + WebCrypto AES-256-GCM + RFC8785 JCS AAD），**冻结 Golden Vector 逐字节匹配**。
- 持久化：IndexedDB 只存 DEPMAP_CONTAINER_V1 容器密文；persist-before-ack；锁定即 terminate worker。
- 多标签页单写者 lease（IDB 事务 + BroadcastChannel 心跳 + pagehide 立即过期 + 争夺重试）。
- PWA：manifest + service worker（只缓存 shell）+ 离线可用。
- 完整页面：Onboarding / Lock / Now / Infrastructure / Node 详情 / Dependency + Group / Findings / Change Center / Plan（Current·Transition·After）/ Records（三时钟）/ Import / 我 / 全局搜索 / 隐私遮蔽。
- 静态门禁：`check-boundary`（UI 0 Core import、0 SQL）、`check-remote-assets`（0 远程运行时依赖）、`check-web-generated`（产物与源码同步）。

## 2. 证据（本地实跑）

| 门禁         | 命令                                                                    | 结果                                                             |
| ------------ | ----------------------------------------------------------------------- | ---------------------------------------------------------------- |
| codegen      | `node tools/codegen/generate.mjs --check`                               | PASS                                                             |
| conformance  | `node tools/conformance/run.mjs`                                        | PASS（128 cases；platform reports 如实 NOT_RUN）                 |
| Core         | `cd core && npm run check`                                              | PASS（492 tests / 0 circular / 0 network / 0 secrets / UI gate） |
| Web 类型     | `npx tsc --noEmit -p web/tsconfig.json`                                 | PASS                                                             |
| Web 单测     | `npx vitest run`（web/）                                                | 20 PASS（含 DEPMAP golden vector / shim / lease）                |
| Web 构建     | `npm run build:web`                                                     | PASS（9 files / 1847 KiB）                                       |
| E2E Chromium | `npx playwright test --project=chromium`                                | 16 PASS                                                          |
| 布局审计     | `npx playwright test --project=chromium tests/e2e/layout-audit.spec.ts` | 6 PASS                                                           |
| 视觉矩阵     | `npx playwright test --project=visual-chromium`                         | 2 PASS（21 张截图）                                              |
| 边界         | `node scripts/check-boundary.mjs`                                       | PASS                                                             |
| 远程依赖     | `node scripts/check-remote-assets.mjs`                                  | PASS                                                             |

## 3. 浏览器矩阵

| 浏览器                                         | 功能 E2E                     | 说明                                                                                                                                                                                                             |
| ---------------------------------------------- | ---------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Chromium (build 1243 / Chrome for Testing 153) | **PASS ×25**                 | 16 功能 + 6 布局审计 + 3 导入/发现；另有视觉矩阵 2/2                                                                                                                                                             |
| Firefox (1543)                                 | **PASS ×25**                 | 同上全部通过                                                                                                                                                                                                     |
| WebKit (2359)                                  | **PASS ×23 / 2 UNSUPPORTED** | 离线两个用例在 WebKit 下不成立：断网 reload 触发 WebKit internal error；SW 接管后二次加载本地引擎不就绪（已加 20 s 启动超时 + 「重新加载」兜底）。其余功能（含导入/发现/备份恢复、多标签页、隐私、篡改）全部通过 |

为匹配本机已缓存的 Chromium（1243），`@playwright/test` 锁定为 `1.63.0`（原 1.64.1 需要 1248，无法下载）。

## 4. 架构 / 权威 / 存储 / 加密

见 `WEB_ARCHITECTURE.md`、`WEB_SECURITY_MODEL.md`、`WEB_RUNTIME_ACCEPTANCE.md`。

一句话版本：

```
UI(React, 只读 projection) --typed RPC--> Domain Worker
     └ VaultRuntime(加密快照/生命周期) + CoreAuthority(Core) + BrowserSqliteDriver(SQLite WASM, :memory:)
Persistent = IndexedDB 里的 DEPMAP_CONTAINER_V1 密文；Runtime = Worker 内存里的明文图
```

## 5. 已知限制

- Web 没有 `FLAG_SECURE` 等价能力：只有隐私遮蔽 + 锁定清屏 + print 遮蔽；文案不写“防截图”。
- 浏览器可能清理 IndexedDB；`persisted()` 为 false 时 UI 明确提示并建议备份。
- JS 无法保证内存擦除；锁定只保证“不再持有可用状态”。
- Import / Proposal / Drift 现已由 E2E-17/18 覆盖；**WeChat 账单 / OFX 适配器仍未跑浏览器 E2E**（仅通用 CSV 有证据）。
- 视觉回归基线比较仍未做（CI 只生成证据，未提交基线图）。
- `sqlite-wasm` 包仍会构建出未使用的 `sqlite3-worker1.js` chunk（第三方包结构）。
- `check:web-generated` 依赖提交后的 `website/app`；提交前必然显示差异。

## 6. 外部阻塞

1. **视觉回归基线只能在 Ubuntu CI 生成**（固定 Chromium + Noto CJK），本机不能混比。
2. **人眼视觉验收**只能由你完成（`HUMAN_VISUAL_ACCEPTED = NOT_RUN`）。
3. **WebKit 离线能力边界**：断网 reload 触发 WebKit internal error；SW 接管后二次加载本地引擎不就绪。已在应用层加 20 s 启动超时 + 「重新加载」兜底，并在测试中显式标为 UNSUPPORTED。
4. **GitHub Actions 运行详情**只能通过网页抓取核对（无 `gh` CLI、未认证 API 触发 403 rate limit）。

## 7. 明确没做的事

未 merge PR #2 / 未 merge `main` / 未 force-push / 未发布 / 未加 backend / 未加云同步 / 未改 `DEPMAP_CONTAINER_V1` / 未上传真实个人数据 / 未把 `NOT_RUN` 写成 `PASS`。

## 8. CI 事实（exact HEAD）

最终运行 `https://github.com/huangdi97/PDIG-DepMap/actions/runs/41579667219`（HEAD `d4bce380`）：

| job                                 | 结果                                            |
| ----------------------------------- | ----------------------------------------------- |
| `Shared Core / Canonical gate`      | PASS（58s）                                     |
| `Web source gates`                  | PASS（28s）                                     |
| `Playwright chromium`               | PASS（2m33s）                                   |
| `Playwright firefox`                | PASS（2m14s）                                   |
| `Playwright webkit`                 | PASS（3m02s；离线两例按设计 skip）              |
| `Visual matrix (Ubuntu + Noto CJK)` | PASS（2m11s，上传 `web-visual-evidence-<sha>`） |

CI-only 缺陷（本地 Windows 不会复现，已修）：

1. `core/src/crypto/depmap.ts → Cannot find module 'hash-wasm'`：CI 只装 `web/node_modules`。修法：tsconfig `paths["hash-wasm"]` + vite `resolve.dedupe`。
2. `generated artifact in sync` 因 CRLF/LF 差异假红。修法：`build:web` 规范化文本产物为 LF，使 `BUILD_SHA256SUMS.txt` 跨平台一致。

仍然 `NOT_RUN`：视觉回归基线比较（CI 只生成证据，未提交基线图）、人眼验收。
