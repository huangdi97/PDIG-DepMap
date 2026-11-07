/**
 * PDIG UI vNext — HarmonyOS (ArkTS/ArkUI) 表现层模型（Presentation Layer）。
 *
 * 铁律（spec/ui-vnext/DESIGN_TOKENS.json policy + PRESENTATION_PROFILE_SCHEMA.json $comment）：
 *  - 这些模型只驱动表现；绝不改变 node identity / dependencies / evidence / confirmation；
 *  - 不写入 .depmap frozen payload；存储只走本地 preferences；
 *  - region 是表现层派生属性（来自 synthetic fixture），不改变 canonical schema。
 * 数据契约真源：spec/ui-vnext/UIVNextDemoFixture.json（移植见 UIVNextDemoFixture.ets）。
 */

/** 屏幕路由（IA.md §4 Screen Registry 的 UI 内表示）。 */
export class VScreenRoute {
  static readonly NOW: string = 'now';
  static readonly INFRASTRUCTURE: string = 'infrastructure';
  static readonly CHANGE: string = 'change';
  static readonly RECORDS: string = 'records';
  static readonly OVERVIEW: string = 'overview';
  static readonly CARDS: string = 'cards';
  static readonly NUMBERS: string = 'numbers';
  static readonly CARD_DETAIL: string = 'card-detail';
  static readonly NUMBER_DETAIL: string = 'number-detail';
  static readonly CARD_CUSTOMIZATION: string = 'card-customization';
  static readonly NUMBER_CUSTOMIZATION: string = 'number-customization';
  static readonly PERSONALIZATION: string = 'personalization';
  static readonly CHANGE_PHONE: string = 'change-phone';
}

/** Globe 状态机（INTERACTION_CONTRACT.md §1：GLOBAL→REGION_HOVER→REGION_SELECTED→REGION_DETAIL）。 */
export class VGlobeState {
  static readonly GLOBAL: string = 'global';
  static readonly REGION_HOVER: string = 'region-hover';
  static readonly REGION_SELECTED: string = 'region-selected';
  static readonly REGION_DETAIL: string = 'region-detail';
}

/** 一级导航条目（IA.md §1）。 */
export class VNavEntry {
  route: string;
  label: string;
  testId: string;
  constructor(route: string, label: string, testId: string) {
    this.route = route;
    this.label = label;
    this.testId = testId;
  }
}

/** 地区呈现模型（IA.md §5；值只由真实数据计算；无数据 = 不显示 / 0）。 */
export class RegionPresentation {
  regionCode: string;
  displayName: string;
  latitude: number;
  longitude: number;
  cardCount: number;
  phoneCount: number;
  accountCount: number;
  serviceCount: number;
  attentionCount: number;
  constructor(regionCode: string, displayName: string, latitude: number, longitude: number) {
    this.regionCode = regionCode;
    this.displayName = displayName;
    this.latitude = latitude;
    this.longitude = longitude;
    this.cardCount = 0;
    this.phoneCount = 0;
    this.accountCount = 0;
    this.serviceCount = 0;
    this.attentionCount = 0;
  }
}

/** 卡片（UI 展示模型；缺失 = 未设置/未知，禁止推断）。 */
export class UiVNextCard {
  id: string;
  nickname: string;
  issuer: string;
  last4: string;
  masked: string;
  region: string;
  currency: string;
  type: string;      // debit | credit
  form: string;      // physical | virtual
  network: string;
  expiry: string;
  status: string;
  attention: boolean;
  usages: string[];
  preset: string;    // themeId（呈现）
  constructor(id: string, nickname: string, issuer: string, last4: string, masked: string, region: string,
    currency: string, type: string, form: string, network: string, expiry: string, status: string,
    attention: boolean, usages: string[], preset: string) {
    this.id = id;
    this.nickname = nickname;
    this.issuer = issuer;
    this.last4 = last4;
    this.masked = masked;
    this.region = region;
    this.currency = currency;
    this.type = type;
    this.form = form;
    this.network = network;
    this.expiry = expiry;
    this.status = status;
    this.attention = attention;
    this.usages = usages;
    this.preset = preset;
  }
}

