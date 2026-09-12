# 变更评审清单（Pull Request / 单人自审通用）

> 本仓当前单人开发、无远端 PR 流程；本模板作为每次变更的自审 checklist 与未来协作模板。

## What changed?

<!-- 一句话说清改了什么（模块/文件级别） -->

## Why?

<!-- 动机：缺陷 / 需求 / 工程基线项，引用控制文件或 blocker 编号 -->

## Schema changed?

- [ ] 否
- [ ] 是 → 迁移测试（N-1 → N / oldest → N / 回滚 / ×50 幂等）已通过

## Security / Privacy impact?

- [ ] 无（说明：不触及持久化、日志、网络、密钥）
- [ ] 有 → SECURITY_PRIVACY_REGRESSION_MATRIX 对照结果：

## New network calls / permissions?

- [ ] 无（`check:network` PASS）
- [ ] 有 → **必须先评审**，MVP 原则默认拒绝

## Impact semantics changed?

- [ ] 否
- [ ] 是 → invariant + property 套件 + FAIL_CLOSED_MATRIX F-11~F-15 复核

## Backward compatible?

- [ ] 是（含 `.depmap` V1 兼容 / DB 迁移路径）
- [ ] 否 → 说明理由与升级路径

## Tests / Coverage?

- [ ] 新增/变更逻辑有对应测试（test-first）
- [ ] coverage 不低于 COVERAGE_POLICY Baseline Gate
- [ ] `npm run check` 全绿

## Platform verification?（涉及平台壳时）

- [ ] 不涉及
- [ ] IMPLEMENTED / COMPILED / TESTED / DEVICE_VERIFIED 分别声明：

## Docs updated?

- [ ] WORK_STATUS.md
- [ ] 受影响 docs/（TEST_STRATEGY / COVERAGE_POLICY / FAIL_CLOSED_MATRIX / …）
- [ ] README 命令仍可实跑
