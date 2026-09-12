# AGENT_DEVELOPMENT_PROTOCOL.md — Agent 开发协议（Engineering Baseline V1）

> 适用：ZCode / WorkBuddy / CodeBuddy / Codex / OpenCode / 其他任何编码 Agent。

## 会话开始（强制顺序）

1. 读 `AGENTS.md`（长期铁律）
2. 读 `WORK_STATUS.md`（当前状态）与 `BLOCKERS.md`
3. `git status` + `git diff` + `git log --oneline -10`（现场事实）
4. 跑 `npm run check` 确认起点绿（或如实记录起点红）

## 协作纪律

- **不覆盖其他 Agent 未提交修改**：发现 dirty files 先判断归属（WORK_STATUS / 命名），
  不得 `git reset --hard` / `git clean -fd` / `git checkout .` / `git restore .`。
- 接力任务从 WORK_STATUS 的 Next 项继续，不从零重建。

## 执行顺序

1. 先读相关 docs（TEST_STRATEGY / ARCHITECTURE_RULES / CHANGE_RISK_POLICY）
2. focused tests（新增测试先行，test-first）
3. implementation
4. regression：`npm run check`
5. milestone：`npm run check:full`
6. 更新 WORK_STATUS（Current / 本轮完成 / 测试证据 / Next）
7. 结束前确认 git status 干净（或明确说明遗留）

## 绝对禁止

- 伪造 PASS / 删除测试 / 降断言 / skip 失败用例 / retry 掩盖 flaky
- synthetic 冒充 real data（Real Data Gate 只能由用户提供真实账单后跑）
- 未编译平台写 COMPILED / TESTED
- 自动 push（除非用户明确要求）
- 引入 AGENTS.md §22 禁止项（LLM/Neo4j/云同步/…）或跳过 Contract Test 合并新 Adapter

## 升级 / 上报

- 编译错误 / 测试失败 / 依赖冲突：自行修复（不是 blocker）。
- 外部依赖（SDK / 账号 / 真实账单 / macOS）：记录 `BLOCKERS.md` 编号后继续其他可执行工作。
