# PRODUCTION_UI_AUDIT.md — PDIG 产品与 UI 审计（Production RC V1）

> 方法：逐页实读 + `core/scripts/check-ui.mjs` 机械校验。
> 关键前提：**本机无 uni-app x 编译器（B10）**，因此本审计为**源码级**；
> 编译 / 真机 / 截图类判定一律 `BLOCKED`。

---

## 1. 信息架构（§22）

### 审计前

17 页平铺，`pages.json` **无 tabBar**，所有页面靠 `navigateTo` 串联，无一级导航。
首页与"节点""模拟"等工程页同级。

### 处置后（一级导航冻结）

```
首页       pages/home/home          今天有需要你处理的事吗
场景       pages/scenarios/scenarios 场景中心
计划       pages/plans/plans        变更计划
我的       pages/settings/settings  我的（安全 / 备份 / 数据与隐私 / 危险操作）
```

二级页面（24 页合计）：`onboarding` · `unlock` · `plan-create` · `plan-detail` ·
`timeline` · `drift` · `candidates` · `proposals` · `group-proposals` · `nodes` ·
`node-detail` · **`declare-relation`** · `import-wechat` · `node-resolution` · `sources` ·
`backup` · `simulate` · `impact-result` · `privacy` · `about`。

> 未强行拆页；未引入"图谱"独立 Tab（Graph 不是默认首页，§29）。

### 补充：主流程可用性（本轮补齐）

审计发现一个**产品级缺口**：Dependency 只能由 Proposal 接受产生，Proposal 只能由导入产生，
而导入在设备上不可用（B20）——意味着**设备上无法建立任何依赖关系**，应用不可用。

处置：新增 **`pages/nodes/declare-relation.uvue`**（三步式：关系类型 → 另一端对象 → 是否必须处理），
并在 `node-detail` 提供入口。语义完全在 MVP01 范围内（`Dependency 存在即用户确认`，AGENTS §9）：

- 方向由「本节点是什么」推导（`declarationRoleOf` / `peerKindsFor`），**不让用户自己猜**；
- 关系选项严格取自 Core schema 的 CHECK 约束（`funding_source` / `merchant_agreement` / `bound_to`）；
- capability 固定 `payment`（MVP 只支持 payment，AGENTS §13）；
- criticality 必须由用户显式选择（`required` / `unknown`），**机器不得自动产生 required**；
- Reality mutation 与 `graphRevision +1` **同事务**；retired 后重新声明复用同一 id。

---

## 2. 首页 3 秒标准（§162）

**审计前**：首页有"需要你处理 / 即将到来 / 常用场景 / 我的基础设施"，但四项中三项是导航行，
且"待验证 / 其他待办"是模糊占位（点了只跳时间线）。

**处置后**：首页顺序 = 定位语 → 需要你处理（真实计数，0 条时明确说明）→ 即将到来 →
常用场景 → 我的基础设施。四项待办各自可点、指向真实页面。

| 问题 | 是否可答 |
|---|---|
| 有没有需要处理的事？ | ✅ 计数 + 空态文案 |
| 近期有没有重要变化？ | ✅ "可能发生了变化" |
| 从哪个场景开始？ | ✅ 常用场景 3 项 |

**判定：PASS（源码级）。**

---

## 3. ChangePlan 页（§31/§32/§163）

**审计前**：`plan-detail` 展示 `PlanReadiness` / `workflow_state` 等内部词；
只有一个隐式状态判断；`mustChange` 未声明标识符导致该页**无法编译**。

**处置后**：

- 状态用 `dp-chip` + 中文标签（还有必须处理的事项 / 还有信息需要确认 / 可以继续 / 需要重新检查）
- 「本次分析依据」卡：四级等级 + 可解释理由 + 最近导入时间 + 明确"不代表安全程度"
- 「影响」卡：必须处理 / 需要确认 分开
- 「要做的事」卡：`✓` / `○` + 阶段中文 + 验证按钮；明确"完成不等于验证"
- 「下一步」卡：**CTA 按状态分化**（处理必须事项 / 重新分析 / 继续确认 / 继续下一步 / 查看结果）
- 版本落后时单独 banner + 「重新分析」

| 问题 | 是否可答 |
|---|---|
| 我要做什么？为什么？ | ✅ |
| 哪些必须做？哪些只是建议确认？ | ✅ 分开呈现 |
| 现在能不能继续？ | ✅ CTA + 说明 |
| 判断基于哪些数据？ | ✅ 本次分析依据 |
| 还有没有未确认信息？ | ✅ 需要确认 / 待确认关系 |

**判定：PASS（源码级）。**

---

## 4. 导入流程与隐私（§36/§37/§164）

**审计前**：导入页只有"选择 CSV" + "开始解析"，**无隐私说明步骤**；
点击"开始解析"后跳到商户页，但 `pendingMerchants` 恒为空 → **点了没反应的假功能**（违反 §76）。

