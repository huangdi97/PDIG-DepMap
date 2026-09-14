# UI_SOURCE_AUDIT.md — UI 源码审计（RC PHASE T + U）

> 审查对象：app/pages/ 全部 11 页（源码级；未编译，B10）。只审计不加功能。

## 逐页结论

| 页面            | 中文 | loading/empty/error                             | 破坏性确认                     | 备注                                                                                                          |
| --------------- | ---- | ----------------------------------------------- | ------------------------------ | ------------------------------------------------------------------------------------------------------------- |
| unlock          | ✅   | 认证失败 toast ✅                               | —                              | 认证取消不进入数据层（biometric 插件契约）                                                                    |
| home            | ✅   | 计数 0 时显示「0 条/0 个」✅；空态无崩溃        | —                              | answer-oriented：变更/风险/待核对分区 ✅；最近备份时间展示 ✅                                                 |
| nodes           | ✅   | 列表空态可见；创建校验（空名 toast）✅          | —                              | 渐进式录入（先名称+类型）✅                                                                                   |
| node-detail     | ✅   | 节点缺失时无渲染崩溃（rows.length==0 return）✅ | 退役关系 showModal 确认 ✅     | 只展示 confirmed Dependency，Proposal 不在此显示 ✅                                                           |
| import-wechat   | ✅   | 未选文件 toast ✅                               | —                              | 明示「文件仅内存处理，不保存原始账单」✅                                                                      |
| node-resolution | ✅   | 空列表可跳过 ✅                                 | —                              | 未确认商户不产生建议的文案说明 ✅                                                                             |
| proposals       | ✅   | 空态「暂无待确认项」✅                          | —                              | 「是/不是/不确定」三态；「是」默认 unknown 关键度，required 需明确选择 ✅；accepted 不重复问（repository 层） |
| group-proposals | ✅   | 空态 ✅                                         | —                              | 「确认会切换/不会/稍后」✅；拒绝后需新证据重提（repository 层）                                               |
| simulate        | ✅   | 未选卡 toast + 按钮置灰 ✅                      | —                              | 明示反事实模拟、不改数据 ✅                                                                                   |
| impact-result   | ✅   | 空结果仅显示原始操作项 ✅                       | —                              | must_change/backup_path/needs_review 中文等级 ✅；**原始操作强制最后** ✅；「系统不替你执行真实操作」✅       |
| settings        | ✅   | 备份时间读取 ✅                                 | 删除全部数据 showModal 确认 ✅ | 隐私声明（不保存密码/CVV/完整卡号/流水）✅                                                                    |

## 严禁事项核对

- **UI 未把 Proposal 显示为 confirmed reality**：proposals 页与 node-detail 页数据源分离
  （dependency_proposals vs dependencies 表）✅
- 无 lorem / placeholder 假数据 / mock 生产数据：grep 检查仅 input `placeholder=` 提示文案 ✅
- 无假 PASS 状态、无演示数据开关 ✅

## Accessibility 源码审计（PHASE U，SOURCE_AUDITED）

- 可操作项均有可读中文文本（无纯图标按钮）✅
- 等级不只靠颜色：must_change/needs_review 另有文字前缀（必须处理/建议检查）✅
- 长中文文本在 card 布局内自动换行（flex 布局，无固定高度）✅
- 破坏性动作文字明确（“删除全部数据”“退役此关系”）且二次确认 ✅
- 触控目标：按钮 padding ≥ 12px（合理；真机可用性需 DEVICE_VERIFIED，当前不可标）
- 状态：**SOURCE_AUDITED**；无真机不得标 DEVICE_VERIFIED（如实）。
