# Android Post-Freeze Production 证据（sourceHead `3c60fc6cd4af3e313e5f531f8895bdf5e01a86dd`）

本目录是 CI 生成证据的**语义子集**：MANIFEST + 每帧 UIAutomator XML + seed 日志 + 候选包清单。
它由 `Android UI vNext Post-Freeze Production Evidence` workflow 产出，不是本地手搓，也不含任何真实用户数据
（Reality 全部由 `PostFreezeEvidenceSeedTest` 通过 `AppContainer` 写入隔离库，值为占位数据）。

## 来源

| 项 | 值 |
|---|---|
| run | [43181985619](https://github.com/huangdi97/PDIG-DepMap/actions/runs/43181985619)（workflow_dispatch，合并后 main） |
| sourceHead | `3c60fc6cd4af3e313e5f531f8895bdf5e01a86dd`（`merge: fix/tablet-capture-swipe into main`） |
| jobs | `phone` · `phone-font15` · `tablet` · `tablet-font15`，**4/4 success** |
| 每腿帧数 | 22（`prod-journey` 19 + `prod-stale` 3） |
| 构建键 | `PRODUCTION_UI_GENERATION=vnext` + `PRODUCTION_VNEXT_CUTOVER_APPROVED=true`，默认路由（无 debug extra） |

各腿 validator 输出（CI 脚本自带，`safe_bottom` = 内容容器真实底部）：

```
measured_font_scale=1.5        # 仅 *-font15 腿
OK (1 test)                    # seedPhoneMakeBeforeBreakJourney
OK (1 test)                    # seedStalePhoneChange
POSTFREEZE_EVIDENCE_MANIFEST=PASS entries=22
POSTFREEZE_LAYOUT_BOUNDS=PASS
```

## 目录

```
phone/ | phone-font15/ | tablet/ | tablet-font15/
  MANIFEST.json                 # 每帧的 screen/state/断言/设备/字号/APK 哈希/secureWindowExpected
  xml/<label>/<shot>.xml        # 语义与布局证据（含 bounds，可复核 safe band）
  seed-logs/<method>.txt        # 两次 seed 的 instrument 输出
  apk/REVIEW_CANDIDATE.txt      # 候选包元信息（vnext / cutover=true / debug 签名）
  apk/SHA256SUMS.txt            # 当轮构建 APK 的 sha256
```

PNG 截图与 APK 二进制**不入 Git**：它们在同名 CI artifact 内（保留 21 天），需要时用
`gh run download 43181985619 -D <dir>` 取回。

## 怎么读

- 标 `secureWindowExpected = true` 的帧是 `FLAG_SECURE` 加密窗口，PNG 预期**全黑**（安全设计，非取证失败）；
  这些帧的判断依据是 XML 里的文案与 `bounds`，不是像素。
- 其余帧（如 `production-change-center`）的 PNG 是真实截图。
- `assertionsMissing` 必须为空；`humanReview` 一律 `NOT_RUN` —— **本目录不构成人眼验收**。

## 状态口径

- `ANDROID_REFERENCE_FREEZE = PASS`（技术取证门禁，范围 = 本目录这类每腿 22 帧的 Production post-freeze 证据）。
- `HUMAN_VISUAL_ACCEPTANCE = NOT_RUN`（只能由 owner 判定）。
- 不覆盖：像素级参考对比（`PIXEL_REFERENCE_LIMITATION`，仓库参考图 ≈320×213）、iOS / HarmonyOS 运行时、Store 发布。
