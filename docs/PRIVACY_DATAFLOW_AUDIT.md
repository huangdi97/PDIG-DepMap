# PRIVACY_DATAFLOW_AUDIT.md — 隐私数据流审计（RC PHASE Q）

> 标注体系：[MEMORY] 导入会话内存 · [PERSIST] 持久化（加密库内） · [ENC] 口令加密文件 · [DISCARD] 会话结束销毁

## 端到端数据流

```text
原始账单文件（用户文件系统）
  → ImportFlow.begin(raw)                       [MEMORY] 字节仅内存，无副本落盘
  → parseWechatBill → Observation[]             [MEMORY] 含 merchantRaw/金额/单号
  → assignFingerprints(fpSecret)                [MEMORY] 派生指纹（HMAC）
  → 用户 Node Resolution（resolveMerchant）      [MEMORY] 决策在会话内
  → finalize():
      fingerprintsRepo.insertBatch              [PERSIST] 只存 HMAC 指纹 + source + version
      proposals.upsert                          [PERSIST] 商户→节点映射关系，无金额/无流水
      evidence.accumulate                       [PERSIST] 只有 count/min/max 时间摘要
      import_sessions.update                    [PERSIST] 只有计数统计
  → finalize 返回后 draft/observations 丢弃      [DISCARD] 无全局引用、无序列化
  → 用户确认 acceptProposal                      [PERSIST] confirmed Dependency（用户已授权的现实断言）
  → simulateScenario（内存图）                   [MEMORY] 不持久化模拟结果
  → exportGraph → .depmap（Argon2id+AES-256-GCM）[ENC] 默认加密导出
```

## 逐项证明

| 要求                                | 证据                                                                                                                                                                                     |
| ----------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Observation/CanonicalEvent 不持久化 | `ImportFlow` 持有于实例字段，无任何 INSERT/表承载 Observations；schema v1 无 observations 表（SCHEMA_V1.md）；grep `INSERT INTO observation` 仅命中 `observation_fingerprints`（指纹表） |
| raw statement 不进入 DB             | 唯一落库路径是 repositories 的固定列 INSERT；无 raw blob 列；db-integrity 测试验证失败回滚后无残留                                                                                       |
| Evidence 不保存交易流水             | evidence 表列：proposal_key/source_type/parser*/session/first/last/count（SCHEMA_V1.md）—— 无金额、无商户、无单号                                                                        |
| local_private ignored               | `.gitignore`: `local_private/*` + `!local_private/README.md`；`git ls-files` 确认仅 README 入库                                                                                          |
| export 默认 encrypted               | 导出链路 = exportGraph(payload) → `.depmap` 容器（Argon2id+AES-256-GCM，CRYPTO_PROTOCOL.md）；无明文导出路径                                                                             |
| no analytics/telemetry/ads/backend  | core runtime 依赖仅 hash-wasm（无网络库）；Android manifest 无 INTERNET 权限；NETWORK_AUDIT.md 全库 0 网络调用                                                                           |
| fpSecret 保护                       | 存 meta 表（平台加密库内），平台层由 Keystore/Keychain/HUKS 包裹（SECURITY_MODEL.md）；日志审计确认无输出                                                                                |

## 失败路径隐私

- finalize 事务化（db-integrity.test 注入失败用例）：中途失败全部回滚，指纹不留半成品。
- 用户在 Node Resolution 放弃：finalize 从未执行 → 指纹零落库（本会话观测完全 [DISCARD]）。

---

## MVP02 Security / Privacy Re-audit（2026-09-13，GOAL §30 / PHASE 22）

新增 Generic CSV / OFX-QFX 数据源后的重新审计。全部结论为本轮 grep + 测试实跑证据（非声明）：

| 重审点                               | 结论 | 证据                                                                                                                                                                                                                                      |
| ------------------------------------ | ---- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| SourceInstance 不含账户秘密          | PASS | `source_instances` 表列仅元数据（adapter/provider_id/account_node_id/label/country/jurisdiction/currencies/state/时间戳）；`src/repositories/source-instance-repository.ts` 无 number/credential/token 字段                               |
| Mapping Profile 不保存 raw statement | PASS | MappingProfile 仅列名与格式选项（`src/sources/types.ts`）；`import_sessions` 无 mapping/raw 列，repository 仅写计数（rawCount/newUnique/duplicate/proposal/error）                                                                        |
| OFX/CSV raw 不持久化                 | PASS | schema 全库**无 observations 表**；adapter `parse` 输出仅存在于导入会话内存；grep 无 `INSERT INTO observation`（仅指纹表）；raw `Uint8Array` 不进入任何 repository 方法签名                                                               |
| Evidence 仍不是交易历史              | PASS | evidence v2 列：proposal_key/source_instance_id/adapter*/evidence_kind/parser*/session/first/last/count —— 无金额、无商户、无单号、无 sourceTxnId                                                                                         |
| sourceTxnId 不明文长期存储           | PASS | `observation_fingerprints` 仅存 HMAC 指纹 + 作用域列（fingerprint/source_instance_id/fingerprint_version/import_session_id/first_seen_at）；无 txnId 明文列；稳定交易号输入 `HMAC(fpSecret, adapterId:sourceInstanceId:sourceTxnId)` 单向 |
| logs 不打印原始行                    | PASS | `grep console.* src/` = **0 命中**（core src 全库无任何 console 调用）；adapter 坏行仅返回行号+reason（`lastParseErrors`），不携带行内容                                                                                                  |
| `.depmap` schema v2 仍被容器 V1 加密 | PASS | 容器协议未动（Golden Vector/负向/tamper 全 PASS）；payload v2 往返经 DEPMAP_CONTAINER_V1 加密封装（tests/integration/graph-payload-v2.test.ts 17 用例）                                                                                   |
| 业务网络调用 = 0                     | PASS | `grep fetch(/XMLHttpRequest/http.request/axios/WebSocket src/` = **0 命中**；runtime 依赖仅 hash-wasm                                                                                                                                     |
| Adapter 不写 Reality                 | PASS | Adapter 契约禁令（types.ts）+ assertFileAdapterContract 测试；确认唯一入口 confirmation-service `userConfirmedBasis`；coverage-semantics 7 用例证明 absence 不否定 Reality                                                                |

审计脚本：`npm run check:secrets`（248 files，0 production secrets）、`npm run check:architecture`（35 files）PASS。
