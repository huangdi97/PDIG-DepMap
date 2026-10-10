# Region Facts v1 — Canonical Proposal

> Date: 2026-10-10  
> Status: **R39 CANONICAL READ CONTRACT IN IMPLEMENTATION / DESIGN COMPLETE**
>
> Goal: give the PDIG Region Lens and Globe a governed production source without
> collapsing several different meanings of “region” into one inferred `regionCode`.
>
> This proposal does **not** change the current `.depmap` version.

---

## 1. Product question

The Region Lens answers:

> **“这些已确认的数字基础设施，在哪个现实地区语境中成立？”**

It does **not** answer:
- where the user is physically located right now;
- where a server physically sits;
- where an IP geolocation service guesses the object is;
- which political/legal interpretation should be inferred from a display label.

The Globe is a projection of governed Region facts. It is not the source of those
facts.

---

## 2. Why one `region` string is insufficient

Different object types can have different valid geographic facets.

Examples:

```text
payment card
  issuance jurisdiction
  provider service market

phone number
  numbering-plan territory
  carrier service market

bank / online account
  provider jurisdiction
  service market

device
  current physical location       (volatile, highly sensitive)
  purchase market                 (different fact)

service
  provider jurisdiction
  service availability market
```

Therefore:

```text
Node.region = "GB"
```

is too ambiguous for Canonical truth.

Permanent rule:

```text
region code without facet
!= governed geographic fact
```

---

## 3. Proposed first-class fact

Conceptual model:

```text
RegionFact
  id
  nodeId
  facet
  territoryCode
  subdivisionCode?
  state
  source
  evidenceRefs[]
  confirmedAt?
  validFrom?
  validUntil?
  createdAt
  updatedAt
```

R39 Reality storage uses:

```text
confirmed
retired
```

A `proposal` is deliberately **not** a RegionFact inside confirmed Node Reality.
Unconfirmed geographic claims remain Proposal/Review state outside the Node until
authority confirms them. Only `confirmed` participates in normal Production
Region Lens aggregation.

---

## 4. Region facet vocabulary

v1 recommended facets:

```text
ISSUANCE_JURISDICTION
NUMBERING_TERRITORY
PROVIDER_JURISDICTION
SERVICE_MARKET
PHYSICAL_LOCATION
USER_CONFIRMED_CONTEXT
```

### ISSUANCE_JURISDICTION

Typical use:
- payment instrument;
- regulated financial account;
- membership issued under a jurisdiction.

It does not mean the user currently lives there.

### NUMBERING_TERRITORY

Typical use:
- confirmed phone number / telecom identity.

It expresses the numbering-plan territory, not current radio roaming location.

### PROVIDER_JURISDICTION

Typical use:
- account/service/provider relationship when the user explicitly records or an
  authoritative source confirms the governing provider jurisdiction.

### SERVICE_MARKET

Typical use:
- an account/service/card/plan is specifically the UK/HK/US market variant.

Provider documentation may propose this fact. It cannot silently confirm it.

### PHYSICAL_LOCATION

Use only for a genuinely physical object/location fact.

This facet is:
- optional;
- volatile;
- privacy-sensitive;
- never inferred from app locale/IP unless explicitly confirmed through a governed
  source.

Do not use it merely to place an abstract account on the Globe.

### USER_CONFIRMED_CONTEXT

Escape hatch for user-confirmed real-world regional association that is useful to
the user but does not fit a provider/legal facet.

It is still a Reality fact and needs explicit authority.

It is **not** a replacement for Identity Context.

---

## 5. Code system

For territory-level v1 facts, use ISO 3166-1 alpha-2 codes where applicable.

Examples:

```text
CN
HK
MO
GB
US
SG
```

For subdivisions, use ISO 3166-2 only when the product genuinely needs that
granularity.

Examples:

```text
CN-SX
US-CA
```

Do not invent ISO-looking custom codes.

Custom life groupings such as:

```text
Europe trip
Greater Bay Area life
UK Financial
```

belong to Identity Context / user grouping, not ISO territory storage.

### Display names

Canonical stores codes, not localized display labels.

Localized consumer names come from a locale data layer such as Unicode CLDR.

Therefore:

```text
Canonical: HK
UI zh: 香港
UI en: Hong Kong
```

Display-name wording changes do not mutate Reality.

---

## 6. Coordinates are Presentation, not Reality

A territory fact does not need a user-object latitude/longitude.

The Globe may use a deterministic presentation catalog:

```text
territoryCode
→ label anchor / visual centroid
```

That catalog belongs to the UI/spatial layer.

Permanent rule:

```text
confirmed RegionFact(code=GB)
+ presentation centroid(GB)
→ projected UK label

centroid
!= actual object physical location
```

