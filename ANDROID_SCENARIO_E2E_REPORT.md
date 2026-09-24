# ANDROID_SCENARIO_E2E_REPORT（G-13：三个 active 场景全闭环）

> 生成时间：2026-09-21（Android Product Finalization）
> 驱动：`local_private/scenario_e2e.py`（复用 core-journey v4 的 UI 驱动原语）
> 设备：AVD `pdig_api34`（Android 14 / API 34 / 1080×2340），emulator-5554
> 结果：**40 / 40 PASS / 0 FAIL**（CRASH-SCAN 0 崩溃）
> Run ID：`scenario-e2e-20260921-151631`

---

## 1. 目的（对照 Goal G-13）

> 分别建立正式 E2E 并实跑 `replace_payment_card`、`expiring_payment_card`、
> `close_payment_instrument`，每个完整走：Scenario Setup → Impact → ChangePlan →
> Action → done → Verification → verified/completed；**三个都要 PASS，不得只测换卡。**

## 2. 数据准备（每个场景共享一套 Reality）

| 步骤                                               | 断言                                                            | 结果 |
| -------------------------------------------------- | --------------------------------------------------------------- | ---- |
| S0 全新安装 → 锁屏 → 解锁 → 首页                   | 冷启动锁屏 + 首页对象数 0                                       | PASS |
| S1 导入（SAF 选 `wechat_synthetic.csv` 1044 B）    | 解析 6 行、Node Resolution、D-16 双断言（回锁≥1 次 + 向导不丢） | PASS |
| S2 待确认页 → 确认 3 次                            | Proposal → Reality（非自动）                                    | PASS |
| S3 基础设施 → 招商银行储蓄卡(1234) → 标记必需 2 次 | 用户显式 required（机器不自动）                                 | PASS |

## 3. 三个场景闭环（每场景 12 条断言）

| 场景                                        | Setup 进入 | 选择支付工具 | 创建计划 | done           | **done≠verified**        | verified       | 回首页 |
| ------------------------------------------- | ---------- | ------------ | -------- | -------------- | ------------------------ | -------------- | ------ |
| **replace_payment_card（更换银行卡）**      | PASS       | PASS         | PASS     | PASS（2 动作） | PASS（『确认验证』仍在） | PASS（2 动作） | PASS   |
| **expiring_payment_card（银行卡即将到期）** | PASS       | PASS         | PASS     | PASS（2 动作） | PASS                     | PASS（2 动作） | PASS   |
| **close_payment_instrument（注销银行卡）**  | PASS       | PASS         | PASS     | PASS（2 动作） | PASS                     | PASS（2 动作） | PASS   |

## 4. 关键语义再确认（设备 UI 实测）

- **done ≠ verified**：三场景中「标记完成」后『确认验证』按钮仍可见——UI 不把 done 当 verified。
- **验证后再完成**：三场景都走到 verified（2 个动作各自标记完成 + 确认验证）。
- **机器不自动必选**：S3 必须由用户点『标记为必需』，影响面才出现 must_change（产品语义未漂移）。
- **D-16 不回归**：每个外部 picker 节点（导入选文件）都跑 `externalPickerDoesNotBypassLock` +
  `externalPickerDoesNotDestroyPendingWorkflow`，均 PASS。

## 5. 崩溃扫描

```
CRASH-SCAN = PASS（com.pdig.app 崩溃=0；工具噪声 0 条）
```

## 6. 证据文件

- `local_private/e2e/scenario-e2e-20260921-151631.txt`
- `local_private/e2e/scenario-e2e-20260921-151631.json`（40 条结果明细）
- 驱动：`local_private/scenario_e2e.py`（local_private 按仓库约定 gitignored，报告为正式证据）

## 7. 结论

```
G-13 THREE_SCENARIO_E2E = PASS（40/40，三个场景全部走通完整闭环，0 崩溃）
```
