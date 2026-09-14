# DEPENDENCY_AUDIT.md — 依赖与安全审计（RC PHASE N）

> 执行：2026-09-12 · `cd core && npm outdated && npm audit && npm ls`
> 网络可用（registry 连通），三项命令均真实执行。

## 直接依赖（npm ls --depth=0）

| 包                  | 版本    | 用途                           | license    | runtime/dev         |
| ------------------- | ------- | ------------------------------ | ---------- | ------------------- |
| hash-wasm           | 4.12.0  | Argon2id（WASM，RFC 9106）     | MIT        | **runtime**（唯一） |
| iconv-lite          | 0.7.3   | GB18030 fixture 生成（脚本用） | MIT        | dev                 |
| typescript          | 5.9.3   | 类型检查                       | Apache-2.0 | dev                 |
| vitest              | 3.2.7   | 测试                           | MIT        | dev                 |
| @vitest/coverage-v8 | 3.2.7   | 覆盖率                         | MIT        | dev                 |
| eslint              | 10.10.0 | lint                           | MIT        | dev                 |
| typescript-eslint   | 8.70.0  | typed lint                     | MIT        | dev                 |
| globals             | 17.12.0 | eslint 全局                    | MIT        | dev                 |
| prettier            | 3.9.6   | format                         | MIT        | dev                 |
| @types/node         | 22.20.1 | Node 类型                      | MIT        | dev                 |

- 依赖面积极小：**runtime 唯一依赖 hash-wasm**（纯 WASM，无 native build）。
- lockfile：core/package-lock.json，与 package.json 一致（npm ls 无 extraneous/invalid）。

## npm audit（实际输出）

```
@vitest/mocker 2.1.0 - 4.1.10 — moderate
Vitest: Path Traversal via @vitest/mocker Redirect Mock (GHSA-82fw-gwwq-j7x9)
fix available: npm audit fix --force → vitest@5.0.0（breaking）
3 moderate severity vulnerabilities（全部在 vitest 测试工具链内）
```

评估：

- 全部 3 项 moderate 均位于 **vitest 开发工具链**（@vitest/mocker 路径遍历，仅影响
  开发机上的测试运行器 web UI 场景）；**不进入 App 运行时**（runtime 依赖仅 hash-wasm）。
- 修复需 vitest 5 大版本升级（breaking）。本轮处置：**记录不升级**（收口轮不引入
  breaking 变更）；列入 MVP02 工具链升级项。生产应用无影响。

## npm outdated（实际输出）

| 包                           | 当前    | 最新    | 处置                                |
| ---------------------------- | ------- | ------- | ----------------------------------- |
| @types/node                  | 22.20.1 | 22.20.2 | 非 breaking，可忽略                 |
| typescript                   | 5.9.3   | 7.0.2   | 大版本，不升级                      |
| vitest / @vitest/coverage-v8 | 3.2.7   | 5.0.0   | 大版本（同上 audit 项），MVP02 处理 |

## 平台依赖（未编译，静态声明）

- Android：SQLCipher (net.zetetic, BSD-style)、BouncyCastle (MIT)、AndroidX (Apache-2.0)
- iOS：SQLCipher (BSD)、CryptoKit/LocalAuthentication (系统)
- HarmonyOS：ArkData/HUKS（系统）
  以上在对应平台工具链可用时以实际解析结果复核（BLOCKERS B1–B3）。