If a true physical location is ever stored, it must use the explicit
`PHYSICAL_LOCATION` facet and separate privacy policy.

---

## 7. Authority pipeline

Region follows the Reality Boundary:

```text
Observation
→ Region Proposal
→ Human Review
→ Confirm
→ RegionFact
```

Allowed examples:

### Manual

User records:

> “This card is my Hong Kong-issued card.”

→ confirmed `ISSUANCE_JURISDICTION = HK`.

### Provider/source proposal

An imported official statement indicates a UK product.

→ proposal `SERVICE_MARKET = GB`.

User/review authority confirms before it enters Reality unless the source class is
explicitly governed as authoritative.

### AI extraction

AI may say:

> “This appears to be a Singapore service-market account.”

AI may create a Proposal with evidence.

AI may not write a confirmed RegionFact directly.

---

## 8. Forbidden inference

Do not confirm Region from:

- currency alone;
- phone prefix alone unless the phone identifier itself is already governed and
  the numbering-territory derivation rule is explicitly approved;
- provider brand name;
- issuer display name;
- app/device locale;
- current timezone;
- IP geolocation;
- VPN endpoint;
- UI theme/preset;
- card artwork;
- search query;
- user’s current physical location;
- object name containing “香港 / UK / US”.

These may at most support a Proposal where policy allows.

---

## 9. Multi-region objects

One object may have multiple confirmed RegionFacts with different facets.

Example:

```text
Card A
  ISSUANCE_JURISDICTION = HK
  SERVICE_MARKET        = HK

Service B
  PROVIDER_JURISDICTION = US
  SERVICE_MARKET        = GB
```

Do not force one global `primaryRegion` into Canonical.

The UI may apply a deterministic **Region Lens projection policy**.

Recommended default consumer precedence:

```text
NUMBERING_TERRITORY
ISSUANCE_JURISDICTION
SERVICE_MARKET
PROVIDER_JURISDICTION
USER_CONFIRMED_CONTEXT
```

But precedence is a presentation/query rule, not a mutation of underlying facts.

If two confirmed facts of the same priority disagree:

```text
Region Lens → needs review / multi-region
```

Never pick one silently.

---

## 10. Region Lens production projection

Conceptual output:

```text
ProductionRegionPresentation
  territoryCode
  objectCount
  cardCount
  phoneCount
  accountCount
  serviceCount
  attentionCount
  truthCoverage
  memberObjectIds[]
```

Aggregation includes only:
- confirmed objects;
- confirmed RegionFacts;
- type/subtype that the production read model can safely identify.

Until phone/email subtype exists:

```text
generic identity_anchor
→ may count as generic identity
→ must not count as phone/email
```

---

## 11. Globe rules

When production has no confirmed RegionFacts:

```text
GPU Earth visible
region labels none
asset pins none
arcs none
copy = 地区定位尚未进入正式数据模型 / 尚未记录
```

When facts exist:

```text
confirmed RegionFacts
→ deterministic aggregation
→ RegionPresentation
→ camera projection
→ labels / list
```

No label may come from Preview fixture data in Production mode.

### Arcs

A geographic arc may visualize:
- two confirmed region aggregates connected by confirmed dependencies;
- or a selected object-to-region context if its semantics are explicit.

It must not imply:
- network traffic;
- financial transfer;
- physical travel;
- dependency if no Dependency exists.

---

## 12. Region list / accessibility

The visual Globe and non-visual Region List use the same projection.

Region list row:

```text
香港
3 张卡 · 2 个号码 · 1 项需要关注
```

Only counts backed by the production projection appear.

If a type cannot be safely classified:

```text
4 项基础设施
```

is preferable to inventing a subtype count.

---

## 13. Filter semantics

Selecting Region applies a **query scope**.

```text
Region=HK
→ show objects whose confirmed selected RegionFact projection includes HK
```

It does not:
- edit object RegionFacts;
- create a Dependency;
- create an Identity Context;
- change the active provider jurisdiction;
- move an object.

Clearing Region returns to Global scope.

---

## 14. Region × Identity Context

Region and Identity Context remain independent.

Allowed future intersection:

```text
Region = GB
AND
IdentityContext = 英国金融
```

This is a query intersection.

Do not persist:

```text
英国金融@GB
```

as a new Node/Dependency.

---

## 15. Region × Change

Change planning may use RegionFacts as contextual input only when relevant.

Examples:

- provider market determines which policy template may apply;
- replacement phone numbering territory helps choose valid user input format;
- jurisdiction may affect provider instructions.

Permanent rule:

```text
RegionFact
!= ProviderPolicy
!= user configuration
!= legal advice
```

