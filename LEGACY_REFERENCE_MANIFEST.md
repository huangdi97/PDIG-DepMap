# LEGACY_REFERENCE_MANIFEST.md

**冻结日期**：2026-09-15
**冻结原因**：PDIG 最终技术决策 —— 彻底退出 uni-app x / UTS / DCloud，切换为 Android / iOS / HarmonyOS 三端原生。

> 本文件是 Legacy 实现的**唯一身份卡**。
> 冻结之后，Legacy **不再是 Production Future**，其正式角色变更为：
> **REFERENCE IMPLEMENTATION / BEHAVIOR ORACLE / FIXTURE GENERATOR / MIGRATION SOURCE / CONFORMANCE REFERENCE**。

---

## 1. 冻结基线

| 项                  | 值                                                                                                     |
| ------------------- | ------------------------------------------------------------------------------------------------------ |
| **Git HEAD**        | `7bc0ed323ea82ce98139acd14eabd040a1ea111e`（`7bc0ed3`）                                                |
| HEAD 提交信息       | `chore(build): ignore the agent workspace memory directory`                                            |
| **Branch**          | `feat/mvp03-living-graph`                                                                              |
| **Tag（本次新增）** | `v0.3.0-uniapp-reference`                                                                              |
| 既有 Tag（不覆盖）  | `v0.2.0-mvp02`、`v0.3.0-mvp03`                                                                         |
| 冻结时工作树        | **clean**（`git status --short -uall` = 0 行）                                                         |
| `git diff --check`  | PASS                                                                                                   |
| Git fsck            | 无损坏对象；仅 `refs/heads/feat/mvp03-living-graph` 有已知 trailingRefContent 噪音（本工作区已知现象） |

---

## 2. 版本与协议

| 项                     | 值                                           |
| ---------------------- | -------------------------------------------- |
| **App Schema Version** | `3`                                          |
| Schema 迁移链          | `[1, 2, 3]`                                  |
| **DEPMAP 协议**        | `DEPMAP_CONTAINER_V1`（`formatVersion = 1`） |
| Graph Payload          | `depmap-logical-graph` v`3`（可从 1/2 迁移） |
| Argon2id               | v19，65536 KiB / 3 / 1，key 32B              |
| AES-256-GCM            | nonce 12B，tag 16B                           |
| JCS                    | RFC 8785（受限值域）                         |

---

## 3. 测试基线（本机实测，2026-09-15）

```
npm test（core/，Node v22.22.2）
  Test Files  43 passed (43)
       Tests  453 passed (453)
    Duration  49.17s
```

**Oracle 状态 = GREEN。** 这是后续 differential testing 的期望值来源。

既有质量门（`core/package.json` scripts）：

| Gate                         | 说明                                                                    |
| ---------------------------- | ----------------------------------------------------------------------- |
| `npm run check`              | format + lint + typecheck + 全部测试 + architecture + network + secrets |
| `npm run check:full`         | milestone 前收口                                                        |
| `npm run check:invariants`   | INV1..INV21 集中不变量套件                                              |
| `npm run check:contract`     | Adapter / Repository 契约                                               |
| `npm run check:property`     | property-based                                                          |
| `npm run check:db-integrity` | DB 完整性                                                               |
| `npm run check:architecture` | circular deps = 0                                                       |
| `npm run check:network`      | 网络原语 = 0                                                            |
| `npm run check:secrets`      | secret scan = 0                                                         |
| `npm run check:ui`           | UI 静态门（30 `.uvue` / 24 pages / 5 components / 34 色）               |
| `npm run check:uts`          | UTS 编译门（15 插件编译，非原生构建）                                   |
| `npm run test:perf`          | 性能 smoke                                                              |
| `npm run test:stability`     | 稳定性（×3 复跑）                                                       |

---

## 4. MVP 状态快照

| Milestone        | 状态                  | 说明                                                                                                                                                                                              |
| ---------------- | --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **MVP01**        | PASS                  | 手动建对象、手动声明支付关系、影响模拟、ChangePlan、Timeline、来源管理                                                                                                                            |
| **MVP02**        | PASS                  | Schema v2 / SourceInstance / EvidenceSourceAdapter / source-scoped fingerprint / 多源 Evidence / RelationDefinitionRegistry / WeChat Adapter / Generic CSV / OFX-QFX / regression / quality gates |
| **MVP03**        | PASS                  | Living Graph & Change Safety：GraphRevision、Rebase、Readiness、Coverage、RealityDrift、DiscoveryCandidate、ScenarioTemplate、Timeline、Verification、Migration v2→v3                             |
| Production RC V1 | `PARTIAL_WITH_REPORT` | UI 产品包受 B10（DCloud 账号）阻断                                                                                                                                                                |

MVP03 Gate 级结论（A–Q）全部 PASS（见 `WORK_STATUS.md`）。

---

## 5. 已实现 UI 页面（24 pages / 30 `.uvue`）

风险提示：**这些页面是 uni-app x `.uvue`，不是原生 UI。**
信息架构可复用，页面代码**不可复用**。

