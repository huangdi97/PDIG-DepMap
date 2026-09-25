# PRODUCT_ERROR_CATALOG_V0_2.md

> PDIG / DepMap v0.2.0 错误体验统一目录（goal §67-69）。
> 每个错误必须回答：**What happened / Was data changed / Can user retry / What should user do**。
> 禁止把 "Something went wrong" 作为唯一文案；禁止向用户暴露 SQLCipherException / SQLite error /
> stack trace / internal file path / key alias / crypto 参数。双端共用本目录；平台差异仅允许呈现形式。
> 更新：2026-09-25。

## 通用错误呈现模板（Android / Desktop 共用）

```
发生了什么（一句人话）
影响：数据没有变化 / 数据可能处于中间状态（会说明）
能否重试：可以重试 / 需要先修正后再试
你应该：具体可执行的下一步
```

## E-01 Import parse error（解析失败/部分失败）

- **What happened**: 文件里有无法识别的内容（哪部分：行号区间或"第 X 行之后"级别，若实现只知数量则显示数量）。
- **Was data changed**: 未确认前不写库；跳过行不参与分析。
- **Can user retry**: 可以（修正文件后重试）。
- **Should do**: 说明支持格式（微信账单/CSV/OFX/QFX）与"跳过的行不会参与分析"，引导重新选择或修复文件。
- 实现位：ImportScreen（Android）/ ImportScreen（Desktop）；禁用堆栈/类型名。

## E-02 Mapping error（字段对应失败）

- **What happened**: 没能自动认出哪一列是日期/金额。
- **Was data changed**: 无。
- **Can user retry**: 是——人工指定列后继续。
- **Should do**: 打开字段对应步骤，展示表头列并允许选择日期/金额列（自动建议 + 人工可改 + 示例值）。
- 实现位：Android CSV 映射步骤（v0.2.0 新增）/ Desktop MappingScreen。

## E-03 Unsupported file（不支持的格式）

- **What happened**: 这个文件不是受支持的格式。
- **Was data changed**: 无。
- **Can user retry**: 是（换文件）。
- **Should do**: 列出支持格式；不要显示 MIME/adapter id。

## E-04 Wrong backup password（备份密码错误）

- **What happened**: 备份密码不正确。
- **Was data changed**: 无（未开始恢复）。
- **Can user retry**: 是（重新输入）。
- **Should do**: 提示密码错误并请重试；不要显示密码校验细节。
- 实现位：RestoreScreen（Android/Desktop）区分"密码错误"。

## E-05 Corrupted backup（备份文件损坏）

- **What happened**: 备份文件损坏或不是有效容器。
- **Was data changed**: 无。
- **Can user retry**: 可以换一个备份文件。
- **Should do**: 提示文件损坏，建议重新导出备份。

## E-06 Future schema（备份版本过新）

- **What happened**: 这个备份由更新的版本创建，当前版本无法读取。
- **Was data changed**: 无。
- **Can user retry**: 升级应用后重试。
- **Should do**: 提示升级应用；不展示 formatVersion 细节到主文案（About/高级信息可说明）。

## E-07 Database open failure（数据库打开失败）

- **What happened**: 本机数据库无法打开。
- **Was data changed**: 无（打开失败不写入）。
- **Can user retry**: 是（重试）；持续失败建议删除全部数据重建。
- **Should do**: 提示"本地数据暂时无法打开"，引导重试或重置；绝不显示 SQLCipher/路径。

## E-08 Security unlock failure（解锁失败）

- **What happened**: 无法用当前凭据解锁（生物识别/设备凭据失败）。
- **Was data changed**: 无。
- **Can user retry**: 是（重试或换方式）。
- **Should do**: 停留在锁屏并允许重试/重新检查设备能力；不显示 key alias/crypto 细节。

## E-09 File permission / IO failure（文件权限或读写失败）

- **What happened**: 无法读取/写入所选文件（被占用、只读、缺失、路径过长、无权限）。
- **Was data changed**: 导入/备份未完成时不产生变更（备份写入失败则文件不存在或不完整，并提示）。
- **Can user retry**: 是（释放文件/换位置后重试）。
- **Should do**: 区分"文件被占用/缺少/只读/路径不支持"等可行动提示；不显示内部路径。

## E-10 Async/流程中断（进程重启、锁中断）

- **What happened**: 导入/恢复/确认流程被中断（应用后台/重启）。
- **Was data changed**: 无半提交——已提交部分以事务原子化；中断回到可继续或明确的中间态。
- **Can user retry**: 是（重新选择文件/重新输入密码）。
- **Should do**: "上次的选择被中断（应用进程已重启），请重新选择文件。"（Android 已有实现）。

## 安全红线（E-交叉规则）

1. UI 不得出现：SQLCipherException、SQLite error、stack trace、internal file path、key alias、crypto 参数、adapter id。
2. 日志不得出现：raw CSV 行、完整用户对象、source transaction id、口令、密钥材料（AGENTS §17）。
3. 所有错误至少满足四问，缺一问即视为未完成（验收项 E1/E2 的判据）。