A RegionFact may help select a ProviderPolicy candidate. It does not itself encode
provider rules.

---

## 16. Region × Privacy

Region can reveal sensitive life structure.

Requirements:
- local-first encrypted storage;
- no analytics of region membership;
- export only after explicit schema decision;
- physical-location facet requires stricter UX and should be hidden by default;
- no current-location permission is required for normal Region Lens;
- a user may use PDIG Region without sharing device GPS.

The Region Lens should prefer territory-level context over precise location.

---

## 17. Temporal semantics

A RegionFact can change over time.

Examples:
- a card is reissued in a different market;
- an account migrates provider jurisdiction;
- a number is ported while numbering territory stays the same.

Use:

```text
validFrom
validUntil
state=retired
```

Do not overwrite historical Region truth if Records/Change needs to explain a past
event.

---

## 18. Migration

Existing payloads have no governed RegionFact.

Migration:

```text
old payload
→ zero confirmed RegionFacts
```

Forbidden:
- scan names for country words;
- infer from currency;
- copy Android Preview `region` fixture fields into Reality;
- derive from user locale.

Production Globe therefore remains truth-empty until the user/import/review path
creates confirmed RegionFacts.

---

## 19. Creation / review UI

After Canonical support:

### Object Detail

```text
地区信息
  发行地区：香港 · 已确认
  服务市场：香港 · 已确认
  服务商辖区：未记录

编辑 / 核对
```

### Review

Proposal card:

```text
建议地区
对象：HSBC Premier
服务市场：GB
依据：官方导入记录 …
[确认] [拒绝] [稍后]
```

### Manual object creation

Region is optional unless a specific object type contract requires it.

Blank remains:

```text
未记录
```

not inferred from locale.

---

## 20. Cross-platform conformance

Required fixtures:

```text
RG-01 confirmed CN territory fact round-trips
RG-02 confirmed HK territory fact round-trips
RG-03 ISO 3166-2 subdivision round-trips
RG-04 proposal excluded from normal Region Lens
RG-05 retired fact excluded from current lens but remains historical
RG-06 two different facets on one object remain separate
RG-07 conflicting same-priority confirmed facts produce needs-review/multi-region
RG-08 currency does not infer region
RG-09 provider name does not infer region
RG-10 phone-looking identity without subtype does not count as phone
RG-11 old payload migrates to zero confirmed RegionFacts
RG-12 localized display name changes do not mutate code
RG-13 Region filter changes query only
RG-14 Region × Identity intersection creates no new graph object
RG-15 physical-location facet is never derived from IP/locale
RG-16 unknown future facet fails closed
```

---

## 21. External standards / research alignment

Design input only; external standards do not create PDIG Reality.

- ISO 3166-1 defines maintained country/territory codes for coded country names:
  https://www.iso.org/standard/72482.html
- ISO 3166-2 defines maintained subdivision codes:
  https://www.iso.org/standard/72483.html
- ISO’s country-code overview explains that ISO 3166 is maintained by the ISO 3166
  Maintenance Agency:
  https://www.iso.org/iso-3166-country-codes.html
- Unicode CLDR provides locale-appropriate territory display names and includes
  countries, special-status territories and macroregions for UI localization:
  https://cldr.unicode.org/translation/displaynames/countryregion-territory-names

PDIG uses standardized identifiers/localized names as representation infrastructure.
They do not answer which RegionFact applies to a user object.

---

## 22. Implementation order

```text
approve RegionFact vocabulary
→ choose additive structured-field storage in the existing Node.fields envelope
→ semantic migration = old payload has zero confirmed RegionFacts
→ codegen
→ fixtures + negative fixtures
→ conformance
→ repositories / review authority
→ production read projection
→ Region Lens / Region List
→ Globe labels
→ runtime + privacy evidence
```

No Android-only side store is allowed once implementation starts.

---

## 23. Stop line

```text
REGION_FACT_DESIGN = COMPLETE
REGION_LENS_PRODUCTION_QUERY_DESIGN = COMPLETE
GLOBE_PRODUCTION_TRUTH_BOUNDARY = COMPLETE

CANONICAL_REGION_FACT_DESIGN = COMPLETE
R39_REGION_FACT_CANONICAL_READ = IN_IMPLEMENTATION
PRODUCTION_REGION_MEMBERSHIP = HOLD_UNTIL_R39_READ_PROJECTION
PRODUCTION_GLOBE_LABELS = HOLD_UNTIL_CONFIRMED_REGION_FACTS

GPU_EARTH_VISUAL_CONTEXT = ALLOWED_WITH_ZERO_FACT_LABELS
SYNTHETIC_REGION_LEAK_IN_PRODUCTION = FORBIDDEN
```
