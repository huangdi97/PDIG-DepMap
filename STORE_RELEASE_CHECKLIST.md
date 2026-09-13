# STORE_RELEASE_CHECKLIST.md — PDIG 商店发布清单（Production RC V1）

> 状态：`PASS` / `FAIL` / `BLOCKED` / `NOT_RUN` / `PENDING_USER`
> 原则：**本轮只做到 Store Submission Ready；不自动提交任何商店。**

---

## 1. Product
- [ ] 核心 Job 明确：模拟更换 / 注销一张银行卡
- [ ] 产品范围冻结（MVP01–03）
- [ ] 无 MVP04 能力混入
- [ ] 无 demo/fixture 写入 Release Graph

## 2. UI
- [ ] 一级导航冻结
- [ ] 所有页面 Empty/Error/Loading 状态
- [ ] 中文文案、无工程词
- [ ] Design System 落地

## 3. Accessibility
- [ ] 触控区 / 对比度 / 字号 / label / 非仅颜色

## 4. Privacy
- [ ] `docs/PRIVACY_POLICY_DRAFT.md` 与实现一致
- [ ] `docs/USER_NOTICE_DRAFT.md`
- [ ] `store/PRIVACY_DISCLOSURE_MATRIX.md`
- [ ] 无绝对化承诺
- [ ] Privacy Policy URL — **PENDING_USER**
- [ ] Support URL — **PENDING_USER**

## 5. Security
- [ ] secret scan PASS
- [ ] network = 0
- [ ] analytics/telemetry/ads = 0
- [ ] crypto golden PASS
- [ ] release 日志无敏感数据

## 6. Permissions
- [ ] Android manifest 每项权限有用途
- [ ] iOS Info.plist usage strings
- [ ] HarmonyOS module permissions

## 7. Version
- [ ] App version 冻结（见 `docs/RELEASE_VERSION_MATRIX.md`）
- [ ] Build number 策略
- [ ] Schema version = 3
- [ ] DEPMAP formatVersion = 1
- [ ] Parser / Adapter 版本记录

## 8. Icons / Splash
- [ ] Android icon 各密度
- [ ] iOS AppIcon 尺寸集
- [ ] HarmonyOS icon
- [ ] Splash / 启动图
- [ ] `docs/APP_ICON_ASSET_SPEC.md`
- 当前状态：**BLOCKED**（无正式 asset；规格文档已就绪）

## 9. Screenshots
- [ ] 截图场景计划（`store/SCREENSHOT_PLAN.md`）
- [ ] 实际截图 — **BLOCKED**（无运行环境）

## 10. Description
- [ ] `store/STORE_LISTING_ZH.md`（名称/副标题/短描述/长描述/关键词/特性/隐私摘要）
- [ ] 不虚构奖项/用户量/银行合作/官方认证

## 11. Signing
- [ ] Android keystore — **PENDING_USER**（B4）
- [ ] iOS signing — **PENDING_USER**（B9）
- [ ] HarmonyOS signing — **PENDING_USER**（B7）
- [ ] 私钥不入 Git

## 12. Build
- [ ] Android AAB/APK — **BLOCKED**（B1）
- [ ] HarmonyOS HAP/App Pack — **BLOCKED**（B2）
- [ ] iOS archive — **BLOCKED**（B3）

## 13. Device Test
- [ ] Android 真机/模拟器 smoke — **BLOCKED**
- [ ] HarmonyOS 真机 — **BLOCKED**
- [ ] iOS 真机 / TestFlight — **BLOCKED**

## 14. Migration
- [ ] v1/v2/v3 payload 支持策略明确
- [ ] 旧用户升级不丢数据（v2→v3 实测 PASS）

## 15. Backup
- [ ] 导出 `.depmap`（加密、需密码）
- [ ] 导入 / 恢复事务性
- [ ] 错误密码 / 不支持版本 行为正确

## 16. Crash Paths
- [ ] DB 升级失败 → 可理解 UI + 数据未修改
- [ ] DB 无法打开 → retry / restore / 诊断（不自动清库）
- [ ] 解密失败 / 生物认证取消

## 17. Network
- [ ] business network calls = 0

## 18. Dependencies / License
- [ ] `check:deps` PASS
- [ ] license 快照（MIT / Apache-2.0）
- [ ] runtime critical vulnerability = 0

## 19. Real Data Decision
- [ ] REAL_DATA_CORRECTNESS / VALUE 状态与决策
- [ ] 公开发布前是否需要真实数据验证 —— **PENDING_USER**

---

## 20. 提交结论

| 项 | 状态 |
|---|---|
| STORE_METADATA_READY | 见 RC 报告 |
| STORE_ASSETS_READY | BLOCKED（无 asset / 无截图环境） |
| STORE_SUBMISSION_READY | 取决于 §4 URL、§11 签名、§8 asset |
| STORE_SUBMITTED | **NO**（本轮不提交） |
