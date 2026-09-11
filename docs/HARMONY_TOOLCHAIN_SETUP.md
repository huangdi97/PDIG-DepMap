# HARMONY_TOOLCHAIN_SETUP.md — HarmonyOS 工具链（RC PHASE W）

> 检测结果（2026-09-12）：DevEco Studio 不存在；hvigor/hvigorw 不存在；
> HarmonyOS SDK 不存在（`%PROGRAMFILES%\Huawei` 为空）。

**结论：ArkTS 编译/检查不可执行 → COMPILED=NO / TESTED=NO（exact blocker B2）。**

## 可复现安装步骤

1. DevEco Studio（含 SDK 管理器）：
   `https://developer.huawei.com/consumer/cn/deveco-studio/` → Windows 版安装
   （需要华为账号接受 SDK license —— 属外部交互，按 GOAL 记录为 blocker）。
2. 首次启动 → Settings → HarmonyOS SDK → 勾选 ArkTS/Stage 模型组件（API 12）。
3. 命令行工具：DevEco 内置 `hvigorw`（工程目录下生成 wrapper 后可用 CLI 编译）。

## 工具链就绪后执行

```bash
cd platforms/harmonyos        # 当前为工程骨架（app.json5 + entry/module.json5）
hvigorw assembleHap --mode module -p product=default
# ArkTS 静态检查：
hvigorw lint
```

## 待验证项（与 RelationalStoreSecureAdapter.ets 对应）

- ArkData 加密库（encrypt: true, SecurityLevel S4）落盘不可读
- HUKS 密钥生命周期与失败路径
- 用户认证取消不得进入数据层
- `.depmap` 互操作：Harmony 侧需 cryptoFramework AES-GCM + Argon2id ArkTS 移植
  （golden vector 已就绪：core/src/crypto/golden.ts），当前该适配为 partial，
  见 docs/PLATFORM_ADAPTERS.md。
