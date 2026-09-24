# ANDROID_REAL_DATA_ACCEPTANCE.md

> 生成时间：2026-09-20（Android Product Finalization）
> 配套：`ANDROID_REAL_DATA_PILOT_PROTOCOL.md` / `ANDROID_REAL_DATA_PRIVACY.md`
> 状态：**NOT EXECUTED（无授权真实账单）** —— Synthetic 结论不得冒充 Real Data 结论（AGENTS §16）。

---

## 1. 验收门禁

| Gate                                  | 判定                    | 现状                     |
| ------------------------------------- | ----------------------- | ------------------------ |
| 授权数据存在（用户提供 1 份真实账单） | 必须 True               | **False**（无授权数据）  |
| REAL_DATA_CORRECTNESS                 | 通过协议 + 脱敏执行一轮 | **BLOCKED_BY_REAL_DATA** |
| REAL_DATA_VALUE                       | 指标达成 + 用户复核     | **BLOCKED_BY_REAL_DATA** |

## 2. 指标目标（协议 §4 的验收值）

| 指标                     | 验收线                                             |
| ------------------------ | -------------------------------------------------- |
| Precision（must_change） | 无假阳性：`confirmed false positive = 0`（硬目标） |
| false must_change        | 0                                                  |
| Node Resolution 正确率   | ≥95%                                               |
| Proposal 有用率          | ≥80%                                               |
| 遗漏 dependency          | 分类记录（不设 0 目标 —— 真实世界永远有未知）      |
| 单次换卡排查时间         | 记录中位数（不预设）                               |

## 3. 现状与证据边界

- 本机证据全部来自 **synthetic fixtures**：conformance 91/91（含 28 个导入 fixture）、
  Core Journey E2E 41/41（`wechat_synthetic.csv`，1044 B）、设备内 58/58。
- 这些证明**引擎在受控输入下正确**，但**不能**回答「真实账单下检测率/误报率是多少」。
- 因此 `REAL_DATA_CORRECTNESS` 与 `REAL_DATA_VALUE` 如实记为 `BLOCKED_BY_REAL_DATA`，
  与「synthetic 全绿」并行存在，互不替代。

## 4. 解除条件（用户动作）

1. 用户提供 1 份**本人**真实账单（微信/OFX/CSV 任一）。
2. 按 `ANDROID_REAL_DATA_PILOT_PROTOCOL.md` 执行一轮。
3. 按 `ANDROID_REAL_DATA_PRIVACY.md` 完成脱敏与清理。
4. 回填本文件指标 → 更新 `REAL_DATA_CORRECTNESS` / `REAL_DATA_VALUE`。

---

## 结论

```
REAL_DATA_CORRECTNESS = BLOCKED_BY_REAL_DATA（协议就绪，等用户授权数据）
REAL_DATA_VALUE       = BLOCKED_BY_REAL_DATA（同上）
Synthetic correctness = PASS（conformance 91/91 + E2E 41/41 + 设备 58/58）—— 不等于 Real Data 结论
```
