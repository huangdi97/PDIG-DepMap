# Presentation Customization — Card Image + Number Appearance

> R19 Android override · 2026-10-09  
> Presentation Layer only; never mutates domain truth.

## 1. The two products are intentionally different

Android no longer treats Card and Number customization as one generic engineering Studio.

### Card Image
A minor consumer feature:

```text
picture → preview → save
```

See `screens/card-customization.md`.

### Number Appearance
A communication-identity appearance surface:

```text
NumberFace preview
→ visual theme
→ presentation options
→ save
```

Number theme never changes semantic role. Choosing “恢复” visual theme does not
make a number a recovery path.

## 2. Shared invariant

Both persist only PresentationProfile:

- local preference only;
- never .depmap;
- never Canonical / PersonalReality;
- never changes node identity;
- never creates/changes Dependency;
- never changes evidence/confirmation;
- privacy masking defaults **off** and remains user-controlled.

## 3. Card geometry

Android Card Image is bounded at every width:

- Compact: available content width;
- Medium: centered, max about 680dp;
- Expanded: centered, max about 760dp.

There is no Android wide three-column Card inspector.

## 4. Number geometry

The richer preview/library/inspector geometry remains valid for Number Appearance
where the identity surface needs it. Mobile remains preview-first; wider layouts
may use multiple columns where readable.

The historical `LAYOUT_CONTRACT.json customization.desktopColumns` therefore
applies to the richer Number/legacy Desktop customization composition, **not** to
Android Card Image.

## 5. Images

Card local image:

- Android content picker;
- <=12 MiB input;
- <=2048px decoded longest dimension target;
- app-private JPEG;
- safe generated filename only in profile;
- no remote URL.

Built-in card art uses stable local ids:

```text
original / ocean / sky / coral / night
```

## 6. Accessibility

- Android touch targets >=48dp;
- preview does not consume/cover Save;
- image picker has a visible text label;
- theme state is never conveyed by color alone;
- system Back / Header Up preserve hierarchy.

## 7. Forbidden regressions

- Card Image regains material/layout/hex controls;
- card image becomes a full-screen engineering dashboard;
- local image URI/path escapes into Canonical or .depmap;
- default masking silently returns to true;
- Number visual theme mutates recovery/login semantics.
