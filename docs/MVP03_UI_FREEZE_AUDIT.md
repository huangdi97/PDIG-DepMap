# MVP03_UI_FREEZE_AUDIT.md — UI 语义冻结审计（Freeze §81–§87）

> 方法：`app/pages/` 全 16 页源码逐条对照 §81–§87 语义清单（静态审计；编译 BLOCKED B10）。

## §81 首页（pages/home/home.uvue）

- [x] answer-oriented 四区块：需要你处理 / 即将到来 / 常用场景 / 我的基础设施
- [x] 无「Nodes/Edges/Sources 数量」主入口（计数只在待办语境出现：`{{ proposalCount }} 条`）

## §82 场景库（pages/scenarios/scenarios.uvue）

- [x] 只显示 3 个 active 可执行模板（静态数组与 registry active 集合一致）
- [x] 无 planned 占位（PLANNED_TEMPLATES 不进入 UI）
- [x] 顶部提示场景边界（数字基础设施；日常生活提醒不属于 PDIG）

## §83 ChangePlan（pages/plans/*.uvue）

- [x] plan-detail 显示 readiness 文案（blocked/review_required/ready_with_known_scope + 标准句式）
- [x] needs_revalidation 顶部横幅 +「重新分析」按钮（§22 口径：不改历史，提示基于 Graph Revision X）
- [x] plans 列表显示「分析版本 N / 当前 M」与 stale 状态
- [x] **Coverage 与 Readiness 不混淆**：当前 UI 只呈现 readiness；ScenarioCoverage 为 core 计算面
      （computeScenarioCoverage），未在 UI 冒充安全结论 —— 无违规混用
- [x] plan-detail 的 readiness 派生已同步 Freeze 语义（显式 resolvesImpactKeys 判定，
      不再用 target 数量 − 动作数量；见 48b38b6 提交）

## §84 Drift UI（pages/drift/drift.uvue）

- [x] 文案「可能发生了变化」（标题）—— 非「已经发生变化」
- [x] 副文案标注 kind（possible_replacement 等），用户四选项明确（已换成新来源/两个都在用/没有变化/稍后确认）
- [x] 提示「已经换成新来源」会确认新来源并停用旧的（Reality 语义预告）

## §85 Candidate UI

- [x] 无「已经加入基础设施」表述；Candidate 概念在 core（discovery-service），
      UI 侧未接入候选确认页（MVP03 UI 最小集成范围，无违规文案；后续接入时须用「发现候选」表述——
      已写入 SCENARIO_TEMPLATE_POLICY 邻接口径与 LIVING_GRAPH 文档）

## §86 Verification UI（plan-detail.uvue）

- [x] done 与 verification 分离渲染：完成动作仅 ✓；未 verified 的 verify 动作显示
      「手动标记已验证 / 确认已生效（新数据支持）」按钮；verified 显示「已验证 ✓（method）」
- [x] 页面底部固定提示「完成 ≠ 验证」

## §87 UI Copy Freeze（禁词全仓扫描）

grep 结果（app/ 全部 .uvue）：

| 禁词                                                              | 命中                                           |
| ----------------------------------------------------------------- | ---------------------------------------------- |
| 100% 安全 / 完全安全 / 绝对不会遗漏 / 可以放心注销 / 确认已经变化 | **0**                                          |
| 已经验证完毕 / 全部搞定                                           | 0                                              |
| 标准句式「基于当前已知并确认的信息」                              | 命中（plan-detail readiness ready 文案，符合） |
| 「可能发生了变化」                                                | 命中（drift 页 + home 待办，符合）             |

## 结论

**UI_SEMANTICS = PASS（源码级）**；COMPILED/DEVICE_VERIFIED 维持 BLOCKED（B10）。
