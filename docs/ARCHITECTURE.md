# ARCHITECTURE.md — DepMap MVP 架构

## 总览

```text
┌───────────────────────────────────────────────────────┐
│                 uni-app x (Vapor) + Vue 3             │
│  app/pages/*.uvue — 中文 UI（11 页，answer-oriented） │
└───────────────┬───────────────────────────────────────┘
                │  UTS 插件（app/uni_modules/depmap-*）
┌───────────────┴───────────────────────────────────────┐
│        平台安全层（Kotlin / Swift / ArkTS）           │
│  SQLCipher+Keystore │ SQLCipher+Keychain │ ArkData+HUKS │
└───────────────┬───────────────────────────────────────┘
                │  同一逻辑 Schema（schema v1 DDL）
┌───────────────┴───────────────────────────────────────┐
│      共享 Core（core/ — 纯 TypeScript，Node 可测）    │
│  domain · schema/migrations · repositories            │
│  impact(kernel) · parser(wechat) · fingerprint        │
│  resolver · services(import-flow/confirmation)        │
│  crypto(.depmap V1)                                   │
└───────────────────────────────────────────────────────┘
```

## 职责边界

| 层        | 内容                                              | 不允许            |
| --------- | ------------------------------------------------- | ----------------- |
| UI 层     | `.uvue` 页面、中文文案、确认交互                  | 业务规则、SQL     |
| UTS 插件  | 平台能力桥接（DB/密钥/认证/文件加密/隐私屏）      | 业务语义          |
| 共享 Core | 全部业务语义与数据规则                            | 平台 API 直接依赖 |
| 平台原生  | SQLite/SQLCipher、ArkData、Keystore/Keychain/HUKS | 语义决策          |

## 关键数据流

1. **导入**：CSV(bytes, 内存) → `ImportFlow.begin`（解析+商户分组）→ 用户 Node Resolution（`resolveMerchant`）→ `finalize`（指纹去重 → 周期识别 → Proposal upsert + Evidence 累计）→ Session 完成。原始账单/Observation 不持久化。
2. **确认**：`ConfirmationService.acceptProposal` → Dependency（UPSERT：INSERT / verify / re-activate 同一 id）。GroupProposal 确认 → DependencyGroup（canonical groupKey UNIQUE）。
3. **影响分析**：确认图 + confirmed Group → `simulateScenario(unavailable: Set<(nodeId, capability)>)` → wave-BFS 传播 → 分类结果 + Action Checklist（原始操作强制最后）。
4. **备份**：全库导出 → `.depmap`（Argon2id+AES-256-GCM+JCS AAD）→ 本地文件；导入反向。

## 目录

- `core/` 共享 TS Core + 106 项测试（vitest）
- `app/` uni-app x 应用（pages/uni_modules）
- `platforms/android|harmonyos|ios/` 原生工程与安全源码
- `docs/` 本文档集

## 平台矩阵（当前真实状态）

见 `WORK_STATUS.md` 与 `FINAL_REPORT.md`。原则：未编译 = COMPILED NO，不虚报。
