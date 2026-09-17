# ADR：从 uni-app x / UTS / DCloud 迁移到三端原生

- **状态**：已接受（2026-09-15）
- **决策者**：用户
- **影响范围**：PDIG 全部客户端与 Production 构建链路

---

## 背景

PDIG 此前以 **uni-app x + UTS** 作为三端统一底座：

- 业务核心：TypeScript（`core/`，453 测试）
- UI：uni-app x `.uvue`（24 页） + 5 个 UTS 原生插件
- 打包：HBuilderX / DCloud Cloud Build

该路线存在三个结构性问题：

1. **产品级构建被外部账号闸门卡住**。UI 无法编译（`UI_BUILD_READY` 依赖 B10：
   DCloud 账号登录），导致"源码完成"无法变成"可安装产物"。
2. **存在两份真相**。Core（TS，有测试）与 App 侧规则镜像（`app/services/rules.uts`）
   并行演进，已产生实际漂移 —— 例如 UI 暴露了 runtime registry 不承认的 `bound_to`
   （见 `LEGACY_BEHAVIOR_CORRECTIONS.md` LC-003）。
3. **HarmonyOS 原生体验与上架要求**无法由跨端框架充分满足。

---

## 决策

**彻底退出 uni-app x / UTS / DCloud Production 技术栈。**

| 平台      | 技术栈                                                                 |
| --------- | ---------------------------------------------------------------------- |
| Android   | Kotlin + Jetpack Compose + Navigation Compose + Coroutines/Flow + SQLCipher + Keystore + BiometricPrompt |
| iOS       | Swift + SwiftUI + NavigationStack + async/await + Observation + SQLCipher + Keychain + LocalAuthentication |
| HarmonyOS | ArkTS + ArkUI + Stage Model + ArkData relationalStore + HUKS + Harmony 认证 |

Production 禁止依赖：uni-app、uni-app x、`.uvue` runtime、UTS runtime、
HBuilderX、DCloud Cloud Build / Native SDK / 登录 / 云打包 / 运行时。

**旧实现保留为 LEGACY_REFERENCE / BEHAVIOR_ORACLE，Cutover 完成前绝不删除。**

---

## Tradeoff

### 为什么不再"修一修 uni-app x"

| 方案                       | 优点                       | 缺点                                        |
| -------------------------- | -------------------------- | ------------------------------------------- |
| 继续 uni-app x             | 复用现有 24 页 UI          | 构建闸门在外部账号；UI 与 Core 双真相；Harmony 原生能力受限 |
| **三端原生**               | 无外部构建闸门；平台原生感 | 三份实现，需要强 Spec + Conformance 约束      |
| Flutter / RN / Capacitor   | 一套代码                   | 不满足 HarmonyOS 原生上架要求（已否决）      |

### 接受的成本

- 三份客户端代码 → 用 **Canonical Spec + Codegen + Conformance** 约束漂移
- 迁移周期长 → 分 N0..N7 阶段推进，每阶段有判据
- 旧 UI 不可复用 → 只复用**信息架构与产品语义**，页面重写

### 拒绝的做法

- 不为"更现代"重写已可靠的 SQL / Crypto
- 不因为迁移引入网络库、分析 SDK、Crash SDK
- 不在迁移途中开启 MVP04 或任何新业务 Domain

---

## 执行顺序

```
SPEC → FIXTURE → TEST → PLATFORM IMPLEMENTATION → CONFORMANCE → PARITY → CUTOVER
```

**唯一业务真相源 = `spec/`。**
三端禁止自行设计业务规则；枚举一律 codegen 生成。

---

## Cutover 条件

只有以下**全部**满足，才能移除 DCloud/UTS/uni-app 的 Production 依赖：

1. Canonical Spec = SINGLE SOURCE OF TRUTH
2. Android / Harmony parity PASS；iOS source parity PASS
3. 跨端 Conformance PASS
4. Crypto Golden PASS
5. Migration PASS
6. Backup / Restore PASS
7. UI critical flow parity PASS
8. Security PASS
9. **Production build 不再依赖 DCloud / UTS / uni-app**

在条件满足前，`app/`、`core/`、`platforms/` **保持在主线**，
仅通过 `LEGACY_REFERENCE_MANIFEST.md` 与 `legacy/README.md` 标识为 reference。

---

## 兼容性承诺

- **`.depmap` 协议 `DEPMAP_CONTAINER_V1` 冻结不变**（除非发现明确 security bug）
- **App Schema 继续 v3**，不因原生重写回退到 v1
- **Native App 必须能读取现有 `.depmap`** —— 这是 Cutover 的核心要求
- 逻辑 Schema 跨端一致；物理 DDL 允许不同（SQLCipher vs ArkData）

---

## 相关文档

- `GOAL_PDIG_NATIVE_MIGRATION.md`
- `NATIVE_MIGRATION_STATUS.md`
- `NATIVE_MIGRATION_ACCEPTANCE.md`
- `LEGACY_REFERENCE_MANIFEST.md`
- `LEGACY_BEHAVIOR_CORRECTIONS.md`
- `spec/README.md`
