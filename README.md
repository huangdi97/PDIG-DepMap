# DepMap / 个人数字依赖图

这是项目的 ZCode 启动 Workspace。

## 第一次启动

先读：

`START_HERE.md`

然后在 ZCode 中直接发送：

`ZCODE_FIRST_PROMPT.txt`

## 设计

唯一产品/Schema/Impact 母版：

`CANONICAL_DESIGN.md`

## 当前目标

`GOAL_MVP01.md`

目标不是先做漂亮 UI，而是：

Schema → tests → Impact Kernel → crypto → secure storage → WeChat Parser → Resolver → Proposals → E2E → UI → 三端构建。

## 三端

主应用：
- uni-app x Vapor
- Vue 3
- TypeScript / UTS

原生：
- Android Kotlin
- iOS Swift
- HarmonyOS ArkTS

## 隐私

MVP：
- 无后端
- 无账号
- 无 analytics
- 无广告
- 原始账单不持久化
- 完整数据库加密
- `.depmap` 加密导入导出

## 禁止

不要把旧 IMPLEMENTATION_NOTES 放回 Workspace。
不要在 MVP01 引入 LLM / Agent / GraphRAG / Neo4j。
