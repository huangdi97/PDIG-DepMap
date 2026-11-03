# WEB_RUNTIME_ACCEPTANCE — Web 运行时验收

> 本文只回答一个问题：**Web 运行时（Worker / Core / SQLite / 加密 / 持久化）是否真的跑通，用什么证据证明。**

## 1. 权威链路

```
React UI  ──typed RPC──▶  Domain Worker
                            ├─ VaultRuntime（生命周期 + 加密快照 + IndexedDB）
                            ├─ CoreAuthority（Core Repository / Service / Kernel）
                            └─ BrowserSqliteDriver（@sqlite.org/sqlite-wasm，:memory:）
```

- UI 侧静态断言：`node scripts/check-boundary.mjs` → `WEB BOUNDARY GATE: PASS (32 files scanned; UI 0 Core imports, 0 SQL)`
- Worker 是唯一持有明文图与派生密钥的地方；lock 直接 `terminate()`。

## 2. 证据（本地实跑）

| 项目            | 命令                                                                    | 结果                                                                              |
| --------------- | ----------------------------------------------------------------------- | --------------------------------------------------------------------------------- |
| 类型            | `npx tsc --noEmit -p web/tsconfig.json`                                 | 0 errors                                                                          |
| 构建            | `npm run build:web`（web/）                                             | 9 files / 1847 KiB → `website/app/` + `sw-manifest.json` + `BUILD_SHA256SUMS.txt` |
| 单测            | `npx vitest run`（web/）                                                | 20 passed / 3 files                                                               |
| 边界            | `node scripts/check-boundary.mjs`                                       | PASS                                                                              |
| 远程依赖        | `node scripts/check-remote-assets.mjs`                                  | PASS（8 built files，0 remote runtime deps）                                      |
| E2E（Chromium） | `npx playwright test --project=chromium`                                | 28 passed（16 功能 + 6 布局 + 6 导入/发现/漂移）                                  |
| 布局审计        | `npx playwright test --project=chromium tests/e2e/layout-audit.spec.ts` | 6 passed（6 视口 × 6 路由）                                                       |
| 视觉矩阵        | `npx playwright test --project=visual-chromium`                         | 2 passed（含 6 断点无横向溢出）                                                   |
| Core 门禁       | `cd core && npm run check`                                              | 492 passed / 0 circular / 0 network / 0 secret                                    |

E2E 覆盖（Chromium 实测）：

- E2E-01 建立 vault → 空现实 → 锁定 → 解锁
- E2E-02 对象 CRUD → reload 后仍存在
- E2E-03 依赖默认 `criticality=unknown`，写入推进 `graphRevision`
- E2E-04 变更计划创建 → 分析 → Current / Transition / After（Plan Projection）
- E2E-05 Reality 变化 → 计划 `needs_revalidation` → 动作按钮被禁用
- E2E-06 重新分析 → 真实 diff（分析基准 X → Y）→ 不自动 done/verified
- E2E-07 done ≠ verified（记录完成后仍是「待验证」）
- E2E-08 Make-Before-Break（新路径未验证 → 旧路径退休被拒；验证后放行）
- E2E-09 Records 三时钟：`发生时间未记录` / 记录时间 / 验证证据未记录
- E2E-10 隐私遮蔽：可见文本 / aria-label / title / 搜索结果均不泄漏
- E2E-11 导出 .depmap → 删除本地数据 → 恢复 → 深等价 → 重新解锁
- E2E-12 错误口令 fail closed，无半解锁
- E2E-13 篡改本地容器 → 认证失败，旧 vault 不被破坏
- E2E-14 多标签页单写者：第二个标签页被阻断
- E2E-15 / 15b 离线 reload → 解锁 → CRUD / 锁定后再解锁
- E2E-16 Findings：`SINGLE_POINT_OF_FAILURE`（唯一 required 来源）与 `PENDING_VERIFICATION`（done 未验证）均来自 Core 规则
- E2E-17 Import：通用 CSV + 显式字段映射 + **账单归属账户** → 商户解析 → Proposal → 接受后才成为现实依赖（criticality: unknown）
- E2E-18 Drift：证据只产生「可能变化」，`dismiss` 不改写 Reality（对象仍无该关系）
- E2E-19 Drift「确认为额外路径」：新边加入且旧边保持 active
- E2E-20 Drift「确认为替代」：新边加入且旧边被 retire（`已终止关系 1 条`）
- E2E-21 Import（OFX/QFX）：无需字段映射，解析后同样只产生提案

## 3. 未跑 / 未验证（不得写成 PASS）

| 项目                  | 状态                         | 原因                                                                                                                                            |
| --------------------- | ---------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| Firefox               | **PASS ×28**                 | 功能 E2E + 布局审计 + 导入/发现/漂移全部通过（firefox-1543）                                                                                    |
| WebKit                | **PASS ×26 / 2 UNSUPPORTED** | 离线两个用例在 WebKit 下不成立：断网 reload 触发 WebKit internal error；SW 接管后二次加载本地引擎不就绪（已加 20 s 启动超时与「重新加载」兜底） |
| ESLint                | NOT_RUN（未采用）            | Web 采用 TypeScript strict + Prettier + 边界静态门禁；未引入 ESLint 规则集                                                                      |
| `check:web-generated` | PASS                         | 本地与 CI 均通过（`build:web` 幂等，文本产物统一 LF）                                                                                           |
| 视觉回归基线          | NOT_RUN                      | 基线只能在 Ubuntu CI + 固定 Chromium + Noto CJK 下生成；本地只产出候选证据                                                                      |

## 4. 运行时已知边界

- 浏览器可能在存储压力下清理 IndexedDB；`navigator.storage.persisted()` 为 false 时 UI 明确提示。
- JS 无法保证内存擦除；锁定只能保证「不再持有可用状态」。
- 多标签页 lease TTL 6 s + 心跳 2 s；reload 时依靠 `pagehide` 立即过期 + 争夺窗口重试，避免假「另一标签页」提示。

## 5. CI（exact HEAD 事实）

| 项目                                | 结果                                                |
| ----------------------------------- | --------------------------------------------------- |
| `Shared Core / Canonical gate`      | **PASS**                                            |
| `Web source gates`                  | **PASS**                                            |
| `Playwright chromium`               | **PASS**（2m33s）                                   |
| `Playwright firefox`                | **PASS**（2m14s）                                   |
| `Playwright webkit`                 | **PASS**（3m02s；离线两例按设计 skip，见 §3）       |
| `Visual matrix (Ubuntu + Noto CJK)` | **PASS**（2m11s，上传 `web-visual-evidence-<sha>`） |

最终 CI 运行：`https://github.com/huangdi97/PDIG-DepMap/actions/runs/41579667219`（HEAD `d4bce380`，6 个 job 全绿）。

期间定位并修掉的两个 CI-only 缺陷（值得记录，因为它们不会被 Windows 本地跑出来）：

1. `../core/src/crypto/depmap.ts: Cannot find module 'hash-wasm'` —— TS/Vite 从 importing 文件所在目录解析裸包名，CI 只安装 `web/node_modules`，`core/node_modules` 不存在。修法：`web/tsconfig.json` 加 `paths["hash-wasm"]`、`vite.config.ts` 加 `resolve.dedupe: ['hash-wasm']`。
2. `generated artifact in sync` 假红 —— Windows 工作区（autocrlf）产出 CRLF 文本，Linux CI 产出 LF，导致 `BUILD_SHA256SUMS.txt` 哈希不同。修法：`build:web` 把文本产物统一规范为 LF，使产物跨平台字节确定。
