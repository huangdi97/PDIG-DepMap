# Android UI vNext — Five Primary Destinations Decision

> Date: 2026-10-09  
> Status: **PRODUCT DECISION / FROZEN FOR R19**  
> Scope: Android UI vNext information architecture

## Decision

The Android primary navigation contains **five** destinations:

```text
现在
基础设施
变更
记录
我
```

`我` is intentionally a first-class destination. It must not be demoted to an
avatar-only shortcut, settings utility, overflow item, or secondary rail entry.

This decision supersedes the earlier four-destination interpretation in Android
translation notes.

## Why five is valid on Android

Android's current Material 3 guidance defines a NavigationBar for compact windows
with **three to five destinations of equal importance**:

- https://developer.android.com/develop/ui/compose/components/navigation-bar
- https://developer.android.com/develop/ui/compose/designsystems/material3

Therefore five primary destinations are within the native Android navigation
component's intended range. The product decision, not an arbitrary cleanup rule,
determines whether `我` deserves primary status.

## Product role of “我”

`我` is not just “Settings”.

It is the personal control surface for:

```text
我的数字生活
privacy / masking
personal presentation preferences
source visibility
workspace preferences
help / onboarding re-entry
future personal continuity profile
```

The destination answers a different top-level question than the other four:

```text
现在        → What needs my attention now?
基础设施    → What do I own / depend on?
变更        → What am I changing?
记录        → What happened / what is recorded?
我          → How is my personal digital-life workspace configured?
```

That separation is sufficient to justify first-class status.

## Phone translation

Compact Android:

```text
NavigationBar
├─ 现在
├─ 基础设施
├─ 变更
├─ 记录
└─ 我
```

Requirements:

- all five remain directly visible;
- no overflow;
- each destination owns an equal-width hit region;
- tap target remains at least 48dp;
- labels remain visible in Chinese;
- selected state uses icon + label + visual indicator;
- `我` remains reachable even if the top-right avatar shortcut is removed.

The avatar may remain as a convenience shortcut, but it is not the canonical
navigation affordance.

## Medium / Expanded translation

Wide layouts retain the same five primary destinations in the main rail.

Low-frequency child utilities may stay below the primary group:

```text
Primary
  现在
  基础设施
  变更
  记录
  我

Utility / child shortcuts
  数据源
  设置
```

When Settings / Personalization / Sources is open:

- the `我` primary parent may remain selected;
- the concrete child utility may also be highlighted;
- this is parent/child context, not two competing primary selections.

## Root / Back / Up semantics

All five primary destinations are roots:

```text
upDestination(现在) = null
upDestination(基础设施 root) = null
upDestination(变更 root) = null
upDestination(记录) = null
upDestination(我) = null
```

System Back remains chronological and may return to the previously visited primary
destination.

Examples:

```text
Cards → 我 → system Back = Cards
我 → 设置 → Up = 我
我 → 数据源 → Up = 我
```

Do not add a hierarchical Up arrow to the `我` root merely because it was entered
from another tab.

## Acceptance contract

Source/unit:

```text
PRIMARY_ENTRIES.size == 5
PRIMARY_ENTRIES == [NOW, INFRASTRUCTURE, CHANGE, RECORDS, ME]
ME !in SECONDARY_ENTRIES
upDestination(ME) == null
```

Runtime phone:

```text
all five bottom labels visible
tap 我 → Me workspace
tap 现在 → Now
cold launch → 我 still reachable from bottom nav
```

Runtime tablet/expanded:

```text
all five primary rail entries visible
Me selected on Me
Me remains parent context for Settings / Sources
```

## Forbidden regressions

- “restore four-item navigation” cleanup;
- avatar-only Me;
- Me moved into Settings;
- Me moved to overflow;
- phone shows four primary destinations while tablet shows five;
- test scripts tap only the avatar and therefore fail to prove the fifth tab exists.

## Evidence status

This document freezes source/product intent only.

```text
FIVE_PRIMARY_IA_DESIGN = FROZEN
FIVE_PRIMARY_IA_SOURCE = IMPLEMENTED
FIVE_PRIMARY_IA_RUNTIME = PENDING_FRESH_R19_EVIDENCE
```
