# ANDROID_REAL_DEVICE_ACCEPTANCE_PLAN.md

> 生成时间：2026-09-22（ANDROID_CANONICAL_FREEZE → Production/Reality Closure 轮）
> 状态：**PROTOCOL READY / NOT EXECUTED（无真机）**
> 对应 Gate：`ANDROID_REAL_DEVICE_VERIFIED = BLOCKED_BY_MISSING_REAL_DEVICE`
> 原则：AVD 已充分覆盖（设备内 androidTest 59/59 · Core Journey 41/41 · 三场景 40/40），
> 真机验收是补「真实硬件 / 真实系统 / 真实传感器」维度，**不是形式测试、不是 AVD 的复读**。
> 自动收集工具：`scripts/android_real_device_acceptance.ps1`（或仓库内等价工具）。

---

## 0. 执行前置

| #   | 前置                                            | 说明                                                                                  |
| --- | ----------------------------------------------- | ------------------------------------------------------------------------------------- |
| 0.1 | 一台真实 Android 手机（Android 8.0+ / API 26+） | 与 minSdk=26 匹配                                                                     |
| 0.2 | 开发者选项 + USB 调试开启                       | `adb devices -l` 可见且授权                                                           |
| 0.3 | 签名 APK（release）或 debug APK                 | 建议 release 签名（见 ANDROID_PRODUCTION_SIGNING_ACCEPTANCE.md §2 步骤 6-7）          |
| 0.4 | 无敏感数据                                      | 测试设备上不登录任何真实账户、不导入真实账单                                          |
| 0.5 | 记录基线                                        | 先跑 `scripts/android_real_device_acceptance.ps1 -DeviceSerial <serial>` 收集设备信息 |

> 每一小节给出「执行步骤 / 预期 PASS / 记录字段」。任何一项 FAIL 都必须记录复现步骤并回报，
> **不得**把 FAIL 记为 PASS。

---

## 1. Installation（安装与启动）

| 项          | 执行步骤                            | 预期 PASS                                    | 记录字段                  |
| ----------- | ----------------------------------- | -------------------------------------------- | ------------------------- |
| install APK | `adb install -r <release>.apk`      | `Success`；桌面出现图标                      | install 输出 / APK SHA256 |
| upgrade APK | 装旧版 → 装新版（versionCode 递增） | 数据保留、应用可更新                         | 新旧 versionName/Code     |
| cold start  | 杀进程后冷启动                      | 先 App Lock（若启用）→ 解锁 → 首页；无 crash | 启动耗时 / 是否 crash     |
| warm start  | Home → 回桌面 → 立即回应用          | 回到原页面或按锁策略回锁                     | 页面恢复情况              |
| relaunch    | 应用在后台 → 最近任务 → 重新拉起    | 不崩溃、状态一致                             | 是否重走启动流程          |

---

## 2. Security（安全）

| 项                      | 执行步骤                  | 预期 PASS              | 记录字段     |
| ----------------------- | ------------------------- | ---------------------- | ------------ |
| App Lock 启用           | 设置里开启 App Lock       | 冷启动先锁、解锁后可进 | 锁屏出现时机 |
| fingerprint 成功        | 已录入指纹 → 锁屏指纹解锁 | 解锁成功               | 解锁路径     |
| fingerprint 失败        | 未录入手指 → 指纹         | 拒绝并提示；不绕过     | 错误提示文本 |
| fingerprint cancel      | 弹出系统验证后按返回/取消 | 回锁屏 + 「已取消」    | 回退行为     |
| Device Credential       | 锁屏 PIN → 解锁           | PIN 正确解锁、错误拒绝 | PIN 路径结果 |
| screen lock             | 亮屏锁屏状态进入          | 锁屏完整渲染           | 锁屏显示     |
| background / foreground | 应用退后台再回前台        | 按策略回锁（敏感）     | 回锁时机     |

---

## 3. Lifecycle（生命周期）

