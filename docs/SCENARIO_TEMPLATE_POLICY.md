# SCENARIO_TEMPLATE_POLICY.md — 场景模板准入政策（MVP03 §39）

## 准入规则（唯一判据）

只有会改变以下至少一项的事件，才允许成为 PDIG ScenarioTemplate：

数字身份 / 数字访问 / 支付 / 恢复路径 / 数字控制权 / 数据可用性 /
关键数字服务连续性 / 数字账户·设备·资源状态。

## 永久禁止（通用提醒类，与「提醒场景商店」的边界）

喝水 / 生日 / 纪念日 / 节日 / 植物浇水 / 普通会议 / 考试倒计时 / 普通运动打卡 /
普通日程管理 / 姨妈周期 / 任何与数字基础设施无关的生活提醒。

## 本轮借鉴边界

从提醒类产品只借鉴三件事：**模板库形态 / Upcoming 时间线 / 一键启动具体场景**。
不复刻其内容生态。

## 自动化 gate

- `ST-005`（tests/services/scenario-timeline.test.ts）：注册表扫描禁止 water/birthday/
  plant/meeting/exercise/exam/anniversary/holiday 等关键词进入模板 id；
  每个模板必须声明数字基础设施相关 capability。
- planned 模板 factory=null，不可执行（ST-002）。

## 执行口径

- MVP03 Impact 仅 payment → active 模板仅支付类；
- 未来 capability 扩展（access/recovery/identity）必须先扩 Impact kernel 并过
  HIGH 风险 Gate（CHANGE_RISK_POLICY），才允许对应模板转 active；
- 转正流程：SCENARIO_CATALOG_FUTURE.md 中的设计稿 → 实现评估 → registry 注册
  （availability: active）→ 契约/invariant/property 测试 → UI 展示。