**处置后**：导入页新增「导入前请了解」卡（本地处理 / 不长期保存原始流水 / 只保留证据摘要 /
无账号无统计无广告）；识别结果区有明确空态；并在「当前构建说明」中**如实标注**
本机解析桥接尚未接入、暂不写入数据。

| 问题 | 是否可答 |
|---|---|
| 文件是否上传？ | ✅ 明确"不会上传" |
| 哪些内容会保存？ | ✅ |
| 哪些不会保存？ | ✅ |
| 备份如何加密？ | ✅ 备份页 |

**判定：PASS（源码级）。注意：导入功能在设备上尚不可用（B20）。**

---

## 5. Drift / Candidate（§34/§35）

| 页 | 审计前 | 处置后 |
|---|---|---|
| Drift | 标题已是"可能发生了变化"；但选项文案工程化，且**直接裸 SQL 提升 graphRevision** | 标题按类型分（可能换了支付来源 / 可能新增了一条支付路径）；展示"当前已确认 / 最近观察到"；四选项（已经换成新来源 / 两个都在使用 / 没有变化 / 稍后确认）；写入经服务层事务 |
| Candidate | **无独立页面** | 新增「待确认服务」页：是我的 / 不是 / 说明"确认只建对象，不自动建关系" |

**判定：PASS（源码级）。**

---

## 6. 状态完备性（§40–§43）

| 页 | Empty | Error | Loading | 破坏性确认 |
|---|---|---|---|---|
| home | ✅ | ✅ | ✅ | — |
| plans | ✅ | ✅ | ✅ | — |
| plan-detail | ✅（无影响时说明） | —（写入失败用 toast） | ✅ | — |
| timeline | ✅ | ✅ | ✅ | — |
| drift | ✅ | ✅ | ✅ | — |
| candidates | ✅ | —（toast） | ✅ | — |
| proposals | ✅ | ✅ | ✅ | — |
| group-proposals | ✅ | —（toast） | ✅ | — |
| nodes | ✅ | —（toast） | ✅ | — |
| node-detail | ✅ | —（toast） | — | ✅ 停用关系确认 |
| sources | ✅ | —（toast） | ✅ | ✅ 删除来源确认 |
| backup | — | ✅ | ✅ | ✅ 口令校验 |
| settings | — | ✅ | — | ✅ 重置二次确认 |
| simulate | ✅ | — | ✅ | — |
| impact-result | ✅ | — | ✅ | — |

**判定：PASS（源码级；`check:ui` U8 强制数据驱动页面必须有状态处理）。**

---

## 7. 文案与工程词（§57–§60/§165）

- 已消除模板中的工程词（`PlanReadiness` / `confidence` / `must_change` / `graphRevision`）
- `check:ui` U3 强制禁止词表
- 未使用"危险 / 高危 / 严重风险"
- 未做绝对化隐私承诺（见 `docs/PRIVACY_POLICY_DRAFT.md` §9）

**判定：PASS。**

---

## 8. 可访问性 / 响应式 / Safe Area（§52–§56）

| 项 | 状态 |
|---|---|
| 触控区 ≥ 44px | PASS（按钮 / 行 / 选项统一 `min-height: 44px`） |
| 状态非仅颜色 | PASS（`dp-chip` 始终带中文标签） |
| 错误信息可见 | PASS（`dp-state` / toast） |
| 重复提交防护 | PASS（`dp-button` loading/disabled 吞掉点击） |
| 长中文换行 | PASS（无固定宽度、无 `ellipsis`） |
| Safe Area / insets | **NOT_RUN** —— 需真机或 HBuilderX 预览验证（B1/B2/B10） |
| 键盘遮挡 | **NOT_RUN**（同上） |
| 对比度量化 | **NOT_RUN** —— 未做工具化对比度测量 |

**判定：PARTIAL_WITH_REPORT。**

---

## 9. 截图证据（§168）

**BLOCKED**：无 HBuilderX / 无 Web-H5 运行环境，无法生成 Home / Scenario / Plan / Timeline /
Drift / Import / Settings 截图。已准备截图场景计划（`store/SCREENSHOT_PLAN.md`）。

---

## 10. 结论

```
PRODUCT_READY      = PARTIAL_WITH_REPORT（IA / 首页 / 计划 / 导入隐私 / Drift / Candidate 已产品化；
                     但导入与备份在设备上不可用 —— B20/B21）
UI_UX_READY        = PARTIAL_WITH_REPORT（源码级 PASS；编译与真机 BLOCKED）
DESIGN_SYSTEM      = PASS（docs/DESIGN_SYSTEM.md + tokens + 5 组件）
ONBOARDING         = PASS（3 屏，不写业务数据）
UI_STATIC_GATE     = PASS（check:ui 8 类校验，0 命中）
```
