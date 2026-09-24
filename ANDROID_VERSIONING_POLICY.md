# ANDROID_VERSIONING_POLICY.md

> 生成时间：2026-09-23（ANDROID_2026_PRODUCTION_REALITY_CLOSURE · API36 工程全速收口）
> 依据：粘贴 Goal §16「Version Strategy」+ 批准契约 E3。
> 目标：versionCode / versionName 规则正式化，满足——单调递增、debug/internal/production 可追踪、
> release artifact 可映射 commit、后续 update 不冲突。禁止日期随机值。

---

## 1. 结论（规则）

### 1.1 versionName（对外，语义化版本）

```
<major>.<minor>.<patch>[-<prerelease>]
```

- **正式对外首发建议**：`1.0.0`（`.0` 表示对外里程碑稳定；是否采用由用户 R-3 决策，见
  `ANDROID_RELEASE_IDENTITY_DECISION.md`）。
- 当前代码值：`0.1.0-milestone`（内部里程碑名，**不是**对外最终版本）。
- prerelease 命名只允许 ASCII 且避免歧义：`-milestone`、`-alpha.N`、`-beta.N`、`-rc.N`。
- 规则：**每个正式对外发布 = 一次 semver 提升**；pre-release 只允许出现在非生产轨道。

### 1.2 versionCode（整型，Play 唯一真值）

- **单调递增，永不回头**：已上传 Play 的 versionCode 绝不复用、绝不下降。
- 编码方案（保持简单、可追踪、防冲突）：

```
1xxxxx  = 生产轨道（production）
2xxxxx  = 内部测试轨道（internal testing）
3xxxxx  = 封闭测试轨道（closed testing）
4xxxxx  = 开放测试轨道（open testing）
9xxxxx  = CI / 临时构建（不得上传生产）
```

- 每轨内部从 `xxx001` 顺序递增。首发建议：**生产 100001**（如果用户定案首发版本）。
  当前代码值 `versionCode = 1` 属于「未定案前占位」，首个真实上传前由用户确认统一按本策略改写。
- 理由：
  - 轨道前缀让"这个包是从哪条轨道来的"一眼可判（debug/internal/production 可追踪）；
  - 同一构建首次上传后，后续任何轨道升级自然大于当前值，不会冲突；
  - 不采用日期随机值（如 `202609231234`）——日期随机既不可读、又可能跨轨道混淆、还易在
    「同一天多次上传」时意外相等。

### 1.3 artifact → commit 可映射

- **每个 release artifact 必须携带来源 commit**：`versionName` + `versionCode` +
  `git rev-parse HEAD` 三者写入 `ANDROID_PRODUCTION_RC_MANIFEST.md` 的同一行。
- 构建时通过 `BuildConfig`（`buildConfigField`）把 `VERSION_NAME / VERSION_CODE /
GIT_SHA` 注入，About 页显示三者，避免"装的这版是什么"不可答。
  - 若本轮未给 `:app` 加 `buildConfigField`（改动有回归成本），则至少在 RC manifest 中保证
    可映射，并在报告中明确记录当前实现方式（进度项，不做隐性承诺）。

### 1.4 升级（update）规则

- 任何 bugfix / RC 调整：`versionCode 单调 +1`，`versionName` 按 semver 决定是否升版本段。
- 同一 release 的多次重传（审前撤回）也**必须**用新的 versionCode（旧号不上传可复用，
  但为简单和可审计，本轮要求已上传即不复用）。
- 不删除、不降级 Play 上任何轨道版本。

---

## 2. 当前状态（实查 `android/app/build.gradle.kts`）

```kotlin
versionCode = 1
versionName = "0.1.0-milestone"
```

| 字段                            | 当前值            | 本轮是否改动                     | 说明                                  |
| ------------------------------- | ----------------- | -------------------------------- | ------------------------------------- |
| versionCode                     | `1`               | **不改**（未定案前占位）         | 首个上传前用户 R-3 定案后按 §1.2 改写 |
| versionName                     | `0.1.0-milestone` | **不改**                         | 对外首发名用户 R-3 决策               |
| applicationId                   | `com.pdig.app`    | **不改**（OPEN，上架后不可变）   | R-1                                   |
| minSdk / targetSdk / compileSdk | 26 / 36 / 36      | targetSdk/compileSdk 本轮已升 36 | 见 ANDROID_PLATFORM_BASELINE.md       |

> 本轮不改 versionCode/Name 的原因：正式 keystore、applicationId、对外品牌均未定案，
> 且 `versionCode` 一旦上传即不可复用。**未到上传点，不擅自占用版本号。**

---

## 3. 可追踪性矩阵（示例，非最终值）

| 场景     | versionCode 区间 | versionName 示例               | 轨道               | 是否可上传 Production |
| -------- | ---------------- | ------------------------------ | ------------------ | --------------------- |
| 生产首发 | `1xxxxx`         | `1.0.0`                        | Production         | ✅                    |
| 内部测试 | `2xxxxx`         | `1.0.0-alpha.N` / `1.0.0-rc.N` | Internal           | ❌（测试轨道）        |
| 封闭测试 | `3xxxxx`         | `1.0.0-beta.N`                 | Closed             | ❌                    |
| 开放测试 | `4xxxxx`         | `1.0.0-beta.N`                 | Open               | ❌                    |
| CI/临时  | `9xxxxx`         | `0.0.1-ci.SHA[:7]`             | 任意（仅本次验证） | ❌                    |

---

## 4. 本策略与既有证据的一致性

- release AAB 当前 SHA256 / `versionName=0.1.0-milestone` / `versionCode=1`：
  属「canonical-freeze 基线构建」（`ANDRoid_PLATFORM_BASELINE.md` §5）。
- 正式首次上传时：用户确认 R-1..R-5 → 应用本策略产生新 versionCode（生产 1xxxxx）+ 新 AAB +
  新 RC manifest，**不复用**当前 `1 / 0.1.0-milestone` 上传。

---

## 5. 决策项（仍 HUMAN_REQUIRED）

| #    | 决策                                                             | 状态                                  |
| ---- | ---------------------------------------------------------------- | ------------------------------------- |
| R-3  | 对外 versionName 首版（建议 `1.0.0`）                            | OPEN（用户定案）                      |
| R-3b | 首发 versionCode 起点（建议生产 100001，或按 §1.2 轨道前缀方案） | OPEN                                  |
| —    | 是否采用 `buildConfigField` 注入 GIT_SHA 到 About                | 待用户确认改造范围（不影响本轮 Gate） |
