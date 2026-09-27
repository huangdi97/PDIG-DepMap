# PRE_RELEASE_BASELINE.md — product-v0.3.1 Corrective Release

> 生成：2026-09-27（v0.3.1 Corrective Release Closure 第 0 阶段，§4）。
> 全部数值来自本轮真实 git/gh 输出，非复制旧报告。

## 1. Git 状态（§4 命令输出）

| 项 | 值 |
| --- | --- |
| HEAD | 5781ce60fd03bcdd40e6d6878b1548ed3db224c7 |
| main | 5781ce60fd03bcdd40e6d6878b1548ed3db224c7 |
| origin/main | 5781ce60fd03bcdd40e6d6878b1548ed3db224c7 |
| worktree | clean（git status --short -uall 空；## main...origin/main） |
| product-v0.2.0（tag） | ff69a3e07a98a82892e03e585eb67e28e507b018 |
| product-v0.3.0（annotated tag object） | effbdd608261d16574ec1aac310ff5e7132d5a57 |
| product-v0.3.0^{}（peeled commit） | be3bc81720e6f27270666ff5bbc06556840ffc3e |

## 2. GitHub Release product-v0.3.0

gh release view product-v0.3.0 --json tagName,isDraft,isPrerelease：
{"isDraft":false,"isPrerelease":true,"tagName":"product-v0.3.0"}
（v0.3.0 为已发布 Pre-release，保持不变。）

## 3. product-v0.3.0 之后的 commits（git log product-v0.3.0^{}..main --reverse）

Closure 阶段共 31 个 commit，内容分三类：
- iOS closure 工程/排障：xcodegen 工程壳、XCUITest bundle、CI workflow 迭代（CFBundleVersion、derivedData、xctestrun、iPad 弹窗、seed 时序、xctestrun .app 补丁等）；
- Closure 收口：Schema v4 证据、Harmony H3/H4 host 闭环、Desktop profiles/keys、Android/Website/Branding/Security 证据、最终矩阵；
- CI 修复：xcresult 残留、单次 action、配方恢复、确定性 xctestrun 修复。

详见 V0_3_0_TO_V0_3_1_CHANGE_AUDIT.md。

## 4. 版本基线（§8 修改前）

| 平台 | 当前值 |
| --- | --- |
| Desktop | packageVersion = "0.3.0"；Main.kt VERSION = "0.3.0" |
| Android production | versionCode = 1 / versionName = "0.1.0-milestone"（占位） |
| Android preview | versionCode = 200004 |
| iOS | MARKETING_VERSION = "0.3.0"；CURRENT_PROJECT_VERSION = "1" |
| Harmony | versionCode = 1000000 / versionName = "1.0.0" |

## 5. Windows 打包现状（§7 修改前）

scripts/release/build-desktop-package.ps1：NSIS 安装器 + 手写 app-image 便携包；无图标处理（无 Icon/InstallIcon/--icon）；-Version 默认 0.2.0，调用时传入版本。

## 6. 旧 tag immutable 声明

product-v0.2.0、product-v0.3.0 在本轮全程不移动、不删除、不重建、不 force-update；product-v0.3.0 继续作为历史真实发布存在。本轮只新增 product-v0.3.1。
