# QUALITY_GATES.md

## Q0 Repo
- [ ] git diff --check PASS
- [ ] no accidental binaries
- [ ] no real data
- [ ] no signing materials
- [ ] line endings sane

## Q1 Static
- [ ] format:check PASS
- [ ] lint errors = 0
- [ ] typecheck PASS
- [ ] architecture check PASS
- [ ] release-blocking TODO = 0

## Q2 Tests
- [ ] unit PASS
- [ ] integration PASS
- [ ] parser PASS
- [ ] crypto PASS
- [ ] impact PASS
- [ ] migration PASS
- [ ] proposal lifecycle PASS
- [ ] synthetic E2E PASS
- [ ] determinism PASS
- [ ] idempotency PASS
- [ ] negative tests PASS
- [ ] fuzz/property smoke PASS

## Q3 Security/Privacy
- [ ] production secret findings = 0
- [ ] sensitive log findings = 0
- [ ] raw bill persistence = 0
- [ ] fail-closed audit PASS
- [ ] business network calls = 0
- [ ] encrypted export remains default

## Q4 Dependencies
- [ ] npm/package tree valid
- [ ] lockfile consistent
- [ ] dependency audit generated
- [ ] license audit generated

## Q5 Clean Environment
- [ ] clean install PASS
- [ ] npm run check PASS
- [ ] clean clone simulation PASS

## Q6 UI Source
- [ ] no mock production data
- [ ] loading/empty/error reviewed
- [ ] Proposal != Reality
- [ ] destructive final action last
- [ ] Chinese default
- [ ] accessibility source audit

## Q7 Platform
### Android
- [ ] STATIC_AUDITED
- [ ] COMPILED if toolchain available
- [ ] TESTED if toolchain available
### HarmonyOS
- [ ] STATIC_AUDITED
- [ ] COMPILED if toolchain available
- [ ] TESTED if toolchain available
### iOS
- [ ] STATIC_AUDITED
- [ ] Mac handoff complete
- [ ] COMPILED only if Xcode available

## Q8 Docs/Release
- [ ] README commands verified
- [ ] version matrix
- [ ] permission audit
- [ ] network audit
- [ ] crypto audit
- [ ] privacy dataflow audit
- [ ] clean clone report
- [ ] RC report
