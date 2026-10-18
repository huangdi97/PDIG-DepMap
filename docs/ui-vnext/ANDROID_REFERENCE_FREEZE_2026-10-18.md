# Android Reference Freeze — 2026-10-18

> **Decision:** `APPROVED` by the user on 2026-10-18, together with visual
> acceptance and Production Cutover.
>
> This freeze applies to the R52+ Android UI candidate and the approved VNext
> release artifact. It does not by itself authorize a store upload; production
> signing remains `HOLD`.

## Frozen identity

| Item | Value |
| --- | --- |
| Frozen code tree | `7aaa612c6a6dfdd0ef431a8a67879f1caef63f96` |
| Release artifact `GIT_SHA` | `01d0de3a` |
| Package | `com.pdig.app` |
| Version | `0.3.1` (2) |
| Release APK SHA-256 | `cd8afab1e5981ac844c55a648c75c921016d84fcd5a221efc997a91e0be99d9c` |
| Legacy control SHA-256 | `287afd586aba3e5ba72ae311c738ef5cf1d550c1b3c6b4809e991a59a2120316` |
| Signer | `CN=PDIG NON-PRODUCTION TEST KEY` |
| GitHub prerelease | [android-ui-vnext-cutover-candidate-7aaa612](https://github.com/huangdi97/PDIG-DepMap/releases/tag/android-ui-vnext-cutover-candidate-7aaa612) |
| Evidence root | `artifacts/runtime-evidence/2026-10-17-r61-plus/` |
| Evidence manifest | `artifacts/runtime-evidence/2026-10-17-r61-plus/MANIFEST.json` |
| Evidence size at freeze | 344 files / 159 PNGs |
| Acceptance record | `R52_PLUS_VISUAL_ACCEPTANCE_MATRIX.md` |

## Frozen visual scope

- Phone Compact 1.0× / 1.3× / 1.5×, 1080×2340 and 1080×2400.
- Phone landscape 851×393 dp.
- Tablet MEDIUM 800×1280 dp at 1.0× / 1.3× / 1.5×.
- Tablet EXPANDED 1280×800 dp at 1.0× / 1.5×.
- Production VNext 1.0× / 1.5×, including the two-key release candidate.

## Known exceptions

1. The original 1536×1024 reference board is absent; the in-repo copy is a
   truncated 320×213 JPEG, so no pixel-level diff was performed.
2. The approved artifact uses the non-production test signing key. No production
   keystore is available, so `STORE_RELEASE = HOLD`.
3. Production Records is `FLAG_SECURE`; its screenshot is intentionally black and
   its semantics are captured in XML.
4. The master document `PDIG_v2.3-R1_..._2026-10-07.md` is absent from both local
   and remote trees.

## Change policy after freeze

Any subsequent UI change to this candidate requires:

```text
new exact source SHA
→ new runtime captures
→ updated evidence manifest
→ new user acceptance
→ new freeze record
```

The approved release artifact remains valid only for the frozen code tree and
hashes above. Rebuilding from a different tree without re-acceptance is not covered
by this freeze.
