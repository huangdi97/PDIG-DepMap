# RC_PRE_AUDIT.md — RC 收口前基线（PHASE A）

> 采集时间：2026-09-12 · 只读采集，未改代码

## Git

- branch: `master`
- HEAD: `b9eeb3c444258e5152478dbdb6b2056733d23aa8`（PHASE 14-15 提交）
- status：仅本轮新增 RC 控制文件未跟踪（GOAL_MVP01_RC_AUDIT / RC_AUDIT_RULES / QUALITY_GATES / RC_ACCEPTANCE / TONIGHT_RUNBOOK / ZCODE_RC_FIRST_PROMPT），无未提交源码改动

## 工具链

| 工具 | 版本 |
|---|---|
| Node.js | v22.15.0 |
| npm | 11.3.0 |
| TypeScript | 5.9.3（local） |
| Vitest | 3.2.7（local） |
| eslint / prettier | 未安装（本轮安装） |
| git | 2.55.0.windows.5 |
| lockfile | core/package-lock.json（npm） |

## 结构

- `core/`：13 个 src 目录（adapters/crypto/db/domain/fingerprint/impact/parser/repositories/resolver/schema/services/utils）+ 8 个测试目录
- `app/`：11 个 .uvue 页面 + 5 个 UTS 插件（每个含 interface + app-android/app-ios/app-harmony）
- `platforms/`：16 个文件（10 个 .kt/.swift/.ets 原生源码 + Gradle/manifest/json5/测试）

## 测试基线

- test files：8（repository×3, impact×1, crypto×1, parser×1, unit×1, integration×1）
- test cases（vitest 实际执行）：106，全 PASS（约 6–8s）

## 静态基线（grep 采集）

- `any` / `as any` / `@ts-ignore` / `@ts-expect-error`（core/src）：**0**
- non-null `!.` 断言（core/src）：**4**（均在 import-pipeline.ts 的 `this.session!`，begin() 后有 guard；本轮重构为局部变量消除）
- TODO/FIXME/HACK/XXX/PLACEHOLDER（代码）：**0**（grep 命中的 2 处为 .uvue 模板 `placeholder=` HTML 属性，非代码标记）
- console.* 调用：core/src **0**；app/App.uvue 1 处 `console.log('DepMap launched')`（无敏感内容，本轮审计后决定去留）
- 日志红线敏感模式（raw CSV/txn id/key/secret）：**0**

## scripts 现状

```
test        vitest run
test:watch  vitest
typecheck   tsc --noEmit
lint        tsc --noEmit   ← 名不符实，本轮替换为真实 eslint
```

缺失：format / format:check / lint(eslint) / check / check:full / check:architecture / coverage / db integrity —— 本轮建立。

## tsconfig 现状（core）

strict=true, noUncheckedIndexedAccess=true, noFallthroughCasesInSwitch=true, exactOptionalPropertyTypes=false（本轮评估开启）, noImplicitReturns 未设置（本轮开启）。

## 当前 blockers（沿用 BLOCKERS.md）

B1 Android SDK/JDK17 · B2 DevEco · B3 macOS/Xcode · B10 HBuilderX（编译类）；B4–B9/B11–B13（账号/签名/账单，本轮不涉及）。

## 本轮基线结论

无已知失败测试、无 skip、无全局 disable；起点干净，可直接进入收口。
