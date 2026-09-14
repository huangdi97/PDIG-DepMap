# GOAL_PRODUCTION_RC_V1.md — PDIG Production RC V1 目标与控制文件

> 本文件是本轮（Production RC V1）的**范围与判定总纲**。与 `PRODUCTION_ACCEPTANCE.md`（验收清单）、
> `PRODUCTION_TEST_MATRIX.md`（测试矩阵）、`UI_UX_ACCEPTANCE.md`（UI Gate）、
> `STORE_RELEASE_CHECKLIST.md`（发布清单）、`PLATFORM_RELEASE_MATRIX.md`（平台矩阵）配套。
>
> 真相源：**`WORK_STATUS.md`**。最终判定：**`PRODUCTION_RC_V1_REPORT.md`**。

---

## 1. 本轮目标

把项目推进到：**当前环境允许范围内，可以真实安装、真实使用、真实构建，并已完成上线前代码、UI、交互、
安全、迁移、测试、文档、构建与商店发布准备的 Release Candidate。**

**必须区分**（不得合并为一句"已上线"）：

`CORE_READY` / `PRODUCT_READY` / `UI_READY` / `ANDROID_READY` / `HARMONY_READY` /
`IOS_SOURCE_READY` / `STORE_METADATA_READY` / `REAL_DATA_VALIDATED` / `STORE_SUBMITTED`

真实环境无法完成的项必须 `BLOCKED` + 原因 + 下一步。**不伪造上线。**

---

## 2. 范围冻结（Scope Freeze）

本轮产品范围 = **MVP01 + MVP02 + MVP03 已有能力**。

**不进入 MVP04。** 不上：PayPal / Open Banking / Plaid / 手机号完整 domain / 邮箱 recovery domain /
Browser Discovery / Google Takeout / GitHub-Domain-Cloud 新 Domain / AI-LLM / Agent 产品能力 /
GraphRAG / 云同步 / 后端账号 / 社交 / 广告 / 通用提醒。

新想法写入 `FUTURE.md`，不实现。

---

## 3. 永久正确性原则（不得违反）

```
Observation != Reality          Proposal != Reality
DiscoveryCandidate != Node      RealityDrift != Reality change
Evidence != Transaction History Recorded absence != real-world absence
两个路径 != confirmed fallback   机器推断 != required
Action done != verified         ScenarioCoverage != PlanReadiness
TimelineItem != Truth           ScenarioTemplate != Reality
Future observation != 自动 verified  confidence != confirmation
```

> **宁可漏报，不可把"不确定"伪装成"必须处理"。** Precision > Recall。

---

## 4. 执行顺序（§160 优先级）

| 优先级 | 内容                                                            |
| ------ | --------------------------------------------------------------- |
| **P0** | MVP03 correctness / final freeze                                |
| **P1** | 产品核心 UX（IA / 首页 / 场景 / ChangePlan / Timeline / Drift） |
| **P2** | UI / Design System / 前端架构分层                               |
| **P3** | Backup / Security / Migration / Release 审计                    |
| **P4** | 平台实际构建（工具链存在时）                                    |
| **P5** | Store metadata / assets / 外部 Blocker 收敛                     |

**不得**先调圆角而后 Core correctness 未冻结。

---

## 5. 状态口径（强制词汇）

`PASS` / `FAIL` / `BLOCKED` / `NOT_RUN` / `PARTIAL_WITH_REPORT`

**禁止**："基本完成" / "大概可用" / "理论上支持"。

`PASS` 最低要求：**代码存在 + 测试存在 + 本次或可信近期命令证据**。

---

## 6. 平台状态分层（不得合并）

`SOURCE_READY` → `BUILD_READY` → `DEVICE_VERIFIED` → `SIGNING_READY` → `STORE_ASSETS_READY` →
`STORE_METADATA_READY` → `STORE_SUBMISSION_READY` → `STORE_SUBMITTED`

Android / HarmonyOS / iOS 各自独立判定（见 `PLATFORM_RELEASE_MATRIX.md`）。
**Windows 上 iOS `BUILD_READY` 必须 BLOCKED。**

---

## 7. "做到能上线"的定义

**不是**已经上架商店。**而是**：

> 在当前可获得环境范围内，产品功能、UI、代码、安全、迁移、文档、发布配置均已达到 Release Candidate；
> 具备工具链的平台已真实 build/test；缺失的 Mac / 签名 / 账号 / 链接等被收敛为**少量明确外部 Blocker**；
> 用户补齐后无需再次大规模开发，只需 **build → sign → device verify → upload → store review**。

---

## 8. Git 纪律

- 保护历史：**不** `reset --hard` / `clean -fd` / force push / 历史改写
- 小步 commit：`fix(mvp03):` `test(mvp03):` `feat(ui):` `refactor(frontend):` `style(ui):`
  `test(ui):` `security(release):` `docs(release):` `chore(rc):`
- **不自动 push**（除非用户明确授权）
- **不自动发布商店**；只做到 Store Submission Ready
- 禁止提交 secrets / keystore / 真实账单 / local DB / decrypted `.depmap`

---

## 9. 外部 Blocker 定义（只有这些允许 BLOCKED）

开发者账号 / 签名证书 / macOS-Xcode / 真实物理设备 / 正式品牌决策 / 正式支持 URL / 正式隐私 URL /
用户真实账单。其余（编译错误、测试失败、依赖冲突、CSS 问题）必须自行修复。

---

## 10. 本轮交付物清单

**控制文件**：本文件、`PRODUCTION_ACCEPTANCE.md`、`PRODUCTION_TEST_MATRIX.md`、
`UI_UX_ACCEPTANCE.md`、`STORE_RELEASE_CHECKLIST.md`、`PLATFORM_RELEASE_MATRIX.md`、`STORE_EXTERNAL_BLOCKERS.md`

**报告**：`MVP03_FREEZE_REPORT.md`（追加复跑证据）、`PRODUCTION_UI_AUDIT.md`、
`FRONTEND_ARCHITECTURE_AUDIT.md`、`SECURITY_RELEASE_AUDIT.md`、`PRIVACY_RELEASE_AUDIT.md`、
`docs/RELEASE_VERSION_MATRIX.md`、`PRODUCTION_RC_V1_REPORT.md`

**产品/UI**：Design System、组件库、IA 冻结（tabBar）、Onboarding、首页、场景中心、ChangePlan、
Timeline、Drift、Candidate、来源管理、导入、备份/恢复、设置、关于/隐私

**商店**：`store/STORE_LISTING_ZH.md`、`PRIVACY_DISCLOSURE_MATRIX.md`、`SCREENSHOT_PLAN.md`、
`RELEASE_NOTES.md`、`PLATFORM_REQUIREMENTS.md`

**隐私/法务草案**：`docs/PRIVACY_POLICY_DRAFT.md`、`docs/USER_NOTICE_DRAFT.md`

---

## 11. 最终判定字段（PRODUCTION_RC_V1_REPORT.md 必含）

见 `PRODUCTION_RC_V1_REPORT.md` §1 完整字段表（§153 全部字段）。
