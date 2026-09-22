# PLAY_APP_SIGNING_DECISION.md

> 生成时间：2026-09-22（ANDROID_CANONICAL_FREEZE → Production/Reality Closure 轮）
> 状态：**DECISION PACKAGE READY / DECISION OPEN（需用户选择）**
> 本文解释 Google Play App Signing 相关概念与项目推荐方案，**不替用户创建账号、不生成正式 key、不上传任何密钥**（Goal §11）。

---

## 1. 背景：为什么需要关心两把 key

Android 应用在 Play 上的签名涉及**两把不同的 key**：

```text
┌────────────────────────────────────────────────────────────────┐
│  Google Play 用户下载的 APK 签名  =  App Signing Key            │
│    （由 Google 托管，若启用 Play App Signing）                  │
├────────────────────────────────────────────────────────────────┤
│  开发者上传到 Play 的 AAB 签名    =  Upload Key                 │
│    （本地生成，只用于"上传"这个动作）                            │
└────────────────────────────────────────────────────────────────┘
```

- **Upload Key**：你在本地 keystore 里生成的 key，用于签名上传到 Play Console 的 AAB。
- **App Signing Key**：真正给最终 APK 签名的 key。
  - **不启用 Play App Signing**：App Signing Key = Upload Key（同一把），由你自持。
  - **启用 Play App Signing**：App Signing Key 由 Google 生成/托管；你只需保管 Upload Key。

---

## 2. 三个概念（澄清）

| 概念 | 是什么 | 本项目对应 |
|------|--------|-----------|
| **Upload Key** | 本地生成的签名密钥，仅用于「上传 AAB 到 Play」这一步 | `ANDROID_PRODUCTION_SIGNING_*` 中的生产 keystore（K-1） |
| **App Signing Key** | 用户设备上 APK 的最终签名密钥 | 启用 Play App Signing 后由 Google 托管；不启用 = 与 Upload Key 相同 |
| **Google Play App Signing** | Play 的一项服务：Google 保管 App Signing Key，开发者只上传（用 Upload Key 签的）AAB | 推荐启用（见 §4） |

> 对用户可感知的影响：**更新应用时要求签名一致** —— 而「一致」的比较对象是 App Signing Key，
> 不是 Upload Key。所以保管好哪把 key 决定了丢失时的后果（§3）。

---

## 3. key 丢失 / 泄露的后果

| 场景 | 不启用 Play App Signing | 启用 Play App Signing |
|------|------------------------|----------------------|
| Upload Key 丢失 | **灾难**：无法再签更新（应用身份 = 该 key） | **可恢复**：向 Play 申请「重置上传密钥」（需验证身份，约 1 周） |
| App Signing Key 丢失 | 同左（就是 Upload Key） | Google 托管，不受影响 |
| Upload Key 泄露 | 攻击者可签「合法更新」推毒 | 仅能冒名上传 AAB；可重置上传密钥切断 |
| App Signing Key 泄露 | 同上，灾难级 | Google 侧处理（风险集中在 Google 一侧） |

---

## 4. 项目推荐方案（供用户决策，不默认执行）

```text
推荐：启用 Google Play App Signing
```

理由：

1. **单人 / 小团队项目**丢失本地 keystore 的概率远大于 Google 数据中心的故障概率；
   启用后 Upload Key 丢失仍可恢复，这是最便宜的保险。
2. **Play 的新应用（2021 年 8 月后创建）必须启用 Play App Signing** —— 新应用没有选择权，
   本应用尚未创建 Play 应用，届时默认即为「启用」。本决策包提前解释，避免届时被动。
3. 启用不影响本地构建：本地仍用生产 keystore（Upload Key）签 AAB；Play 负责最终 APK 签名。

```text
备选（不推荐）：自持签名密钥
  仅当：组织有严格合规要求必须完全自持签名、且有可靠的密钥管理系统（HSM / 多人门限）。
```

> **决策权在用户**。Play Console 首次上传 AAB 时选择「Google 管理签名密钥」即启用。

---

## 5. key backup / rotation / recovery（启用后的运维策略）

| 主题 | 策略 |
|------|------|
| **Backup** | ① keystore 文件（Upload Key）→ 密码管理器 + 离线加密介质 ×2（异地）；② 三个口令分开放置（密码管理器 + 纸质副本）；③ 记录证书 SHA-256 指纹（可验证备份是否完好） |
| **Rotation（轮转）** | ① **App Signing Key 不可轮转**（更换 = 新应用）；② **Upload Key 可重置**：Play Console → App signing → 请求重置上传密钥（用新的 key pair 签名替换请求，需账号身份验证） |
| **Recovery** | ① Upload Key 丢失 → Play Console 申请重置（约 1 周，需公司/身份证明）；② App Signing Key 丢失（未启用托管时）→ **无法恢复**，只能换 applicationId 重新上架 |
| **泄露响应** | 立即申请重置 Upload Key；同时撤销并重建本地 keystore；评估是否需要新 applicationId（App Signing Key 未泄露则不必） |

---

## 6. 与本仓库的衔接

| 仓库内对象 | 说明 |
|------------|------|
| `ANDROID_PRODUCTION_SIGNING_RUNBOOK.md` §5 | Play Console 设置步骤（创建应用 → 选「Google 管理签名密钥」→ 上传 AAB） |
| `ANDROID_PRODUCTION_SIGNING_ACCEPTANCE.md` §5 | 备份 / 轮转 / 恢复的口径（本文件是其展开） |
| `ANDROID_RELEASE_IDENTITY_DECISION.md` R-4 | 「是否启用 Play App Signing」是待用户决策项之一 |
| `NATIVE_RELEASE_MATRIX.md` §7 | Store 复用与更新项（技术栈描述待更新为原生） |

---

## 7. 状态

```text
PLAY_APP_SIGNING_DECISION = PACKAGE_READY / DECISION_OPEN（推荐启用，待用户确认）
  阻塞类别：PRODUCT_DECISION_REQUIRED（+ 关联 STORE_ACCOUNT_REQUIRED）
  必需外部输入：用户在创建 Play 应用时选择是否启用（或授权默认启用）
  禁止：替用户创建 Play 账号 / 生成正式 key / 上传任何密钥
```
