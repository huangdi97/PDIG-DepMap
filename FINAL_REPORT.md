# FINAL_REPORT.md — MVP01 执行报告

> 执行：ZCode / GLM-5.3-Flash · 2026-09-06 → 2026-09-11
> 范围：GOAL_MVP01 PHASE 0–15 · 依据 AGENTS.md / CANONICAL_DESIGN.md / PLATFORM_DECISION.md / MVP_ACCEPTANCE.md

## 1. 平台状态矩阵（无伪完成）

### 共享 Core（TypeScript，Node 22.15.0）

```text
IMPLEMENTED      = YES（domain/schema/repositories/impact/parser/fingerprint/resolver/services/crypto/adapters）
COMPILED         = YES（tsc --noEmit 0 error）
TESTED           = YES（106/106 PASS，vitest）
DEVICE_VERIFIED  = N/A（纯 TS，Node 环境即目标运行时）
STORE_READY      = N/A
```

证据：`core/tests/**`（8 个测试文件）；`npm test` 输出 106 passed；`npm run typecheck` 0 error。

### Android

```text
IMPLEMENTED      = YES（Kotlin 安全层 4 模块 + Schema DDL + .depmap V1 + Gradle 工程 + golden 测试代码）
COMPILED         = NO（本机无 JDK17 / Android SDK / Gradle —— 外部 Blocker B1）
TESTED           = NO（golden 测试 TEST READY 未运行）
DEVICE_VERIFIED  = NO
STORE_READY      = NO（签名/账号缺 —— B4/B5）
```

### HarmonyOS

```text
IMPLEMENTED      = YES（ArkData+HUKS 适配、app.json5/module.json5、UTS 桥）
COMPILED         = NO（无 DevEco Studio / HarmonyOS SDK —— B2）
TESTED           = NO
DEVICE_VERIFIED  = NO
STORE_READY      = NO（B6/B7）
```

### iOS

```text
IMPLEMENTED      = YES（Swift SQLCipher+Keychain+LA 适配、SPM、golden skip-tests）
COMPILED         = NO（无 macOS/Xcode —— B3，预期内）
TESTED           = NO（XCTest 就绪，含 XCTSkip 占位）
DEVICE_VERIFIED  = NO
STORE_READY      = NO（B8/B9）
```

### uni-app x 应用层

```text
IMPLEMENTED      = YES（11 页中文 UI + 5 个 UTS 插件 + manifest/pages.json）
COMPILED         = NO（无 HBuilderX / uni-app x CLI —— B10）
TESTED           = NO（依赖编译工具链）
DEVICE_VERIFIED  = NO
```

## 2. 验收对照（MVP_ACCEPTANCE）

- **A Core Correctness**：11/11 达成（对应测试全绿）
- **B Impact**：12/12 PASS + 确定性顺序 + checklist 末位 + canonical fixture A/B/B2
- **C Crypto**：wrong-password/tag/header tamper/malicious-bounds/Golden 三值 = PASS；
  reference↔Android/iOS/Harmony 互操作 = 向量与测试代码就绪，**未运行**（对应平台工具链缺失）
- **D Parser**：10/10 PASS（normal/BOM/GBK/header-offset/refund/duplicate/same-amount/malformed/recurring/non-recurring）
- **E Platform**：三端 IMPLEMENTED=yes，COMPILED/TESTED/DEVICE_VERIFIED=no（如实）
- **F Product Flow**：synthetic 端到端 PASS（导入→去重→解析→确认→Group→simulate→checklist→原始操作最后）；
  UI 中文、answer-oriented 首页（源码级）
- **G Real Data**：Correctness Gate = NOT_RUN；Value Gate = NOT_RUN（无真实账单；CLI 已备好）

## 3. 关键实现语义（与 CANONICAL 一致）

- Dependency 存在即用户确认；logical key UNIQUE；retired 同 id 复活
- criticality 只有 required/unknown；机器不产生 required
- Group/GroupProposal 均有确认生命周期；groupKey canonical（成员乱序同组）
- Proposal UPSERT；accepted 不重问；rejected 需新观测 ≥3 + ≥1 完整周期才重提
- Observation/CanonicalEvent 只在导入会话内存；指纹 HMAC-SHA256(fpSecret, source:txnId)，
  回退 canonicalRow+ordinal；UNIQUE(source,fingerprint)
- Impact：状态键 (nodeId, capability)；wave-BFS 防环；needs_review 不产生 must_change；
  Proposal 任何置信度不参与确定性传播；must_change 只来自 required 边或 confirmed Group 失败
- `.depmap` V1：Argon2id 65536/3/1 + AES-256-GCM + RFC8785 JCS AAD；边界先于 KDF；
  Golden Vector 冻结（Node 已复现）

## 4. 外部 Blocker（详见 BLOCKERS.md）

B1 JDK17+Android SDK ｜ B2 DevEco/鸿蒙 SDK ｜ B3 macOS+Xcode ｜ B4 Android keystore
B5 Google Play 账号 ｜ B6 Huawei 身份 ｜ B7 鸿蒙签名 ｜ B8 Apple 账号 ｜ B9 iOS 签名
B10 HBuilderX/uni-app x 编译工具链 ｜ B11 最终 bundle id ×3 ｜ B12 隐私政策 URL ｜ B13 真实微信账单

这些 blocker 不阻塞本报告以上全部已完成工作；解除任一后的验证路径已在对应文档写明。

## 5. 下一步（按优先级）

1. 用户在机器上安装 JDK17 + Android cmdline-tools → `cd platforms/android && gradle test`（golden 互操作）
2. 安装 HBuilderX → 导入 `app/` → 自定义基座真机运行 → Android security spike（SQLCipher 离线不可读/锁）
3. macOS 环境 → `cd platforms/ios && swift test` → Xcode 工程收尾
4. 真实微信账单 → `core/scripts/validate-real-bill.ts` → 双 Gate
5. 全部绿后进入上架资料定稿（STORE_RELEASE_INPUTS.md）
