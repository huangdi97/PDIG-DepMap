# ANDROID_PLAY_CONSOLE_READINESS.md

> 生成时间：2026-09-23（ANDROID_2026_PRODUCTION_REALITY_CLOSURE · API36 工程全速收口）
> 依据：粘贴 Goal §45–§60 + 批准契约 F1。
> 目标：把 Play Console 首建 App 所需的**每一个输入字段**与当前状态 / 阻塞映射清楚，
> 让用户拿到账号后可按顺序直接执行。不代替用户注册账号或做不可逆决策。

---

## 0. 前置事实（本轮已定 / 未定）

| 项 | 状态 |
|----|------|
| Google Play 开发者账号 | **无**（未注册、未付款）→ `PLAY_DEVELOPER_IDENTITY_READY = BLOCKED_BY_STORE_ACCOUNT`（E-5） |
| applicationId | `com.pdig.app`（**占位，OPEN**，上架后不可改）→ R-1 |
| 最终品牌（中/英文名） | **NOT_FINAL_BRAND**（工作名：个人数字依赖图 / PDIG）→ R-2 |
| versionName / versionCode | `0.1.0-milestone` / `1`（占位；首个上传前按 `ANDROID_VERSIONING_POLICY.md` 定案）→ R-3 |
| 生产 keystore | **未提供**（本轮 non-prod 验证）→ K-1..K-4（E-2） |
| 隐私/支持公网 URL | **无**（草稿就绪）→ E-6 |
| 真机 / 真实数据 | **无**（本轮 BLOCKED）→ E-1 / E-7 |

**顺序原则**（粘贴 Goal §45 §47）：先冻结 Release Identity（R-1..R-3）→ 再注册账号 → 创建 App →
启 Play App Signing → 上传 AAB → 填 Store Listing → 提审。**不得**抢先创建临时 applicationId 的正式 App。

---

## 1. 创建正式 App 的输入清单（Play Console 实际字段）

### 1A. App 基本信息（创建页）

| 字段 | 值（本轮就绪状态） | 阻塞/来源 |
|------|--------------------|-----------|
| App name（默认语言） | 最终品牌定案后填写（工作名「个人数字依赖图」） | R-2 |
| Default language | `zh-CN` 或按最终品牌（建议中文 + 英文均可） | R-2 |
| App / Game | **App**（工具/财务依赖梳理，非游戏） | 就绪 |
| Free / Paid | **Free**（产品无内购设计；最终以用户商业决策为准） | 用户 R-4（未定则标记 HUMAN_REQUIRED） |
| Developer declarations | 按要求勾选（隐私政策、出口法规、儿童隐私等） | 需正式提交时逐项确认 |

### 1B. Release Identity（创建前必须先定，`ANDROID_RELEASE_IDENTITY_DECISION.md` R-1..R-5）

| 决策 | 状态 | 就绪材料 |
|------|------|----------|
| R-1 applicationId | OPEN（占位 `com.pdig.app`） | 决策文档 §1 |
| R-2 品牌名 ZH/EN | OPEN | 决策文档 §2 + ANDROID_BRAND_ASSET_SPEC.md |
| R-3 versionName/versionCode | OPEN（策略就绪） | ANDROID_VERSIONING_POLICY.md |
| R-4 收费模式 | OPEN | 本文件 1A |
| R-5 Play App Signing 方案 | 决策文档就绪（PLAY_APP_SIGNING_DECISION.md） | 用户确认后启用 |

### 1C. Play App Signing（上传前）

| 项 | 本轮状态 |
|----|----------|
| Upload Key | 由用户在 `keytool` 生成（提供路径/密码到本机安全位置）；本轮已就绪 `ANDROID_PRODUCTION_SIGNING_RUNBOOK.md` 步骤 |
| App Signing Key | Play 托管；上传后由 Play Console 显示指纹（记录到 PLAY_APP_SIGNING_EVIDENCE.md） |
| 密钥备份/恢复 | PLAY_APP_SIGNING_DECISION.md 已给出方案（下载 .pepk / 上传密钥记录） |

### 1D. Store Listing（主列表）

