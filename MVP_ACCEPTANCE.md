# MVP_ACCEPTANCE.md — 验收状态（2026-09-11 更新）

> [x] = 已达成并有测试/证据；[ ] = 未达成（注明原因）。禁止伪完成。

## A. Core Correctness — 全部达成

- [x] Schema v1 migration 可重复执行（migration.test）
- [x] Dependency logical key UNIQUE（migration.test + repositories.test）
- [x] retired Dependency 可 re-activate 且 id 不变（repositories.test + pipeline.test）
- [x] Group canonical groupKey 去重（repositories.test）
- [x] Proposal UPSERT 正确（proposal-lifecycle.test）
- [x] rejected 有新 Evidence 才重提（proposal-lifecycle.test：≥3 新观测 + ≥1 完整周期）
- [x] GroupProposal 生命周期正确（proposal-lifecycle.test）
- [x] Observation 不持久化（架构保证：仅 ImportFlow 内存；fixtures 无任何 raw 落库路径）
- [x] CanonicalEvent 不持久化（同上，MVP 未实现跨来源关联）
- [x] Fingerprint 重复导入测试通过（wechat.test：1–6/1–8 月仅累计 7–8 月）
- [x] Evidence 只累计新 unique observation（proposal-lifecycle.test + pipeline.test）

## B. Impact — 全部 PASS（kernel.test 17 项）

- [x] required dependency loss（T1）
- [x] confirmed ANY backup（T2）
- [x] confirmed ALL failure（T3）
- [x] proposal .999 不产生 must_change（T4）
- [x] multi-hop propagation（T5）
- [x] cycle termination（T6 + 60 节点大环）
- [x] unconfirmed group => needs_review（T7）
- [x] criticality unknown => needs_review（T8）
- [x] retired edge ignored（T9）
- [x] reactivated edge active（T10）
- [x] multi-disable scenario（T11）
- [x] payment 不传播到 recovery/access（T12）

原则：

> must_change false positive = 0（must_change 仅来自 required 边失效或 confirmed Group 失败）

## C. Crypto — Node 侧全 PASS；跨端互操作待工具链

- [x] `.depmap` wrong password fail（depmap.test）
- [x] tag tamper fail
- [x] header tamper fail（含 in-bounds KDF 参数篡改 → AAD 失配）
- [x] malicious KDF bounds rejected before Argon2（含 <200ms 快速失败断言）
- [x] Golden derivedKey fixed（复现 66c4be…0c86）
- [x] Golden ciphertext fixed
- [x] Golden tag fixed
- [ ] 至少 reference ↔ Android 互操作（Kotlin 测试代码就绪；需 B1 工具链）
- [ ] Harmony 实现加入后互操作（Argon2id ArkTS 移植待做）
- [ ] iOS 实现加入后互操作（需 B3 macOS）

## D. Parser — 全部 PASS（wechat.test 18 项）

- [x] normal fixture
- [x] BOM
- [x] GBK/GB18030
- [x] header offset
- [x] refund（退款不参与周期识别）
- [x] malformed（4 坏行计数，好行不受影响）
- [x] same-amount-twice
- [x] recurring（monthly 置信度 >0.7）
- [x] non-recurring（判 null）
- [x] duplicate import 1–6 / 1–8 只累计新增

## E. Platform — 源码完成，编译验证被外部环境阻塞

### Android
- [x] IMPLEMENTED
- [ ] COMPILED（需 B1：JDK17 + Android SDK）
- [ ] TESTED（golden 测试 TEST READY 未运行）
- [ ] encrypted DB evidence（需真机 + B10 基座）
- [ ] lock evidence（需真机）
- [x] release config（工程级；signing 材料属 B4）

### HarmonyOS
- [x] IMPLEMENTED
- [ ] COMPILED（需 B2：DevEco）
- [ ] TESTED
- [ ] ArkData encrypted evidence（需设备）
- [ ] HUKS/auth evidence（需设备）
- [x] release config（工程级）

### iOS
- [x] IMPLEMENTED
- [ ] COMPILED（需 B3：macOS/Xcode）
- [ ] TESTED（XCTest 就绪，XCTSkip 占位）
- [x] Keychain/LocalAuthentication（源码）
- [x] release config（工程级）

## F. Product Flow — synthetic 全流程 PASS；UI 源码完成

- [x] 创建银行卡/微信/服务节点（pipeline.test + nodes 页）
- [x] 导入微信账单（ImportFlow 三段式）
- [x] Node Resolution（begin/resolveMerchant/finalize）
- [x] Dependency Proposal 分别确认（ConfirmationService.acceptProposal）
- [x] Group Proposal 确认/拒绝（detectGroupProposals/accept/reject）
- [x] simulateScenario（kernel）
- [x] Action Checklist（impact-result）
- [x] 原始注销动作最后（kernel checklist 强制 + UI）
- [x] 中文 UI（11 页 .uvue 源码；编译验证属 B10）

## G. Real Data — NOT_RUN

- Correctness Gate: **NOT_RUN**（无真实账单；CLI 已备好 `core/scripts/validate-real-bill.ts`）
- Value Gate: **NOT_RUN**

禁止 synthetic 测试冒充 real-data PASS —— 本文件如实标注。
