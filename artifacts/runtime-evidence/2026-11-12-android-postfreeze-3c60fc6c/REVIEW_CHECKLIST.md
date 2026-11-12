# 人眼验收核对清单（Android Post-Freeze Production 证据）

由 `MANIFEST.json` 生成，按变更旅程排序；四腿各 22 帧，合计 88 帧。

**这份清单不能替代判定**：它只列出每帧"该看到什么"与已知限制。`humanReview` 在 MANIFEST 里一律 `NOT_RUN`，`HUMAN_VISUAL_ACCEPTANCE` 只有 owner 能改。

## 已知限制（读之前先接受）

- 标 `FLAG_SECURE` 的帧 PNG 就是全黑：这是安全设计，判据是 XML 里的文案与 `bounds`，不是像素。
- 仓库参考图为 ≈320×213 历史小图，本清单不做像素级 diff（`PIXEL_REFERENCE_LIMITATION`）。
- 每帧 `assertionsMissing` 必须为空（CI 已校验）；人眼要看的是语义是否诚实，不是断言是否通过。
- 禁止在身份面出现的推断词汇：Passkey, Authenticator, Secure Enclave, Trusted device, MFA, 通行密钥。


## 腿 `phone`（sdk_gphone64_x86_64 · 1080x2400 @ 420 · fontScale 1.0）

