# LOGGING_AUDIT.md — 日志审计（RC PHASE P）

> 扫描：`grep -rn "console\." core/src`（TS/JS）+ Kotlin/Swift/ArkTS 源码人工审查
> 日期：2026-09-12

## 共享 Core（TS）

- `console.*` 调用：**0**（eslint `no-console: error` 强制，全仓无文件级豁免）
- 唯一输出例外：`core/scripts/validate-real-bill.ts` CLI（人审计工具）。
  审查其输出：仅打印**行数/坏行数/商户名/周期统计/影响清单等级与文案**；
  不打印 raw CSV 行、source transaction id、金额序列、fpSecret、文件内容。
  `paymentMethods`（如“招商银行信用卡(4417)”）为用户自己的卡尾号，仅在本地终端显示，
  不写入任何文件/日志系统。
- 测试代码（vitest 运行器内）：perf smoke 使用 `process.stdout.write` 输出耗时数字，
  无敏感内容。

## 平台源码（人工审查）

| 文件                                             | 日志调用                              | 评估                                     |
| ------------------------------------------------ | ------------------------------------- | ---------------------------------------- |
| Kotlin ×4（SqlCipher/Keystore/Biometric/Crypto） | 无 Log.d/i/w 调用                     | PASS                                     |
| Swift（SQLCipher adapter）                       | 无 print/os_log                       | PASS                                     |
| ArkTS（RelationalStore/HUKS/UserAuth）           | 无 hilog 输出                         | PASS                                     |
| app/App.uvue                                     | `console.log('DepMap launched')` 1 处 | 常量字符串，无敏感数据；保留（启动标记） |
| app/pages/*.uvue                                 | 无 console                            | PASS                                     |

## 红线核对（AGENTS §17）

raw CSV row / full user object / source transaction id / password / SQLCipher secret /
HUKS/Keychain material / fileEncryptionKey / fpSecret / decrypted depmap / 真实账单内容
—— **0 处输出**。

## 结论

sensitive log findings = 0。