/** 号码（UI 展示模型）。 */
export class UiVNextNumber {
  id: string;
  nickname: string;
  maskedNumber: string;
  region: string;
  countryCode: string;
  carrier: string;
  simKind: string;   // SIM | eSIM
  role: string;      // primary | secondary | keep
  usages: string[];
  status: string;
  recoveryOnly: boolean;
  attention: boolean;
  constructor(id: string, nickname: string, maskedNumber: string, region: string, countryCode: string,
    carrier: string, simKind: string, role: string, usages: string[], status: string,
    recoveryOnly: boolean, attention: boolean) {
    this.id = id;
    this.nickname = nickname;
    this.maskedNumber = maskedNumber;
    this.region = region;
    this.countryCode = countryCode;
    this.carrier = carrier;
    this.simKind = simKind;
    this.role = role;
    this.usages = usages;
    this.status = status;
    this.recoveryOnly = recoveryOnly;
    this.attention = attention;
  }
}

export class UiVNextService {
  id: string;
  name: string;
  region: string;
  kind: string;
  constructor(id: string, name: string, region: string, kind: string) {
    this.id = id;
    this.name = name;
    this.region = region;
    this.kind = kind;
  }
}
export class UiVNextRelation {
  from: string;
  to: string;
  kind: string;      // funding | authenticates | twoFA
  constructor(from: string, to: string, kind: string) {
    this.from = from;
    this.to = to;
    this.kind = kind;
  }
}
export class AttentionItem {
  id: string;
  severity: string;  // critical | warning
  title: string;
  target: string;
  constructor(id: string, severity: string, title: string, target: string) {
    this.id = id;
    this.severity = severity;
    this.title = title;
    this.target = target;
  }
}
export class ActiveChange {
  id: string;
  title: string;
  phase: string;
  target: string;
  constructor(id: string, title: string, phase: string, target: string) {
    this.id = id;
    this.title = title;
    this.phase = phase;
    this.target = target;
  }
}
export class UpcomingItem {
  id: string;
  title: string;
  days: number;
  constructor(id: string, title: string, days: number) {
    this.id = id;
    this.title = title;
    this.days = days;
  }
}
export class ChangeStage {
  stage: number;
  key: string;
  status: string;    // completed | current | verifying | not_started | blocked | upcoming
  blockReason: string;
  constructor(stage: number, key: string, status: string, blockReason: string) {
    this.stage = stage;
    this.key = key;
    this.status = status;
    this.blockReason = blockReason;
  }
}
export class ChangeMigration {
  service: string;
  status: string;    // migrated | waiting | not_started | blocked
  constructor(service: string, status: string) {
    this.service = service;
    this.status = status;
  }
}

/** 弧线（真实跨区关系；globe.arcCountPolicy：禁止装饰性连线）。 */
export class ArcPair {
  a: string;
  b: string;
  constructor(a: string, b: string) {
    this.a = a;
    this.b = b;
  }
}

/**
 * PresentationProfile（PRESENTATION_PROFILE_SCHEMA.json）—— 本地偏好，绝不进 .depmap。
 * 编辑对象 = 呈现层配置；preset visual ≠ semantic role。
 */
export class PresentationProfile {
  targetType: string;
  targetId: string;
  themeId: string;
  material: string;
  accentColor: string;
  backgroundKind: string;
  backgroundValue: string;
  layout: string;
  maskSensitive: boolean;
  constructor(targetType: string, targetId: string, themeId: string, material: string, accentColor: string,
    backgroundKind: string, backgroundValue: string, layout: string, maskSensitive: boolean) {
    this.targetType = targetType;
    this.targetId = targetId;
    this.themeId = themeId;
    this.material = material;
    this.accentColor = accentColor;
    this.backgroundKind = backgroundKind;
    this.backgroundValue = backgroundValue;
    this.layout = layout;
    this.maskSensitive = maskSensitive;
  }

