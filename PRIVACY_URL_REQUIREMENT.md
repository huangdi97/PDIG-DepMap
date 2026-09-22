# PRIVACY_URL_REQUIREMENT.md

> 生成时间：2026-09-22（ANDROID_CANONICAL_FREEZE → Production/Reality Closure 轮）
> 状态：**CONTENT READY / URL BLOCKED_BY_PUBLIC_URL**（无公开域名）
> 对应契约：Goal §17「无公开域名时保持 BLOCKED_BY_PUBLIC_URL，不以 GitHub raw URL 冒充正式页面」。

---

## 1. 需求

Google Play 要求每个应用在商店列表中提供**可公开访问的隐私政策 URL**。上架前必须：

- [ ] 有公网可访问的隐私政策页面（域名由用户决定/托管）
- [ ] 内容至少覆盖（草稿见 `PRIVACY_POLICY_DRAFT.md` / `store/DATA_SAFETY_DRAFT.md`）：
  - 数据仅本地存储、不上传
  - 不收集个人信息
  - 无第三方统计 / 广告 / 崩溃 SDK
  - 权限说明（USE_BIOMETRIC / USE_FINGERPRINT）
  - 数据删除方式（卸载 / 清除数据 / L-37 删除所有数据）
  - 联系方式（待用户提供）
- [ ] URL 稳定（不随版本变化）

## 2. 当前状态

| 项 | 状态 |
|----|------|
| 隐私政策内容草稿 | ✅ READY（`PRIVACY_POLICY_DRAFT.md`） |
| 公网 URL | ❌ **BLOCKED_BY_PUBLIC_URL**（无公开域名） |
| 联系人 | ❌ 待用户提供 |

## 3. 禁止

- ❌ 用 GitHub raw URL（`raw.githubusercontent.com/...`）冒充正式隐私页面 ——
  它不是稳定、面向用户、可受控的隐私政策托管，且内容可能被误读为仓库文件。
- ❌ 把草稿页面声明为「已上线隐私政策」。

## 4. 解除流程（用户执行）

1. 选择托管方式：自有域名 / GitHub Pages / 静态托管 / 隐私政策服务（由用户决定）。
2. 将 `PRIVACY_POLICY_DRAFT.md` 内容发布为正式页面。
3. 把最终 URL 回填到 Play Console 商店列表 + 本文件状态更新为 `URL_READY`。

## 状态

```text
PRIVACY_URL = BLOCKED_BY_PUBLIC_URL（内容 READY；需用户提供公网 URL）
```

## 配套文档

- `PRIVACY_POLICY_DRAFT.md`（内容草稿）
- `store/DATA_SAFETY_DRAFT.md`（Data safety 表单）
- `SUPPORT_URL_REQUIREMENT.md`（支持 URL 需求）