| # | 帧 | 安全窗口 | 该看到什么 | 断言 | XML |
| --- | --- | --- | --- | --- | --- |
| 1 | `production-lock` | 普通截图 | 锁定态先于任何 Reality 可见；只应有锁门与风险说明，不得出现对象名/金额/身份。 | PDIG 已锁定<br>已知悉风险，本次进入 | `xml/phone/production-lock.xml` |
| 2 | `production-now` | 普通截图 | 五个一级目的地齐全（现在/基础设施/变更/记录/我），世界观标记在位。 | 你的数字基础设施 · 已记录的全球上下文<br>现在<br>基础设施<br>变更<br>记录<br>我 | `xml/phone/production-now.xml` |
| 3 | `production-infrastructure` | FLAG_SECURE | Hub 首屏；只显示已记录对象的存在性，不显示推断。 | 基础设施 | `xml/phone/production-infrastructure.xml` |
| 4 | `production-device-empty` | FLAG_SECURE | 空状态文案必须同时说明"空列表 ≠ 外部不存在"；不得出现任何设备名。 | 设备<br>当前没有该类型的已确认对象。空列表不代表外部不存在，只表示这里尚未记录。 | `xml/phone/production-device-empty.xml` |
| 5 | `production-account-detail` | FLAG_SECURE | 身份 hero + 关系清单；每条关系标注确认来源，不出现恢复路径/安全结论推断。 | 账户身份<br>主账户 | `xml/phone/production-account-detail.xml` |
| 6 | `production-account-detail-change-boundary` | FLAG_SECURE | 变更边界卡：标题 + 正文 + CTA 三者必须同时在内容窗口内（这是 1.5 字号曾假红的地方）。 | 此对象暂不能直接创建变更计划<br>账户变更尚未开放正式可执行模板；不会用通用步骤替代真实变更语义。<br>查看变更中心 → | `xml/phone/production-account-detail-change-boundary.xml` |
| 7 | `production-email-detail` | FLAG_SECURE | 邮箱身份 hero；恢复/认证用途分开列，不合并成"安全"结论。 | 通信身份<br>主邮箱 | `xml/phone/production-email-detail.xml` |
| 8 | `production-email-detail-change-boundary` | FLAG_SECURE | 同账户：正文说明"不会由界面自行猜测恢复/认证影响"。 | 此对象暂不能直接创建变更计划<br>邮箱更换尚未开放正式可执行模板；恢复与认证影响不会由界面自行猜测。<br>查看变更中心 → | `xml/phone/production-email-detail-change-boundary.xml` |
| 9 | `production-service-detail` | FLAG_SECURE | 服务详情；关系只作对象间确认，不当作迁移计划。 | 服务节点<br>登录服务 | `xml/phone/production-service-detail.xml` |
| 10 | `production-service-detail-change-boundary` | FLAG_SECURE | 正文必须写明"不把关系数量当作迁移计划"。 | 此对象暂不能直接创建变更计划<br>服务迁移尚未开放正式可执行模板；不会把关系数量当作迁移计划。<br>查看变更中心 → | `xml/phone/production-service-detail-change-boundary.xml` |
| 11 | `production-change-center` | 普通截图 | 计划列表：正式状态标签、数据版本、未执行入口的诚实禁用。 | 变更<br>已记录变更计划<br>更换手机号 | `xml/phone/production-change-center.xml` |
| 12 | `production-change-current` | FLAG_SECURE | CURRENT 投影：影响数/必须处理数/完成度分别独立，且"完成后"明确标注只是计划预期。 | 变更全程<br>当前<br>过渡中<br>完成后（计划） | `xml/phone/production-change-current.xml` |
| 13 | `production-change-pending-verification` | FLAG_SECURE | 已完成未验证：CTA 变成"确认验证"，文案写明"完成 ≠ 已验证"。 | 确认验证这一步<br>动作已记录完成，结果尚未验证<br>确认验证会写入正式记录 | `xml/phone/production-change-pending-verification.xml` |
| 14 | `production-change-retirement-blocked` | FLAG_SECURE | 停用旧号被前置阻断：应显示"等待前置步骤"，主 CTA 不得是停用。 | 停用旧手机号（新路径全部验证后）<br>等待前置步骤 | `xml/phone/production-change-retirement-blocked.xml` |
| 15 | `production-change-retirement-eligible-cta` | FLAG_SECURE | 新路径全部验证后才出现"记录完成这一步"（停用入口）。 | 记录完成这一步 | `xml/phone/production-change-retirement-eligible-cta.xml` |
| 16 | `production-change-retirement-eligible` | FLAG_SECURE | 停用步骤卡身份可核对（步骤名 + 前置条件）。 | 停用旧手机号（新路径全部验证后） | `xml/phone/production-change-retirement-eligible.xml` |
| 17 | `production-change-after-plan` | FLAG_SECURE | 完成后仍是"计划预期·未发生"，不得宣称目标已达成。 | 计划预期<br>未发生 | `xml/phone/production-change-after-plan.xml` |
| 18 | `production-records` | FLAG_SECURE | 记录工作区：只呈现已发生的操作与验证证据，按计划分组。 | 已记录的操作与验证证据 · 按计划分组 | `xml/phone/production-records.xml` |
| 19 | `production-records-trace` | FLAG_SECURE | 三时钟：发生/记录/验证时间分列，缺记录时写"未记录"而不是补值。 | 已记录完成<br>已验证<br>来源：正式变更计划<br>发生时间未记录<br>记录于 <br>验证于  | `xml/phone/production-records-trace.xml` |
| 20 | `production-change-stale` | FLAG_SECURE | 图变化后的 stale 投影：必须提示重新分析，且不得允许继续写入。 | 基础设施已发生变化<br>重新分析当前影响 | `xml/phone-stale-reanalysis/production-change-stale.xml` |
| 21 | `production-change-stale-boundary` | FLAG_SECURE | stale 边界说明：说明为什么停止写入。 | 使用上方的「重新分析当前影响」；界面不会用旧快照继续写入。 | `xml/phone-stale-reanalysis/production-change-stale-boundary.xml` |
| 22 | `production-change-reanalyzed` | FLAG_SECURE | 重新分析后：只比较前后正式影响投影，不把重新分析当成完成动作。 | 重新分析完成<br>分析基准 6 → 8<br>新增 1 · 已不在当前影响 0 · 状态变化 0<br>这里只比较重新分析前后的正式影响投影；不会自动把任何步骤标记为已完成或已验证。<br>准备状态：<br>分析时数据版本 8 · 当前数据版本 8 | `xml/phone-stale-reanalysis/production-change-reanalyzed.xml` |

## 腿 `phone-font15`（sdk_gphone64_x86_64 · 1080x2400 @ 420 · fontScale 1.5）

