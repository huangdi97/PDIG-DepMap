# QUALITY_GATES.md — Gate 状态（2026-09-12 RC 收口）

## Q0 Repo
- [x] git diff --check PASS
- [x] no accidental binaries
- [x] no real data（local_private 仅 README）
- [x] no signing materials
- [x] line endings sane（.gitattributes：源码 LF、fixtures 保字节）

## Q1 Static
- [x] format:check PASS（prettier，TS/JS/JSON/MD/YAML；.uvue/.uts 明确排除）
- [x] lint errors = 0（eslint typed，0 warnings）
- [x] typecheck PASS（strict 全开）
- [x] architecture check PASS（check:architecture，27 files）
- [x] release-blocking TODO = 0

## Q2 Tests（166/166）
- [x] unit PASS
- [x] integration PASS
- [x] parser PASS
- [x] crypto PASS
- [x] impact PASS
- [x] migration PASS
- [x] proposal lifecycle PASS
- [x] synthetic E2E PASS
- [x] determinism PASS
- [x] idempotency PASS
- [x] negative tests PASS
- [x] fuzz/property smoke PASS

## Q3 Security/Privacy
- [x] production secret findings = 0
- [x] sensitive log findings = 0
- [x] raw bill persistence = 0
- [x] fail-closed audit PASS
- [x] business network calls = 0
- [x] encrypted export remains default

## Q4 Dependencies
- [x] npm/package tree valid
- [x] lockfile consistent（npm ci）
- [x] dependency audit generated（3 moderate，dev-only，已评估）
- [x] license audit generated（THIRD_PARTY_NOTICES.md）

## Q5 Clean Environment
- [x] clean install PASS
- [x] npm run check PASS
- [x] clean clone simulation PASS

## Q6 UI Source
- [x] no mock production data
- [x] loading/empty/error reviewed
- [x] Proposal != Reality
- [x] destructive final action last
- [x] Chinese default
- [x] accessibility source audit（SOURCE_AUDITED）

## Q7 Platform
### Android
- [x] STATIC_AUDITED
- [ ] COMPILED if toolchain available（不可用：B1）
- [ ] TESTED if toolchain available（不可用：B1）
### HarmonyOS
- [x] STATIC_AUDITED
- [ ] COMPILED if toolchain available（不可用：B2）
- [ ] TESTED if toolchain available（不可用：B2）
### iOS
- [x] STATIC_AUDITED
- [x] Mac handoff complete
- [ ] COMPILED only if Xcode available（不可用：B3）

## Q8 Docs/Release
- [x] README commands verified（npm install/test/typecheck + validate-real-bill CLI 实跑）
- [x] version matrix（docs/VERSION_MATRIX.md）
- [x] permission audit（docs/PERMISSION_AUDIT.md）
- [x] network audit（docs/NETWORK_AUDIT.md）
- [x] crypto audit（docs/CRYPTO_RC_AUDIT.md）
- [x] privacy dataflow audit（docs/PRIVACY_DATAFLOW_AUDIT.md）
- [x] clean clone report（docs/CLEAN_CLONE_REPORT.md）
- [x] RC report（MVP01_RC_AUDIT_REPORT.md）
