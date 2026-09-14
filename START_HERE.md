# START_HERE.md

你现在拿到的是 **DepMap / 个人数字依赖图** 的空目录启动控制包。

## 你要做的只有 4 步

1. 把本启动包所有文件放到你的空项目目录根目录。
2. 用 ZCode 打开这个目录作为 Workspace。
3. 选择你要使用的 GLM-5.3-Flash。
4. 把 `ZCODE_FIRST_PROMPT.txt` 里的内容复制给它。

不要先自己创建 Flutter / React Native / Capacitor 工程。

让 ZCode 按 `GOAL_MVP01.md` 初始化。

## 控制文件

- `AGENTS.md`：长期工程宪法。以后换 GOAL 也保留。
- `CANONICAL_DESIGN.md`：唯一完整产品/Schema/Impact 设计母版，已修订为 Android+iOS+HarmonyOS 三端工程基线。
- `PLATFORM_DECISION.md`：三端技术栈决策。
- `GOAL_MVP01.md`：本轮从空目录一直做到 MVP 的长目标。
- `WORK_STATUS.md`：Agent 每阶段更新。
- `BLOCKERS.md`：只记录真正需要你介入的外部阻塞。
- `FUTURE.md`：所有暂不允许做的新功能丢这里。
- `MVP_ACCEPTANCE.md`：最终验收。
- `STORE_RELEASE_INPUTS.md`：以后上架时需要你填的信息，不含秘密。
- `.gitignore`：防止真实账单、密钥、签名材料入库。

## 最重要

不要把旧的 `IMPLEMENTATION_NOTES_v0.4.1*` 放进新目录。

它们已经被 `CANONICAL_DESIGN.md` 吸收并 superseded，放进去反而会让 Agent 混淆。

## 第一次建议

第一轮让 ZCode 连续做 Core / tests / Android / HarmonyOS 能做的部分。

你当前若是 Windows，iOS 真正的 Xcode build / signing 仍需要 macOS。Agent 应该完成 iOS 共享代码、Swift Adapter、工程配置和文档，然后把 macOS/Xcode 记录为外部 Blocker，而不是因此停止项目。

## 什么时候给真实账单

不要一开始给。

先让：

- synthetic parser fixtures
- fingerprint duplicate import
- Impact Kernel
- Proposal lifecycle
- crypto vector

全部绿。

到 `REAL_DATA_VALIDATION` 阶段，再把你的真实微信账单放到 `local_private/`。这个目录默认被 `.gitignore` 排除。
