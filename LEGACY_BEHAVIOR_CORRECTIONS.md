# LEGACY_BEHAVIOR_CORRECTIONS.md

Legacy（uni-app x / TypeScript）实现中发现的**真实缺陷**记录。

> 原则（§216）：**不得盲目把旧 bug 当作 Canonical Behavior。**
> 每条都必须判断"Spec 本意是什么"，并有测试 / 文档 / 现有设计共同支撑。
> 判定顺序（§217）：用户产品原则 → Canonical Design → MVP Freeze Acceptance
> → Correctness Invariants → Tests → Legacy implementation。

**状态图例**：`CONFIRMED_DEFECT` / `SUSPECTED` / `NOT_A_DEFECT`

---

## LC-001 — `csv-utf8-bom.csv` 实际不含 BOM

| 项       | 内容                                                                                             |
| -------- | ------------------------------------------------------------------------------------------------ |
| 类型     | 测试覆盖空洞（fixture 名不副实）                                                                  |
| 状态     | **CONFIRMED_DEFECT**                                                                             |
| 位置     | `core/tests/fixtures/csv-utf8-bom.csv`                                                            |
| 证据     | 首 3 字节为 `64 61 74 65`（`date`），**不是** `EF BB BF`                                          |
| 影响     | `core/tests/sources/generic-csv.test.ts` 的 E2b「UTF-8 BOM 被剥离」在**无 BOM** 时也会通过 → **该路径从未被真正覆盖** |
| Spec 本意 | 解析器必须能剥离 BOM（`MappingProfile` 与 import pipeline 都声明支持 BOM）                        |
| 处置     | **Native 端必须用真实 BOM fixture 覆盖**。见下"替代 fixture"                                     |
| 替代     | `utf8-bom.csv`（BOM 在偏移 0，真实带 BOM）与 `header-offset.csv`（BOM 在偏移 2，前置 CRLF）        |
| 不得     | 不得因为"legacy 测试是绿的"就认为 BOM 剥离已验证                                                  |

---

## LC-002 — `csv-missing-required-column.csv` 并未缺失必需列

| 项       | 内容                                                                                       |
| -------- | ------------------------------------------------------------------------------------------ |
| 类型     | 命名误导                                                                                    |
| 状态     | **CONFIRMED_DEFECT（文档/命名层）**                                                         |
| 位置     | `core/tests/fixtures/csv-missing-required-column.csv`                                       |
| 证据     | 文件实际含完整 4 列 `date,counterparty,amount,currency`；真实用途是**坏行保守拒绝**（1 行空 amount、1 行坏日期、1 行坏金额） |
| 影响     | 真正的"缺列"用例是 E3b，用的是 `csv-us-credit-card.csv` + 映射到不存在的 `NoSuchColumn`（抛 `mapping column not found`） |
| Spec 本意 | ①坏行必须被拒绝且不抛异常 ②映射到不存在的列必须明确报错                                      |
| 处置     | Native fixture 中把两件事**拆成两个独立 fixture**，各自命名准确                              |

---

## LC-003 — `rules.uts` 暴露了 runtime registry 不承认的 `bound_to`

| 项       | 内容                                                                                                       |
| -------- | ---------------------------------------------------------------------------------------------------------- |
| 类型     | **功能性缺陷**（用户可选到会被后端拒绝的选项）                                                              |
| 状态     | **CONFIRMED_DEFECT**                                                                                        |
| 位置     | `app/services/rules.uts` → `DECLARABLE_RELATIONS`                                                           |
| 证据     | `DECLARABLE_RELATIONS` 含 `{ value: 'bound_to', label: '已绑定' }`；但 `core/src/domain/relation-registry.ts` 的 `RELATION_DEFINITIONS` **只有** `funding_source` / `merchant_agreement` |
| 后果     | 用户选择「已绑定」→ `validateRelationUse('bound_to')` 返回 `{ ok: false, reason: "relation 'bound_to' is not in the runtime registry" }` → 声明失败 |
| 掩盖方式 | DB 的 `CHECK` 约束**允许** `bound_to`（5 值），所以不会在 schema 层立刻炸，只会在 registry 校验层失败 |
| Spec 本意 | UI 可声明集合必须**严格等于** runtime registry 集合。`app/services/...` 的注释本身也写了"必须与 Core schema 的 CHECK 约束完全一致"——但 schema CHECK ≠ runtime registry，注释搞错了对象 |
| 处置     | **Canonical Spec 采用 `Relation.runtimeValues = [funding_source, merchant_agreement]`**。Native 端的可声明关系只能是这两个。`bound_to` 仅作为 Future / Backlog |
| 不得     | 不得为了"迁就旧 UI"而在 Native domain 里放开 `bound_to`（会违反 §11「本轮不要启用 Future relation」）        |

