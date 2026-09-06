# MVP_ACCEPTANCE.md

## A. Core Correctness

必须全部满足：

- [ ] Schema v1 migration 可重复执行
- [ ] Dependency logical key UNIQUE
- [ ] retired Dependency 可 re-activate 且 id 不变
- [ ] Group canonical groupKey 去重
- [ ] Proposal UPSERT 正确
- [ ] rejected 有新 Evidence 才重提
- [ ] GroupProposal 生命周期正确
- [ ] Observation 不持久化
- [ ] CanonicalEvent 不持久化
- [ ] Fingerprint 重复导入测试通过
- [ ] Evidence 只累计新 unique observation

## B. Impact

至少以下全部 PASS：

- [ ] required dependency loss
- [ ] confirmed ANY backup
- [ ] confirmed ALL failure
- [ ] proposal .999 不产生 must_change
- [ ] multi-hop propagation
- [ ] cycle termination
- [ ] unconfirmed group => needs_review
- [ ] criticality unknown => needs_review
- [ ] retired edge ignored
- [ ] reactivated edge active
- [ ] multi-disable scenario
- [ ] payment 不传播到 recovery/access

原则：

> must_change false positive = 0

## C. Crypto

- [ ] `.depmap` wrong password fail
- [ ] tag tamper fail
- [ ] header tamper fail
- [ ] malicious KDF bounds rejected before Argon2
- [ ] Golden derivedKey fixed
- [ ] Golden ciphertext fixed
- [ ] Golden tag fixed
- [ ] 至少 reference ↔ Android 互操作
- [ ] Harmony 实现加入后互操作
- [ ] iOS 实现加入后互操作

## D. Parser

- [ ] normal fixture
- [ ] BOM
- [ ] GBK/GB18030
- [ ] header offset
- [ ] refund
- [ ] malformed
- [ ] same-amount-twice
- [ ] recurring
- [ ] non-recurring
- [ ] duplicate import 1–6 / 1–8 只累计新增

## E. Platform

### Android
- [ ] IMPLEMENTED
- [ ] COMPILED
- [ ] TESTED
- [ ] encrypted DB evidence
- [ ] lock evidence
- [ ] release config

### HarmonyOS
- [ ] IMPLEMENTED
- [ ] COMPILED
- [ ] TESTED
- [ ] ArkData encrypted evidence
- [ ] HUKS/auth evidence
- [ ] release config

### iOS
- [ ] IMPLEMENTED
- [ ] COMPILED（需 macOS）
- [ ] TESTED
- [ ] Keychain/LocalAuthentication
- [ ] release config

## F. Product Flow

- [ ] 创建银行卡/微信/服务节点
- [ ] 导入微信账单
- [ ] Node Resolution
- [ ] Dependency Proposal 分别确认
- [ ] Group Proposal 确认/拒绝
- [ ] simulateScenario
- [ ] Action Checklist
- [ ] 原始注销动作最后
- [ ] 中文 UI

## G. Real Data

只有使用用户本地真实账单后才可填写：

- Correctness Gate: NOT_RUN / PASS / FAIL
- Value Gate: NOT_RUN / PASS / FAIL

禁止 synthetic 测试冒充 real-data PASS。
