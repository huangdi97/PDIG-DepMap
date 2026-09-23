# ANDROID_CLOSED_TEST_PLAN.md

> 生成时间：2026-09-23（ANDROID_2026_PRODUCTION_REALITY_CLOSURE · API36 工程全速收口）
> 依据：粘贴 Goal §60–§64 + 批准契约 F4。
> 状态：**计划就绪 / 执行 BLOCKED_BY_STORE_ACCOUNT**（无 Play 账号、无测试者）。
> 依据：若开发者账号在 2023-11-13 之后创建且为个人账号，Google Play 要求 Production 前
> 满足 closed-testing 要求 —— 12 名测试者 / 连续 14 天 / 持续 opt-in（以 Play Console 实际要求为准）。

---

## 1. 目标与前提

- **目标**：满足 Google Play 个人新账号的 closed-testing 要求，并在过程中收集真实用户反馈。
- **前提**：`ANDROID_PLAY_CONSOLE_READINESS.md` 项 1–3 完成（账号 + R-1..R-3 + Upload Key）。
- **诚实声明**：本轮**未启动** closed test（没有账号、没有真实测试者，绝不伪造 12 个假账号、
  绝不 fake 14 天 clock）。

---

## 2. 测试者与数据伦理

- 测试者必须为**真实人员**（≥12 名，Google 以「持续 opt-in 且测试者已安装」口径计数）。
- **不要求测试者提供真实金融数据**：提供安全的 synthetic fixture
  （`local_private/e2e_fixtures/wechat_synthetic.csv` 同类；正式 reviewer 包一并提供）。
- 测试者反馈通道：Play Console 反馈 + 支持邮箱（最终内容见 store/SUPPORT_PAGE_DRAFT.md）。
- 隐私：测试者数据若被收集须遵循 `REAL_DATA_PRIVACY_PROTOCOL.md` + 隐私政策草稿。

---

## 3. 测试轨道发布流程（账号到位后执行）

1. 创建 Closed Testing 轨道（track = closed）。
2. 上传正式候选 AAB（versionCode 按 `ANDROID_VERSIONING_POLICY.md` 规则：closed 前缀 `3xxxxx`）。
3. 填写 release notes（见 store/RELEASE_NOTES_DRAFT.md）。
4. 添加测试者（邮箱列表 / Google Group，≥12 人，**真实**）。
5. 发送 opt-in 链接，等待测试者安装 + 保持安装（14 天连续要求以 Play Console 状态为准）。
6. 期间收集：crash / ANR / 文案困惑 / 导航问题 / 导入问题 / 误判 / 无障碍问题。

---

## 4. 测试矩阵（每个测试者至少覆盖关键链）

| # | 关键路径 | 验收目标 |
|---|----------|----------|
| 1 | 安装 + 首启 | 冷启动锁屏 → 解锁 → 首页 |
| 2 | Onboarding（首次） | 引导完成 |
| 3 | 导入（synthetic CSV） | Node Resolution 正确、确认导入 |
| 4 | Proposal Review | 候选关系出现；未确认不进入 Reality |
| 5 | Confirm Reality | 确认后 Dependency 建立 |
| 6 | Scenario（replace/expiring/close） | Setup→Impact→Plan→Action→Verification→verified |
| 7 | Backup | 导出 `.depmap` + UI/文件一致 |
| 8 | Restore | 正确密码恢复 / 错误密码拒绝 / 篡改拒绝 |
| 9 | Delete All Data | 删除后清空 |
| 10 | App Lock 前后台 | 回后台→回前台重新锁定，不可绕过 |

> 反馈表模板（正式阶段放 `local_private/` 或 Play 渠道）：
> 可复现路径 / 期望 / 实际 / 设备型号与 Android 版本 / 截图。

---

## 5. 反馈管理与 bug 闭环

- 任何真实 defect：进入缺陷清单 → 修复 → **新 versionCode** → 重新上传 closed track。
- 不为了「凑满 14 天」冻结明显 bug（粘贴 Goal §62）。
- 修复后通知测试者更新；反馈关闭条件：测试者确认或复测通过。

---

## 6. Production Access 申请

- Closed testing 满足后，在 Play Console 申请 Production access：
  真实回答 App 用途 / 测试过程 / 测试者数量 / 反馈 / 发布就绪度。
- **禁止夸大测试规模**（粘贴 Goal §64）。

---

## 7. 当前阻塞（BLOCKERS 引用）

| Gate | 状态 | 精确 blocker | Required action |
|------|------|--------------|-----------------|
| `PLAY_CLOSED_TEST_READY` | BLOCKED_BY_STORE_ACCOUNT | 无 Play 开发者账号、无测试者 | 用户注册账号 + 提供 ≥12 真实测试者 |
| `PLAY_PRODUCTION_ACCESS_READY` | BLOCKED_BY_CLOSED_TEST_NOT_RUN | closed test 未开始（上一条未解除） | 先完成 closed test |

## 8. 状态

```text
ANDROID_CLOSED_TEST_PLAN = READY（计划完成；执行 BLOCKED_BY_STORE_ACCOUNT）
PLAY_CLOSED_TEST_REQUIREMENT = BLOCKED（账号类型确认后才能判定 NOT_APPLICABLE 或执行）
```