| # | 帧 | 安全窗口 | 该看到什么 | 断言 | XML |
| --- | --- | --- | --- | --- | --- |
| 1 | `production-lock` | 普通截图 | 锁定态先于任何 Reality 可见；只应有锁门与风险说明，不得出现对象名/金额/身份。 | PDIG 已锁定<br>已知悉风险，本次进入 | `xml/phone-font15/production-lock.xml` |
| 2 | `production-now` | 普通截图 | 五个一级目的地齐全（现在/基础设施/变更/记录/我），世界观标记在位。 | 你的数字基础设施 · 已记录的全球上下文<br>现在<br>基础设施<br>变更<br>记录<br>我 | `xml/phone-font15/production-now.xml` |
| 3 | `production-infrastructure` | FLAG_SECURE | Hub 首屏；只显示已记录对象的存在性，不显示推断。 | 基础设施 | `xml/phone-font15/production-infrastructure.xml` |
| 4 | `production-device-empty` | FLAG_SECURE | 空状态文案必须同时说明"空列表 ≠ 外部不存在"；不得出现任何设备名。 | 设备<br>当前没有该类型的已确认对象。空列表不代表外部不存在，只表示这里尚未记录。 | `xml/phone-font15/production-device-empty.xml` |
| 5 | `production-account-detail` | FLAG_SECURE | 身份 hero + 关系清单；每条关系标注确认来源，不出现恢复路径/安全结论推断。 | 账户身份<br>主账户 | `xml/phone-font15/production-account-detail.xml` |
| 6 | `production-account-detail-change-boundary` | FLAG_SECURE | 变更边界卡：标题 + 正文 + CTA 三者必须同时在内容窗口内（这是 1.5 字号曾假红的地方）。 | 此对象暂不能直接创建变更计划<br>账户变更尚未开放正式可执行模板；不会用通用步骤替代真实变更语义。<br>查看变更中心 → | `xml/phone-font15/production-account-detail-change-boundary.xml` |
| 7 | `production-email-detail` | FLAG_SECURE | 邮箱身份 hero；恢复/认证用途分开列，不合并成"安全"结论。 | 通信身份<br>主邮箱 | `xml/phone-font15/production-email-detail.xml` |
| 8 | `production-email-detail-change-boundary` | FLAG_SECURE | 同账户：正文说明"不会由界面自行猜测恢复/认证影响"。 | 此对象暂不能直接创建变更计划<br>邮箱更换尚未开放正式可执行模板；恢复与认证影响不会由界面自行猜测。<br>查看变更中心 → | `xml/phone-font15/production-email-detail-change-boundary.xml` |
| 9 | `production-service-detail` | FLAG_SECURE | 服务详情；关系只作对象间确认，不当作迁移计划。 | 服务节点<br>登录服务 | `xml/phone-font15/production-service-detail.xml` |
| 10 | `production-service-detail-change-boundary` | FLAG_SECURE | 正文必须写明"不把关系数量当作迁移计划"。 | 此对象暂不能直接创建变更计划<br>服务迁移尚未开放正式可执行模板；不会把关系数量当作迁移计划。<br>查看变更中心 → | `xml/phone-font15/production-service-detail-change-boundary.xml` |
| 11 | `production-change-center` | 普通截图 | 计划列表：正式状态标签、数据版本、未执行入口的诚实禁用。 | 变更<br>已记录变更计划<br>更换手机号 | `xml/phone-font15/production-change-center.xml` |
| 12 | `production-change-current` | FLAG_SECURE | CURRENT 投影：影响数/必须处理数/完成度分别独立，且"完成后"明确标注只是计划预期。 | 变更全程<br>当前<br>过渡中<br>完成后（计划） | `xml/phone-font15/production-change-current.xml` |
| 13 | `production-change-pending-verification` | FLAG_SECURE | 已完成未验证：CTA 变成"确认验证"，文案写明"完成 ≠ 已验证"。 | 确认验证这一步<br>动作已记录完成，结果尚未验证<br>确认验证会写入正式记录 | `xml/phone-font15/production-change-pending-verification.xml` |
| 14 | `production-change-retirement-blocked` | FLAG_SECURE | 停用旧号被前置阻断：应显示"等待前置步骤"，主 CTA 不得是停用。 | 停用旧手机号（新路径全部验证后）<br>等待前置步骤 | `xml/phone-font15/production-change-retirement-blocked.xml` |
| 15 | `production-change-retirement-eligible-cta` | FLAG_SECURE | 新路径全部验证后才出现"记录完成这一步"（停用入口）。 | 记录完成这一步 | `xml/phone-font15/production-change-retirement-eligible-cta.xml` |
| 16 | `production-change-retirement-eligible` | FLAG_SECURE | 停用步骤卡身份可核对（步骤名 + 前置条件）。 | 停用旧手机号（新路径全部验证后） | `xml/phone-font15/production-change-retirement-eligible.xml` |
| 17 | `production-change-after-plan` | FLAG_SECURE | 完成后仍是"计划预期·未发生"，不得宣称目标已达成。 | 计划预期<br>未发生 | `xml/phone-font15/production-change-after-plan.xml` |
| 18 | `production-records` | FLAG_SECURE | 记录工作区：只呈现已发生的操作与验证证据，按计划分组。 | 已记录的操作与验证证据 · 按计划分组 | `xml/phone-font15/production-records.xml` |
| 19 | `production-records-trace` | FLAG_SECURE | 三时钟：发生/记录/验证时间分列，缺记录时写"未记录"而不是补值。 | 已记录完成<br>已验证<br>来源：正式变更计划<br>发生时间未记录<br>记录于 <br>验证于  | `xml/phone-font15/production-records-trace.xml` |
| 20 | `production-change-stale` | FLAG_SECURE | 图变化后的 stale 投影：必须提示重新分析，且不得允许继续写入。 | 基础设施已发生变化<br>重新分析当前影响 | `xml/phone-font15-stale-reanalysis/production-change-stale.xml` |
| 21 | `production-change-stale-boundary` | FLAG_SECURE | stale 边界说明：说明为什么停止写入。 | 使用上方的「重新分析当前影响」；界面不会用旧快照继续写入。 | `xml/phone-font15-stale-reanalysis/production-change-stale-boundary.xml` |
| 22 | `production-change-reanalyzed` | FLAG_SECURE | 重新分析后：只比较前后正式影响投影，不把重新分析当成完成动作。 | 重新分析完成<br>分析基准 6 → 8<br>新增 1 · 已不在当前影响 0 · 状态变化 0<br>这里只比较重新分析前后的正式影响投影；不会自动把任何步骤标记为已完成或已验证。<br>准备状态：<br>分析时数据版本 8 · 当前数据版本 8 | `xml/phone-font15-stale-reanalysis/production-change-reanalyzed.xml` |