| 项                     | 执行步骤                 | 预期 PASS                                                 | 记录字段    |
| ---------------------- | ------------------------ | --------------------------------------------------------- | ----------- |
| process death          | 后台杀进程 → 重新打开    | 保守恢复：意图保留、文件作废（`INTERRUPTED`），绝不半恢复 | 恢复状态    |
| activity recreation    | 旋转（若支持）/ 系统重建 | 状态不丢、不 crash                                        | 重建结果    |
| navigation restoration | 深链/系统恢复导航        | 回到合理页面                                              | 恢复路径    |
| file picker            | 导入/恢复拉起系统选择器  | D-16 双断言（见 §5）                                      | 工作流存活  |
| App Lock interaction   | picker 期间锁屏          | 解锁后工作流仍在（不丢）                                  | 跨锁存活    |
| orientation（若支持）  | 旋转到横竖屏             | 无布局崩溃                                                | 方向支持    |
| low memory smoke       | 后台压内存 → 回前台      | 无 OOM crash                                              | logcat 检查 |

---

## 4. Import（导入）

| 项                 | 执行步骤                  | 预期 PASS                                | 记录字段        |
| ------------------ | ------------------------- | ---------------------------------------- | --------------- |
| WeChat             | 真实 SAF 选择微信账单 CSV | 解析正确行数、Node Resolution 正常       | 解析行数/错误行 |
| Generic CSV        | 通用 CSV                  | 同上                                     | 同上            |
| OFX/QFX            | OFX/QFX 文件              | 同上                                     | 同上            |
| SAF picker         | 拉起系统文件选择器        | picker 正常                              | 选择器行为      |
| cancel picker      | 取消选择                  | 回到导入页、工作流保留或明确终止、不解锁 | 取消行为        |
| return from picker | 选择文件返回              | **锁内不解锁**、导入在解锁后于页面中解析 | D-16 断言       |

---

## 5. D-16 专项重验（强制，禁止回退）

> 方案 A（已实现，必须保持）：工作流状态在 **Activity 作用域** `FileWorkflowCoordinator`；
> `ActivityResult` launcher 注册在 `MainActivity.onCreate`（稳定层）；文件选择用 `OpenDocument` +
> `takePersistableUriPermission(READ)`；拿到文件**绝不解锁、绝不自动提交**；进程死亡保守恢复；敏感内容不进 `SavedStateHandle`。

| #     | 断言                                                               | 预期 PASS         | 禁止回退到的旧实现                            |
| ----- | ------------------------------------------------------------------ | ----------------- | --------------------------------------------- |
| D16-1 | picker 拉起后 Activity 进入 onStop → LockGate.lockNow() → 锁屏出现 | 锁屏出现          | overlay-only（锁定时继续组合 NavHost 靠遮罩） |
| D16-2 | 选完文件回来，**不会自动解锁**                                     | 仍锁              | picker 返回即绕过 lock                        |
| D16-3 | 解锁后 Import/Restore 向导状态仍在（不丢）                         | 向导恢复          | 状态随 NavHost uncompose 丢失                 |
| D16-4 | `takePersistableUriPermission(READ)` 可跨锁读取 Uri                | 可读              | —                                             |
| D16-5 | 进程死亡后恢复为 `INTERRUPTED`，不半恢复                           | 保守恢复          | 半恢复                                        |
| D16-6 | 原始账单 / 口令 / 解密内容从不进 SavedState                        | SavedState 无敏感 | 敏感进 SavedState                             |

> 设备内已有关键测试：`FileWorkflowD16Test`（6/6）、Core Journey E2E v4 中每个外部 picker 节点
> 独立断言 `externalPickerDoesNotBypassLock` 与 `externalPickerDoesNotDestroyPendingWorkflow`。
> 真机验收是**重跑以上断言于真实系统 UI**。

---

## 6. Scenario（三场景完整闭环）

每个场景独立执行完整链：**Setup → Impact → Plan → Action → Verification**。

| 场景                       | Setup                         | Impact               | Plan           | Action   | Verification                |
| -------------------------- | ----------------------------- | -------------------- | -------------- | -------- | --------------------------- |
| `replace_payment_card`     | 建立 1 支付工具 + ≥2 依赖关系 | 影响面列出受影响下游 | 变更计划生成   | 逐项完成 | 逐项验证（done ≠ verified） |
| `expiring_payment_card`    | 同上 + 即将到期标识           | 影响面含到期提醒     | 计划含到期处理 | 完成     | 验证                        |
| `close_payment_instrument` | 同上                          | 影响面含注销迁移     | 计划含迁移项   | 完成     | 验证                        |

