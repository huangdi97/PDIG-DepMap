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

| 要求 | 证据 |
|---|---|
| Observation/CanonicalEvent 不持久化 | `ImportFlow` 持有于实例字段，无任何 INSERT/表承载 Observations；schema v1 无 observations 表（SCHEMA_V1.md）；grep `INSERT INTO observation` 仅命中 `observation_fingerprints`（指纹表） |
| raw statement 不进入 DB | 唯一落库路径是 repositories 的固定列 INSERT；无 raw blob 列；db-integrity 测试验证失败回滚后无残留 |
| Evidence 不保存交易流水 | evidence 表列：proposal_key/source_type/parser*/session/first/last/count（SCHEMA_V1.md）—— 无金额、无商户、无单号 |
| local_private ignored | `.gitignore`: `local_private/*` + `!local_private/README.md`；`git ls-files` 确认仅 README 入库 |
| export 默认 encrypted | 导出链路 = exportGraph(payload) → `.depmap` 容器（Argon2id+AES-256-GCM，CRYPTO_PROTOCOL.md）；无明文导出路径 |
| no analytics/telemetry/ads/backend | core runtime 依赖仅 hash-wasm（无网络库）；Android manifest 无 INTERNET 权限；NETWORK_AUDIT.md 全库 0 网络调用 |
| fpSecret 保护 | 存 meta 表（平台加密库内），平台层由 Keystore/Keychain/HUKS 包裹（SECURITY_MODEL.md）；日志审计确认无输出 |

## 失败路径隐私

- finalize 事务化（db-integrity.test 注入失败用例）：中途失败全部回滚，指纹不留半成品。
- 用户在 Node Resolution 放弃：finalize 从未执行 → 指纹零落库（本会话观测完全 [DISCARD]）。