## 腿 `tablet`（sdk_gphone64_x86_64 · 2560x1600 @ 320 · fontScale 1.0）

| # | 帧 | 安全窗口 | 该看到什么 | 断言 | XML |
| --- | --- | --- | --- | --- | --- |
| 1 | `production-lock` | 普通截图 | 锁定态先于任何 Reality 可见；只应有锁门与风险说明，不得出现对象名/金额/身份。 | PDIG 已锁定<br>已知悉风险，本次进入 | `xml/tablet/production-lock.xml` |
| 2 | `production-now` | 普通截图 | 五个一级目的地齐全（现在/基础设施/变更/记录/我），世界观标记在位。 | 你的数字基础设施 · 已记录的全球上下文<br>现在<br>基础设施<br>变更<br>记录<br>我 | `xml/tablet/production-now.xml` |
| 3 | `production-infrastructure` | FLAG_SECURE | Hub 首屏；只显示已记录对象的存在性，不显示推断。 | 基础设施 | `xml/tablet/production-infrastructure.xml` |
| 4 | `production-device-empty` | FLAG_SECURE | 空状态文案必须同时说明"空列表 ≠ 外部不存在"；不得出现任何设备名。 | 设备<br>当前没有该类型的已确认对象。空列表不代表外部不存在，只表示这里尚未记录。 | `xml/tablet/production-device-empty.xml` |
| 5 | `production-account-detail` | FLAG_SECURE | 身份 hero + 关系清单；每条关系标注确认来源，不出现恢复路径/安全结论推断。 | 账户身份<br>主账户 | `xml/tablet/production-account-detail.xml` |
| 6 | `production-account-detail-change-boundary` | FLAG_SECURE | 变更边界卡：标题 + 正文 + CTA 三者必须同时在内容窗口内（这是 1.5 字号曾假红的地方）。 | 此对象暂不能直接创建变更计划<br>账户变更尚未开放正式可执行模板；不会用通用步骤替代真实变更语义。<br>查看变更中心 → | `xml/tablet/production-account-detail-change-boundary.xml` |
| 7 | `production-email-detail` | FLAG_SECURE | 邮箱身份 hero；恢复/认证用途分开列，不合并成"安全"结论。 | 通信身份<br>主邮箱 | `xml/tablet/production-email-detail.xml` |
| 8 | `production-email-detail-change-boundary` | FLAG_SECURE | 同账户：正文说明"不会由界面自行猜测恢复/认证影响"。 | 此对象暂不能直接创建变更计划<br>邮箱更换尚未开放正式可执行模板；恢复与认证影响不会由界面自行猜测。<br>查看变更中心 → | `xml/tablet/production-email-detail-change-boundary.xml` |
| 9 | `production-service-detail` | FLAG_SECURE | 服务详情；关系只作对象间确认，不当作迁移计划。 | 服务节点<br>登录服务 | `xml/tablet/production-service-detail.xml` |
| 10 | `production-service-detail-change-boundary` | FLAG_SECURE | 正文必须写明"不把关系数量当作迁移计划"。 | 此对象暂不能直接创建变更计划<br>服务迁移尚未开放正式可执行模板；不会把关系数量当作迁移计划。<br>查看变更中心 → | `xml/tablet/production-service-detail-change-boundary.xml` |
| 11 | `production-change-center` | 普通截图 | 计划列表：正式状态标签、数据版本、未执行入口的诚实禁用。 | 变更<br>已记录变更计划<br>更换手机号 | `xml/tablet/production-change-center.xml` |
| 12 | `production-change-current` | FLAG_SECURE | CURRENT 投影：影响数/必须处理数/完成度分别独立，且"完成后"明确标注只是计划预期。 | 变更全程<br>当前<br>过渡中<br>完成后（计划） | `xml/tablet/production-change-current.xml` |
| 13 | `production-change-pending-verification` | FLAG_SECURE | 已完成未验证：CTA 变成"确认验证"，文案写明"完成 ≠ 已验证"。 | 确认验证这一步<br>动作已记录完成，结果尚未验证<br>确认验证会写入正式记录 | `xml/tablet/production-change-pending-verification.xml` |
| 14 | `production-change-retirement-blocked` | FLAG_SECURE | 停用旧号被前置阻断：应显示"等待前置步骤"，主 CTA 不得是停用。 | 停用旧手机号（新路径全部验证后）<br>等待前置步骤 | `xml/tablet/production-change-retirement-blocked.xml` |
| 15 | `production-change-retirement-eligible-cta` | FLAG_SECURE | 新路径全部验证后才出现"记录完成这一步"（停用入口）。 | 记录完成这一步 | `xml/tablet/production-change-retirement-eligible-cta.xml` |
| 16 | `production-change-retirement-eligible` | FLAG_SECURE | 停用步骤卡身份可核对（步骤名 + 前置条件）。 | 停用旧手机号（新路径全部验证后） | `xml/tablet/production-change-retirement-eligible.xml` |
| 17 | `production-change-after-plan` | FLAG_SECURE | 完成后仍是"计划预期·未发生"，不得宣称目标已达成。 | 计划预期<br>未发生 | `xml/tablet/production-change-after-plan.xml` |
| 18 | `production-records` | FLAG_SECURE | 记录工作区：只呈现已发生的操作与验证证据，按计划分组。 | 已记录的操作与验证证据 · 按计划分组 | `xml/tablet/production-records.xml` |
| 19 | `production-records-trace` | FLAG_SECURE | 三时钟：发生/记录/验证时间分列，缺记录时写"未记录"而不是补值。 | 已记录完成<br>已验证<br>来源：正式变更计划<br>发生时间未记录<br>记录于 <br>验证于  | `xml/tablet/production-records-trace.xml` |
| 20 | `production-change-stale` | FLAG_SECURE | 图变化后的 stale 投影：必须提示重新分析，且不得允许继续写入。 | 基础设施已发生变化<br>重新分析当前影响 | `xml/tablet-stale-reanalysis/production-change-stale.xml` |
| 21 | `production-change-stale-boundary` | FLAG_SECURE | stale 边界说明：说明为什么停止写入。 | 使用上方的「重新分析当前影响」；界面不会用旧快照继续写入。 | `xml/tablet-stale-reanalysis/production-change-stale-boundary.xml` |
| 22 | `production-change-reanalyzed` | FLAG_SECURE | 重新分析后：只比较前后正式影响投影，不把重新分析当成完成动作。 | 重新分析完成<br>分析基准 6 → 8<br>新增 1 · 已不在当前影响 0 · 状态变化 0<br>这里只比较重新分析前后的正式影响投影；不会自动把任何步骤标记为已完成或已验证。<br>准备状态：<br>分析时数据版本 8 · 当前数据版本 8 | `xml/tablet-stale-reanalysis/production-change-reanalyzed.xml` |

