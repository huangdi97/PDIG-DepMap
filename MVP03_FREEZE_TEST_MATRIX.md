# MVP03_FREEZE_TEST_MATRIX.md

Readiness:
FR-READ-001 one target / multiple actions
FR-READ-002 multiple targets / shared action
FR-READ-003 unrelated completed action
FR-READ-004 unresolved must-change
FR-READ-005 unknown criticality
FR-READ-006 pending proposal
FR-READ-007 unresolved candidate
FR-READ-008 revision mismatch
FR-READ-009 all known resolved
FR-READ-010 confidence bypass forbidden
FR-READ-011 event-stream absence
FR-READ-012 deterministic

GraphRevision:
FR-GR-001 fresh=0
FR-GR-002 Dependency confirm +1
FR-GR-003 Evidence no bump
FR-GR-004 Proposal no bump
FR-GR-005 retire +1
FR-GR-006 reactivate +1
FR-GR-007 Group confirm +1
FR-GR-008 restart
FR-GR-009 failure rollback
FR-GR-010 duplicate replay
FR-GR-011 property random ops

Rebase:
FR-RB-001 unchanged revision
FR-RB-002 mismatch
FR-RB-003 added impact
FR-RB-004 removed impact
FR-RB-005 changed impact
FR-RB-006 completed history immutable
FR-RB-007 cancelled stable
FR-RB-008 deterministic ordering

Drift:
FR-RD-001 positive alternative evidence
FR-RD-002 absence-only no drift
FR-RD-003 no auto Reality mutation
FR-RD-004 dismiss
FR-RD-005 replacement resolution
FR-RD-006 additional path
FR-RD-007 duplicate evidence
FR-RD-008 upsert key
FR-RD-009 multi-source provenance

Candidate:
FR-DC-001 candidate upsert
FR-DC-002 no Impact
FR-DC-003 no revision bump
FR-DC-004 accept one Node
FR-DC-005 replay no duplicate
FR-DC-006 dismiss no Node
FR-DC-007 privacy fields

Template:
FR-ST-001 active has factory
FR-ST-002 planned factory null
FR-ST-003 planned cannot execute
FR-ST-004 active capability supported
FR-ST-005 generic reminder policy reject
FR-ST-006 production registry only supported templates

Timeline:
FR-TL-001 valid source refs
FR-TL-002 deterministic sort
FR-TL-003 overdue priority
FR-TL-004 projection only
FR-TL-005 1k items smoke

Verification:
FR-VR-001 done != verified
FR-VR-002 manual confirm
FR-VR-003 future observation suggestion
FR-VR-004 no Reality mutation
FR-VR-005 duplicate evidence
FR-VR-006 invalid transition reject
FR-VR-007 restart persistence

Migration / depmap:
FR-MIG-001 fresh v3
FR-MIG-002 v2→v3
FR-MIG-003 restart
FR-MIG-004 ×50
FR-MIG-005 failure rollback
FR-MIG-006 payload v1→v3
FR-MIG-007 payload v2→v3
FR-MIG-008 v3 roundtrip
FR-MIG-009 future reject
FR-DPM-001 old Golden unchanged

Regression:
MVP01 full
MVP02 full
Engineering Baseline full
npm run check
npm run check:full
3x full suite
10x critical suite
