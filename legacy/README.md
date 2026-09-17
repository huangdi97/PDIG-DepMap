# legacy/ — PDIG Legacy Reference

> **这个目录不是产品代码。**
> 它是被冻结的旧实现的**索引页**，用于回答"当年是怎么做的"。

---

## 1. 为什么存在

PDIG 曾以 **uni-app x / UTS / DCloud** 作为三端统一技术栈。
在 2026-09-15 用户做出最终技术决策：**彻底退出该技术栈**，
改为 Android(Kotlin/Compose) / iOS(Swift/SwiftUI) / HarmonyOS(ArkTS/ArkUI) 三端原生。

迁移**不是**"大重写然后希望没问题"。旧实现被保留为
**BEHAVIOR ORACLE**，用来回答所有"到底应该是什么行为"的问题。

---

## 2. 最后版本

| 项         | 值                                        |
| ---------- | ----------------------------------------- |
| Git HEAD   | `6d268c0`                                  |
| Git tag    | `v0.3.0-uniapp-reference`                  |
| Schema     | v3                                         |
| DEPMAP     | `DEPMAP_CONTAINER_V1`（formatVersion 1）    |
| 测试       | 453 passed / 43 files                      |
| 冻结清单   | `LEGACY_REFERENCE_MANIFEST.md`（根目录）    |

---

## 3. 代码实际在哪里

**没有把源码搬进本目录。** 原因：搬迁会打断 Git 历史（§7「不要为了目录漂亮破坏 Git 历史」）。
Legacy 源码仍在原位，通过文档标识为 reference：

| 路径                       | 内容                                                |
| -------------------------- | --------------------------------------------------- |
| `core/`                    | TypeScript 领域核心 + 全部测试（**BEHAVIOR ORACLE**） |
| `app/`                     | uni-app x UI（24 页 / 30 `.uvue`）+ 5 个 UTS 插件     |
| `platforms/`               | 旧路线的原生验证层（crypto / SQLCipher / Keystore / HUKS 桥） |
| `core/tests/fixtures/`     | 30 个原始输入 fixture（CSV / OFX / QFX）             |
| `app/theme/tokens.uts`     | 设计 token 源（已抽出为 `spec/ui/design-tokens.json`） |
| `app/services/rules.uts`   | 纯规则镜像（已审计，见 `LEGACY_BEHAVIOR_CORRECTIONS.md`） |

> `platforms/` 里的 Kotlin / ArkTS / Swift 文件是**旧路线的产物**，
> 它们服务于 UTS 桥接架构，**不是** Native SDK 客户端。
> 其中的 crypto 实现可作为参考，但必须重新审计后才能进 Native 工程。

---

## 4. 不可继续开发

**禁止**：

- 给 Legacy 增加新业务功能
- 把 Legacy 当作 Production 未来
- 让 Legacy 参与 Cutover 后的 Production build

**允许**：

- 生成 fixture
- 作为 differential test 的 oracle
- 修复迁移中发现的 Canonical Spec 模糊问题
- 修复 Reference 自身明显 bug（记录到 `LEGACY_BEHAVIOR_CORRECTIONS.md`）

---

## 5. 如何查看

```bash
# 冻结点
git show v0.3.0-uniapp-reference --stat

# 旧核心
ls core/src/domain core/src/services core/src/repositories

# 旧 UI 信息架构
ls app/pages

# 旧 fixture
ls core/tests/fixtures
```

---

## 6. 如何使用 Oracle fixtures

Legacy 的价值在于**能对任意输入给出权威输出**。做法：

```bash
cd core
# 1. 跑全量测试，确认 oracle 是绿的（冻结基线为 453/453）
npm test

# 2. 针对某个具体语义生成 expected（示例：readiness 纯规则）
npx vitest run tests/services/plan-readiness-freeze.test.ts

# 3. 生成 fixture（脚本已存在）
node scripts/generate-fixtures.ts
node scripts/generate-golden.ts
```

生成的平台中立 fixture 放在仓库根的 `fixtures/`，
期望值放 `conformance/expected/`，登记到 `conformance/CONFORMANCE_MANIFEST.json`。

**规则**：一旦 fixture 生成并确认，**不要随意重新生成 expected**。
期望行为的变更必须先改 `spec/`（§51）。

---

## 7. 相关文档

| 文档                                   | 内容                              |
| -------------------------------------- | --------------------------------- |
| `LEGACY_REFERENCE_MANIFEST.md`         | 冻结身份卡（版本 / 测试 / 页面 / 向量） |
| `LEGACY_BEHAVIOR_CORRECTIONS.md`       | 发现的真实缺陷与判定              |
| `docs/ADR_NATIVE_MIGRATION.md`         | 为什么转原生、tradeoff、cutover 条件 |
| `spec/README.md`                       | Canonical Spec 的法律地位          |
| `GOAL_PDIG_NATIVE_MIGRATION.md`        | 迁移总纲（N0–N7 阶段）             |