## 腿 `tablet-font15`（sdk_gphone64_x86_64 · 2560x1600 @ 320 · fontScale 1.5）

| # | 帧 | 安全窗口 | 该看到什么 | 断言 | XML |
| --- | --- | --- | --- | --- | --- |
| 1 | `production-lock` | 普通截图 | 锁定态先于任何 Reality 可见；只应有锁门与风险说明，不得出现对象名/金额/身份。 | PDIG 已锁定<br>已知悉风险，本次进入 | `xml/tablet-font15/production-lock.xml` |
| 2 | `production-now` | 普通截图 | 五个一级目的地齐全（现在/基础设施/变更/记录/我），世界观标记在位。 | 你的数字基础设施 · 已记录的全球上下文<br>现在<br>基础设施<br>变更<br>记录<br>我 | `xml/tablet-font15/production-now.xml` |
| 3 | `production-infrastructure` | FLAG_SECURE | Hub 首屏；只显示已记录对象的存在性，不显示推断。 | 基础设施 | `xml/tablet-font15/production-infrastructure.xml` |
| 4 | `production-device-empty` | FLAG_SECURE | 空状态文案必须同时说明"空列表 ≠ 外部不存在"；不得出现任何设备名。 | 设备<br>当前没有该类型的已确认对象。空列表不代表外部不存在，只表示这里尚未记录。 | `xml/tablet-font15/production-device-empty.xml` |
| 5 | `production-account-detail` | FLAG_SECURE | 身份 hero + 关系清单；每条关系标注确认来源，不出现恢复路径/安全结论推断。 | 账户身份<br>主账户 | `xml/tablet-font15/production-account-detail.xml` |
| 6 | `production-account-detail-change-boundary` | FLAG_SECURE | 变更边界卡：标题 + 正文 + CTA 三者必须同时在内容窗口内（这是 1.5 字号曾假红的地方）。 | 此对象暂不能直接创建变更计划<br>账户变更尚未开放正式可执行模板；不会用通用步骤替代真实变更语义。<br>查看变更中心 → | `xml/tablet-font15/production-account-detail-change-boundary.xml` |
| 7 | `production-email-detail` | FLAG_SECURE | 邮箱身份 hero；恢复/认证用途分开列，不合并成"安全"结论。 | 通信身份<br>主邮箱 | `xml/tablet-font15/production-email-detail.xml` |
| 8 | `production-email-detail-change-boundary` | FLAG_SECURE | 同账户：正文说明"不会由界面自行猜测恢复/认证影响"。 | 此对象暂不能直接创建变更计划<br>邮箱更换尚未开放正式可执行模板；恢复与认证影响不会由界面自行猜测。<br>查看变更中心 → | `xml/tablet-font15/production-email-detail-change-boundary.xml` |
| 9 | `production-service-detail` | FLAG_SECURE | 服务详情；关系只作对象间确认，不当作迁移计划。 | 服务节点<br>登录服务 | `xml/tablet-font15/production-service-detail.xml` |
| 10 | `production-service-detail-change-boundary` | FLAG_SECURE | 正文必须写明"不把关系数量当作迁移计划"。 | 此对象暂不能直接创建变更计划<br>服务迁移尚未开放正式可执行模板；不会把关系数量当作迁移计划。<br>查看变更中心 → | `xml/tablet-font15/production-service-detail-change-boundary.xml` |
| 11 | `production-change-center` | 普通截图 | 计划列表：正式状态标签、数据版本、未执行入口的诚实禁用。 | 变更<br>已记录变更计划<br>更换手机号 | `xml/tablet-font15/production-change-center.xml` |
| 12 | `production-change-current` | FLAG_SECURE | CURRENT 投影：影响数/必须处理数/完成度分别独立，且"完成后"明确标注只是计划预期。 | 变更全程<br>当前<br>过渡中<br>完成后（计划） | `xml/tablet-font15/production-change-current.xml` |
| 13 | `production-change-pending-verification` | FLAG_SECURE | 已完成未验证：CTA 变成"确认验证"，文案写明"完成 ≠ 已验证"。 | 确认验证这一步<br>动作已记录完成，结果尚未验证<br>确认验证会写入正式记录 | `xml/tablet-font15/production-change-pending-verification.xml` |
| 14 | `production-change-retirement-blocked` | FLAG_SECURE | 停用旧号被前置阻断：应显示"等待前置步骤"，主 CTA 不得是停用。 | 停用旧手机号（新路径全部验证后）<br>等待前置步骤 | `xml/tablet-font15/production-change-retirement-blocked.xml` |
| 15 | `production-change-retirement-eligible-cta` | FLAG_SECURE | 新路径全部验证后才出现"记录完成这一步"（停用入口）。 | 记录完成这一步 | `xml/tablet-font15/production-change-retirement-eligible-cta.xml` |
| 16 | `production-change-retirement-eligible` | FLAG_SECURE | 停用步骤卡身份可核对（步骤名 + 前置条件）。 | 停用旧手机号（新路径全部验证后） | `xml/tablet-font15/production-change-retirement-eligible.xml` |
| 17 | `production-change-after-plan` | FLAG_SECURE | 完成后仍是"计划预期·未发生"，不得宣称目标已达成。 | 计划预期<br>未发生 | `xml/tablet-font15/production-change-after-plan.xml` |
| 18 | `production-records` | FLAG_SECURE | 记录工作区：只呈现已发生的操作与验证证据，按计划分组。 | 已记录的操作与验证证据 · 按计划分组 | `xml/tablet-font15/production-records.xml` |
| 19 | `production-records-trace` | FLAG_SECURE | 三时钟：发生/记录/验证时间分列，缺记录时写"未记录"而不是补值。 | 已记录完成<br>已验证<br>来源：正式变更计划<br>发生时间未记录<br>记录于 <br>验证于  | `xml/tablet-font15/production-records-trace.xml` |
| 20 | `production-change-stale` | FLAG_SECURE | 图变化后的 stale 投影：必须提示重新分析，且不得允许继续写入。 | 基础设施已发生变化<br>重新分析当前影响 | `xml/tablet-font15-stale-reanalysis/production-change-stale.xml` |
| 21 | `production-change-stale-boundary` | FLAG_SECURE | stale 边界说明：说明为什么停止写入。 | 使用上方的「重新分析当前影响」；界面不会用旧快照继续写入。 | `xml/tablet-font15-stale-reanalysis/production-change-stale-boundary.xml` |
| 22 | `production-change-reanalyzed` | FLAG_SECURE | 重新分析后：只比较前后正式影响投影，不把重新分析当成完成动作。 | 重新分析完成<br>分析基准 6 → 8<br>新增 1 · 已不在当前影响 0 · 状态变化 0<br>这里只比较重新分析前后的正式影响投影；不会自动把任何步骤标记为已完成或已验证。<br>准备状态：<br>分析时数据版本 8 · 当前数据版本 8 | `xml/tablet-font15-stale-reanalysis/production-change-reanalyzed.xml` |

## 验收时要给的三类结论

1. **可接受**：语义诚实、无禁词、CTA 与计划状态一致。
2. **需修改**：指出帧 id + 具体文案/布局问题（不要只说"不好看"）。
3. **无法判定**：说明缺什么证据（例如某屏被 FLAG_SECURE 挡住且 XML 也不足）。

