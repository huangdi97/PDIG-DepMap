# CROSS_PLATFORM_RUNTIME_DIFF_REPORT.md

> 2026-09-26；同一 synthetic fixture 在可运行平台的 normalize 后语义 diff；backup 互操作矩阵。

## 1. Canonical Fresh 对照（§101）

| 平台                                     | 本轮 fresh                                                             | normalized 结果                                                                                                   |
| ---------------------------------------- | ---------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| Android（JVM conformance，android.json） | 91/91 PASS                                                             | impact/readiness/coverage/relations/depmap/jcs/scenario/migration/state-machine/parser/timeline/backup 全类别一致 |
| Desktop（共享 :core/:conformance JVM）   | 同一 JVM 91/91（15:07 同构建）                                         | 与 Android canonical 同源                                                                                         |
| Harmony host                             | 87/91 host 执行 PASS + 4 device-blocked（非语义分歧，属 runtime 缺失） | 87 条 expected 逐字节一致                                                                                         |
| iOS                                      | runner canonical（run 36231032190）                                    | 若完成：与冻结 expected 对照（ios.json）                                                                          |

> normalized diff 工具：`tools/conformance/run.mjs` 输出各平台 json；类别计数一致（impact 13/readiness 16/coverage 6/relations 18/depmap 3/jcs 1/scenario 1/migration 2/state-machine 5/parser 22/timeline 3/backup 1 = 91）。

## 2. Backup 互操作（§104；可运行平台对）

| 方向                                                   | 结果                                                                                   | 证据          |
| ------------------------------------------------------ | -------------------------------------------------------------------------------------- | ------------- |
| Desktop export → Desktop restore                       | PASS（逐字节 roundtrip）                                                               | smoke step 16 |
| Android export → Desktop restore（CLI/conformance 层） | 未做跨容器实测（两台本地协作不足）；canonical backup fixture 三端 frozen expected 一致 | 记录为受限    |
| Android ↔ Harmony / Desktop ↔ Harmony                  | BLOCKED（Harmony runtime E-9）                                                         | —             |
| iOS ↔ *                                                | NOT_IMPLEMENTED（无 app）                                                              | —             |

> 说明：本轮对端互操作受 Harmony runtime 与 Android 取回环境限制；canonical 层（DEPMAP_CONTAINER_V1 冻结 fixture）三端实现侧一致。

## 3. 术语一致性（§106）

| 术语                        | Desktop                 | Android   | Harmony    | iOS |
| --------------------------- | ----------------------- | --------- | ---------- | --- |
| 待确认服务/关系（Proposal） | “待确认关系/待确认服务” | “待确认…” | —（无 UI） | —   |
| 可能发生了变化（Drift）     | “可能发生了变化”        | 同        | —          | —   |
| 必须处理                    | “必须处理”              | 同        | —          | —   |
| 需要确认/需要重新检查       | 同义                    | 同义      | —          | —   |
| 查看验证                    | 同义                    | 同义      | —          | —   |

> Desktop 与 Android 文案来源一致（共享 copy 口径）；Harmony/iOS 无 UI 文本可比。无平台漂移发现。

## 4. 运行时语义（§103）

Proposal 生成/确认、graphRevision、Impact、PlanReadiness、ScenarioCoverage、ChangePlan、Verification、Timeline projection、Backup canonical payload —— 均由三端（Android/Desktop/Harmony-host）对同一 fixture 输出一致（canonical 91 对照 + host 87 逐字节）。iOS 待 runner 结果对照。
