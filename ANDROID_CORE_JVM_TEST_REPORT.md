# ANDROID_CORE_JVM_TEST_REPORT

> 轮次：Android P0 Runtime Closure（P0-2）
> 日期：2026-09-16
> 范围：`:core` 真实 JVM 单元测试套件（此前为 `NO-SOURCE`）
> 结论：**PASS** — 6 个 suite / **71 tests / 0 failures / 0 errors / 0 skipped**

---

## 1. 命令与结果

```
export JAVA_HOME="<ANDROID_STUDIO_HOME>/jbr"
export PDIG_ASCII_BUILD_ROOT=%USERPROFILE%/pdig-build
cd <repo>\android
./gradlew -I %USERPROFILE%/pdig-gradle/ascii-build.gradle.kts :core:test --console=plain
```

```
BUILD SUCCESSFUL in 41s
```

测试报告：`%USERPROFILE%\pdig-build\core\reports\tests\test\index.html`
结果 XML：`%USERPROFILE%\pdig-build\core\test-results\test\TEST-*.xml`

## 2. 测试计数（从 XML 汇总，非声称）

| suite                                                   |  tests | failures | errors |
| ------------------------------------------------------- | -----: | -------: | -----: |
| `com.pdig.core.domain.DomainInvariantTest`              |     15 |        0 |      0 |
| `com.pdig.core.impact.ImpactKernelTest`                 |     11 |        0 |      0 |
| `com.pdig.core.plan.PlanReadinessTest`                  |     14 |        0 |      0 |
| `com.pdig.core.schema.MigrationSemanticsTest`           |     10 |        0 |      0 |
| `com.pdig.core.statemachine.GraphRevisionSemanticsTest` |      7 |        0 |      0 |
| `com.pdig.core.statemachine.StateMachineTest`           |     14 |        0 |      0 |
| **合计**                                                | **71** |    **0** |  **0** |

skipped = 0（没有用 skip 制造绿色）。

## 3. 覆盖的不变量（逐类）

### DomainInvariantTest（15）

- 机器永不产生 `criticality=required`：`unknownCannotAutoBecomeRequired`
- `criticality` 只允许 `required` / `unknown` 两态
- **Proposal ≠ Reality**：`candidateIsNotANodeUntilAccepted`
- **done ≠ verified**：`doneIsNotVerified`、`resolutionCountsDoneNotVerification`
- 漂移检测不改 Reality：`driftDetectionDoesNotMutateReality`
- 待确认提案只会降低 readiness、不会提升：`pendingProposalsLowerReadinessNeverRaiseIt`
- 关系/分组使用必须过运行时注册中心校验：`relationUseIsValidated`、`groupUseIsValidatedAgainstTheRuntimeRegistry`、`runtimeRegistryOnlyContainsSupportedRelations`
- `logicalKey` 跨平台稳定、`groupKey` 与成员顺序无关且去重
- `impactStateKey` 往返稳定

### ImpactKernelTest（11）

- `required` 边且无替代 ⇒ MUST_CHANGE
- 未确认（`unknown`）不是"未受影响"，而是 NEEDS_REVIEW
- 存在未确认替代 ⇒ NEEDS_REVIEW
- 已确认 ANY 组：成员全失 ⇒ MUST_CHANGE；仍有存活成员 ⇒ BACKUP_PATH
- **Proposal 即使 confidence=0.999 也不被提升为 MUST_CHANGE**
- `access` 能力边不参与支付传播；非支付能力 key 被忽略
- 环图终止、每个 key 只处理一次
- 多次运行输出顺序确定
- 原始操作始终是清单最后一项

### PlanReadinessTest（14）

- 未解决的 MUST_CHANGE ⇒ blocked
- 待确认 NEEDS_REVIEW ⇒ review_required
- blocked 优先于 review_required
- 全部 claimant done 才解决该 key（不因无关动作而"减一"）
- 过期证据 ⇒ review_required

### MigrationSemanticsTest（10，跑在真实 SQLite 上）

用 `sqlite-jdbc` 内存库 + `JdbcTestDriver`（测试替身，仅 Domain/Schema 层用得到的能力），验证 `Migrations.kt` 头部契约：

