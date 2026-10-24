# INTERACTION_CONTRACT.md — PDIG UI vNext 交互状态机（四端语义一致）

> No-Vision Visual Contract 一部分。状态机四端一致；手势/键位各端原生映射。

## 1. Globe 状态机

```
GLOBAL ──hover(region)──▶ REGION_HOVER
REGION_HOVER ──click──▶ REGION_SELECTED
REGION_SELECTED ──click(selected)──▶ REGION_DETAIL（Region Drawer 展开）
REGION_DETAIL ──Escape/关闭──▶ REGION_SELECTED
REGION_SELECTED/DETAIL ──Escape──▶ GLOBAL
任何状态 ──click(empty space)──▶ GLOBAL
```

## 2. Globe 输入映射

| 输入 | 行为 | 备注 |
| --- | --- | --- |
| drag | 旋转球体（水平=经度，垂直=纬度，垂直 clamp ±60°） | 交互期间暂停 idle rotation |
| scroll / pinch | 缩放 zoom （clamp 0.7–1.9） | |
| hover 地区锚点 | 高亮该 node、显示 tooltip（name + counts） | REGION_HOVER |
| click 地区锚点 | focus/flyTo（620ms ease）+ 该锚点 active + 其它锚点降强调 + 区域过滤 region=xx + 上下文抽屉出现 | REGION_SELECTED |
| click 已选锚点 | 全屏 Region Drawer：Cards / Numbers / Accounts / Services + 查看全部/查看卡片/查看号码 | REGION_DETAIL |
| Escape | 回到 Global | 所有状态 |
| click 空白 | 回到 Global | |

## 3. 地区聚焦副作用（region filter）

选定地区后：
1. camera/focus 移向该地区坐标；
2. 该地区锚点 active（大、亮、标签常显）；
3. 其它锚点降强调（regionNodeLo）；
4. 基础设施查询过滤 `region = 该地区`（区域范围内的卡片/号码/账户/服务）；
5. 上下文抽屉出现（Cards/Numbers/Accounts/Services 摘要 + 3 个动作：查看全部/查看卡片/查看号码）。

不能只是 `navigate("/hongkong")` —— 必须保持 globe 舞台 + 区域上下文（spec §10）。

## 4. 非视觉替代

- Globe 旁/下必须有 **Region List**（键盘可达、screen reader 可读）：
  「中国大陆，5 张卡，2 个号码」「香港，2 张卡，1 个号码」
- Region List 同样驱动 region filter 与 drawer。
- `globe != sole navigation method`。

## 5. 键盘（Desktop）

| 键 | 行为 |
| --- | --- |
| Tab / Shift+Tab | 焦点循环（rail → top bar → 内容 → region list） |
| Enter / Space | 激活焦点项 |
| Escape | globe 回退 / 关闭抽屉 / 关闭聚焦 |
| Arrow keys | region list / 表格行导航；globe 上箭头微调视角 |
| Ctrl/Cmd + K | 打开命令/搜索条（top bar） |

焦点可见：primaryBright ring，任何键盘可达元素必有。

## 6. 屏幕间导航

- 一级导航固定为 **现在 / 基础设施 / 变更 / 记录 / 我** 五项；`我` 是产品一级目的地，不是头像-only 工具。
- 五个一级页均为 hierarchy root；Header Up 在一级页不出现。
- 一级导航切换 = 全应用状态切换；System Back 仍按真实访问历史返回上一个页面/Tab。
- 设置 / 个性化 / 数据源以「我」为 hierarchy parent；从这些子页 Header Up → 我。
- 待复核以「现在」为 hierarchy parent；Header Up → 现在。
- 建立基础设施以「数据源」为 hierarchy parent；手工记录以「建立基础设施」为 parent；手工记录关系以「手工记录」为 parent。
- Establish / Review / Detail / Studio 都是聚焦子流程，不增加第六个一级 Tab。
- 手机详情 / Studio / 搜索可临时隐藏 bottom nav 形成聚焦流程，但返回后必须恢复五项一级导航。
- 卡片网格项 → 卡详情（identity 优先）→（可选）卡定制工作室。
- 号码列表行 → 号码详情（identity 优先）→（可选）号码定制工作室。
- 抽屉「查看卡片」→ 进入 `/infrastructure/cards?region=HK` 带过滤状态。

## 7. Change Phone（flagship）阶段状态机

```
1 影响分析 ──▶ 2 建立新号码 ──▶ 3 验证新号码 ──▶ 4 迁移关键账户 ──▶ 5 检查恢复路径 ──▶ 6 停用旧号码
```

- 每个阶段渲染 Continuity Rail 状态：completed（实勾）/ current（空心+编号）/ blocked（! 原因）/ verifying（时钟）/ upcoming（灰）。
- stage 6 在 stage 3 验证通过前 = **disabled + 明文原因**（「新手机号验证通过后才能停用旧手机号」），绝不只禁用。
- 迁移项状态：migrated / waiting / not started / blocked。
- 视觉模型：`OLD NUMBER → services/accounts → NEW NUMBER`；projection 态（plan）必须带「计划」label，不得伪造已完成。

## 8. Presentation Customization 交互

### Card Image（Android）
- 选择相册图片 / 内置图片 → Live Preview 立即更新 → 保存并返回。
- 三种 Android width class 都保持“小功能”；Medium/Expanded 只扩大留白与 Preview，不恢复工程 Inspector。
- 保存 = 本地 PresentationProfile；`r10-art` / `local-image` 必须在 List / Inspector / Detail / Preview 复用。
- 默认不遮蔽；用户的全局 privacy 选择优先。
- local image 只存 app-private 安全 filename，不持久化任意 content URI / filesystem path。

### Number Appearance
- Mobile：Preview-first；主题 / 呈现选项位于其后。
- Wider window 可使用更丰富的 library / preview / inspector 组合。
- preset visual ≠ semantic role；“恢复”主题绝不创建恢复依赖。

### Shared
- 编辑以 PresentationProfile 为对象；保存 = 本地 preference。
- 不改变 node / Dependency / evidence / confirmation；不写 .depmap。

## 9. 状态反馈原则

- 所有状态行：icon + label + color（三通道）。
- disabled 必须附原因文案。
- 异步保存/加载：短 loading + 完成 toast/inline 确认；错误 = danger 软底 + 人话 + 重试。
- 删除/破坏性动作 = 显式确认（命名对象 + 不可逆后果）。