### LC-003 的跨端固化状态（2026-09-18 复核）

**裁决方向：Canonical correction，不是任何单平台的特例。**

> Legacy TS / uni-app x 只是 **Behavior Oracle**；已知 Legacy defect 必须经
> `LEGACY_BEHAVIOR_CORRECTIONS` 记录，**不得反向污染 Canonical**。
> 冻结的 `tag v0.3.0-uniapp-reference` 与其历史行为保持原样，不重写历史。

`bound_to → reject` 已经**永久固化在跨端 fixture 集**中，三端同掺同一批 fixture：

| Fixture | 断言 | 性质 |
| ------- | ---- | ---- |
| `fixtures/relations/relation-reject-non-runtime-relation.json` | `validateRelationUse(payment_instrument, bound_to, account, payment)` → `ok:false`，reason `relation 'bound_to' is not in the runtime registry` | 负向 `from\|bound_to\|to\|payment` |
| `fixtures/relations/relation-group-bound_to-ANY.json` | `validateRelationGroupUse(bound_to, ANY)` → `ok:false`（同上 reason） | 负向，Group 路径 |

因此**不存在**为补这条契约而新增同义 fixture 的空间 —— 新增只会让总数
从 91 涨到 92 而覆盖面积为 0，属于虚增。

三端判定来源（均为 `RelationDefinitionRegistry` 查找，**非**各自的 wire 枚举）：

| 端 | relation 合法性判定 | 是否已拒绝 `bound_to` |
| -- | ------------------- | --------------------- |
| TS core（oracle） | `validateRelationUse` / `validateRelationGroupUse` → registry 查找 | **是** |
| Kotlin | `Relations.kt` 同构 transfer（区别于 `CanonicalEnums.Relation` wire 枚举） | **是**（`DomainInvariantTest.kt` 已断言） |
| ArkTS | `LogicalKey.parseDependencyLogicalKey` → `getRelationDefinition()` | **是**（2026-09-18 起仅用 registry，见下） |

**关于「第二份枚举」的处置意见（勿删）：**
`core/src/domain/types.ts` 的 `Relation` 联合类型与 Kotlin `CanonicalEnums.Relation`
**保留** `verifies / recovers / bound_to` 三个值，**不得删**。理由是它们不是合法性判定器，
而是**存储/wire 表示类型**：冻结 Schema v1 的 `CHECK(relation IN (5 值))` 允许这些值存在于
历史行中，删掉它们会让历史行无法被读出 —— 那属于「为追求洁净而改动 Canonical 能表达的行集合」，
与「不得反向污染 Canonical」是同一类错误的反方向。

已核实这三处**没有**任何一处把该枚举当 runtime 判定器使用：

- TS 侧不存在 `RELATION_VALUES` 之类的运行期集合，唯一的运行期权威是
  `relation-registry.ts` 的 `RELATION_DEFINITIONS`；
- Kotlin 侧同理，`Relations.kt` 的校验走 registry（`CanonicalEnums.Relation` 仅作 wire 表示）；
- ArkTS 侧自 2026-09-18 起 `LogicalKey.parseDependencyLogicalKey` 只调 `getRelationDefinition()`。

一句话：**存得下 ≠ 合法**。前者由 Schema 决定（含 Legacy 历史），后者只由 registry 决定。

