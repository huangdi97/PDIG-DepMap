# Native Migration（N0–N7）

> 面向仓库读者的迁移现状入口。逐格细节见仓库根部的
> `NATIVE_MIGRATION_STATUS.md`、`NATIVE_PARITY_MATRIX.md`、`CROSS_PLATFORM_CONFORMANCE_MATRIX.md`。
>
> 状态枚举：`PASS` / `FAIL` / `BLOCKED` / `NOT_RUN` / `PARTIAL_WITH_REPORT`。
> **`NOT_RUN` 不等于 `PASS`**：未执行的能力一律记为未执行，不得以"理论支持"充当已验证。

## 1. 迁移策略

PDIG/DepMap 采用「**规范先行、三端原生**」的迁移路径：

1. `spec/` 维护**机器可读**的 Canonical 规范（枚举 / 实体 / 关系 / 状态机 / 错误码 / Schema / 安全 / UI）。
2. `tools/codegen` 由 spec 生成三端枚举（Kotlin / Swift / ArkTS），并用 `--check` 做漂移门禁。
3. `fixtures/`（**91 个一致性用例** + 28 个输入 fixture + sha256 清单）作为跨端共同判据。
4. 三端各自实现，通过 `tools/conformance/run.mjs` 输出可比对的机器报告。
5. 旧版 uni-app x / UTS 实现**冻结为行为 Oracle**（tag `v0.3.0-uniapp-reference`），
   只作为语义参照，**不作为实现来源**，也不参与发布。

## 2. 阶段状态

| Phase | 名称 | 状态 | 关键证据 |
| --- | --- | --- | --- |
| N0-A | 恢复仓库现场 | **PASS** | 全套 Git 审计；工作树 clean |
| N0-B | Legacy 冻结 | **PASS** | tag `v0.3.0-uniapp-reference` + manifest + README |
| N0-C | Canonical Spec | **PASS** | `spec/` 机器可读，八类规范齐备 |
| N0-D | Codegen + Gate | **PASS** | 三端 generated 产物；`--check` PASS |
| N0-E | Golden Fixtures | **PASS** | 91 用例 + 28 输入 fixture + sha256 清单 |
| N0-F | Conformance Harness | **PASS** | `node tools/conformance/run.mjs` |
| N1 | Android 垂直切片 | **PASS** | 设备端核心链路 E2E `41/41 PASS / 0 FAIL` |
| N2 | Android 全量对等 | **PARTIAL_WITH_REPORT** | 对等矩阵 **62 / 73**，11 项未关闭 |
| N3 | HarmonyOS 全量对等 | **ACTIVE / 0 / 73** | 工程可构建并产出 HAP；一致性未执行 |
| N4 | iOS 全量对等 | **NOT_STARTED** | 仅 codegen 产物；构建 `BLOCKED_BY_MACOS` |
| N5 | 跨端一致性 | **PARTIAL_WITH_REPORT** | Android 91/91；Harmony / iOS 未执行 |
| N6 | Legacy 切流 | **NOT_STARTED** | 前置条件（三端对等）未达成 |
| N7 | 原生生产 RC | **NOT_STARTED** | — |

## 3. Android（N1 / N2）——`CORE_FROZEN / MAINTENANCE_ONLY`

- `N1_ANDROID_VERTICAL_SLICE` = **PASS**：核心垂直链
  `import → proposal → reality → impact → changeplan → done → verified`
  在真实设备上端到端跑通，崩溃 0。
- `N2_ANDROID_FULL_PARITY` = **PARTIAL_WITH_REPORT（62 / 73）**。
  计数口径：分母 = 矩阵实际行数 73；"已完成" = `TESTED` 及以上
  （`IMPLEMENTED` / `PARTIAL` / `BLOCKED` / `NOT_RUN` 均不计入）。
- `ANDROID_PRODUCTION_RELEASE_READY` = **BLOCKED_BY_PRODUCTION_SIGNING**（缺生产签名密钥）。
- `ANDROID_NATIVE_CORE_HANDOFF` = **PASS**：仅表示 Android 可作为 Harmony N3 的原生参照，
  **不替代** N2，也**不替代**发布就绪判定。
