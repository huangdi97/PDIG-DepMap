# iOS N4 渲染截图（macos-15 / arm64，2026-11-13）

来自 CI `iOS` workflow，run [43347855211](https://github.com/huangdi97/PDIG-DepMap/actions/runs/43347855211)
（`macos-15`，arm64，Xcode 16.4）的 `Generate screenshots (macOS SwiftUI render)` 步骤，原样入库。

- **60 张 PNG**（30 屏 × light/dark）+ `evidence-index.json`
- 该 run 全部步骤 success：`Build (PDIGCore + PDIGConformance + PDIGApp)`、`Test (canonical 91+37 …)`、
  `Test (PDIGAppTests)`、`Generate screenshots (macOS SwiftUI render)`、两个 upload

## 口径（不要混用）

| 项 | 值 | 说明 |
| --- | --- | --- |
| `IOS_CONFORMANCE` | **PASS**（161/161） | 见 `artifacts/runtime-evidence/2026-11-12-ios-conformance/` |
| `IOS_RENDER` | **PASS** | 本目录 60 张即证据（macOS SwiftUI 真实渲染，非 mock） |
| `IOS_HUMAN_VISUAL_ACCEPTANCE` | **NOT_RUN** | 只有 owner 能判定；本目录不代表验收通过 |
| `IOS_XCUITEST` | **FAIL** | 见下：模拟器启动与 App 启动已通过，XCUITest 阶段 `xcodebuild` exit 65 |
| `IOS_RUNTIME` | **NOT_VERIFIED** | 与 `IOS_XCUITEST` 一致，等该失败被定位 |

## 同轮 XCUITest 状态（run 43347855293，同一个 `macos-15`）

- `Select and boot iPhone + iPad simulators` → **success**
- `Diagnostic - manual app launch (uitest-demo)` → **success**（App 在模拟器上真的启动）
- `XCUITest on iPhone simulator` → **failure**（`Process completed with exit code 65`）
- `XCUITest on iPad` → skipped；完整 xcresult/日志在该 run 的 206MB artifact 内

这两条合起来说明：**Intel VM 的 wedge/Metal 问题在 arm64 上已消失**（启动与渲染都跑通），
剩下的失败是 XCUITest 断言/工程层面的问题，需要从 xcresult 里取具体用例。
