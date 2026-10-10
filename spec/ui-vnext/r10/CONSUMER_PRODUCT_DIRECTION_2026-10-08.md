# PDIG Android R10 — Consumer Experience Direction (2026-10-08)

> Supersedes the **R9 phone presentation behavior** where it conflicts.
> User screenshots and direct human feedback outrank old `ANDROID_REFERENCE_FREEZE = PASS`.
> This is NOT a Canonical/PersonalReality schema change.

## Five immutable product decisions

1. **Five first-level tabs:** `现在 / 基础设施 / 变更 / 记录 / 我`. Personal tools, privacy and help live under `我`. No engineering-dashboard clutter in the phone top command bar.
2. **Back = previous screen, never unconditional Home.** All forward navigation (root tab or nested page) pushes its actual previous screen onto history. System back and top back invoke the same pop. Only the original launch root with empty history delegates to system exit.
3. **Privacy default = visible.** On a clean install, WorkspacePreferences.privacyMask must be false. User controls it in `我`. Existing explicitly persisted user preference stays intact. Masking does not imply data completeness; fixture records may themselves contain redacted values.
4. **Card personalization = image selection, not engineering controls.** Choose built-in picture / choose photo from Android gallery / preview / Save & return. Material, layout, hex-color and preset IDs are not displayed in the consumer Card Studio. Number identity remains distinct and may retain legacy personalization until translated.
5. **Actual visual reconstruction, not component patching.** The phone preview uses the independent R9/R10 consumer composition. Decorative changes cannot claim completion without real APK pixels and human comparison to the original eight visible reference panels.

## R10 implemented paths

- `ui/VAppState.kt`: tab + nested previous-screen stack.
- `ui/NavigationModel.kt`, `ui/r9/R9Chrome.kt`: five primary destinations.
- `ui/r9/R10TopBar.kt`: consumer toolbar for Preview phone.
- `ui/r9/R10MeScreen.kt`: fifth-tab personal workspace.
- `ui/WorkspacePreferences.kt`, `ui/WorkspacePreferenceStore.kt`: fresh-install visibility.
- `ui/r9/R10CardImageStudio.kt`: simple photo/preset selection.
- `ui/r9/R10CardArtwork.kt`: private image import + same card renderer shared by Studio/List/Detail.

## Card image data boundaries

- Images are picked through Android system's content provider, max 12 MiB input,
  decoded to max 2048 px, converted to JPEG, and kept inside app-private files.
- PresentationProfile stores only an app-private safe image **filename**.
  User images MUST NOT go to `.depmap`, Canonical, PersonalReality or the repo.
- No URL fetch, fabricated institution logo, or implicit import of a bank statement.
- The same selected picture must render in live editor, card listing and card detail.
- Saved images remain local to the **particular application ID**, so SHA-isolated
  preview versions cannot transfer photos automatically.
- The user chooses whether to mask display-sensitive fields; app default false.

## Acceptance matrices

| User journey | Required behavior | Verification |
|---|---|---|
| Clean launch | Five primary tabs; no default mask | UI screenshot + preference state |
| Now → Infrastructure → Cards → Detail | Back returns Cards → Infrastructure → Now | Instrumented regression |
| Detail → Card image | Gallery/preset, preview picture immediately | Emulator UI; Android local image import |
| Card image → Save | Return to original detail, same photo on card | UI proof and store re-read |
| Card image saved → reopen listing | Thumbnail uses saved photo | UI proof |
| Me → toggle Privacy | Default off; on/off reversible and persistent | Preference disk test |
| Me → settings/data sources/help | All reachable from fifth-tab workspace | UI proof |
| Production builds | R9/R10 preview UI must not alter domain data or canonical schema | Existing core/conformance |

## Release / Review governance

```ini
R10_FIVE_TABS = SOURCE_IMPLEMENTED
R10_PREVIOUS_SCREEN_BACK = SOURCE_IMPLEMENTED
R10_PRIVACY_DEFAULT_VISIBLE = SOURCE_IMPLEMENTED
R10_CARD_IMAGE_STUDIO = SOURCE_IMPLEMENTED
R10_REAL_APK_BUILD = PENDING_EXACT_HEAD
R10_RUNTIME_PIXEL_PROOF = PENDING_EXACT_HEAD
R10_HUMAN_VISUAL_ACCEPTANCE = HOLD
```

Never claim `DONE` based on Kotlin source code or GitHub release alone.
Reference images are an **art direction**, not permission to hardcode synthetic brand
identities or pretend that source fixtures are PersonalReality.