  static defaultFor(targetType: string, targetId: string, preset: string): PresentationProfile {
    return new PresentationProfile(targetType, targetId, preset, 'glass', '#4D74FF',
      'preset', preset, 'standard', true);
  }
}

/** 主题 preset 列表（PRESENTATION_PROFILE_SCHEMA.json / screens/card-customization.md）。 */
export const CARD_THEME_PRESETS: string[] = ['minimal', 'deep-space', 'region', 'city', 'glass', 'metal', 'abstract'];
export const NUMBER_THEME_PRESETS: string[] = ['country', 'city', 'minimal', 'banking', 'travel', 'recovery', 'work', 'private'];
export const MATERIAL_PRESETS: string[] = ['glass', 'metal', 'matte', 'paper', 'none'];

/** 稳定 testId 常量（DESIGN_TOKENS.json testIds.rule：四端同一语义 id）。 */
export class VTestIds {
  static readonly NAV_RAIL: string = 'pdig.nav.rail';
  static readonly NAV_TOP: string = 'pdig.nav.top';
  static readonly GLOBE_STAGE: string = 'pdig.globe.stage';
  static readonly GLOBE_CANVAS: string = 'pdig.globe.canvas';
  static readonly OVERVIEW_ACTIVITY: string = 'pdig.overview.activity';
  static readonly OVERVIEW_QUICK: string = 'pdig.overview.quick';
  static readonly NOW_GLOBE: string = 'pdig.now.globe';
  static readonly NOW_ATTENTION: string = 'pdig.now.attention';
  static readonly NOW_CHANGES: string = 'pdig.now.changes';
  static readonly NOW_UPCOMING: string = 'pdig.now.upcoming';
  static readonly REGION_LIST: string = 'pdig.region.list';
  static readonly REGION_ITEM: string = 'pdig.region.item';
  static readonly CARD_GRID: string = 'pdig.card.grid';
  static readonly CARD_LIST: string = 'pdig.card.list';
  static readonly CARD_VIEW_TOGGLE: string = 'pdig.card.viewToggle';
  static readonly CARD_CARD: string = 'pdig.card.card';
  static readonly PHONE_LIST: string = 'pdig.phone.list';
  static readonly PHONE_INSPECTOR: string = 'pdig.phone.inspector';
  static readonly PHONE_ROW: string = 'pdig.phone.row';
  static readonly CARD_DETAIL_IDENTITY: string = 'pdig.card.detail.identity';
  static readonly CARD_DETAIL_INFO: string = 'pdig.card.detail.info';
  static readonly PHONE_DETAIL_IDENTITY: string = 'pdig.phone.detail.identity';
  static readonly CUSTOMIZATION_LIBRARY: string = 'pdig.customization.library';
  static readonly CUSTOMIZATION_PREVIEW: string = 'pdig.customization.preview';
  static readonly CUSTOMIZATION_INSPECTOR: string = 'pdig.customization.inspector';
  static readonly CHANGE_PHONE_RAIL: string = 'pdig.change.phone.rail';
  static readonly CHANGE_PHONE_GATE: string = 'pdig.change.phone.gate';
  static readonly CHANGE_PHONE_ITEM: string = 'pdig.change.phone.item';
  static readonly SETTINGS_PERSONALIZATION: string = 'pdig.settings.personalization';
}

/** 一级导航条目（IA.md §1；mobile bottom nav ≤5 项）。 */
export const PRIMARY_NAV_ITEMS: VNavEntry[] = [
  new VNavEntry(VScreenRoute.NOW, '现在', 'pdig.nav.now'),
  new VNavEntry(VScreenRoute.INFRASTRUCTURE, '基础设施', 'pdig.nav.infrastructure'),
  new VNavEntry(VScreenRoute.CHANGE, '变更', 'pdig.nav.change'),
  new VNavEntry(VScreenRoute.RECORDS, '记录', 'pdig.nav.records'),
  new VNavEntry(VScreenRoute.SETTINGS, '设置', 'pdig.nav.settings'),
];
