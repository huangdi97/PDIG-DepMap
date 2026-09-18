# DEPENDENCY_POLICY.md — 依赖治理（Engineering Baseline V1）

> 配套：`docs/DEPENDENCY_AUDIT.md`（RC 轮）、`THIRD_PARTY_NOTICES.md`（许可证清单）。
> 自动化：`npm run check:deps`（树健康 + 审计 + lockfile 同步 + 许可证快照）。

## 直接依赖清单（2026-09-13）

| 包                                   | 版本                       | runtime/dev | 用途                               | 接触敏感数据       | 替代方案                          | 许可证     |
| ------------------------------------ | -------------------------- | ----------- | ---------------------------------- | ------------------ | --------------------------------- | ---------- |
| hash-wasm                            | 4.12.0                     | **runtime** | Argon2id（KDF）+ 哈希              | 不接触（纯计算）   | 无（必须 v19 Argon2id）           | MIT        |
| typescript                           | 5.9.3                      | dev         | 编译                               | 否                 | —                                 | Apache-2.0 |
| vitest / @vitest/coverage-v8         | 3.2.7                      | dev         | 测试/覆盖                          | 否（dev-only）     | —                                 | MIT        |
| eslint + typescript-eslint + globals | 10.10.0 / 8.70.0 / 17.12.0 | dev         | 静态检查                           | 否                 | —                                 | MIT        |
| prettier                             | 3.9.6                      | dev         | 格式化                             | 否                 | —                                 | MIT        |
| @types/node                          | 22.20.1                    | dev         | 类型                               | 否                 | —                                 | MIT        |
| iconv-lite                           | 0.7.3                      | dev         | GB18030 fixture 生成脚本           | 否                 | —                                 | MIT        |
| fast-check                           | 4.x                        | dev         | property-based testing（本轮新增） | 否（测试种子固定） | 自写 PRNG（已有 mulberry32 后备） | MIT        |

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

## Vendored 原生依赖（不经过 npm，独立登记）

> 规则 3 的"必须先更新本表"适用于以下条目。vendored 源码**必须**有机器可校验的
> 完整性清单，并由 Gate 自动对账，不接受"人工确认没改过"。

| 组件                      | 版本 / 固定点                                            | 宿主平台             | 用途                     | 接触敏感数据                             | 许可证              | 完整性清单                          |
| ------------------------- | -------------------------------------------------------- | -------------------- | ------------------------ | ---------------------------------------- | ------------------- | ----------------------------------- |
| argon2（PHC 参考实现）    | tag `20190702` / commit `62358ba2123abd17fccf2a108a301d4b52c01a7c` | HarmonyOS (ohos NDK) | DEPMAP_CONTAINER_V1 的 Argon2id KDF | 接触（口令与派生密钥仅在内存中瞬时存在） | CC0-1.0 OR Apache-2.0 | `third_party/argon2/VENDOR.json`    |

**argon2 的引入理由（2026-09-18 决策）**：
Harmony 托管的 `cryptoFramework` 与 `HUKS` 均**不提供 Argon2**（证据级排除，见
`HARMONY_ARGON2_FEASIBILITY.md` §2），而 `argon2id` 是 `DEPMAP_CONTAINER_V1` 冻结协议的一部分，
**不可降级**为 PBKDF2/HKDF（那会破坏三端互操作）。因此必须链接经审计的外部实现，
而不是自研原语（spec §K 明确禁止自研密码学 primitive）。

**Graph / 追踪义务**：
`check:deps` 不会看到 `third_party/`。对应 Gate 是
`node tools/harmony/check-third-party-hashes.mjs`
（CI 里为 `Harmony third-party integrity` job），它对 `VENDOR.json` 里登记的每个文件
逐字节校验 SHA-256，并对**未登记文件**报错。
