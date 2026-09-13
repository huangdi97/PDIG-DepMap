# PRIVACY_RELEASE_AUDIT.md — PDIG 发布隐私审计（Production RC V1）

> 目标：确保**对外隐私表述与真实实现一致**，不做虚假承诺。
> 状态：`PASS` / `FAIL` / `BLOCKED` / `NOT_RUN` / `PARTIAL_WITH_REPORT`

---

## 1. 数据流（实际）

```
用户选择账单文件
      │
      ▼
系统文件选择器（uni.chooseFile）
      │  文件内容仅进入会话内存
      ▼
解析 / 指纹 / 证据摘要        ← 设备端（当前构建：桥接未接入，B20）
      │
      ▼
本地加密数据库（SQLCipher / ArkData 加密）
      │  只持久化：对象档案、已确认关系、证据摘要、指纹、建议状态
      ▼
用户主动导出 → 加密 .depmap（Argon2id + AES-256-GCM）
```

**无服务器环节。** 无账号、无上传、无遥测。

## 2. 逐项核对（表述 vs 实现）

| 表述（对外） | 实现 | 一致？ |
|---|---|---|
| "文件在本机处理" | 文件经系统选择器读入会话内存；无上传代码路径 | ✅ |
| "不会上传到服务器" | `check:network`：业务源码 0 网络原语 | ✅ |
| "原始交易记录不会作为长期账本保存" | 持久化对象仅 `evidence` 摘要 / `observation_fingerprints` / `import_sessions`；**无交易明细表** | ✅ |
| "系统只保留必要的关系证据摘要和你确认的结果" | Schema 中无 raw statement 表 | ✅ |
| "无账号、无统计、无广告" | 无 SDK、无网络 | ✅ |
| "备份是加密的，需要口令" | `DEPMAP_CONTAINER_V1`：Argon2id + AES-256-GCM | ✅（Core 已 golden 验证） |
| "口令不会以明文保存、无法找回" | 密钥派生自口令，不存口令 | ✅ |
| "不使用生物识别数据本身" | 仅调用系统认证能力 | ✅（源码级；真机 NOT_RUN） |
| "删除数据" | `resetLocalData()` 清空全部业务表 | ✅ **本轮已对齐**：文案改为"会清空本应用已保存的服务、关系、计划、来源与待确认记录。不会删除数据库文件本身，也不会删除系统安全存储中的密钥，不做存储级物理擦除"（见 §4） |
| 未声称"绝对不会泄露""100% 安全" | `PRIVACY_POLICY_DRAFT.md` §9 明确不做绝对化承诺 | ✅ |
| 未声称物理擦除 | 设置页与政策均写明"不对设备存储做物理擦除" | ✅ |

## 3. 新增对象隐私面（本轮）

| 对象 | 持久化内容 | 是否含敏感原文 |
|---|---|---|
| `change_plans` | 计划标题、状态、快照、动作 | 标题可能含卡名（用户自填）；无卡号 |
| `reality_drifts` | 候选关系引用、证据引用、计数 | 仅引用 ID，无原文 |
| `discovery_candidates` | 展示名、归一化键、来源 ID、计数 | 展示名为商户名（必要） |
| `app/services/*` | 无持久化 | — |

**判定：PASS**（新对象只存引用 / ID / 计数，不引入新的敏感原文）。

## 4. 发现的不一致（必须修或改文案）

| # | 问题 | 处置 |
|---|---|---|
| P-1 | `resetLocalData()` 只清空数据库表，**未删除加密密钥与数据库文件**；而设置页原文案为"删除全部数据与密钥" | **已闭环（收紧文案）**。`app/pages/settings/settings.uvue` 与 `app/pages/privacy/privacy.uvue` 均已改为精确表述：清空业务数据记录；不删数据库文件、不删系统安全存储中的密钥、不做物理擦除；如需彻底移除请卸载应用。服务层注释同步更新。 |
| P-2 | 导入/备份在设备上不可用 | UI 已如实标注"尚未接入，不写入数据"，与实现一致 ✅ |

> **P-1 处置理由（记录决策，避免以后反复）**：
> 真正彻底的方案是 crypto-erase（删除数据库文件 + 销毁 Keystore/Keychain/HUKS 密钥）。
> 该方案需要为 `SecureDb` 接口新增 `destroy()` 并在三端原生层实现——在当前**无编译器**（B10）
> 的条件下属于"未经验证的原生代码"；且**只删密钥不删文件**会让应用再也打不开既有数据库，
> 比不删更糟。因此本轮选择**收紧文案**而不是塞入未验证的实现，并把 crypto-erase 写入 `FUTURE.md`。
> 补充说明：数据库整体由设备绑定密钥加密，行删除后即使 free page 残留，内容也不可读。

## 5. 与商店问卷相关的事实清单

见 `store/PRIVACY_DISCLOSURE_MATRIX.md`（按平台准备，不代填问卷）。

## 6. 未验证项

| 项 | 状态 | 原因 |
|---|---|---|
| 真机数据流验证（无抓包外联） | **NOT_RUN** | 无设备（B1/B2/B3） |
| 备份文件实际熵 / 口令强度策略 | **NOT_RUN** | 设备端未接入（B21） |
| 剪贴板 / 截图泄露 | **NOT_RUN** | 需真机（B1/B3） |

---

## 结论

```
PRIVACY_ZERO_NETWORK        = PASS
PRIVACY_POLICY_CONSISTENCY  = PASS（P-1 已通过收紧文案闭环；源码级核对一致）
ABSOLUTE_CLAIMS             = 无（PASS）
NEW_OBJECT_PRIVACY          = PASS
DEVICE_DATAFLOW             = NOT_RUN

PRIVACY_RELEASE_AUDIT = PASS（源码与文案层面）；DEVICE_DATAFLOW = NOT_RUN
```
