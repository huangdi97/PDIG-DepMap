# REPORT — HarmonyOS v0.3.0 closure evidence (2026-09-27)

Scope: Harmony host conformance re-run (post H3/H4 host-wiring closure:
`migration-db-v1-to-v3` and `backup-depmap-export-restore-roundtrip` moved from
`RUNTIME_BLOCKED_CASES` to host-executed PASS via the pure-ArkTS `data/` layer —
`data/MigrationChain.ets` / `GraphStore.ets` / `GraphRepository.ets` /
`PayloadCodec.ets` / `SchemaV3.ets`), and the 2-gate `RUNTIME_BLOCKED` audit
(H1/H2, real NAPI gates). No fixture expectations were modified; nothing was
committed or pushed.

## 1. Toolchain

**DevEco found: YES** (not on the default `C:\Program Files` / `%LOCALAPPDATA%`
paths, but the previous install root referenced by the repo's `local.properties`
is present):

| Item               | Path                                                                                                           | Present                          |
| ------------------ | -------------------------------------------------------------------------------------------------------------- | -------------------------------- |
| DevEco Studio      | `D:\Code\Harmony\DevEco Studio` (product-info.json: version **5.0.5.310**, buildNumber 233.14475.28.36.505310) | ✓                                |
| hvigor             | `D:\Code\Harmony\DevEco Studio\tools\hvigor\hvigor\bin\hvigor.js`                                              | ✓                                |
| hvigor-ohos-plugin | `D:\Code\Harmony\DevEco Studio\tools\hvigor\hvigor-ohos-plugin`                                                | ✓                                |
| SDK                | `D:\Code\Harmony\DevEco Studio\sdk\default` (`openharmony` + `hms`)                                            | ✓                                |
| Emulator binaries  | `D:\Code\Harmony\DevEco Studio\tools\emulator\Emulator.exe`                                                    | ✓ (but no system image — see §3) |

Checked-and-absent paths (for the record, in case a future host loses `D:`):
`C:\Program Files\Huawei\DevEcoStudio*` (no `C:\Program Files\Huawei`),
`%LOCALAPPDATA%\OpenHarmony*`, `%LOCALAPPDATA%\Huawei*`, `%HOMEDRIVE%%HOMEPATH%\DevEco*`
(only `C:\Users\Kaiser\.hvigor` cache exists), `harmony/hvigor/hvigorw.bat` (repo
holds only `hvigor/hvigor-config.json5` — the wrapper is not committed),
`harmony/node_modules/.bin/hvigor*`, `hvigorw`/`hvigor` on PATH (none).
`PDIG_DEVECO_HOME` was unset; it was set to `D:\Code\Harmony\DevEco Studio` for
the runs below.

## 2. Host conformance (re-run)

Command (repo root `E:\AI\号卡管理`):

```text
$env:PDIG_DEVECO_HOME = "D:\Code\Harmony\DevEco Studio"
node tools/harmony/run-conformance-host.mjs
```

Key output (post H3/H4 host-wiring, single fresh run):

```text
> hvigor Finished :entry:default@UnitTestArkTS... after 12 s 627 ms
> hvigor BUILD SUCCESSFUL in 28 s 862 ms
[harmony] BUILD SUCCESSFUL
[conformance-host] 执行面 : hvigor 本地单元测试（ArkTS，无设备）
[conformance-host] 用例   : 181 条（含 3 条元测试）
[conformance-host] 汇总   : run=181 pass=181 fail=0 error=0
[ canonical ] HARMONY_TOTAL_CANONICAL    = 128
[ canonical ] HARMONY_HOST_EXECUTED      = 126   (fail=0)
[ canonical ] HARMONY_HOST_IMPL_MISSING  = 0
[ canonical ] HARMONY_ENV_BLOCKED        = 0
[ canonical ] HARMONY_DEVICE_BLOCKED     = 2   (Argon2id 原生 / ArkData)
HARMONY_CONFORMANCE_HOST=PASS
HARMONY_HOST_PASS=126/128
EXITCODE=0
```

Result file (fresh; final line `Tests run: 181, Failure: 0, Error: 0, Pass: 181,
Ignore: 0`):
`C:\Users\Kaiser\pdig-harmony-build\harmony\entry\.test\default\intermediates\test\coverage_data\test_result.txt`
Both newly-wired cases show `Success` there (`test=migration/migration-db-v1-to-v3`
and `test=backup/backup-depmap-export-restore-roundtrip`).
Machine-readable canonical report regenerated in-repo:
`conformance/reports/harmony.json` (`pass=126, fail=0, notImplemented=0,
envBlocked=0, runtimeBlocked=2, total=128`; H3 → `migration`/PASS, H4 →
`backup`/PASS; the 2 remaining gates registered as `RUNTIME_BLOCKED` with
per-case reasons).

## 3. HAP build (clean)

Command (repo root):

```text
$env:PDIG_DEVECO_HOME = "D:\Code\Harmony\DevEco Studio"
node tools/harmony/build-ascii-mirror.mjs --clean assembleHap
```