每场景记录：影响面节点数 / 计划项数 / 全部 verified 时间 / 是否有 false must_change（**任何 false must_change 即 FAIL**）。

---

## 7. Backup / Restore

| 项                   | 执行步骤                          | 预期 PASS               | 记录字段                       |
| -------------------- | --------------------------------- | ----------------------- | ------------------------------ |
| export               | 导出 `.depmap`（MediaStore 落盘） | 文件出现、大小合理      | 文件路径/大小/SHA256           |
| password             | 导出时设置口令                    | 成功                    | 口令强度记录（不记录口令本身） |
| restore              | 选 `.depmap` → 口令 → 确认        | 恢复成功、数据一致      | 恢复行数                       |
| wrong password       | 错误口令                          | 拒绝、无部分恢复        | 错误提示                       |
| tampered file        | 篡改容器                          | 拒绝（bounds/认证失败） | 拒绝行为                       |
| future schema        | 未来版本容器                      | 明确拒绝、库不清空      | 拒绝行为                       |
| process interruption | 恢复中途杀进程                    | 保守恢复 `INTERRUPTED`  | 恢复状态                       |

---

## 8. Accessibility（无障碍）

| 项                  | 预期 PASS                                      | 记录字段            |
| ------------------- | ---------------------------------------------- | ------------------- |
| TalkBack            | 关键屏可完整读屏导航（读屏无乱序）             | 通过的页面列表      |
| focus order         | 焦点顺序符合阅读顺序                           | 顺序描述            |
| labels              | 交互节点均有标签（语义树 14/14 已在 AVD 验证） | 无标签节点数        |
| state announcements | 选中/完成/锁定状态有读屏播报                   | 播报内容            |
| error announcements | 错误提示有读屏播报                             | 播报内容            |
| 48dp target         | 触摸目标 ≥48dp                                 | 小于 48dp 的节点数  |
| font scale          | 最大字号下无截断/无布局崩溃                    | 字号 1.3x/2.0x 结果 |
| scroll              | 滚动容器可达                                   | 滚动行为            |
| IME                 | 输入法呼出不遮挡关键内容                       | IME 行为            |

> TalkBack 实机读屏正是 parity 剩余 4 项之一（`REAL_DEVICE_REQUIRED`）——真机验收必须执行。

---

## 9. Performance（性能）

| 项                   | 记录字段                                        | 备注                                                  |
| -------------------- | ----------------------------------------------- | ----------------------------------------------------- |
| cold start           | 首次渲染耗时                                    | 建议 3 次取中位                                       |
| screen navigation    | 主要页面切换耗时                                | Home→场景→计划                                        |
| import               | 1 万行 CSV 解析耗时（AVD 参考线：无错误）       | 与 AVD 数字对比                                       |
| scenario calculation | 影响面计算耗时                                  | 视节点数                                              |
| backup               | 导出耗时                                        | —                                                     |
| restore              | 恢复耗时                                        | —                                                     |
| memory smoke         | 峰值内存 / 无 OOM                               | `adb shell dumpsys meminfo`                           |
| ANR / crash          | logcat 中 `ANR in` / `FATAL EXCEPTION` 计数 = 0 | `scripts/android_real_device_acceptance.ps1` 自动统计 |

---

## 10. 结论记录模板

```text
ANDROID_REAL_DEVICE_VERIFIED = PASS | BLOCKED_BY_MISSING_REAL_DEVICE | FAIL（附复现）
设备：<model> / <Android 版本> / API <n> / <ABI> / <density> / <resolution>
执行日期：<date>  执行人：<user>
结果：Installation=<PASS/FAIL> · Security=<…> · Lifecycle=<…> · Import=<…> · D-16=<…>
     Scenario=<…> · Backup/Restore=<…> · Accessibility=<…> · Performance=<…>
```

## 配套文档

- `scripts/android_real_device_acceptance.ps1`（自动收集工具）
- `ANDROID_PRODUCTION_SIGNING_ACCEPTANCE.md`（签名 APK 安装前置）
- `ANDROID_SCENARIO_E2E_REPORT.md`（三场景 AVD 证据）