| 分组        | 页面                                          |
| ----------- | --------------------------------------------- |
| 引导 / 解锁 | `onboarding`、`unlock`                        |
| 首页        | `home`                                        |
| 场景        | `scenarios`                                   |
| 计划        | `plans`、`plan-create`、`plan-detail`         |
| 时间轴      | `timeline`                                    |
| 提案        | `proposals`、`group-proposals`                |
| 变更发现    | `drift`、`candidates`                         |
| 图与对象    | `nodes`、`node-detail`、`declare-relation`    |
| 模拟        | `simulate`、`impact-result`                   |
| 来源与导入  | `sources`、`import-wechat`、`node-resolution` |
| 备份        | `backup`                                      |
| 设置 / 隐私 | `settings`、`privacy`、`about`                |

组件（5）：`dp-button`、`dp-card`、`dp-chip`、`dp-row`、`dp-state`
设计 token：`app/theme/tokens.uts`（已抽取为 `spec/ui/design-tokens.json`）
纯规则镜像：`app/services/rules.uts`（已审计，见 `LEGACY_BEHAVIOR_CORRECTIONS.md` LC-003/004/005）

---

## 6. 已实现 Adapter（3）

| adapterId          | version | sourceKind       | coverageMode   | authoritativeFor |
| ------------------ | ------- | ---------------- | -------------- | ---------------- |
| `wechat_statement` | 1       | `statement_file` | `event_stream` | `[]`             |
| `generic_csv`      | 1       | `statement_file` | `event_stream` | `[]`             |
| `ofx_qfx`          | 1       | `statement_file` | `event_stream` | `[]`             |

**全部 `event_stream` ⇒ absence 永不产生现实否定。** 这是跨端必须保持的语义。

---

## 7. Golden Vector

`DEPMAP_CONTAINER_V1` 固定向量（`core/src/crypto/golden.ts`）：

| 项            | 值                                                                                     |
| ------------- | -------------------------------------------------------------------------------------- |
| password      | `depmap-test`（精确 UTF-8）                                                            |
| salt (hex)    | `00112233445566778899aabbccddeeff`（16B）                                              |
| nonce (hex)   | `a1b2c3d4e5f60718293a4b5c`（12B）                                                      |
| plaintext     | `{"app":"depmap","schemaVersion":1,"nodes":[],"dependencies":[]}`                      |
| derivedKeyHex | `66c4bec7f5e98856747d7b41d0a021bdc092d5e12d492852bd80647bd0ff0c86`                     |
| ciphertextB64 | `KNSpbKK6waj4En3ADgeBB74H96Q7GCoBmNffeOG5QSQ9XJwx4LoJCQ0j8lEinA7GN85U6JwaVMqhAkqWDdG7` |
| tagB64        | `5qpABhovPbNet1q2GNEhkg==`                                                             |

**该向量已成为三端 Conformance Fixture。** 三端必须产出完全相同的
`derivedKey` / `ciphertext` / `tag`，并能互相解密。

---

## 8. Migration 版本

| 链              | 版本                      |
| --------------- | ------------------------- |
| DB Schema       | v1 → v2 → v3              |
| Graph Payload   | v1 → v2 → v3              |
| legacy 来源实例 | `legacy-wechat-statement` |
| legacy adapter  | `wechat_statement` v1     |

---

## 9. Legacy 的正式角色（冻结后）

**允许**（§4 / §5）：

1. 作为 **BEHAVIOR ORACLE** —— 为任意 input 产 expected output
2. 作为 **FIXTURE GENERATOR** —— 生成平台中立 JSON fixture
3. 作为 **MIGRATION SOURCE** —— 提供迁移前的真实数据结构
4. 作为 **CONFORMANCE REFERENCE** —— 跨端 differential test 的基准
5. 修复迁移中发现的 **Canonical Spec 模糊问题**
6. 修复 Reference 自身**明显 bug**（记录在 `LEGACY_BEHAVIOR_CORRECTIONS.md`）

**禁止**：

- 继续给 Legacy 增加新业务功能
- 把 Legacy 当作 Production 未来
- 让 Legacy 参与 Production build pipeline

---

## 10. 保护规则

工作区已知现象（务必遵守）：

- `.git/refs/heads/**` 的 loose ref 会被外部进程回收；
  `git commit` / `git update-ref` 可能返回 0 却不推进 HEAD。
- 规避方式：`git add` → `git write-tree` → `git commit-tree -p <parent>` →
  把分支写入 **`.git/packed-refs`**；每次提交后校验 `git rev-parse HEAD`。

**绝对禁止**：

```
git reset --hard
git clean -fd
git checkout .
git restore .
git push --force
history rewrite
```

---

## 11. 归档状态

| 项                           | 状态                                             |
| ---------------------------- | ------------------------------------------------ |
| 代码位置                     | **仍在主线**（`app/`、`core/`、`platforms/`）    |
| 是否参与 Production build    | **尚未 CUTOVER；当前仍参与，Cutover 后必须为 0** |
| Git tag                      | `v0.3.0-uniapp-reference`                        |
| Legacy Production dependency | **尚未清零**（Cutover 条件未满足）               |
| 删除                         | **禁止**（在三端 parity 达成前绝不删除）         |

Cutover 条件见 `GOAL_PDIG_NATIVE_MIGRATION.md` §211 与 `NATIVE_MIGRATION_ACCEPTANCE.md`。
