# RC_ACCEPTANCE.md

> `[x]` 仅代表有可复现证据。真实数据 Gate 本轮固定 NOT_RUN。

## A. Static Quality
- [ ] format PASS
- [ ] lint PASS
- [ ] typecheck PASS
- [ ] architecture PASS
- [ ] TODO release blockers = 0

## B. Tests
- [ ] all existing tests PASS
- [ ] determinism PASS
- [ ] idempotency PASS
- [ ] negative/error paths PASS
- [ ] property/fuzz smoke PASS
- [ ] performance smoke PASS

## C. Security / Privacy
- [ ] secret scan PASS
- [ ] logging audit PASS
- [ ] crypto RC audit PASS
- [ ] privacy dataflow audit PASS
- [ ] fail-closed audit PASS
- [ ] network audit PASS

## D. Data / DB
- [ ] migration replay PASS
- [ ] DB integrity PASS
- [ ] no orphan logical refs
- [ ] import transactionality PASS
- [ ] backup/import rollback PASS

## E. Dependencies / License
- [ ] dependency tree valid
- [ ] lockfile clean
- [ ] vulnerability audit executed or exact blocker
- [ ] THIRD_PARTY_NOTICES updated

## F. Clean Environment
- [ ] clean install PASS
- [ ] full check after clean install PASS
- [ ] clean clone simulation PASS

## G. UI
- [ ] 11 pages source audited
- [ ] no production mock data
- [ ] empty/loading/error paths reviewed
- [ ] Proposal/Reality distinction preserved
- [ ] destructive final action last
- [ ] accessibility source audit complete

## H. Platform
### Android
- [ ] STATIC_AUDITED
- [ ] COMPILED
- [ ] TESTED
- [ ] DEVICE_VERIFIED

### HarmonyOS
- [ ] STATIC_AUDITED
- [ ] COMPILED
- [ ] TESTED
- [ ] DEVICE_VERIFIED

### iOS
- [ ] STATIC_AUDITED
- [ ] COMPILED
- [ ] TESTED
- [ ] DEVICE_VERIFIED

工具链/设备缺失项保持未勾选并写 Blocker。

## I. Real Data
- Correctness Gate: **NOT_RUN**
- Value Gate: **NOT_RUN**

## J. Final
- [ ] WORK_STATUS updated
- [ ] BLOCKERS updated
- [ ] MVP_ACCEPTANCE updated truthfully
- [ ] MVP01_RC_AUDIT_REPORT generated
- [ ] git diff --check PASS
- [ ] production secrets = 0
- [ ] real user data tracked = 0
- [ ] MVP01_DEV_CLOSEOUT verdict written
