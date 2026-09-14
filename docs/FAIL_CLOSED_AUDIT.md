# FAIL_CLOSED_AUDIT.md — 错误模型 / Fail Closed（RC PHASE AD）

> 逐项验证（证据 = 测试文件 + 代码路径）；核心错误使用结构化错误
> （DepmapError.code / GraphImportError / DepmapDBError），不靠字符串比较。

| 要求                                 | 证据                                                                                                                                        | 状态 |
| ------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------- | ---- |
| crypto fail 不返回 partial plaintext | AES-GCM `final()` 失败即抛，`openDepmapContainer` 不返回部分输出；crypto-negative corrupted payload 用例验证                                | ✅   |
| auth cancel 不开数据层               | unlock 页仅 `res.ok===true` 才 open DB；BiometricGateAdapter/LocalAuthenticationGate/UserAuthGate 取消映射 ok=false（源码审查）             | ✅   |
| migration fail rollback              | migration.test「rolls back completely」+ driver.transaction 语义                                                                            | ✅   |
| import fail 不提交半成品             | db-integrity.test AE 用例：事务内注入 RAISE(ABORT) → 指纹/建议/证据零残留；移除后重试成功（retry 安全）                                     | ✅   |
| unresolved 不建 Dependency           | ImportFlow.finalize 未 resolution 商户 continue；ConfirmationService.acceptProposal 先 nodes.getExisting（negative.test missing node 用例） | ✅   |
| 未确认 Group 不推断 backup           | kernel 只接收 active confirmed groups；T7（unconfirmed → needs_review）                                                                     | ✅   |
| unknown criticality => needs_review  | T8 + integration 场景 A                                                                                                                     | ✅   |
| unsupported version 明确失败         | migration：schema_version newer → throw；payload：GraphImportError unsupported payloadVersion；container：formatVersion/kdf.version bounds  | ✅   |
| 错误不泄密                           | DepmapError 消息为固定语义文案（无 key/明文/SQL）；LOGGING_AUDIT 0 敏感输出                                                                 | ✅   |
| 空口令/非法参数                      | hash-wasm 错误包装为 DepmapError('kdf')（crypto-negative 用例）                                                                             | ✅   |

## 结构化错误清单

| 层            | 错误类型                                         | code 字段                                                     |
| ------------- | ------------------------------------------------ | ------------------------------------------------------------- |
| crypto        | `DepmapError`                                    | invalid_json / invalid_structure / bounds / kdf / auth_failed |
| graph payload | `GraphImportError`                               | （消息语义；unsupported payloadVersion / missing column 等）  |
| DB（iOS）     | `DepmapDBError`                                  | notOpen / keyError / sqlite(code) / schemaNewer               |
| DB（Android） | Exception + adapter 错误回调字符串（UTS 桥接层） | 待编译验证（B1）                                              |
