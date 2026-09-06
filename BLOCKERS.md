# BLOCKERS.md

> 只记录无法由代码本身解决、确实需要用户/外部环境介入的事项。
> 普通编译错误、测试错误、依赖冲突不属于 Blocker。

## Active blockers

当前：无。由 Agent 执行 PHASE 0 后根据真实环境更新。

## 可能后续需要用户提供

- [ ] Apple Developer Account
- [ ] macOS + Xcode 环境（如当前机器不是 Mac）
- [ ] iOS signing / provisioning
- [ ] Google Play Developer Account（正式发布时）
- [ ] Android release keystore（正式发布时）
- [ ] Huawei Developer / AppGallery Connect 身份
- [ ] HarmonyOS release signing
- [ ] 最终 Android applicationId
- [ ] 最终 iOS bundle identifier
- [ ] 最终 HarmonyOS bundleName
- [ ] 隐私政策 URL
- [ ] 真实微信账单（仅 REAL_DATA Gate 时）

## 禁止记录

不要在这里写：
- 密码
- API key
- keystore 密码
- signing secret
- 真实账单内容
- 银行卡号
