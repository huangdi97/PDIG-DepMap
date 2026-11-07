# LOCAL FINAL EVIDENCE — Android Production VNext (exact HEAD)

本目录是 `LOCAL_FINAL_REPORT.md` 的证据实体，对应远端运行时证据 run `40686250651`
（`head_sha = a3d08e63307230e0de4fc5400464471c5094007d`，`android/` 树 `e68948d5`）。

## 内容

```
phone-1.0/  MANIFEST.json + phone/*.png + phone-stale-reanalysis/*.png
            + xml/phone/*.xml + xml/phone-stale-reanalysis/*.xml + seed-logs/
phone-1.5/  同上（fontScale 1.5，目录名 phone-font15*）
apk/        SHA256SUMS.txt / IDENTITY.txt          ← 仅身份与哈希，APK 二进制未入库
analysis/   xml-bounds.csv                          ← 44 屏边界 / 触控 / 文本重叠分析
```

## 绑定关系（可复核）

| 项 | 值 |
| --- | --- |
| 证据 run | `40686250651`（`Android UI vNext Post-Freeze Production Evidence`，success） |
| `MANIFEST.sourceHead` | `a3d08e63307230e0de4fc5400464471c5094007d` |
| `MANIFEST.androidTreeHead` | `a3d08e6` = `git log -1 --format=%h -- android/` |
| entries | 22 / 矩阵（1.0 与 1.5 各 22），`assertionsMissing = []`，`humanReview = NOT_RUN` |
| 设备 | `sdk_gphone64_x86_64`，API 36，1080×2400，density 420 |
| dataMode | `isolated-postfreeze-evidence-reality`（instrumentation seed，非 Production 用户数据） |
| CI APK SHA-256 (1.0) | `ebb476b8d85546ac535de92ecd6d00a23960ab0561b3deb9ef77ba3b57889172` |
| CI APK SHA-256 (1.5) | `dbef57e27f50ff6495ff48f26ab9406dac14083b3b52863f087466449d28fae0` |
| 本机构建 APK SHA-256 | `942358fdbc571ae24fd7eaa86b718898e20c1af451bed585ddb4207e72d234a7` |
| BuildConfig | `PRODUCTION_UI_GENERATION="vnext"`、`PRODUCTION_VNEXT_CUTOVER_APPROVED=true`、`GIT_SHA="e68948d5"` |
| package | `com.pdig.app` 0.3.1 (2)，signer `CN=Android Debug`（非上架件） |

不同构建机的 APK 不要求字节一致；绑定的是 `source HEAD + BuildConfig + APK SHA + package`。

## 关于黑帧（重要，不是缺陷）

44 张 PNG 中 **38 张为全黑**，因为 Production 的 Records / 详情 / 变更页按设计强制
`FLAG_SECURE`，ADB 截屏只能得到黑帧 —— 这是正确的安全行为。仅根级页面可截：
`production-lock`、`production-now`、`production-change-center`（1.0 与 1.5 各 3 张）。

安全页的语义/布局证明以 UIAutomator XML 为准（本目录 `xml/**`），并未为截图放宽任何安全策略。

## XML 独立分析结论

`analysis/xml-bounds.csv` 覆盖 44 屏（22 × 2 字级）：

- 越界节点：0
- 带标签且 <48dp 的点击目标：0（density 420 → 48dp = 126px）
- 叶子文本相互重叠（>30% 面积）：0

## 复现

```bash
# 1) 取同 HEAD 证据（需 gh 或 GitHub token）
gh run download 40686250651 --repo huangdi97/PDIG-DepMap

# 2) 或本机构建后跑既有 harness（需可启动的 API36 设备）
cd android
gradle :app:assembleProductionDebug :app:assembleProductionDebugAndroidTest \
  -PpdigProductionUiGeneration=vnext -PpdigProductionVNextCutoverApproved=true
PDIG_EVIDENCE_SOURCE_HEAD=$(git rev-parse HEAD) PDIG_EVIDENCE_LABEL=phone \
  PDIG_EVIDENCE_FONT_SCALE=1.0 bash android/scripts/ci_postfreeze_production_evidence.sh
```

本机（Windows）无法复跑的原因见 `LOCAL_FINAL_REPORT.md` §3.1：宿主无 WHPX/Hyper-V，
`emulator -accel-check` 返回 6，x86_64 API36 镜像在纯 TCG 下崩溃。
