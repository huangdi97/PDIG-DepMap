# WEB_ARCHITECTURE — PDIG Web 运行架构

> 本文只回答一个问题：**Web 端运行时是怎么组织的，为什么 Truth 不可能来自 UI。**

## 1. 两层进程模型

```
┌──────────────────────────────── UI Thread ────────────────────────────────┐
│  React 组件（src/app, src/features）                                       │
│  - 只渲染 projection DTO                                                  │
│  - 只发送 typed command / query                                           │
│  - 不计算 continuity / stale / readiness / findings / verification         │
└───────────────────────────────┬───────────────────────────────────────────┘
                                │ postMessage（typed RPC，见 src/runtime/protocol.ts）
┌───────────────────────────────▼──── Domain Worker ────────────────────────┐
│  src/runtime/worker/domain.worker.ts                                      │
│    ├── VaultRuntime（生命周期 / 加密快照 / IndexedDB）                     │
│    ├── CoreAuthority（PDIG Core Repository + Service + Kernel）            │
│    └── BrowserSqliteDriver（@sqlite.org/sqlite-wasm，内存库，同步 API）     │
└───────────────────────────────────────────────────────────────────────────┘
```

为什么把整个 Core 放进 Worker，而不是给 UI 一个 async DB proxy：

- Core 的 `SqliteDriver` 是**同步**接口（Repository / Migration / Service 全部同步）。
- 只有把 Core 整个放进 Worker，才能既保持 Core 语义不变，又让 UI 保持异步 RPC。
- 副作用正是我们想要的：UI bundle 里不可能出现 SQL 或 Repository 调用，`check:web-boundary` 会静态验证这一点。

## 2. SQLite WASM

- 包：`@sqlite.org/sqlite-wasm`（锁定版本 `3.54.1-build1`）。
- **不使用**已 deprecated 的 `Worker1` / `Promiser1` 协议；Worker 自己 `import` 模块并初始化一次。
- 数据库是 `:memory:`：
  - 因此不需要 OPFS、不需要 COOP/COEP 跨源隔离；
  - 持久化由加密快照负责（见 §3），磁盘上永远没有明文数据库。
- Core 的迁移在内存库上按 `LATEST_SCHEMA_VERSION` 执行（当前 4）。

## 3. 存储模型

```
IndexedDB「pdig-web」
  vault   : { id:'active', containerJson, updatedAt, graphRevision, schemaVersion, demo }
  lease   : { id:'lease', ownerId, expiresAt, updatedAt }
```

- `containerJson` 是一个完整的 **DEPMAP_CONTAINER_V1** 容器（Argon2id + AES-256-GCM），
  所以“静止态”和“导出的 .depmap”是同一种加密格式。
- IndexedDB 里**没有**明文 nodes / dependencies / 邮箱 / 手机号 / 搜索历史。
- `localStorage` 只存非敏感偏好（`pdig.web.prefs.v1`：隐私遮蔽开关、自动锁定分钟）。

## 4. 持久化时序（persist-before-ack）

```
command → Core 事务（内存图变更）
        → exportGraph() → payloadJson
        → AES-256-GCM（复用 unlock 时派生的 key，fresh nonce）
        → IndexedDB 原子写（单个事务）
        → 向 UI 回 ack
```

- 失败时不谎报：`status.persisted=false`，UI 顶部显示“变更尚未安全保存”。
- Argon2id 只在 unlock/create/restore 派生一次；每次保存只做一次 AES-GCM。

## 5. 生命周期与单写者

- 状态：`no_vault / locked / unlocking / unlocked / locking / auth_failed / corrupt_vault /
unsupported_vault / storage_error / lease_denied`（不是 boolean）。
- `lock`：终止 Worker（内存明文图与密钥随之丢弃）→ 重启干净的 locked Worker。
- 多标签页：IndexedDB 事务内 read-modify-write 的 lease + BroadcastChannel 心跳。
  第二个标签页显示「PDIG 已在另一个标签页中打开」并保持只读阻断。

## 6. 构建与部署

- 源码真源：`web/`（Vite + TypeScript strict + React）。
- 部署产物：`website/app/`（`npm run build:web` 生成，含 `sw-manifest.json` 与 `BUILD_SHA256SUMS.txt`）。
- `npm run check:web-generated` 会重建一次并用 `git diff -- website/app` 证明产物与源码同步。
- Core 源码通过 Vite alias `@core/*` 直接参与构建（不复制、不 fork），因此 Web 不会与 Canonical 漂移。

## 7. 为什么没有（也不应该有）这些

- **没有云端后端**：PDIG 的威胁模型是“本机 + 只有你知道的口令”；加云端会把 Chronicle 级隐私问题带回来。
- **没有明文持久 SQLite（OPFS）**：浏览器沙箱不是 at-rest 加密。
- **没有 Remote CDN / 远程字体 / 图标服务**：`check:remote-assets` 会在构建产物上静态验证 0 远程依赖。
- **没有把 Graph 当主界面**：图只是对象详情里的局部检查视图。

## 8. 已知边界

- Web 没有 `FLAG_SECURE` 等价能力：只能做「隐私遮蔽 + 锁定时清空 + pagehide 时清屏」。
- 浏览器可能在存储压力下清理 IndexedDB：`navigator.storage.persisted()` 为 false 时 UI 明确告知并建议导出备份。
- `sqlite-wasm` 包内仍会构建出 `sqlite3-worker1.js` chunk（未使用），属于第三方包结构，不影响运行路径。
