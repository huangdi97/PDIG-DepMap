# CONFORMANCE.md

> 跨平台一致性的**唯一判据**。本文是入口说明，详细个案定义在 `fixtures/` 与 `conformance/CONFORMANCE_MANIFEST.json`。
> 生成时间：2026-09-17

---

## 1. 它解决什么问题

三端（Android / HarmonyOS / iOS）各自实现同一套语义。如果只靠"三端都写了类似代码"，
必然漂移。Conformance 的做法是：

> **把语义冻结成平台中立的用例，让三端对同一批输入给出逐字节一致的输出。**

Canonical Spec（`spec/`）是唯一真源，`fixtures/` 是它的可执行表达。

---

## 2. 用例构成（91 个）

| 类别 | 数量 | 覆盖 |
| --- | --- | --- |
| `impact` | 13 | 影响传播与 required 语义 |
| `readiness` | 16 | PlanReadiness 三值判定 |
| `coverage` | 6 | ScenarioCoverage 四级 |
| `relation` | 18 | 关系语义与 logicalKey / groupKey |
| `depmap` | 3 | 容器：正常 / 错误口令 / 篡改 |
| `jcs` | 1 | JCS（RFC 8785）序列化 |
| `scenario` | 1 | 场景模板 |
| `migration` | 2 | schema 迁移语义 |
| `state` | 5 | 状态机合法/非法迁移 |
| `parser` | 22 | CSV / OFX / QFX 解析（含 hostile bounds） |
| `timeline` | 3 | Timeline 确定性投影 |
| `backup` | 1 | 备份 / 恢复往返 |

另有 `fixtures/import/` 下 **28 个**原始输入（CSV / OFX / QFX），三端共用。

**fixture 性质**：全部为 **synthetic / canonical** 测试数据，**不是用户真实账单**。

---

## 3. 判据链（严格顺序）

```
1. CODEGEN GATE      spec → 三端 generated 一致性（手改 generated → 失败）
2. FIXTURE INTEGRITY 每个 fixture 的 sha256 必须与 manifest 一致
3. ORACLE SELFCHECK  冻结的 TS oracle 必须逐字节复现全部 fixture
4. PLATFORM REPORTS  逐用例 diff conformance/reports/<platform>.json
5. SUMMARY           汇总矩阵 + 退出码
```

```bash
node tools/conformance/run.mjs
```

退出码：`0` = 全部可判定项 PASS；`1` = 存在 FAIL 或完整性错误。

---

## 4. 平台报告契约

各端 runner 必须产出：

```
conformance/reports/<platform>.json
```

```json
{
  "platform": "android | harmony | ios",
  "specVersion": "1.0.0",
  "generatedAt": "<ISO8601>",
  "results": {
    "<caseId>": { "status": "PASS | FAIL | NOT_IMPLEMENTED", "actual": "<any>" }
  }
}
```

---

## 5. 诚实口径（重要）

| 状态 | 含义 |
| --- | --- |
| `PASS` | 用例数与实际一致 |
| `FAIL` | 明确不一致 |
| `NOT_IMPLEMENTED` | 该端尚未实现 |
| **`NOT_RUN`** | **该端未产出报告（报告文件缺失）** |

> **`NOT_RUN` 不算 PASS，也不算 FAIL。**
> 「平台报告缺失」绝不记成通过 —— 这是本项目最容易被动摇的一条纪律，
> 任何把 `NOT_RUN` 写成 PASS 的行为都应被拒绝。

---

## 6. 当前状态（2026-09-17 实跑）

| 平台 | 结果 |
| --- | --- |
| **Android** | **PASS — 91 pass / 0 fail / 91 total** |
| **HarmonyOS** | **NOT_RUN**（`conformance/reports/harmony.json` 缺失） |
| **iOS** | **NOT_RUN**（`conformance/reports/ios.json` 缺失） |

| Gate | 结果 |
| --- | --- |
| codegen | PASS |
| fixtureIntegrity | PASS（91/91 cases + 28/28 imports） |
| oracleSelfcheck | PASS（91 cases reproduce exactly） |
| **VERDICT** | **PASS** |

---

## 7. 如何新增一个用例

1. **先改 `spec/`**（Canonical Spec 是唯一真源）
2. 在 `core/scripts/generate-conformance.ts` 中生成该用例的期望值
3. 重新生成 manifest（`cd core && node --experimental-strip-types scripts/generate-conformance.ts`）
4. 在三端各自实现并产出报告
5. 跑 `node tools/conformance/run.mjs`

**不允许**先改平台实现再补 spec —— 会让三端漂移。

---

## 8. 相关文档

- `spec/README.md` — Canonical Spec 约定
- `conformance/CONFORMANCE_MANIFEST.json` — 用例清单与 sha256
- `tools/conformance/run.mjs` — 判据链实现
- `CROSS_PLATFORM_CONFORMANCE_MATRIX.md` — 三端逐用例矩阵
- `docs/FIXTURE_POLICY.md` — fixture 准入与性质要求
