# store/DATA_SAFETY_DRAFT.md

> 状态：**DRAFT / NOT_SUBMITTED**（Google Play Data safety 表单草稿）
> 生成时间：2026-09-22（ANDROID_CANONICAL_FREEZE → Production/Reality Closure 轮）
> 原则：如实回答；不声明「100% 安全」；以实际清单权限与代码事实为准。

---

## 1. 数据收集声明（Data collection）

| 数据类别 | 是否收集 | 说明 |
|----------|----------|------|
| 位置 | ❌ 否 | 无 LOCATION 权限 |
| 个人信息（姓名/邮箱/电话） | ❌ 否 | 无账号系统、无通讯录/短信权限 |
| 财务信息 | ❌ 否 | 不收集银行卡号/账号；账单文件仅本机解析（session-only） |
| 健康/健身 | ❌ 否 | — |
| 消息 | ❌ 否 | — |
| 照片/视频 | ❌ 否 | 无存储权限（SAF 按需授权） |
| 音频 | ❌ 否 | — |
| 应用活动 | ❌ 否 | 无 analytics |
| 应用崩溃信息 | ❌ 否 | 无 crash SDK、无 telemetry |
| 设备/其他标识符 | ❌ 否 | 无广告 ID、无设备指纹采集 |

**结论：不收集、不共享任何数据类别。**

> 唯一例外：用户**主动提供**的账单文件（导入功能）与**主动导出**的 `.depmap` 备份 ——
> 两者均为用户发起的本机操作，不上传、不收集。

## 2. 数据共享（Data sharing）

- **不与第三方共享任何数据**（无广告、无分析、无社交分享）。
- 无后端服务器（NO BACKEND / NO ACCOUNT / NO CLOUD SYNC）。

## 3. 安全处理（Security practices）

| 项 | 说明 |
|----|------|
| 传输加密 | 不适用（无网络传输） |
| 存储加密 | ✅ 本地数据库 SQLCipher（AES-256）；密钥由 Android Keystore 包裹存储 |
| 删除机制 | 卸载应用 / 清除数据 / 设置内「删除所有数据」（L-37） |
| 备份 | 用户主动导出 `.depmap`（AES-256-GCM + 用户口令） |

## 4. 合规红线

- 不写「100% 安全」「银行级安全」。
- 不暗示「官方银行合作」或「自动管理银行账户」。

## 状态

```text
DATA_SAFETY_DRAFT = READY（表单草稿；提交前由用户在 Play Console 按界面确认）
```
