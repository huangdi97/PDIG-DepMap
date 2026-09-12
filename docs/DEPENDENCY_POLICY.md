# DEPENDENCY_POLICY.md — 依赖治理（Engineering Baseline V1）

> 配套：`docs/DEPENDENCY_AUDIT.md`（RC 轮）、`THIRD_PARTY_NOTICES.md`（许可证清单）。
> 自动化：`npm run check:deps`（树健康 + 审计 + lockfile 同步 + 许可证快照）。

## 直接依赖清单（2026-09-13）

| 包 | 版本 | runtime/dev | 用途 | 接触敏感数据 | 替代方案 | 许可证 |
|---|---|---|---|---|---|---|
| hash-wasm | 4.12.0 | **runtime** | Argon2id（KDF）+ 哈希 | 不接触（纯计算） | 无（必须 v19 Argon2id） | MIT |
| typescript | 5.9.3 | dev | 编译 | 否 | — | Apache-2.0 |
| vitest / @vitest/coverage-v8 | 3.2.7 | dev | 测试/覆盖 | 否（dev-only） | — | MIT |
| eslint + typescript-eslint + globals | 10.10.0 / 8.70.0 / 17.12.0 | dev | 静态检查 | 否 | — | MIT |
| prettier | 3.9.6 | dev | 格式化 | 否 | — | MIT |
| @types/node | 22.20.1 | dev | 类型 | 否 | — | MIT |
| iconv-lite | 0.7.3 | dev | GB18030 fixture 生成脚本 | 否 | — | MIT |
| fast-check | 4.x | dev | property-based testing（本轮新增） | 否（测试种子固定） | 自写 PRNG（已有 mulberry32 后备） | MIT |

## 规则

1. **lockfile 必须提交**；`npm ci` 是唯一受支持的安装方式（clean install Gate）。
2. `engines.node >= 22.5.0`（node:sqlite 起始版本）+ `packageManager` 字段已声明。
3. **生产禁止引入**未经审计的 crypto/random 库 —— runtime 面只有 hash-wasm；
   任何新 runtime 依赖必须先更新本表 + THIRD_PARTY_NOTICES + 评审 bundle impact。
4. 审计政策：**生产依赖 0 high/critical**；dev-only moderate 允许但必须登记（当前：
   vitest 3 链 @vitest/mocker 3 moderate，修复需 breaking 升 vitest 5 —— 已评估，不阻塞，
   留待 vitest 5 计划性升级轮）。
5. major 版本升级（TS 7 / vitest 5 等）单独开轮处理，不夹带。
6. `npm outdated` 每轮 `check:deps` 如实记录（当前：@types/node patch 可跟；TS/vitest major 挂起）。
