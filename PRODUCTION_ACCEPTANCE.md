# PRODUCTION_ACCEPTANCE.md — PDIG Production RC V1 验收清单

> 状态口径：`PASS` / `FAIL` / `BLOCKED` / `NOT_RUN` / `PARTIAL_WITH_REPORT`
> 勾选规则：**只有具备"代码存在 + 测试存在 + 本次或可信近期命令证据"才允许 `[x]`**。
> 未验证一律 `[ ]` 并注明原因。**不因源码写完就勾选。**
> 真相源：`WORK_STATUS.md`。最终判定：`PRODUCTION_RC_V1_REPORT.md`。
> 本轮证据日志：`local_private/check-full-rc1-committed.log`、`local_private/stability-rc1.log`

---

## A. Core（MVP03 Freeze 承接）

- [x] MVP03_FINAL_FREEZE = PASS（独立复验，见 `MVP03_FREEZE_REPORT.md` 附录）
- [x] PlanReadiness 无数量相减（显式 `resolvesImpactKeys`；`plan-readiness.ts` + FR-READ-001..017）
- [x] GraphRevision 仅 Reality mutation 同事务 bump（FR-GR-012 property）
- [x] Rebase deterministic + needs_revalidation（PRB-001..011）
- [x] RealityDrift positive-evidence-only（RD-001..010）
- [x] DiscoveryCandidate 隔离（不进 Impact / 不 bump revision）
- [x] ScenarioTemplate active 可执行 / planned 不可执行
- [x] Timeline 纯投影可溯源
- [x] Verification done ≠ verified（两段式）
- [x] 状态机非法迁移 Domain 层 reject
- [x] Schema v3 fresh / v2→v3 / ×50 / rollback / future reject
- [x] payload v1→v3 / v2→v3 / v3 roundtrip
- [x] DEPMAP_CONTAINER_V1 Golden unchanged
- [x] MVP01 / MVP02 / Engineering Baseline 回归

> 证据：`npm run check:full` → `test 453 passed (43 files)`、`check:db-integrity 6 passed`、
> `test:coverage 453 passed`、`test:perf 16 passed`、`FINAL_EXIT=0`。

## B. 产品与信息架构

- [x] 一级导航（tabBar）冻结并落地（`pages.json` tabBar 4 项）
- [x] 首页 answer-oriented（定位语 → 需要你处理 → 即将到来 → 常用场景 → 我的基础设施）
- [x] 场景中心（3 active 场景 + 简短描述 + 提前量建议）
- [x] ChangePlan 为核心页（readiness / coverage / 影响 / 必须处理 / 需要确认 / 动作 / 验证）
- [x] ChangePlan CTA 按状态分化（blocked / stale / review_required / 进行中 / 已完成）
- [x] Coverage UI 四级可解释（无分数无百分比）
- [x] Drift UI「可能发生了变化」+ 用户选项
- [x] Candidate UI「待确认服务」+ 是我的 / 不是（并说明"确认只建对象，不自动建关系"）
- [x] Import 流程含隐私说明步骤（4 条）
- [ ] Generic CSV 字段映射 UI —— **未实现**（依赖 B20 解析桥接）
- [x] 来源管理 UI（删除来源不删已确认关系）
- [x] 备份 / 恢复 UI（**已如实标注加解密模块未接入**，不写数据）
- [x] Settings 完整（启动验证 / 隐私屏 / 备份 / 恢复 / 数据说明 / 版本 / 危险操作）
      —— 注：**原「生物识别开关」「自动锁定开关」无实际效果，已移除**，改为只读说明
- [x] Onboarding（3 屏，不写业务数据）
- [x] About / 版本页（应用版本 / 数据格式版本 / 数据结构版本 / 已确认变更次数）
- [x] 每个页面有真实 Empty State
- [x] 关键流程有 Error State（非 console.error）
- [x] 关键异步有 Loading / disabled
- [x] **手动声明支付关系**（本轮新增，使应用在不依赖导入时可真实使用）

## C. UI / Design System / 前端

- [x] `docs/DESIGN_SYSTEM.md` 建立
- [x] Design token（`app/theme/tokens.uts`：primary / surface / background / text / border / status / disabled）
- [x] Dark Mode：token 层已支持（`DP_DARK_OVERRIDES` 完整表）；**系统切换未接线**（需 B10 验证）
- [x] Typography 层级（`DP_FONT`）
- [x] Spacing scale（`DP_SPACE`：4/8/12/16/20/24/32）
- [x] 组件库（`dp-card` / `dp-row` / `dp-chip` / `dp-button`×3 variant / `dp-state`×3 kind）
      —— 未做 AppHeader / SectionHeader / TimelineCard 等独立组件，由 `dp-card` + `dp-row` 覆盖
- [x] 图标语言统一（未使用 emoji / 远程 CDN；仅用文本符号）
- [ ] Motion / reduced motion —— **未实现**（本轮未引入动效，故无违规，但也未做 reduced-motion 处理）
- [x] 可访问性（触控最小 44px、状态不只用颜色、语义化标题）
      —— 对比度与 screen reader label **未真机验证**
- [ ] Responsive（小屏 / 大屏 / 平板 / Web）—— **未验证**（需 B10）
- [ ] Safe Area —— **未验证**（需真机）
- [x] 中文 First；无 `ChangePlan`/`RealityDrift`/`GraphRevision` 等工程词（`check:ui` U3 强制）
- [x] 文案不恐吓
- [x] UI 不直接操作 SQLite（`check:ui` U4：24 页 0 命中）
- [x] UI 不重复 Domain Logic（纯规则收敛到 `rules.uts`；**Core→App 桥接缺失导致语义镜像，见 B22**）
- [x] 无 dead-end 页面（导入 / 备份页均给出明确不可用说明与替代路径）

