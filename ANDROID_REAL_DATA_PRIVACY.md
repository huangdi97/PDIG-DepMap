# ANDROID_REAL_DATA_PRIVACY.md

> 生成时间：2026-09-20（Android Product Finalization）
> 配套：`ANDROID_REAL_DATA_PILOT_PROTOCOL.md` / `ANDROID_REAL_DATA_ACCEPTANCE.md`

---

## 1. 数据边界（什么能碰、什么不能碰）

| 数据 | 可处理 | 说明 |
|------|--------|------|
| 用户授权的账单文件（一份） | ✅ 本机解析 | 仅用于本次 pilot |
| 服务 / 商户名（如「腾讯视频」） | ✅ 可保留 | 用于 Node Resolution 判断 |
| 金额 / 日期 / 币种 | ✅ 统计用 | 不进入任何报告正文 |
| 卡号 / 账号 / 手机号 / 邮箱 | ❌ | 出现即脱敏为 `***` |
| 任何可识别单个人的组合 | ❌ | 报告发布前二次检查 |

## 2. 处理链（每步约束）

1. **文件到达**：用户放入 `local_private/`（gitignored），或设备本机；**立即从版本管理中排除**（local_private 全忽略）。
2. **解析**：app 内内存解析（产品本身即如此）；日志不输出原始行（AGENTS §17）。
3. **证据采集**：UI 截图若含金额/卡号 → 截图前遮盖或直接不截。
4. **报告**：只输出计数与**脱敏后的服务名**。

## 3. 生命周期

- pilot 结束后，原始账单文件从本机删除（用户确认）。
- 报告保留脱敏结论；不保留原始账单副本。
- 任何「已删除」都不说谎：删除后 `Get-ChildItem` 复核为空。

## 4. 违反处理

发现任何一处真实数据进入报告/log/截图 → 该轮 pilot 作废重跑，
并在 `ANDROID_REAL_DATA_ACCEPTANCE.md` 记录违规类型与修复。

---

## 结论

```
ANDROID_REAL_DATA_PRIVACY = READY（协议就绪，未执行 —— 无授权数据）
```