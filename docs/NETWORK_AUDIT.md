# NETWORK_AUDIT.md — 网络审计（RC PHASE AK）

> 扫描命令：
> `grep -rnE "fetch\(|axios|uni\.request|XMLHttpRequest|URLSession|OkHttp|http\.createServer|WebSocket|@kit.NetworkKit" core/src app platforms`

## 结果（2026-09-12 实测）

- core/src（TS）：**0** 命中（runtime 依赖仅 hash-wasm，无任何网络库）
- app/pages/*.uvue、stores：**0** 命中
- platforms/android/kotlin：**0**（无 OkHttp/HttpURLConnection/Retrofit import）
- platforms/ios/swift：**0**（无 URLSession/Foundation networking import）
- platforms/harmonyos/arkts：**0**（无 @kit.NetworkKit / http import）

## 结论

```
business network calls = 0
analytics = 0
telemetry = 0
ads = 0
```

- Android manifest 无 INTERNET 权限（系统层面断网，见 PERMISSION_AUDIT.md）
- iOS 无后台模式；HarmonyOS 无网络权限声明
- 唯一网络使用：开发工具链（npm install/audit），不进入产物

## Release 配置核对

- usesCleartextTraffic=false（Android）
- 无 debug menu / test key / 测试库 / synthetic demo 数据开关（UI_SOURCE_AUDIT.md）；
  validate-real-bill CLI 仅本地人工审计工具，不打包进 App
