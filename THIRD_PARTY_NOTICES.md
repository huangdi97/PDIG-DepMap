# THIRD_PARTY_NOTICES.md — 第三方组件声明

> 本项目 MVP 运行时与开发工具链使用的第三方开源组件（2026-09-13 Engineering Baseline V1
> 轮刷新，版本见 core/package-lock.json；许可证快照由 `npm run check:deps` 自动生成核对）。

## 共享 Core 运行时

| 组件      | 版本   | License | 来源                                 |
| --------- | ------ | ------- | ------------------------------------ |
| hash-wasm | 4.12.0 | MIT     | https://github.com/Daninet/hash-wasm |

## 开发工具链（不进入发布产物）

| 组件                                   | License    |
| -------------------------------------- | ---------- |
| TypeScript                             | Apache-2.0 |
| Vitest / @vitest/coverage-v8           | MIT        |
| ESLint / typescript-eslint / globals   | MIT        |
| Prettier                               | MIT        |
| @types/node                            | MIT        |
| iconv-lite（fixture 生成脚本）         | MIT        |
| fast-check（property-based 测试，4.x） | MIT        |

## 平台原生（进入对应平台发布产物，待工具链编译时复核版本）

| 平台      | 组件                                    | License                      |
| --------- | --------------------------------------- | ---------------------------- |
| Android   | SQLCipher Community Edition             | BSD-style                    |
| Android   | BouncyCastle (bcprov-jdk18on)           | MIT（Bouncy Castle License） |
| Android   | AndroidX core/biometric/fragment/sqlite | Apache-2.0                   |
| iOS       | SQLCipher                               | BSD                          |
| HarmonyOS | ArkData / HUKS / cryptoFramework        | 华为系统组件（随 SDK 分发）  |

## 说明

- hash-wasm 内含经 WASM 编译的哈希实现集合，其生成源与许可见上游仓库。
- 本项目自身代码默认 GPL-3.0（CANONICAL_DESIGN §13.1）；产品名称/商标单独声明。
