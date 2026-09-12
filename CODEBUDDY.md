# CODEBUDDY.md — PDIG WorkBuddy Project Context

每次新会话先读：
- AGENTS.md
- CANONICAL_DESIGN.md
- WORK_STATUS.md
- BLOCKERS.md
- 当前 Goal

当前阶段：
WORKBUDDY_MVP02_CONTINUE_GOAL.md

这是 ZCode → WorkBuddy 接力任务。
不要从零初始化。
先检查 Git、测试和实际文件，再继续。

永久原则：
- 宁可漏报，不可把不确定伪装成必须处理
- Observation ≠ Dependency
- Proposal ≠ Reality
- absence ≠ nonexistence
- two edges ≠ confirmed fallback
- machine inference cannot set required
- raw statements are not persistent data
- MVP Impact remains payment-only

本轮范围：
Schema v2 / SourceInstance / EvidenceSourceAdapter / source-scoped fingerprint /
multi-source Evidence / RelationDefinitionRegistry / WeChat Adapter /
Generic CSV / OFX-QFX / regression / quality gates

不要实现 NEXT_BACKLOG。

禁止：
- git reset --hard
- git clean -fd
- 覆盖未提交 ZCode 修改
- 删除测试制造 PASS
- 降 strict/lint
- synthetic 冒充 real data
- 未编译平台写 compiled

工作方法：
inspect → test → fix → focused test → regression → update status → next