**更正一条先前写错的结论**：`HARMONY_N3_CONFORMANCE_REPORT.md` §7.8 曾称
「Harmony 比 TS 基准更严，构成跨端行为差异」。该说法有误 —— TS core
**没有** `parseDependencyLogicalKey`；TS 侧真正解析 logical key 的地方
（`core/src/services/confirmation-service.ts` 成员键解析）走的正是
`validateRelationGroupUse` → registry。三端结论一致，无差异可裁决。
Harmony 侧现已从「枚举判定 + registry 判定」双份真源收敛为**仅 registry**，
消除与 registry 分叉的第二份枚举。

---

## LC-004 — `rules.uts` 的 `relationLabel` 含不存在的 `wallet_binding`

| 项       | 内容                                                              |
| -------- | ----------------------------------------------------------------- |
| 类型     | 死代码 / 命名漂移                                                  |
| 状态     | **CONFIRMED_DEFECT（低危）**                                      |
| 位置     | `app/services/rules.uts` → `relationLabel()`                      |
| 证据     | 分支 `if (relation === 'wallet_binding')`；`wallet_binding` 不在 DB CHECK、不在 registry、也不在 Future 词表 |
| 影响     | 无运行时后果（死分支），但会诱导后续实现者以为存在该 relation       |
| Spec 本意 | relation 词表唯一来源是 spec；`futureRelations` 目前为 `card_on_file` / `direct_debit_mandate` / `payout_destination` / `wallet_funding`（注意是 `wallet_funding`，**不是** `wallet_binding`） |
| 处置     | Native 端不得实现 `wallet_binding`；label 映射只覆盖 canonical 词表 |

---

## LC-005 — `plan-readiness` readiness 文案：`ready_with_known_scope` 两种措辞

| 项       | 内容                                                                                                        |
| -------- | ----------------------------------------------------------------------------------------------------------- |
| 类型     | 文案不一致（非语义缺陷）                                                                                     |
| 状态     | **CONFIRMED_DEFECT（文案层）**                                                                              |
| 证据     | `app/theme/tokens.uts` → `statusLabel` 返回 `'可以继续'`；`spec/ui/copy-zh.json` 与 §66 要求 `'基于当前信息，可以继续。'` |
| 影响     | 三端可能出现两种说法                                                                                         |
| Spec 本意 | 统一为 §66 的完整句                                                                                          |
| 处置     | `spec/ui/copy-zh.json` 为唯一文案源；三端都取该值                                                            |

---

## LC-006 — 不变量测试的临时目录泄漏（已修）

| 项       | 内容                                                                             |
| -------- | -------------------------------------------------------------------------------- |
| 类型     | 资源泄漏                                                                          |
| 状态     | **已修复**（LEGACY 侧，历史欠账 T-1）                                             |
| 位置     | `core/tests/invariants/invariants.test.ts`                                        |
| 证据     | 修复前 `%TEMP%` 累积 2400+ 个 `depmap-inv-*`；修复后 delta = 0                     |
| 影响     | 与本轮 Native 迁移无关；记录以证明"legacy 已被审计过"                              |
| 处置     | Native 测试同样**必须**清理临时资源，不得重演                                      |

---

## 汇总：对 Native 迁移的直接影响

| 编号   | 是否影响 Canonical Spec | 动作                                                     |
| ------ | ----------------------- | -------------------------------------------------------- |
| LC-001 | 否（fixture 覆盖）      | Native fixture 补真实 BOM 用例                            |
| LC-002 | 否（fixture 组织）      | 拆成两个 fixture：「坏行拒绝」与「缺列报错」              |
| LC-003 | **是**                  | `RuntimeRelation` 锁定为 2 值；不实现 `bound_to`          |
| LC-004 | 否（词表澄清）          | 不实现 `wallet_binding`；Future 词表用 `wallet_funding`    |
| LC-005 | **是（文案）**          | 以 `spec/ui/copy-zh.json` 为唯一文案源                    |
| LC-006 | 否                      | Native 测试自行清理临时资源                                |

> 以上均**不**允许通过修改 Canonical Spec 去迁就 legacy 的行为；
> 唯一例外是 LC-003/LC-005，因为那两条本身就是"legacy 偏离了本意"。