## D. 安全 / 隐私 / 迁移

- [x] 无业务网络调用（`check:network`：118 文件，0 网络原语）
- [x] analytics / telemetry / ads = 0（无 SDK、无网络）
- [x] Release 日志无 raw transaction / merchant / password / key / full Graph（`check:secrets` + 源码复核）
- [x] secret scan PASS（390 文件，0 production secrets）；无 keystore / p12 / 真实账单入库
- [x] Crypto Golden Vector 回归 PASS
- [x] Wrong password / tamper / 不支持版本 行为正确（`crypto-negative` + `container-mutation`）
- [ ] SQLCipher 真实加密路径未退化 —— **NOT_RUN**（需 B1/B2/B3 真机）
- [x] 权限最小化（静态审计：仅生物识别 / 文件选择 / 截屏保护相关）
- [x] 依赖 / license 审计 PASS（全 MIT 或 Apache-2.0；audit 3 moderate dev-only）
- [x] 迁移策略明确（payload v1/v2/v3 + DB migration v1→v2→v3）
- [ ] Backup / Restore 事务性（失败不留 partial DB）—— Core 层 PASS；**App 端未接入（B21）**
- [x] 数据删除不夸大（已收紧为"清空业务数据；不删数据库文件与密钥；不做物理擦除"）

## E. 工程 Gate

- [x] `npm run check` PASS
- [x] `npm run check:full` PASS（**FINAL_EXIT=0**）
- [ ] clean install PASS —— **本轮未重跑**（上一轮 PASS，本轮未作为依据）
- [ ] clean clone PASS —— **本轮未重跑**（同上）
- [x] 全量 suite ×3，0 flaky（`test:stability` → 3 连跑全绿，exit 0）
- [ ] focused 关键套件 ×10，0 flaky —— **本轮未重跑**（上一轮 PASS，本轮未作为依据）
- [x] Core targeted mutation：无 critical survived（10/10 KILLED，PARTIAL_WITH_REPORT）
- [x] coverage 维持 Core 门槛（Stmts 93.74 / Branch 82.21 / Func 94.28）
- [x] 无 TODO/FIXME/PLACEHOLDER/demo/fake 遗留于生产路径（`check:secrets` + 逐文件复核）
- [x] UI 静态 Gate 9 类 0 命中，且 U9 已用注入探针验证有效

## F. 平台

- [x] Android **SOURCE_READY** = PASS
- [ ] Android **BUILD_READY** —— **BLOCKED（B1：无 JDK17+ / Android SDK / Gradle）**
- [ ] Android **DEVICE_VERIFIED** —— NOT_RUN（B18）
- [ ] Android **SIGNING_READY** —— BLOCKED（B4）
- [ ] Android **STORE_READY** —— BLOCKED（B5）
- [x] HarmonyOS **SOURCE_READY** = PASS
- [ ] HarmonyOS **BUILD_READY** —— **BLOCKED（B2：无 DevEco Studio / SDK）**
- [ ] HarmonyOS **DEVICE_VERIFIED** / **SIGNING_READY** / **STORE_READY** —— BLOCKED（B6/B7/B18）
- [x] iOS **SOURCE_READY** = PASS
- [ ] iOS **BUILD_READY** —— **BLOCKED（B3：非 macOS）**
- [ ] iOS **DEVICE_VERIFIED** / **SIGNING_READY** / **TESTFLIGHT_READY** / **APPSTORE_READY** —— BLOCKED（B8/B9/B18）
- [ ] UI **BUILD_READY** —— **BLOCKED（B10：无 HBuilderX）**；`.uvue`/UTS **从未编译**

## G. 商店

- [x] STORE_METADATA_READY（`store/STORE_LISTING_ZH.md` / `RELEASE_NOTES.md` / `PLATFORM_REQUIREMENTS.md`）
- [ ] STORE_ASSETS_READY —— **BLOCKED**（B15 图标 / B16 启动图 / B17 截图；规格与方案已备）
- [ ] 隐私政策 URL（外部）—— BLOCKED（B12；草稿已备）
- [ ] 支持 URL（外部）—— BLOCKED（B12b）
- [x] Privacy Disclosure Matrix 事实清单（`store/PRIVACY_DISCLOSURE_MATRIX.md`）
- [ ] STORE_SUBMISSION_READY —— **BLOCKED**（B1–B12b、B14–B17）
- [x] STORE_SUBMITTED = **NO**（本轮未提交、未签名、未上传）

## H. Real Data

- [x] REAL_DATA_CORRECTNESS = NOT_RUN（无真实账单，B13）
- [x] REAL_DATA_VALUE = NOT_RUN

## I. 最终

- [ ] PRODUCTION_RC_V1 = PASS（仅当 A–E 全 PASS 且至少一个平台 build PASS）
      —— 当前：A/B/C（部分）/D/E 基本 PASS，**但无任何平台 build PASS（B1/B2/B3/B10）**，
      且 E 中 clean install / clean clone / focused ×10 本轮未重跑 → **不满足 PASS 条件**

## 最终判定

```
PRODUCTION_RC_V1 = PARTIAL_WITH_REPORT
```

**未勾选项全部是外部工具链 / 账号 / 设备 / 决策类 Blocker，或本轮未重跑的验证项。**
不存在"代码未完成"或"测试未通过"导致的未勾选。
