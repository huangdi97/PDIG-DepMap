# ANDROID_SCREENSHOT_SHOT_LIST.md

> 生成时间：2026-09-22（ANDROID_CANONICAL_FREEZE → Production/Reality Closure 轮）
> 状态：**SHOT LIST READY / 截图 NOT_FINAL_BRAND_ASSET（未拍摄定稿）**
> 对应契约：Goal §16（至少覆盖 10 类屏幕；截图不得暴露真实账单/手机号/银行信息/卡片/私密 secret）。
> 拍摄环境：AVD（真实界面，合成数据）；最终品牌素材批准后再定稿。

---

## 0. 数据卫生红线（所有截图适用）

- ❌ 真实用户账单 / 真实手机号 / 真实银行信息 / 真实银行卡 / 私密 secret
- ❌ 内部 ID / UUID / 枚举 / JSON / debug / localhost / mock 字样
- ✅ 只使用**明确标记的合成数据**（如「示例银行信用卡」「示例服务」）
- 拍摄完成后重置本地数据（L-37 删除所有数据）

---

## 1. Shot List（10 类必拍 + 2 备选）

| #          | 屏幕                  | 来源路由                             | 画面要点（合成数据示例文案）                                                  | 拍摄要求                   |
| ---------- | --------------------- | ------------------------------------ | ----------------------------------------------------------------------------- | -------------------------- |
| 1          | **Home / Attention**  | `Route.HOME`                         | 「需要你处理」1–2 项（如「确认 2 条待确认关系」）；「即将到来」显示一张卡到期 | 六区可见；无真实数据       |
| 2          | **Import privacy**    | `Route.IMPORT`                       | 导入前隐私说明：「文件仅在本机解析，不上传」                                  | 展示隐私承诺               |
| 3          | **Import review**     | `Route.IMPORT_REVIEW`                | 解析结果预览：N 行解析 / Node Resolution 结果                                 | 展示脱敏后的服务名（示例） |
| 4          | **Proposal review**   | `Route.PENDING_REVIEW`               | 待确认 Proposal 列表：「示例服务与示例卡可能有关联？」+ 确认/忽略按钮         | 突出「未确认 ≠ 事实」      |
| 5          | **Infrastructure**    | `Route.INFRASTRUCTURE`               | 节点列表：示例服务 / 示例卡 / 示例账户                                        | 无真实账号                 |
| 6          | **Scenario Center**   | `Route.SCENARIO_CENTER`              | 三场景卡：更换支付卡 / 银行卡即将到期 / 注销银行卡                            | 展示场景覆盖分级           |
| 7          | **Impact**            | `Route.SCENARIO_SETUP`（影响面）     | 更换支付卡 → 受影响下游列表                                                   | 展示影响面                 |
| 8          | **ChangePlan**        | `Route.CHANGE_PLAN_DETAIL`           | 计划详情：状态标签 + 要做的事 + 下一步                                        | 展示 done≠verified 双态    |
| 9          | **Verification**      | `Route.CHANGE_PLAN_DETAIL`（验证态） | 逐项验证列表：「确认已验证」按钮                                              | 展示 verification          |
| 10         | **Timeline / Backup** | `Route.TIMELINE` / `Route.BACKUP`    | 时间线分桶 + 备份导出入口                                                     | 展示时间线投影与备份       |
| 11（备选） | **Restore**           | `Route.RESTORE`                      | 选择 .depmap → 口令 → 显式确认                                                | 展示加密恢复               |
| 12（备选） | **Privacy 设置**      | `Route.PRIVACY`                      | 隐私说明页 / 数据管理                                                         | 展示本地优先               |

> 屏幕路由名以 `android/app/src/main/kotlin/com/pdig/app/ui/` 实际代码为准；
> 若个别路由在最终产品中不可达，则跳过并在记录中注明（不伪造截图）。

---

## 2. 拍摄流程

1. AVD 全新安装 app → 解锁（App Lock 关闭或用测试 PIN）。
2. 用**合成数据**建立 1 个支付工具 + 若干依赖关系（示例对象名）。
3. 逐屏导航到 shot list 目标，`adb exec-out screencap -p > <name>.png`。
4. 检查：无真实数据 / 无内部 ID / 中文无截断 / 安全区正常。
5. 拍摄完成后 L-37 删除所有数据。

---

## 3. 质量检查清单（每张截图）

- [ ] 无内部 ID / UUID / JSON / 枚举
- [ ] 无调试信息 / localhost / mock 字样
- [ ] 无真实卡号 / 真实商户 / 真实姓名 / 真实手机号
- [ ] 中文无截断、无错别字
- [ ] 状态栏 / 安全区正常（无刘海遮挡、无手势条压字）
- [ ] 与 `store/STORE_LISTING_DRAFT.md` 文案不冲突
- [ ] 与 `ANDROID_STORE_METADATA.md` §12 合规红线一致

---

## 4. 状态

```text
ANDROID_SCREENSHOT_SHOT_LIST = READY（10 类必拍已定义；未拍摄定稿）
  当前可拍摄（AVD 可用、合成数据）；定稿依赖最终品牌批准（FINAL_BRAND_REQUIRED）
```

## 配套文档

- `ANDROID_BRAND_ASSET_SPEC.md`（像素/安全区规格）
- `store/STORE_LISTING_DRAFT.md`（文案对齐）
- `ANDROID_STORE_METADATA.md`（商店材料总表）
