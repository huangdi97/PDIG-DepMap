# ANDROID_PERFORMANCE_SMOKE_REPORT.md

> 不发明 SLA。只记录**本机本轮真实观测值**。
> 本文件明确作废上一轮的无效性能数字（见 §1）。

更新时间：2026-09-15

---

## 1. 旧数字作废声明（INVALIDATED）

上一轮报告中出现过以下数字：

| 指标 | 旧值 |
| --- | --- |
| `parse_generic_csv_10k_rows` | 2427 ms |
| `insert_10k_rows_in_one_transaction` | 4839 ms |
| `db_open_encrypted_sqlcipher` | 138 ms |
| `migrate_v0_to_v3` | 671 ms |
| `build_timeline` | 242 ms |
| `export_graph` | 704 ms |
| `depmap_encrypt_export` | 1937 ms |
| `depmap_decrypt_and_import` | 9509 ms |

**全部作废（INVALIDATED），不得用于任何性能结论。**

原因：那次运行的 `csvRowsParsed = 0`。
`GenericCsvParser` 的 `MappingOptions.dateFormats` 默认是 `["YYYY-MM-DD"]`，
而 smoke 造的数据是完整 ISO 时间戳 `2026-01-01T10:00:00Z`，
导致 1 万行**全部落入 "bad date" 错误分支**，一个 Observation 都没解析出来。
也就是说，所谓"解析 1 万行 2427ms" 实际是"1 万行全部解析失败"的耗时。

已做的修复（代码层面，已生效）：

1. `dateFormats = ["YYYY-MM-DDTHH:mm:ssZ", "YYYY-MM-DD"]`
2. 增加强断言 `assertEquals(10_000, rows)` —— 再出现空解析会直接 FAIL，不会静默记 0
3. 输出增加 `csvParseErrors` 字段

---

## 2. 测试环境

| 项 | 值 |
| --- | --- |
| 设备 | Android SDK built for x86_64（AVD `PDIG_API34_DEFAULT`） |
| API / ABI | 34 / x86_64 |
| 屏幕 | 1080x2400，density 420 |
| 加速 | swiftshader_indirect（软件渲染） |
| AVD 内存 | 3072 MB |
| 宿主可用内存 | 约 5.6 GB / 31.8 GB（**内存紧张**） |
| 构建 | `app-debug.apk`，debug 变体 |

> 这是**软件渲染的模拟器**，绝对数值不代表真机表现，只能用于同环境纵向对比。
> 宿主内存紧张会放大耗时波动。

---

## 3. 本轮实测结果（有效）

来源：设备内 `PerfSmokeEvidenceTest.perfSmoke`，
结果 `OK (1 test)`，`Time: 34.257`，
产物 `/data/data/com.pdig.app/files/perf-smoke.json`（`run-as` 拉回）。

### 3.1 前置校验（决定数字是否可用）

| 项 | 值 | 结论 |
| --- | --- | --- |
| `csvRowsInput` | 10,000 | — |
| `csvRowsParsed` | **10,000** | ✅ 与上轮 0 形成对比 |
| `csvParseErrors` | **0** | ✅ 无错误行被吞掉 |
| `assertEquals(10_000, rows)` | 通过 | ✅ 强断言生效 |

只有 `csvRowsParsed == 10_000` 才允许记录下列数值 —— 本轮满足。

### 3.2 观测值

| 指标 | 实测（ms） | 说明 |
| --- | --- | --- |
| `db_open_encrypted_sqlcipher` | **149** | SQLCipher 打开加密库 |
| `migrate_v0_to_v3` | **831** | 空库迁移到 schema v3 |
| `parse_generic_csv_10k_rows` | **6058** | 解析 1 万行 CSV（**有效**） |
| `insert_10k_rows_in_one_transaction` | **8679** | 单事务写入 1 万行指纹 |
| `query_nodes_empty` | **0** | 空表查询 |
| `build_timeline` | **448** | 时间线分桶 |
| `export_graph`（1,999,690 字节） | **624** | 导出图 payload |
| `depmap_encrypt_export` | **2722** | `.depmap` 加密导出 |
| `depmap_decrypt_and_import` | **11290** | `.depmap` 解密 + 导入 |
| 用例总时长 | **34,257** | 含未单独计时的步骤 |

### 3.3 DB row count

- 插入语句执行条数：**10,000**（单事务内循环）
- 回读计数：**NOT_MEASURED** —— 本轮测试未加 `SELECT count(*)` 回读断言，
  因此只能说明"执行了 1 万条 insert"，不能声称"库里确有 1 万行"。
  这是一个已知取证缺口，不拿它凑结论。

---

## 4. 异常与诚实说明

1. **单次采样**：以上均为**一次运行**的观测值，不是多次中位数。
   宿主内存紧张 + 软件渲染，波动可能很大。**不得当作 SLA。**

2. **设备内测试进程曾被 OOM 杀掉**：
   在 19 个用例的单进程全量运行中，测试进程出现过
   `Process crashed` / `signal 9 (Killed)`。
   按类分批 + 每次 `pm clear` 后，5 个类分别跑全部 `OK`。
   最终 `connectedDebugAndroidTest` 全量 19/19 通过（见最终 Gate 报告）。
   说明在 3GB AVD + 宿主内存紧张时，测试进程稳定性是**真实脆弱点**。

3. **最重的一项是 `.depmap` 解密导入（11.3s）**：
   主要由 Argon2id 口令派生造成（设计上就该慢），
   不是缺陷，但确实是端到端最慢的一步。

---

## 5. Gate

| Gate | 状态 |
| --- | --- |
| `ANDROID_PERFORMANCE_SMOKE` | **PASS_WITH_REPORT** |
| 说明 | 数字有效（10,000 行解析、0 错误、强断言通过），但仅单次采样、环境为软件渲染模拟器，不构成 SLA；DB 回读计数未测量 |