- 全新库迁移到当前版本；重复迁移 50 次严格 no-op
- 未来 `schema_version` **明确拒绝**（不猜测兼容）
- v1 建基础表、v1→v3 补 source-scoped fingerprint / plan 表，且重建临时表必须 DROP
- v2 保留 evidence 行并归属到 legacy SourceInstance
- 迁移失败回滚、不推进版本号、不留半迁移状态
- legacy WeChat SourceInstance 唯一且确定性

### GraphRevisionSemanticsTest（7）

- 会 bump 的事件与永不 bump 的事件**互斥**（`bumpsOn` ∩ `neverBumpsOn` = ∅）
- 单调递增、原子性

### StateMachineTest（14）

- ChangePlan / Drift / Candidate / Verification 的合法与非法迁移
- 终态不可再流转
- **Evidence 记录永远不会自动把动作变成 verified**

## 4. 本轮修复的构建/测试缺陷（不降 Gate）

### 4.1 JUnit Platform 未绑定（测试根本没跑）

只声明 `kotlin("test")` 时 JVM 上没有绑定的测试框架，Gradle 用默认 JUnit4 runner 加载类，
表现为全部 `initializationError` / `ClassNotFoundException`。

修复（`android/core/build.gradle.kts`）：

```kotlin
testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
tasks.withType<Test>().configureEach { useJUnitPlatform() }
```

### 4.2 Windows 中文路径 + Gradle 8.9 `@argfile` → 测试类全部 ClassNotFoundException

**表现**：加了 JUnit5 后，10 个测试类**全部** `ClassNotFoundException`，但同样的类用
`java -cp <含中文路径>` 手动加载**成功**。

**根因（已用最小实验复现）**：

| 方式                                   | 结果                        |
| -------------------------------------- | --------------------------- |
| 命令行 `-cp`（含中文）                 | ✅ 加载成功                 |
| `@argfile` 以 **UTF-8** 写入（含中文） | ❌ `ClassNotFoundException` |
| `@argfile` 以 **GBK** 写入（含中文）   | ✅ 加载成功                 |

Gradle 8.9 在 Windows 上把 test worker 的 classpath 写进 UTF-8 的 `@argfile`
（`%TEMP%\gradle-worker-classpath*.txt`），JVM launcher 却按系统 ANSI 代码页（本机 GBK）解析，
于是所有含非 ASCII 字符的 classpath 条目被破坏。

**这是构建环境缺陷，不是代码缺陷。** 尝试过的方案：

- `mklink /J` 建 ASCII 联接 → **无效**，Gradle 会把 junction 规范化回真实中文路径（已验证）。

**采用的修正**：init script 把 **build 输出目录**重定向到纯 ASCII 路径，使 test worker classpath
全 ASCII；源码仍在原中文路径，仓库构建脚本零改动。

- init script：`%USERPROFILE%\pdig-gradle\ascii-build.gradle.kts`
- 触发条件：环境变量 `PDIG_ASCII_BUILD_ROOT`
- 诊断脚本：`local_private/print_cp.gradle.kts`（打印 test runtime classpath / testClassesDirs）

### 4.3 一处测试自身 bug（不是产品 bug）

`MigrationSemanticsTest.futureSchemaVersionIsRejected` 在**全新库**上直接
`INSERT INTO meta`，而此时 `meta` 尚未建立。改为：先 `migrate()` 到当前版本，
再把 `schema_version` 改成 99 —— 这样被断言的行为只剩"未来版本必须明确拒绝"。

## 5. 诚实边界

- `:core` 7 个源文件中，`JdbcTestDriver` 是**测试替身**，只实现 Domain/Schema 层实际用到的
  `SqliteDriver` 能力，不支持并发、不支持跨连接。它不是产品代码路径。
- 真实 Android `SQLiteDatabase`/SQLCipher 路径**不在**本套件覆盖范围内（由
  `connectedDebugAndroidTest` 与 E2E 覆盖）。
- `:app:testDebugUnitTest` 本轮为 **NO-SOURCE**（app 模块当前无 JVM 单测），如实记录，不用 core 的数字冒充。
