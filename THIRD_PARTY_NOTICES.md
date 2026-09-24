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

| 平台      | 组件                                    | Version / 固定点 | License                      |
| --------- | --------------------------------------- | ---------------- | ---------------------------- |
| Android   | SQLCipher Community Edition             | —                | BSD-style                    |
| Android   | BouncyCastle (bcprov-jdk18on)           | —                | MIT（Bouncy Castle License） |
| Android   | AndroidX core/biometric/fragment/sqlite | —                | Apache-2.0                   |
| iOS       | SQLCipher                               | —                | BSD                          |
| HarmonyOS | ArkData / HUKS / cryptoFramework        | 随 SDK 分发      | 华为系统组件                 |

### Vendored 源码（复制进仓库，非包管理器依赖）

| 平台      | 组件                   | Version / 固定点                                                   | License               | 完整性清单                       | 为什么需要                                                                                                                                         |
| --------- | ---------------------- | ------------------------------------------------------------------ | --------------------- | -------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- |
| HarmonyOS | argon2（PHC 参考实现） | tag `20190702` / commit `62358ba2123abd17fccf2a108a301d4b52c01a7c` | CC0-1.0 OR Apache-2.0 | `third_party/argon2/VENDOR.json` | Harmony 托管的 cryptoFramework / HUKS **无 Argon2**；而 `argon2id v19` 是 `DEPMAP_CONTAINER_V1` 冻结协议，不可降级为 PBKDF2/HKDF，也不允许自研原语 |

> 上游版权声明（逐字保留，未修改）：
> `Copyright 2015 Daniel Dinu, Dmitry Khovratovich, Jean-Philippe Aumasson, and Samuel Neves`
>
> 该组件为**双许可**（CC0-1.0 或 Apache-2.0，二选一）。本项目同时满足两条分支的义务：
> 完整 `LICENSE` 随源码保留在 `third_party/argon2/LICENSE`，来源与版本在本文件与
> `VENDOR.json` 中显式登记。本地修改数 = **0**（由 `check-third-party-hashes.mjs` 逐字节校验）。

## 说明

- hash-wasm 内含经 WASM 编译的哈希实现集合，其生成源与许可见上游仓库。
- argon2 为 **vendored 源码**（不经 npm / ohpm），完整性由 `tools/harmony/check-third-party-hashes.mjs`
  对 `VENDOR.json` 逐文件 SHA-256 校验；该 Gate 同时拒绝未登记文件。
- 本项目自身代码默认 GPL-3.0（CANONICAL_DESIGN §13.1）；产品名称/商标单独声明。
