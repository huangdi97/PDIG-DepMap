# store/PERMISSION_RATIONALE.md

> 状态：**DRAFT / READY** —— 权限说明（以实际构建产物 AndroidManifest.xml 为准）
> 生成时间：2026-09-22（ANDROID_CANONICAL_FREEZE → Production/Reality Closure 轮）

---

## 1. 实际申请的权限

| 权限 | 用途 | 何时触发 | 能否撤销 |
|------|------|----------|----------|
| `USE_BIOMETRIC` | 应用锁的生物识别解锁 | 启用 App Lock 后，解锁时 | 关闭 App Lock 即可不再使用 |
| `USE_FINGERPRINT` | 同上（旧 API 兼容声明） | 同上 | 同上 |

> 系统可能显示 `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` —— 由 AndroidX 自动注入，非本应用主动申请。

## 2. 明确未申请的权限（最小化原则）

- ❌ `INTERNET` / `ACCESS_NETWORK_STATE` —— 无网络功能（local-first，无后端）
- ❌ `READ/WRITE_EXTERNAL_STORAGE` —— 文件交互走 SAF（用户按需授权具体文件）与 MediaStore（导出）
- ❌ `CAMERA` / `LOCATION*` / `READ_CONTACTS` / `READ_SMS` / `READ_CALL_LOG` 等
- ❌ 任何后台权限

## 3. 为什么无 INTERNET

产品是 **local-first**：无后端、无账号、无云同步、无 analytics、无遥测（AGENTS §16）。
数据只在设备本地加密存储。不因 Store 准备而添加不必要的网络权限（Goal §18）。

## 4. 文件访问方式说明

- **导入账单**：系统文件选择器（SAF）→ 用户显式选择文件 → 仅读权限 → 流程结束归还。
- **导出备份**：MediaStore → 用户指定位置落盘 `.depmap`（不经外部文件选择器，不受 D-16 影响）。
- **恢复备份**：SAF 选择 `.depmap` → 重新输入口令 → 显式确认开始恢复。

## 状态

```text
PERMISSION_RATIONALE = READY（可放入商店权限说明 / 隐私政策）
```
