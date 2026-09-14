# MVP02_TEST_MATRIX.md

## A Schema/Migration

- fresh v2
- v1→v2
- restart after migration
- migrate ×50 idempotent
- injected failure rollback
- no orphan SourceInstance
- legacy WeChat dedupe retained
- evidenceId→evidenceRefs

## B SourceInstance

- two instances same adapter
- same txn ID across instances no collision
- retired source keeps provenance
- lastIngestedAt update
- no secret fields

## C Fingerprint

- same instance/same txn duplicate
- different instance/same txn independent
- fingerprintVersion isolation
- canonical fallback deterministic
- fpSecret export/import continuity

## D WeChat Regression

全部 MVP01 parser/fingerprint/pipeline tests 继续 PASS。

## E Generic CSV

- US signed amount
- EU semicolon
- debit/credit columns
- BOM
- quoted delimiter
- CRLF / CR-only
- bad date / bad amount
- missing mapping
- same txn ID different source
- 10k rows
- multi-currency metadata
- deterministic ×50

## F OFX/QFX

- basic/multiple
- FITID fingerprint
- missing FITID fallback
- invalid date
- signed amounts
- malformed
- QFX basic
- duplicate FITID across SourceInstance
- deterministic ×50

## G Multi-source Evidence

- one Proposal, two evidenceRefs
- evidence counts separate
- accepted not re-asked
- rejected re-proposal only single stream threshold
- multi-source does not set required
- multi-source does not create Group

## H Coverage Semantics

- event_stream absence does not retire Dependency
- does not reject Proposal
- does not confirm fallback
- Freshness may update independently

## I Relation Registry

- funding_source valid
- merchant_agreement valid
- invalid fromKind/toKind rejected
- unsupported relation/capability rejected

## J `.depmap`

- container golden unchanged
- payload schema v2 export/import
- payload v1 migrate
- unsupported payload schema rejected
- wrong password/tamper regression

## K E2E

- WeChat
- Generic CSV
- OFX
- two SourceInstances → one logical Proposal
- confirmation → one Dependency
- Impact unchanged
- ChangePlan ordering unchanged

## L RC Regression

format/lint/typecheck/architecture/secret/privacy/clean install/clean clone 全部继续 PASS。
