# SECURITY_PRIVACY_REGRESSION_MATRIX.md — 安全/隐私回归矩阵（Engineering Baseline V1）

> 每项 = 永久断言。任何变更后 `check` + `check:full` + 人工 grep 必须维持全 NO/0。
> 配套：`docs/PRIVACY_DATAFLOW_AUDIT.md`（数据流）、`docs/SECRET_SCAN_REPORT.md`、`docs/LOGGING_POLICY.md`。

| #    | 断言                                              | 状态   | 证据来源                                                                                    |
| ---- | ------------------------------------------------- | ------ | ------------------------------------------------------------------------------------------- |
| S-01 | 原始账单（raw statement）持久化                   | **NO** | schema 无 raw 列；PRIVACY_DATAFLOW_AUDIT MVP02 重审（grep 实证）                            |
| S-02 | Observation / 单笔交易持久化                      | **NO** | 无 observations 表；Observation 仅会话内存（AGENTS §12）                                    |
| S-03 | sourceTxnId 明文持久化                            | **NO** | 指纹表无明文列；C1–C7（sourceTxnId 仅入 HMAC 输入）                                         |
| S-04 | password / 密钥 / fpSecret 日志输出               | **NO** | src 零 console；LOGGING_POLICY；secret scan                                                 |
| S-05 | fileEncryptionKey / SQLCipher secret 持久化或日志 | **NO** | Keystore/Keychain 契约（平台侧 STATIC_AUDITED）                                             |
| S-06 | 业务网络调用                                      | **0**  | `check:network` PASS（84 files，0 原语）                                                    |
| S-07 | analytics / telemetry                             | **0**  | 无依赖、无调用面（DEPENDENCY_AUDIT：runtime dep 仅 hash-wasm）                              |
| S-08 | 广告 SDK                                          | **0**  | 同上                                                                                        |
| S-09 | 明文备份 / 明文导出                               | **0**  | 导出仅 `.depmap` 加密容器（AES-256-GCM + Argon2id）；ui 无明文导出路径                      |
| S-10 | 生产 secrets 入库                                 | **0**  | `check:secrets` PASS（256 files，0 production secrets）                                     |
| S-11 | 真实用户数据 tracked                              | **0**  | `local_private/*` gitignore（仅 README 入库）；`git ls-files` 复核                          |
| S-12 | 数据库明文存储                                    | **NO** | SQLCipher（Android/iOS）/ ArkData 加密（Harmony）契约；自制 PIN 禁止（AGENTS §7）           |
| S-13 | 机器推断写入 required                             | **NO** | RelationRegistry `defaultCriticality: 'unknown'`；registry 测试；INV5；eslint+DB CHECK      |
| S-14 | Proposal 直接转 Reality                           | **NO** | 仅 ConfirmationService 写路径；INV11/INV12；kernel P4/P5                                    |
| S-15 | `.depmap` V1 协议静默变更                         | **NO** | golden vector 回归（crypto/depmap.test.ts + container-mutation F6）；formatVersion 边界拒绝 |