Key output:

```text
[harmony] mirror cleaned: C:/Users/Kaiser/pdig-harmony-build
[harmony] exec: ...hvigor.js --mode module -p product=default assembleHap --no-daemon
> hvigor Finished :entry:default@BuildNativeWithCmake... after 8 s 970 ms
> hvigor Finished :entry:default@CompileArkTS... after 32 s 324 ms
> hvigor Finished :entry:default@PackageHap... after 733 ms
> hvigor BUILD SUCCESSFUL in 44 s 450 ms
[harmony] BUILD SUCCESSFUL
EXITCODE=0
```

**HARMONY_HAP_BUILD = PASS (SUCCESSFUL).** Artifact (still present after the
final conformance re-run):

```text
C:\Users\Kaiser\pdig-harmony-build\harmony\entry\build\default\outputs\default\entry-default-unsigned.hap
3,690,222 bytes | sha256 CD689C773790C539E279DDFA642DB803C944A62424A7ADC42E7E613F4BAB5FEC
```

The NAPI Argon2id lib was cross-compiled and packaged for both ABIs:
`libpdiargon2.so` in `.../intermediates/libs/default/arm64-v8a/` and `x86_64/`
(the `CompileArkTS` step emitted only the known non-blocking NAPI verification
warning for `Argon2idNative.ets:24:26`). Emulator execution remains impossible
on this host: DevEco emulator binaries exist but **no emulator system image is
installed** (no `system-images` dir under `D:\Code\Harmony`; stored audit
records `hdc list targets = [Empty]`, E-9 in `HARMONY_RUNTIME_ENVIRONMENT_AUDIT.md`
lines 52/61).

## 4. Two-gate audit summary (H3/H4 closed)

Full per-gate closure template: `artifacts/runtime-evidence/2026-09-27-closure-harmony/BLOCKERS_AUDIT.md`.
Fixture IDs cross-checked against `ConformanceRunner.ets` (`RUNTIME_BLOCKED_CASES`,
lines 273–291) and `harmony/entry/src/test/fixtures/FixtureBundle.ets`.

| ID  | Fixture                                  | Required runtime API                                                              | Classification                                                                                                                                                                                                       |
| --- | ---------------------------------------- | --------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| H1  | `depmap-golden-v1`                       | Argon2id NAPI (`libpdiargon2.so`) + AES-256-GCM + JCS                             | **REAL_RUNTIME_ONLY** (capability compiled & packaged; host can't load OHOS-ABI `.so`; mock would be a second implementation)                                                                                        |
| H2  | `depmap-utf8-password-normalization`     | Argon2id NAPI                                                                     | **REAL_RUNTIME_ONLY** (same reasoning)                                                                                                                                                                               |
| H3  | `migration-db-v1-to-v3`                  | none for the host-asserted semantics (ArkData physical driver tracked separately) | **CLOSED — host-executed PASS** (`runMigrationV1ToV3Host`, `ConformanceRunner.ets` lines 1088–1191; migration chain via `data/MigrationChain.ets` on in-memory `GraphStore`)                                         |
| H4  | `backup-depmap-export-restore-roundtrip` | none for the host-asserted payload round-trip (container half covered by H1/H2)   | **CLOSED — host-executed PASS** (`runBackupHost`, lines 1236–1302; 11-table payload export→restore→re-export byte-identical, T2 same caliber; expected projected to `{counts, imported, roundtripEqual, integrity}`) |

## 5. Verdicts

| Verdict                  | Value                                                                                                                                                                                                            |
| ------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| HARMONY_CONFORMANCE_HOST | **PASS** — 181/181 (run=181, pass=181, fail=0, error=0); canonical 126 executed + 0 impl-missing + 0 env-blocked + 2 RUNTIME_BLOCKED = 128                                                                       |
| HARMONY_HAP_BUILD        | **PASS** — clean `assembleHap` BUILD SUCCESSFUL (44 s 450 ms); HAP sha256 CD689C77…, NAPI argon2 packaged for arm64-v8a + x86_64                                                                                 |
| HARMONY_ENGINEERING_GAP  | **0** — H3/H4 closed as host-executed PASS (runner branches + in-memory `data/` layer wiring; see BLOCKERS_AUDIT.md §Gate 3/4)                                                                                   |
| HARMONY_RUNTIME          | **EXTERNAL_GATE (2/4)** — H1/H2 are device-runtime-gated only (E-9: no emulator system image / no device; plus a small runner execution-branch wiring item each); H3/H4 are closed on the host execution surface |

Exact evidence paths: fresh result
`C:\Users\Kaiser\pdig-harmony-build\harmony\entry\.test\default\intermediates\test\coverage_data\test_result.txt`
(final line `Tests run: 181, Failure: 0, Error: 0, Pass: 181, Ignore: 0`);
`conformance/reports/harmony.json` (`pass=126, runtimeBlocked=2, total=128`),
regenerated in-repo by the gate run; HAP as above. No commits made.
