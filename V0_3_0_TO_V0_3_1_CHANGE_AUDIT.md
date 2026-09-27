# V0_3_0_TO_V0_3_1_CHANGE_AUDIT.md — product-v0.3.1 Corrective Release

> 生成：2026-09-27（v0.3.1 Corrective Release Closure，§6）。
> 命令：git diff --stat/--name-status product-v0.3.0^{}..main；git log product-v0.3.0^{}..main --oneline --reverse。
> 变更总量：187 files（3054 insertions / 239 deletions），31 commits（全部为 closure 收口/修复/证据，无产品范围变更）。

## 1. 变更分类

| 分类             | 文件数 | 说明 / 代表文件                                                                                                                                                                                           |
| ---------------- | ------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| RUNTIME_EVIDENCE | 130    | artifacts/runtime-evidence/2026-09-27-closure-*（Desktop 82 / iOS 52 / Android 20 / Harmony 2 等证据、截图、日志、summary）                                                                               |
| BRANDING         | 23     | android res 自适应/单色图标（5）、desktop pdig.ico/pdig-256.png（2）、ios Assets.xcassets AppIcon（14）、store feature-graphic/release-artwork（2）                                                       |
| DOCUMENTATION    | 9      | FINAL_V0_3_0_CONTRACT_CLOSURE.md、FINAL_REPORT.md、WORK_STATUS.md、BLOCKERS.md、NATIVE_MIGRATION_STATUS.md、README.md、docs/user/*、RELEASE_NOTES_0_3_0.md 等                                             |
| PRODUCTION_CODE  | 10     | desktop harness（Main/ShotDriver/KeyboardDriver/ProfileDriver + build.gradle.kts）、ios app（PDIGApp.swift/AppSession.swift/BackupScreen.swift/Package.swift/project.yml）、harmony ConformanceRunner.ets |
| BUILD            | 3      | desktop/app/build.gradle.kts、tools/harmony/run-conformance-host.mjs、tools/ios-closure/write_summary.py                                                                                                  |
| RELEASE          | 4      | PRODUCT_V0_3_0_RELEASE_MANIFEST.md、RELEASE_NOTES_0_3_0.md、store/REVIEW_INSTRUCTIONS.md、store/SCREENSHOT_PLAN.md（doc 类）                                                                              |
| TEST             | 3      | core/tests/integration/schema-v4-persistence.test.ts、harmony ConformanceHost.test.ets、ios/UITests/PDIGAppUITests.swift                                                                                  |
| CI               | 1      | .github/workflows/ios-runtime-visual.yml（XCUITest 管线 + 确定性 xctestrun 修复）                                                                                                                         |
| WEBSITE          | 4      | website/{index,privacy,product/pdig,support}/index.html                                                                                                                                                   |

## 2. 范围证明

```
NO_V0_4_SCOPE           = PASS（无任何 v0.4/新 MVP/新 Domain 文件或语义）
NO_NEW_SCENARIO         = PASS（场景集不变：payment/replace_payment_card/expiring/close/replace_phone_number）
NO_NEW_CAPABILITY       = PASS（capability 集不变：payment/access/authentication/recovery/communication/identity）
NO_DOMAIN_SCOPE_EXPANSION = PASS（核心/引擎/规范零语义变更；spec/ 与 fixtures/ 逐字节未动）
```

PRODUCTION_CODE 变更逐一性质：

- desktop：ProfileDriver/KeyboardDriver/ShotDriver 扩展 + Main 参数分发 —— 运行时取证 harness（不影响产品语义）；
- ios：PDIGApp.swift/AppSession.swift —— `--uitest-demo` 测试种子与 `RootView.onAppear` 时序；BackupScreen.swift —— `.confirmationDialog → .alert`（iPad XCUITest 可见性产品修复）；Package.swift —— PDIGArgon2 library product（构建依赖）；project.yml —— xcodegen 工程壳（CI 生成，不提交 .xcodeproj）；
- harmony：ConformanceRunner.ets —— `migration`/`backup` computeCase（H3/H4 host 执行闭环，语义对齐冻结 expected，非改 fixture）。

## 3. 结论

v0.3.0 → v0.3.1 的全部差异 = v0.3.0 capability set + post-release corrective closure（证据/测试/CI/文档/品牌/网站/打包），无产品语义变更。旧 tag 保持 immutable，本轮新增 product-v0.3.1。
