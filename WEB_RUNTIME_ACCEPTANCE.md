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
| E2E（Chromium） | `npx playwright test --project=chromium`                                | 16 passed                                                                         |
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

## 3. 未跑 / 未验证（不得写成 PASS）

| 项目                  | 状态                         | 原因                                                                                                                                            |
| --------------------- | ---------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| Firefox               | **PASS ×22**                 | 功能 E2E + 布局审计全部通过（firefox-1543）                                                                                                     |
| WebKit                | **PASS ×20 / 2 UNSUPPORTED** | 离线两个用例在 WebKit 下不成立：断网 reload 触发 WebKit internal error；SW 接管后二次加载本地引擎不就绪（已加 20 s 启动超时与「重新加载」兜底） |
| ESLint                | NOT_RUN（未采用）            | Web 采用 TypeScript strict + Prettier + 边界静态门禁；未引入 ESLint 规则集                                                                      |
| `check:web-generated` | 待提交后验证                 | 该门禁用 `git diff -- website/app` 判定，必须先把构建产物提交后才可能为 PASS                                                                    |
| 视觉回归基线          | NOT_RUN                      | 基线只能在 Ubuntu CI + 固定 Chromium + Noto CJK 下生成；本地只产出候选证据                                                                      |

## 4. 运行时已知边界

- 浏览器可能在存储压力下清理 IndexedDB；`navigator.storage.persisted()` 为 false 时 UI 明确提示。
- JS 无法保证内存擦除；锁定只能保证「不再持有可用状态」。
- 多标签页 lease TTL 6 s + 心跳 2 s；reload 时依靠 `pagehide` 立即过期 + 争夺窗口重试，避免假「另一标签页」提示。

## 5. CI（exact HEAD 事实）

| 项目                                   | 结果                                                                                                                         |
| -------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| `Shared Core / Canonical gate`         | **PASS**（run 对应 `b5d1f760`）                                                                                              |
| `Web source gates`                     | **FAIL**：`npm run typecheck` 在 `ubuntu-latest` 失败；同一命令在 Windows 上通过；job log 需要鉴权，本轮无法读取具体 TS 报错 |
| `Playwright {chromium,firefox,webkit}` | **NOT_RUN**（web-gate 失败导致 skipped）                                                                                     |
| `Visual matrix (Ubuntu)`               | **NOT_RUN**（同上）                                                                                                          |

已推送的缓解措施（commit `5749f4d`）：CI 的 typecheck 步骤会把错误写入 job summary 并发出 `::error` annotation，下一次运行即可在公开页面/API 读到具体报错。
