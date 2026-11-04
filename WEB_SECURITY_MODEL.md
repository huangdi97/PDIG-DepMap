# WEB_SECURITY_MODEL — PDIG Web 的安全模型与诚实边界

> 本文只回答一个问题：**Web 端做到了什么、明确做不到什么。**

## 1. 威胁模型

PDIG 处理的是：手机号、邮箱、账户、支付工具、服务、恢复路径、支付路径。
Web 端的资产是「Personal Reality（已确认的关系图）」与「导入产生的提案」。

信任边界：

- 信任：本机操作系统、浏览器进程、用户口令。
- 不信任：页面里的任何网络响应（因为没有业务网络）、第三方脚本（没有）、文件内容（一律先校验）。

## 2. 静止态加密（at rest）

- 持久化内容：一个完整 `DEPMAP_CONTAINER_V1` 容器 JSON。
- 参数（冻结，与三端原生实现一致）：
  - KDF：Argon2id v19，65536 KiB / 3 iterations / parallelism 1，salt 16 B
  - Cipher：AES-256-GCM，nonce 12 B，tag 16 B
  - AAD：`UTF8(RFC8785-JCS({format, formatVersion, kdf, cipher}))`
  - 口令：精确 UTF-8 字节，**不做 Unicode 归一化**
- 浏览器实现：`hash-wasm`（Argon2id）+ WebCrypto（AES-GCM）+ 与 Core 相同的 JCS/AAD/base64 规则。
- 已验证：`tests/unit/depmap-web-golden.test.ts` 对冻结 Golden Vector 断言
  `derivedKey / ciphertext / tag` **逐字节相等**，并与 Core 的常量对象整体比对。

结论矩阵：

| 项目                             | 状态                              |
| -------------------------------- | --------------------------------- |
| 明文 Personal Reality 落盘       | NO                                |
| 口令持久化                       | NO                                |
| 派生密钥持久化                   | NO（仅在 unlocked Worker 内存中） |
| 业务网络请求                     | NONE                              |
| 第三方分析/遥测                  | NONE                              |
| localStorage 存 Personal Reality | NO（仅非敏感偏好）                |

## 3. 解锁与锁定

- 口令 → Argon2id（约 64 MiB 内存）→ 32 B key → AES-GCM 解密容器 → `importGraph` 到内存 SQLite。
- 错误口令 / 篡改：GCM 认证失败 → `auth_failed`，**不产生任何部分解密状态**。
- 未来格式：`formatVersion > 1` 或 payload 版本更新 → 直接拒绝（fail closed，不尝试“兼容模式”）。
- 锁定：清空投影与搜索索引 → 关闭 DB → 丢弃 session key → **terminate Worker** → 重启锁定态 Worker。
- JS 无法保证彻底擦除内存；我们不宣称能做到。这里的目标是“不残留可被继续使用的明文状态”。

## 4. 多标签页

- 单写者 lease（IndexedDB 事务 + BroadcastChannel 心跳，TTL 6 s）。
- 第二个标签页：只读阻断（`lease_denied`），不会静默 last-writer-wins。

## 5. XSS 与渲染

- 所有用户输入（名称、发卡机构、服务标签、自定义字段）只用 React 文本节点渲染。
- 全仓禁止 `dangerouslySetInnerHTML`（`check:web-boundary` 静态断言）。
- CSP（`web/index.html`，随构建进入产物）：
  `default-src 'self'; script-src 'self' 'wasm-unsafe-eval'; style-src 'self' 'unsafe-inline';
 img-src 'self' data: blob:; font-src 'self'; connect-src 'self'; worker-src 'self' blob:;
 object-src 'none'; base-uri 'self'; form-action 'none'`
  - `'wasm-unsafe-eval'` 仅用于编译 SQLite WASM 模块（`WebAssembly.compile`），不使用 `eval`/`new Function`。
  - `style-src 'unsafe-inline'` 仅因为组件使用内联 style 属性定位；不加载任何远程样式。
  - `frame-ancestors` 在 meta CSP 中无效，需由部署层 HTTP 头提供（见 §8）。

## 6. 隐私遮蔽（不是“防截图”）

- Web 没有 Android `FLAG_SECURE` 等价能力，因此产品文案只叫「隐私遮蔽」。
- 开启后：名称、发卡机构、账号标签、关系对端、搜索结果均渲染为 `••••••`；
  敏感值不会出现在 `aria-label`、`title`、`data-*` 或搜索结果 DOM 中（E2E 断言）。
- 页面隐藏（`visibilitychange=hidden`）时覆盖全屏遮罩；`print` 样式隐藏敏感文本。
- 搜索索引只在内存；锁定即销毁；不存在“最近搜索”持久化。

## 7. 日志与错误

- 生产默认不输出任何业务数据；错误只保留内部错误码（`AUTH_FAILED` / `KDF_BOUNDS` / `STORAGE_ERROR` …）。
- 禁止输出：口令、派生密钥、容器明文、手机号、邮箱、账户名、服务名、依赖对端、导入原始行。
- 用户文案不泄露密码学细节（例如不区分“口令错误”和“标签被篡改”，统一为“口令错误或文件已损坏”）。

## 8. 明确做不到的事

- 没有系统级截图 / 录屏保护。
- 浏览器运行时受浏览器与操作系统漏洞影响；DevTools 能看到已解锁的页面内容。
- 本地浏览器存储可能被清理（`navigator.storage.persist()` 可能被拒绝）。
- 不能防止拥有本机文件系统访问权的攻击者做离线暴力破解（这是 Argon2id 参数与口令强度的博弈，不是绝对保证）。
- meta CSP 无法设置 `frame-ancestors`；如需防嵌套，必须在部署层加 HTTP 头。
