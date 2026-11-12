# iOS canonical conformance（macos-15-intel，2026-11-12）

`ios.json` 是 CI（`iOS` workflow，run [43276187468](https://github.com/huangdi97/PDIG-DepMap/actions/runs/43276187468)）
在 `macos-15-intel` 上产出的 canonical conformance 报告，原样入库，未做任何改写。

```
platform = ios
summary  = { pass: 161, fail: 0, notImplemented: 0, envBlocked: 0, total: 161 }
```

同一 run 中 `Swift build (PDIGCore + PDIGConformance + PDIGApp)`、`Test (canonical 91+37 + core smoke +
PDIGApp view models)`、`Test (PDIGAppTests)`、XCUITest bundle 构建均 success；只有
`Generate screenshots (macOS SwiftUI render)` 失败（托管 Intel VM 无可用 Metal 设备：
`MTLLoader ... 'Target device architecture is nil'` → SIGABRT 134）。

口径（不要混用）：

- `IOS_CONFORMANCE = PASS`（本文件即证据）。
- `IOS_VISUAL = NOT_RUN`、`IOS_RUNTIME = NOT_VERIFIED` —— 见仓库根 `BLOCKERS.md` 的 iOS 条目。
