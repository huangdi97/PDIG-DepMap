# SUPPORT_URL_REQUIREMENT.md

> 生成时间：2026-09-22（ANDROID_CANONICAL_FREEZE → Production/Reality Closure 轮）
> 状态：**CONTENT READY / URL BLOCKED_BY_PUBLIC_URL**（无公开域名）
> 对应契约：Goal §17。

---

## 1. 需求

Google Play 商店列表需要提供**支持 URL**（support URL），指向用户可以获取帮助的页面。
上架前必须：

- [ ] 有公网可访问的支持页面（域名由用户决定/托管）
- [ ] 内容至少覆盖（草稿见 `store/SUPPORT_PAGE_DRAFT.md`）：
  - 产品简介（本地优先、无网络、数据本地存储）
  - 常见问题（数据存储 / 备份 / 恢复 / 删除 / 忘记口令）
  - 反馈渠道（支持邮箱或联系表单 —— **待用户提供**）
- [ ] URL 稳定、可访问

## 2. 当前状态

| 项 | 状态 |
|----|------|
| 支持页内容草稿 | ✅ READY（`store/SUPPORT_PAGE_DRAFT.md`） |
| 公网 URL | ❌ **BLOCKED_BY_PUBLIC_URL**（无公开域名） |
| 支持邮箱/联系渠道 | ❌ 待用户提供 |

## 3. 禁止

- ❌ 用 GitHub raw URL 冒充正式支持页。
- ❌ 虚构支持邮箱 / 联系渠道。

## 4. 解除流程（用户执行）

1. 选择托管方式（与隐私政策可同站）。
2. 将 `store/SUPPORT_PAGE_DRAFT.md` 内容发布为正式支持页，并补充真实联系渠道。
3. 将最终 URL 回填 Play Console + 本文件状态更新为 `URL_READY`。

## 状态

```text
SUPPORT_URL = BLOCKED_BY_PUBLIC_URL（内容 READY；需用户提供公网 URL 与联系渠道）
```

## 配套文档

- `store/SUPPORT_PAGE_DRAFT.md`（内容草稿）
- `PRIVACY_URL_REQUIREMENT.md`（隐私 URL 需求）
