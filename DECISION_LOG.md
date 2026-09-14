# DECISION_LOG.md

## D-001 — Product canonical baseline

Status: ACCEPTED

`CANONICAL_DESIGN.md` is the only product/domain/schema/impact baseline.
Older v0.4.1/R1/R2 patch files are superseded.

## D-002 — Three-platform application stack

Status: ACCEPTED

Use:

- uni-app x Vapor + Vue 3 + TypeScript/UTS
- Android Kotlin adapter
- iOS Swift adapter
- HarmonyOS ArkTS adapter

Reason:
The product must be published on Android, iOS, and HarmonyOS while retaining a shared business core.

## D-003 — Platform storage

Status: ACCEPTED

- Android/iOS: SQLCipher + OS secure key storage
- HarmonyOS: ArkData encrypted relational storage + HUKS
- One logical Schema, platform-specific secure database adapters

## D-004 — MVP Impact scope

Status: ACCEPTED

Impact Kernel is payment-domain only.
Cross-capability inference is deferred.
