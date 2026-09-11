# DepMap / 个人数字依赖图

> 换卡、换号、换邮箱之前，先看清哪些账户、扣款和恢复路径会跟着受影响。

内部代号 **DepMap**。MVP 唯一核心 Job：**模拟更换 / 注销一张银行卡**，输出 Action Checklist。

## 当前状态（真实，不虚报）

| 部分 | 状态 |
|---|---|
| 共享 Core（schema/repositories/migration/impact/parser/resolver/proposal/crypto） | **TESTED — 166 tests PASS，typecheck 干净**（Node 22） |
| `.depmap` V1 加密容器 + Golden Vector | **TESTED（Node 侧）**；Android/iOS 侧测试代码就绪未运行 |
| uni-app x 中文 UI（11 页 + 5 个 UTS 安全插件） | IMPLEMENTED（源码完成）；未编译（无 HBuilderX） |
| Android Kotlin 安全层 + Gradle 工程 | IMPLEMENTED；未编译（无 JDK17/SDK） |
| HarmonyOS ArkTS 适配 + 工程文件 | IMPLEMENTED；未编译（无 DevEco） |
| iOS Swift 适配 + SPM | IMPLEMENTED；未编译（无 macOS/Xcode） |
| 真实数据双 Gate | **NOT_RUN**（无真实账单） |

平台矩阵与证据：`WORK_STATUS.md` / `FINAL_REPORT.md`；外部依赖：`BLOCKERS.md`。

## 产品

- **是什么**：本地加密的“数字依赖图”。账单 → 支付路径 → 你确认 → 依赖图 → 变更影响清单。
- **不是什么**：密码管理器 / 支付钱包 / 记账软件 / 订阅管理器 / 云服务。
- **第一原则**：宁可漏报，不可把“不确定”伪装成“必须处理”。机器提出，用户确认现实。

## 第一次启动（ZCode）

先读 `START_HERE.md`，然后把 `ZCODE_FIRST_PROMPT.txt` 内容发给 ZCode。

## 本地开发

```bash
cd core
npm install
npm test        # 166 tests
npm run typecheck
```

真实账单验证（账单放 `local_private/`，不入库、不联网）：

```bash
cd core
node --experimental-strip-types scripts/validate-real-bill.ts --file ../local_private/<bill>.csv
```

## 目录

- `core/` — 纯 TypeScript 共享核心 + vitest（domain/schema/impact/parser/resolver/proposal/crypto）
- `app/` — uni-app x（pages + uni_modules UTS 插件 + manifest/pages.json）
- `platforms/` — Android Gradle / HarmonyOS / iOS SPM 原生工程与安全源码
- `docs/` — ARCHITECTURE / SCHEMA_V1 / IMPACT_ENGINE / PARSER_WECHAT / CRYPTO_PROTOCOL / SECURITY_MODEL / PLATFORM_ADAPTERS / TEST_MATRIX / REAL_DATA_VALIDATION / STORE_RELEASE_CHECKLIST

## 设计与目标（控制文件）

- `CANONICAL_DESIGN.md` — 唯一产品/Schema/Impact 母版
- `GOAL_MVP01.md` — 本轮执行目标（PHASE 0–15）
- `WORK_STATUS.md` / `BLOCKERS.md` / `MVP_ACCEPTANCE.md` — 状态与验收

## 解析器覆盖矩阵

| 来源 | 状态 |
|---|---|
| 微信支付账单（UTF-8/BOM/GB18030/说明行/退款/坏行） | 已实现并测试（synthetic） |
| 支付宝 / 银行 CSV | 未实现（FUTURE.md） |

## 已知限制

- Impact 仅 payment capability；其他 capability 仅存边
- Group 仅 ANY/ALL；无 N-of-M
- 三端原生工程尚未经过编译验证（外部工具链缺失，见 BLOCKERS.md）

## 隐私

无后端 · 无账号 · 无 analytics · 无广告 · 无云同步。
原始账单只在导入会话内存处理；不持久化任何单笔交易；数据库全库加密；`.depmap` 备份口令加密。

## 禁止

不要把旧 IMPLEMENTATION_NOTES 放回 Workspace。
不要在 MVP01 引入 LLM / Agent / GraphRAG / Neo4j。
