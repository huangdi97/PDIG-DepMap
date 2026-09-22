# store/RELEASE_NOTES_DRAFT.md

> 状态：**DRAFT / NOT_SUBMITTED**
> 生成时间：2026-09-22（ANDROID_CANONICAL_FREEZE → Production/Reality Closure 轮）
> 版本：内部 `0.1.0-milestone`（对外正式版本名待定，见 ANDROID_RELEASE_IDENTITY_DECISION.md R-3）。

---

## 首发版 Release Notes（草稿）

### 新功能

- **本地依赖关系图**：把账号、支付方式、订阅、邮箱、手机号整理成依赖关系。
- **3 个支付类场景**：更换支付卡、银行卡即将到期、注销银行卡 —— 每个场景走完
  影响面 → 变更计划 → 逐项完成 → 逐项核实。
- **变更计划**：可勾选的变更清单，done ≠ verified（完成后仍需逐项确认）。
- **本地加密存储**：SQLCipher 加密数据库 + Android Keystore。
- **加密备份 / 恢复**：`.depmap` 文件（AES-256-GCM，用户口令）。
- **账单导入辅助**：微信账单 / 通用 CSV / OFX-QFX（仅本机解析，生成待确认建议）。
- **应用锁**：生物识别（指纹 / 设备凭据）解锁。
- **数据管理**：设置内「删除所有数据」。

### 已知限制（如实）

- 依赖关系主要依靠用户明确录入与确认，**不做自动发现全量保证**。
- 无网络同步功能（local-first）。
- 未接入银行或任何金融服务机构。
- 导入的账单仅用于本机解析，不保存完整交易历史。

## 状态

```text
RELEASE_NOTES_DRAFT = READY（草稿；正式版本名/日期待定）
```
