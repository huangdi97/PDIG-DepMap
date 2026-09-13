# PRODUCTION_ACCEPTANCE.md — PDIG Production RC V1 验收清单

> 状态口径：`PASS` / `FAIL` / `BLOCKED` / `NOT_RUN` / `PARTIAL_WITH_REPORT`
> 勾选规则：**只有具备"代码存在 + 测试存在 + 命令证据"才允许 `[x]`**。未验证一律 `[ ]` 并注明原因。
> 真相源：`WORK_STATUS.md`。最终判定：`PRODUCTION_RC_V1_REPORT.md`。

---

## A. Core（MVP03 Freeze 承接）

- [ ] MVP03_FINAL_FREEZE = PASS
- [ ] PlanReadiness 无数量相减（显式 `resolvesImpactKeys`）
- [ ] GraphRevision 仅 Reality mutation 同事务 bump
- [ ] Rebase deterministic + needs_revalidation
- [ ] RealityDrift positive-evidence-only
- [ ] DiscoveryCandidate 隔离（不进 Impact / 不 bump revision）
- [ ] ScenarioTemplate active 可执行 / planned 不可执行
- [ ] Timeline 纯投影可溯源
- [ ] Verification done ≠ verified
- [ ] 状态机非法迁移 Domain 层 reject
- [ ] Schema v3 fresh / v2→v3 / ×50 / rollback / future reject
- [ ] payload v1→v3 / v2→v3 / v3 roundtrip
- [ ] DEPMAP_CONTAINER_V1 Golden unchanged
- [ ] MVP01 / MVP02 / Engineering Baseline 回归

## B. 产品与信息架构

- [ ] 一级导航（tabBar）冻结并落地
- [ ] 首页 answer-oriented（3 秒内可知：有无待办 / 有无变化 / 从哪开始）
- [ ] 场景中心（3 active 场景 + 简短描述）
- [ ] ChangePlan 为产品核心页（状态 / readiness / coverage / revision freshness / 影响 / 必须处理 / 需要确认 / 备用路径 / checklist / verification / timeline）
- [ ] ChangePlan CTA 按状态分化（blocked / review_required / needs_revalidation / ready / verifying / completed）
- [ ] Coverage UI 四级可解释（Unknown / Limited / Partial / Well evidenced，无分数无百分比）
- [ ] Drift UI「可能发生了变化」+ 四选项
- [ ] Candidate UI「发现一个可能属于你的服务」+ 确认/忽略/稍后
- [ ] Import 流程含隐私说明步骤
- [ ] Generic CSV 字段映射 UI
- [ ] 来源管理 UI
- [ ] 备份 / 恢复 UI
- [ ] Settings 完整（生物认证 / 自动锁定 / 备份 / 恢复 / 数据说明 / 隐私 / 版本 / schema 版本 / 重置）
- [ ] Onboarding（≤3 屏，不入正式 Graph）
- [ ] About / 版本页
- [ ] 每个页面有真实 Empty State
- [ ] 关键流程有 Error State（非 console.error）
- [ ] 关键异步有 Loading / disabled / progress

## C. UI / Design System / 前端

- [ ] `docs/DESIGN_SYSTEM.md` 建立
- [ ] Design token（primary / surface / background / text / border / status / disabled）
- [ ] Dark Mode：token 层可支持（或已实现 light/dark/system）
- [ ] Typography 层级（Display/H1/H2/Section/Body/Secondary/Caption/Button）
- [ ] Spacing scale（4/8/12/16/20/24/32）
- [ ] 组件库（AppHeader / SectionHeader / StatusChip / Button×3 / InfoCard / ActionCard / ScenarioCard / TimelineCard / EmptyState / ErrorState / LoadingState / ConfirmationSheet）
- [ ] 图标语言统一（无 emoji/线性/彩色混用；license 已审；无远程 CDN）
- [ ] Motion 轻量 + respect reduced motion
- [ ] 可访问性（触控区 / 对比度 / 字号 / screen reader label / 非仅颜色表达状态 / 焦点顺序）
- [ ] Responsive（小屏 / 普通 / 大屏 / 平板基本布局 / Web）
- [ ] Safe Area（iPhone 刘海与 Home Indicator / Android insets / Harmony insets）
- [ ] 中文 First；无 `ChangePlan`/`RealityDrift`/`GraphRevision` 等工程词
- [ ] 文案不恐吓（无"危险/高危/严重风险"）
- [ ] UI 不直接操作 SQLite（经 Application Service）
- [ ] UI 不重复 Domain Logic
- [ ] 无 dead-end 页面

## D. 安全 / 隐私 / 迁移

- [ ] 无业务网络调用（business network calls = 0）
- [ ] analytics / telemetry / ads = 0
- [ ] Release 日志无 raw transaction / merchant / password / key / full Graph
- [ ] secret scan PASS；无 keystore / p12 / 真实账单入库
- [ ] Crypto Golden Vector 回归 PASS
- [ ] Wrong password / tamper / 不支持版本 行为正确
- [ ] SQLCipher 真实加密路径未退化
- [ ] 权限最小化（每项权限有用途）
- [ ] 依赖 / license 审计 PASS（runtime critical vuln = 0）
- [ ] 迁移策略明确（v1/v2/v3 payload + DB migration）
- [ ] Backup / Restore 事务性（失败不留 partial DB）
- [ ] 数据删除不夸大（不声称物理擦除）

## E. 工程 Gate

- [ ] `npm run check` PASS
- [ ] `npm run check:full` PASS
- [ ] clean install PASS
- [ ] clean clone PASS
- [ ] 全量 suite ×3，0 flaky
- [ ] focused 关键套件 ×10，0 flaky
- [ ] Core targeted mutation：无 critical survived
- [ ] coverage 维持 Core 门槛
- [ ] 无 TODO/FIXME/PLACEHOLDER/mock/demo/fake 遗留于生产路径

## F. 平台

- [ ] Android SOURCE_READY / BUILD_READY / DEVICE_VERIFIED / SIGNING_READY / STORE_READY（分别判定）
- [ ] HarmonyOS SOURCE_READY / BUILD_READY / DEVICE_VERIFIED / SIGNING_READY / STORE_READY
- [ ] iOS SOURCE_READY / BUILD_READY / DEVICE_VERIFIED / SIGNING_READY / TESTFLIGHT_READY / APPSTORE_READY

## G. 商店

- [ ] STORE_METADATA_READY（listing / 描述 / 关键词 / 版本说明）
- [ ] STORE_ASSETS_READY（icon / splash / screenshots）
- [ ] 隐私政策 URL（外部）
- [ ] 支持 URL（外部）
- [ ] Privacy Disclosure Matrix 事实清单
- [ ] STORE_SUBMISSION_READY
- [ ] STORE_SUBMITTED = **NO**（本轮不自动提交）

## H. Real Data

- [ ] REAL_DATA_CORRECTNESS = NOT_RUN（无真实账单）
- [ ] REAL_DATA_VALUE = NOT_RUN

## I. 最终

- [ ] PRODUCTION_RC_V1 = PASS（仅当 A–E 全 PASS 且至少一个平台 build PASS）
