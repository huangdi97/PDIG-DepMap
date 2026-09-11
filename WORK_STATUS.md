# WORK_STATUS.md

> 本文件由 ZCode/GLM 在执行过程中持续更新。不要删除历史关键结论。

## Current

- Phase: PHASE 15 — Final audit（PHASE 0–14 全部执行完毕）
- Status: CORE_ALL_GREEN / PLATFORMS_SOURCE_COMPLETE / BUILDS_BLOCKED_BY_ENV
- Canonical design read: YES
- Repo initialized: YES（git，小步提交）
- Shared TypeScript test harness: YES（vitest + tsc，106/106 PASS）
- uni-app x app scaffold: YES（11 页 + 5 UTS 插件，源码完成未编译）

## Platform Matrix

| Platform | IMPLEMENTED | COMPILED | TESTED | DEVICE_VERIFIED | STORE_READY |
|---|---|---|---|---|---|
| Android | YES | NO | NO | NO | NO |
| HarmonyOS | YES | NO | NO | NO | NO |
| iOS | YES | NO | NO | NO | NO |

（COMPILED/TESTED 被外部工具链缺失阻塞：B1–B3、B10，见 BLOCKERS.md）

## Core Gates

- Schema v1: **TESTED**（migration 幂等/回滚/重启安全 + UNIQUE/CHECK，8 tests）
- Migration: **TESTED**
- Impact Kernel: **TESTED**（T1–T12 + 附加，17 tests）
- `.depmap` V1: **TESTED（Node 侧 28 tests 含 Golden）**；Android/iOS 侧 TEST READY 未运行
- WeChat Parser: **TESTED**（10 fixtures，18 tests）
- Fingerprint: **TESTED**（HMAC + canonical row + 1–6/1–8 月跨会话去重）
- Node Resolver: **TESTED**（alias/exact/fuzzy/ambiguous/unresolved + 支付方式解析）
- DependencyProposal: **TESTED**（UPSERT/accepted 不重问/rejected 重提门槛）
- DependencyGroupProposal: **TESTED**（canonical key/拒绝重提/确认建组）
- Synthetic E2E: **TESTED**（9 integration tests：导入→解析→确认→Group→simulate→checklist）
- Correctness Gate: NOT_RUN（无真实账单）
- Value Gate: NOT_RUN（无真实账单）

## Last completed

- PHASE 14：docs/ 全套 10 文档 + README + STORE_RELEASE_CHECKLIST + REAL_DATA_VALIDATION + 验证 CLI
- PHASE 13/12/11：iOS SPM/Swift、HarmonyOS json5/ArkTS、Android Gradle/Kotlin（源码级）
- PHASE 10：uni-app x 中文 UI 11 页 + UTS 插件
- PHASE 9：ImportFlow 三段式 + 确认服务 + E2E
- PHASE 6–8：Resolver + Proposal/GroupProposal 生命周期
- PHASE 5：微信 Parser + 12 fixtures + 指纹 + 周期识别
- PHASE 4：平台安全适配（接口 + Kotlin/ArkTS/Swift 源码）
- PHASE 3：`.depmap` V1 + Golden Vector
- PHASE 2：Impact Kernel（先失败测试后实现）
- PHASE 1：Schema v1 + repositories
- PHASE 0：审计 + git + 测试骨架

## Current failures

- 无失败测试。未执行的测试均为外部工具链缺失（如实标记 NO/NOT_RUN，不虚报）。

## External blockers

见 `BLOCKERS.md`（B1–B13）。

## Next

1. 解除 B1（JDK17+Android SDK）→ 运行 Android golden 互操作测试
2. 解除 B10（HBuilderX）→ app 编译 + Android 真机 security spike
3. 解除 B13（真实账单）→ 双 Gate
4. 上架资料定稿（STORE_RELEASE_INPUTS.md）
