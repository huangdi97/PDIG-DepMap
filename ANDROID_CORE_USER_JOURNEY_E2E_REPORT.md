# Android 核心用户行程端到端报告（P0-1）

生成时间：2026-09-16
设备：`emulator-5554`（Android 14 / API 34，1080×2400，3-button 导航）
APK：`app-debug.apk`
SHA256：`bf378ec6678f04bf988921528b73ab879d9381a418d3a094ece2e60490305ff1`
驱动脚本：`local_private/core_journey_e2e_v2.py`
原始证据：`local_private/e2e/core-journey-v2-20260916-172044.{txt,json}`
截图：`local_private/e2e/shots/`

**结论：21 / 21 PASS，0 PARTIAL，0 FAIL，被测应用崩溃 0 次。**

全部通过**真实 UI 操作**完成：没有 DB 注入、没有假 repository、没有 mock ViewModel。

---

## 行程结果

| # | 步骤 | 结果 | 关键证据 |
|---|---|---|---|
| J1-install | 卸载后重装 APK | PASS | `Success` |
| J1-first-launch | 全新安装首启 | PASS | 首页出现，对象数 = 0 |
| J2-nav-sources | 首页 → 数据来源与导入 | PASS | — |
| J2-nav-import | → 导入页 | PASS | 「第 1 步」出现 |
| J2-pick-source | 选中已有来源 | PASS | 「已选择」标记出现 |
| J2-pick-file | SAF 选中 `wechat_synthetic.csv` | PASS | 真实系统文件选择器 |
| J2-node-resolution | Node Resolution 预览 | PASS | 支付方式（2）、收款对象（3） |
| J2-commit | 确认导入 | PASS | 「记录 6 行；新增不重复 6 条，重复跳过 0 条。」耗时 8.8s |
| J3-review-screen | 进入待确认服务页 | PASS | 候选关系未确认 |
| J4-confirm-reality | 用户确认候选关系 | PASS | 点击「确认」3 次 → 转为 Reality |
| J4-back-home | 回到基础设施页 | PASS | 出现「招商银行…」 |
| J5-mark-required | 用户显式标记必需 | PASS | 标记 2 次，详情页显示「必需」2 个 |
| J5-impact-screen | 影响范围页 | PASS | **必须处理（2）**，含处理顺序 |
| J6-create-plan | 生成变更计划 | PASS | 计划页生成，revision 8 |
| J7-complete-actions | 标记动作完成 | PASS | 2 个动作 |
| J7-done-is-not-verified | done ≠ verified | PASS | done 后仍剩 2 个「确认验证」按钮，状态「验证：待验证」 |
| J8-verify-actions | 确认验证 | PASS | 2 个动作 |
| J9-relaunch | 进程死亡后重启 | PASS | 数据仍在：「共 5 个对象」 |
| J10-export | 导出 .depmap | PASS | `/sdcard/Download/pdig-backup.depmap`，13,617 字节（本轮时间戳） |
| J10-restore-wrong-password | 错误密码恢复被拒 | PASS | 「无法恢复：密码错误、文件损坏，或版本不受支持。」 |
| CRASH-SCAN | 应用崩溃扫描 | PASS | `com.pdig.app` 崩溃 0 次 |

---

## 被实证的核心语义

1. **Proposal ≠ Reality**：导入只产生候选关系；J3 之后必须用户点「确认」才成为依赖。
2. **机器永不产生 `criticality=required`**：J5 之前必须先由用户在详情页显式点「标记为必需」，
   否则影响分析里「必须处理」恒为 0、变更计划也不会有 action。
   这不是缺陷，是 spec §12 的硬约束——**行程里漏掉这一步会让 J6~J8 全部假失败**。
   本报告的第一版就踩了这个坑（J5 曾以「必须处理（0）」蒙混通过），已改为断言数量 ≥ 1。
3. **done ≠ verified**：J7 标记完成后，UI 同时显示「已完成」与「验证：待验证」，
   「确认验证」按钮仍在——两个状态没有被相互吞掉。
4. **进程死亡不丢数据**：`am kill` 后重启，对象计数仍为 5。
5. **错误密码恢复被拒**：不留下半恢复状态，提示明确。

---

## 运行中发现的问题

### F1 —— 备份导出 UI 误报失败（真实缺陷，2/2 稳定复现）

现象：在备份页输入密码点「生成加密备份」后，App 显示
**「备份失败：无法写入文件。」**，但
`/sdcard/Download/pdig-backup.depmap` **实际被完整写入**（13,617 字节，时间戳即本次操作）。

复测证据：`local_private/_j10_retest.txt`

```
round 1:  删除后 ls =（空）  → App 提示「备份失败：无法写入文件。」→ 导出后 ls = 13617 字节 09:36
round 2:  删除后 ls =（空）  → App 提示「备份失败：无法写入文件。」→ 导出后 ls = 13617 字节 09:37
```

关键补证：用**正确密码恢复**这个文件是成功的 ——
`local_private/_j10_restore_ok.txt` 显示 **「已恢复 29 条记录。」**

结论：`exportBackupToFile()` 返回 `null`（进入 `catch` 分支）时文件其实已经落盘且内容完整。
危害是**用户会以为备份没成功**（不会丢数据，但会误导）。需修：返回 null 前应确认清理真的生效，
或把异常原因暴露出来而不是笼统报「无法写入文件」。

### F2 —— 「确认导入」按钮的有效点击区低于其可见范围（可用性观察）

现象：导入预览页上，Compose 的 `Role.Button` 语义节点报
`(42,2127,1038,2232)`（中心 y=2179），在该范围内点击**长时间无任何反应**
（数据库大小与 mtime 完全不变，可排除"正在写入"）；
而点外层 `clickable=true` 的 View `(42,2186,1038,2295)` 经底部裁剪后的落点 y=2228 则立即生效。

证据：`local_private/_tapmodes.txt`、`_dbwatch.txt`、`_alive.txt`
（同屏的「选择文件并解析」与普通卡片点击均正常，可排除输入通道故障）。

已按后者修正驱动脚本。是否需要在产品侧修（例如给滚动容器补底部 padding，
让按钮完整落在可见区内）建议单独评估——**本轮不做改动，只如实记录**。

---

## 复现方式

```bash
export ANDROID_SERIAL=emulator-5554
python local_private/core_journey_e2e_v2.py     # 完整 J1–J10，约 12 分钟
python local_private/j10_retest.py              # F1 复测
python local_private/j10_restore_ok.py          # 正确密码恢复
```

`PDIG_E2E_SKIP_INSTALL=1` 可跳过重装（仅 `pm clear`），用于快速迭代；
正式取证必须跑完整路径。