- 一致性：`:conformance:run` **pass=91 / fail=0 / total=91**；`:core:test` **71 PASSED / 0 FAILED**。
- 冻结约束：不再为提升对等分数新增功能；剩余 11 格进入 N2 Backlog。

## 4. HarmonyOS（N3）——当前活跃

| 子项 | 状态 | 说明 |
| --- | --- | --- |
| `HARMONY_BUILD` | **PASS** | hvigor 全清重建成功，产出 `entry-default-unsigned.hap`（60,133 B） |
| `HARMONY_DOMAIN` | **PARTIAL_WITH_REPORT** | 首个纯 ArkTS Domain（`Relations.ets`）已编译并打包进 HAP |
| `HARMONY_ARKUI` | **PARTIAL_WITH_REPORT** | Stage Model 骨架 + 占位页面 |
| `HARMONY_DEPMAP` | **BLOCKED_BY_NATIVE_VERIFICATION** | 托管 API（`cryptoFramework` / `HUKS`）未提供 Argon2 —— 已证据级排除；**原生路径（NDK + PHC 参考实现 + NAPI）已打通**：主机侧 Golden Vector 逐字节复现、OHOS arm64 `.so` 编译通过；仍缺设备上复验，故**未**记为 PASS。详见 `HARMONY_ARGON2_FEASIBILITY.md` |
| `HARMONY_RUNTIME_E2E` | **RUNTIME_NOT_RUN** | 无可用的模拟器镜像（`hdc list targets` 为空） |
| 对等计数 | **0 / 73** | 上述三格未达 `TESTED` 及以上，按口径不计入 |

> `DEPMAP_CONTAINER_V1` 的密码学参数（Argon2id v19 / AES-256-GCM / JCS 规范化）是**兼容性闸门**，
> 不允许为适配单一平台而降级或修改容器格式。可行性论证见 `HARMONY_ARGON2_FEASIBILITY.md`。

## 5. iOS（N4）——未开始

- 现状：仅存在 codegen 产物；**未开始**实现对等能力。
- 阻塞：`BLOCKED_BY_MACOS`（构建与运行时验证需要 macOS 环境）。
- 记录口径：与本仓库所有未执行项一致，记为 `NOT_STARTED` / `NOT_RUN`，**不记为 PASS**。

## 6. 跨端一致性（N5）

| 平台 | 一致性结果 |
| --- | --- |
| Android | **91 / 91 PASS**（实跑复验） |
| Harmony | **NOT_RUN**（0 条执行；87 条 `notImplemented` / 4 条 `blocked`） |
| iOS | 未开始 |

一致性链路由五个阶段组成，任一阶段失败即整体失败：

`CODEGEN GATE` → `FIXTURE INTEGRITY`（sha256） → `ORACLE SELFCHECK` → `PLATFORM REPORTS` → `SUMMARY`

## 7. 已知缺陷（公开跟踪）

- `fixtures/import/` 共 28 个文件中 **12 个**的 sha256 与 `CONFORMANCE_MANIFEST.json` 记录不一致，
  导致 `fixtureIntegrity` 子项 FAIL（91 个一致性用例本身全部 ok）。
- 已证明**非**历史净化引入（工作区文件 = 当前 commit blob = 净化前 blob，三者哈希完全一致）。
- 处置原则：**不擅自改写清单数值**。应由 `core/scripts/generate-conformance.ts` 重新生成并逐项核对后再封印。

## 8. 相关文档

- `NATIVE_MIGRATION_STATUS.md` —— 逐阶段状态（权威）
- `NATIVE_PARITY_MATRIX.md` —— 73 格逐格对等矩阵（权威）
- `CROSS_PLATFORM_CONFORMANCE_MATRIX.md` —— 跨端一致性矩阵
- `ANDROID_NATIVE_CORE_FREEZE.md` —— Android 核心冻结边界
- `HARMONY_N3_*` —— Harmony 阶段报告（基线审计 / 实现状态 / 一致性 / 运行时 / Argon2 可行性）
- `docs/CONFORMANCE.md`、`docs/DEPMAP_FORMAT.md` —— 一致性口径与容器格式
