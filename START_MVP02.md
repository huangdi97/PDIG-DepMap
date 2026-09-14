# START_MVP02.md

## 为什么现在进入 MVP02

MVP01 RC-AUDIT 已把代码侧质量收口。下一步最有价值的代码验证不是继续重复审计，而是证明 Core 真正脱离微信。

## 复制到现有项目根目录

- GOAL_MVP02_GLOBAL_SOURCE.md
- MVP02_ARCHITECTURE_FREEZE.md
- SCHEMA_V2_MIGRATION_SPEC.md
- SOURCE_ADAPTER_CONTRACT.md
- MVP02_TEST_MATRIX.md
- MVP02_ACCEPTANCE.md
- NEXT_BACKLOG.md
- ZCODE_MVP02_FIRST_PROMPT.txt
- ZCODE_MVP02_CONTINUE.txt

不要覆盖现有：

- AGENTS.md
- CANONICAL_DESIGN.md
- MVP_ACCEPTANCE.md
- WORK_STATUS.md
- BLOCKERS.md
- MVP01_RC_AUDIT_REPORT.md

## Goal Mode

如果 ZCode 支持 `/goal`，填：

> 完整执行 GOAL_MVP02_GLOBAL_SOURCE.md。从 Schema v1→v2 开始，完成 SourceInstance、EvidenceSourceAdapter、fingerprint scope、多源 Evidence、RelationDefinitionRegistry、WeChat Adapter 化、Generic CSV、OFX/QFX、multi-source synthetic E2E 和全部 MVP01 regression。DEPMAP_CONTAINER_V1 不得改变。Real Data 保持 NOT_RUN。除外部平台工具链外不得提前结束，直到生成 MVP02_FINAL_REPORT.md。

然后发送 `ZCODE_MVP02_FIRST_PROMPT.txt`。

## 完成标志

`MVP02_FINAL_REPORT.md` 中必须真实出现：

```text
MVP02_GLOBAL_SOURCE_ABSTRACTION = PASS
```

并同时：

- schema v2 PASS
- migration PASS
- WeChat regression PASS
- Generic CSV PASS
- OFX/QFX PASS
- multi-source Evidence PASS
- fingerprint source isolation PASS
- `.depmap` container V1 regression PASS
- quality gates PASS
- Real Data = NOT_RUN
