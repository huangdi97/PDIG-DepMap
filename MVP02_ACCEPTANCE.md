# MVP02_ACCEPTANCE.md

> `[x]` 只能在有测试/证据时勾选。Real Data 本轮允许保持 NOT_RUN。

## A Architecture
- [ ] EvidenceSourceAdapter 正式化
- [ ] WeChat 成为普通 Adapter，不是 Domain 特例
- [ ] SourceInstance implemented
- [ ] coverageMode implemented
- [ ] authoritativeFor implemented
- [ ] RelationDefinitionRegistry implemented

## B Schema v2
- [ ] schemaVersion = 2
- [ ] v1→v2 migration PASS
- [ ] repeated migration PASS
- [ ] migration rollback PASS
- [ ] legacy WeChat dedupe retained
- [ ] evidenceRefs migrated
- [ ] no orphan SourceInstance refs

## C Fingerprint
- [ ] source-scoped unique key
- [ ] same txn id across sources safe
- [ ] same source duplicate safe
- [ ] fpSecret continuity PASS

## D Evidence
- [ ] per SourceInstance/proposal stream
- [ ] multi-source evidenceRefs
- [ ] counts not blindly summed
- [ ] conservative re-proposal rule

## E Sources
### WeChat
- [ ] all regression tests PASS
### Generic CSV
- [ ] explicit mapping
- [ ] fixtures PASS
- [ ] deterministic
- [ ] 10k smoke
### OFX/QFX
- [ ] fixtures PASS
- [ ] FITID + fallback fingerprint
- [ ] deterministic

## F Reality Safety
- [ ] file adapters = event_stream
- [ ] absence never retires Reality
- [ ] no adapter auto-confirms Dependency
- [ ] no adapter creates Group
- [ ] no machine-generated required

## G `.depmap`
- [ ] DEPMAP_CONTAINER_V1 unchanged
- [ ] Golden Vector unchanged
- [ ] payload schema v2 PASS
- [ ] payload v1 migration PASS

## H E2E
- [ ] WeChat E2E
- [ ] CSV E2E
- [ ] OFX E2E
- [ ] two SourceInstances → one logical Proposal
- [ ] confirmation → one Dependency
- [ ] Impact regression PASS
- [ ] ChangePlan regression PASS

## I Quality
- [ ] format PASS
- [ ] lint PASS
- [ ] typecheck PASS
- [ ] all tests PASS
- [ ] architecture PASS
- [ ] secret scan PASS
- [ ] privacy audit PASS
- [ ] clean install PASS
- [ ] clean clone PASS

## J Real Data
- Correctness Gate: NOT_RUN
- Value Gate: NOT_RUN

## K Final
- [ ] WORK_STATUS updated
- [ ] BLOCKERS updated
- [ ] docs updated
- [ ] MVP02_FINAL_REPORT generated
- [ ] verdict written