| 字段 | 就绪内容 | 阻塞 |
|------|----------|------|
| App name | store/STORE_LISTING_DRAFT.md ++ R-2 | R-2 |
| Short description | ✅（≤80 字符，草稿） | — |
| Full description | ✅（草稿，含诚实边界） | — |
| App icon（512×512） | 占位 `ic_launcher` → `NOT_FINAL_BRAND_ASSET` | R-2 + ANDROID_BRAND_ASSET_SPEC.md |
| Feature graphic（1024×500） | 无 → NOT_FINAL_BRAND_ASSET | R-2 |
| Phone screenshots | 计划就绪（ANDROID_SCREENSHOT_SHOT_LIST.md）；截图需最终品牌定案后拍摄 | R-2 + 真机/AVD |
| Tablet screenshots | 计划含 ≥600dp AVD（pdig_api36_tablet） | AVD 可用（本轮已建） |
| Contact email | 待用户提供（store/SUPPORT_PAGE_DRAFT.md） | 用户 |
| Support URL | 草稿就绪；公网 URL 无 | E-6 |
| Privacy URL | 草稿就绪；公网 URL 无 | E-6 |

### 1E. App Content（见 `ANDROID_STORE_COMPLIANCE_REPORT.md` §4）

隐私政策 URL / 广告声明 / App access / Target audience / Content rating (IARC) / Data safety /
高敏权限声明 / News 声明 —— 均可预填，最终提交在 Play App 内完成（E-5）。

### 1F. Reviewer Instructions（粘贴 Goal §55）

- PDIG **无云账号系统**：不要虚构测试账号。
- Reviewer 访问路径（将写入 Play Console 备注）：
  1. 安装后首屏为 App Lock（无设备凭据时点「已知悉风险，本次进入」解锁；有凭据走系统验证）。
  2. 首页 → 数据来源与导入 → 导入账单文件 → 用**内置示例 CSV**（将随 AAB 一起提供的 synthetic fixture，
     不含真实用户数据）→ Node Resolution → 确认导入。
  3. 待确认服务 → 确认候选关系；基础设施 → 招商银行储蓄卡(1234) → 标记为必需。
  4. 常用场景 → 三个支付场景逐一演示：影响范围 → 创建变更计划 → 标记完成 → 确认验证。
  5. 设置 → 备份/恢复/删除所有数据（删除后会再次要求验证）。
- 说明：示例数据为合成数据（`local_private/e2e_fixtures/wechat_synthetic.csv` 是本地测试夹具，
  正式 reviewer 包中会附一个**不含任何真实信息的示例文件**，内容在 RC 阶段提供）。

---

## 2. 轨道顺序（Internal → Closed → Production）

| 轨道 | 本轮状态 | 前置 |
|------|----------|------|
| Internal Testing | BLOCKED_BY_STORE_ACCOUNT（无账号，无法上传） | 账号 + R-1..R-3 + Upload Key |
| Closed Testing | BLOCKED（计划就绪 `ANDROID_CLOSED_TEST_PLAN.md`；12 testers / 14 天 / continuous opt-in） | 账号 + Internal 通过 |
| Production Access | BLOCKED（需满足 closed-testing 要求或账号类型豁免） | Closed 数据 + Google 问卷 |
| Production | BLOCKED（全部前置） | 上述全部 + `ANDROID_PRODUCTION_RELEASE_READY` |

---

## 3. 待用户最小动作清单（一次性完成一个即可继续）

1. **冻结 Release Identity**：回复 R-1..R-5 三个决策（applicationId / 品牌 / 版本 + 收费）。
2. **注册 Google Play 开发者账号**并完成身份验证 + $25 付款（Google 官方流程）。
3. **生成 Upload Key**：按 `ANDROID_PRODUCTION_SIGNING_RUNBOOK.md` 用 keytool 生成，
   把 keystore 放到本机安全位置，告诉我路径即可（密码不发送到聊天）。
4. **发布隐私/支持页**到任一公网 URL（GitHub Pages / 自有域名都行）。
5. （后续）**真机**与 **1 份本人授权账单**。

> 每完成一项，本文件与 BLOCKERS.md 对应条目即关闭，后续步骤自动推进。

---

## 4. 状态

```text
ANDROID_PLAY_CONSOLE_READINESS = READY（清单与材料齐备；执行依赖 R-1..R-5 + Play 账号）
PLAY_DEVELOPER_IDENTITY_READY = BLOCKED_BY_STORE_ACCOUNT
PLAY_PACKAGE_REGISTRATION_READY = BLOCKED_BY_APPLICATION_ID_OPEN（R-1 未定 + 无账号）
PLAY_APP_CREATED = BLOCKED_BY_STORE_ACCOUNT
PLAY_APP_SIGNING_READY = BLOCKED_BY_MISSING_UPLOAD_KEY（K-1..K-4）
PLAY_INTERNAL_TEST_READY = BLOCKED_BY_STORE_ACCOUNT
PLAY_CLOSED_TEST_READY = BLOCKED_BY_STORE_ACCOUNT（计划就绪）
PLAY_PRODUCTION_ACCESS_READY = BLOCKED_BY_CLOSED_TEST_NOT_RUN（无账号更无测试者）
```