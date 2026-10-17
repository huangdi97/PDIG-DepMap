/**
 * PDIG UI vNext 主题 —— 从 GeneratedPdigV2Tokens 映射 Color / Spacing / Radius / Typography。
 * 唯一颜色来源：harmony/entry/src/main/ets/generated/GeneratedPdigV2Tokens.ets（已 codegen，禁手写色值）。
 * 页面禁止散用颜色/字号/间距字面量，一律引用本文件。
 */
import { GeneratedPdigV2Tokens as T } from '../generated/GeneratedPdigV2Tokens';

/** 解析 "#RRGGBB" 十六进制 → "#AARRGGBB"（ArkUI 颜色字符串形态）。 */
export function hexColor(value: string): string {
  const cleaned = value.replace('#', '');
  return '#FF' + cleaned;
}

/** 解析 "rgba(r, g, b, a)" → "#AARRGGBB"。 */
export function rgbaColor(value: string): string {
  const inner = value.replace('rgba(', '').replace(')', '').split(',').map((s: string): string => s.trim());
  const r = parseInt(inner[0], 10);
  const g = parseInt(inner[1], 10);
  const b = parseInt(inner[2], 10);
  const a = Math.round(parseFloat(inner[3]) * 255);
  const toHex = (n: number): string => n.toString(16).padStart(2, '0');
  return '#' + toHex(a) + toHex(r) + toHex(g) + toHex(b);
}

/** 给 hex 色附加 alpha → "#AARRGGBB"（alpha 应用不是新色值）。 */
export function withAlpha(hex: string, alpha: number): string {
  const cleaned = hex.replace('#', '');
  const rgb = cleaned.length === 8 ? cleaned.substring(2) : cleaned;
  const a = Math.round(alpha * 255);
  return '#' + a.toString(16).padStart(2, '0') + rgb;
}

/** vNext 环境/数据/状态色（全部来自 GeneratedPdigV2Tokens）。 */
export class VColors {
  static readonly Canvas: string = hexColor(T.COLORS_CANVAS);
  static readonly CanvasDeep: string = hexColor(T.COLORS_CANVAS_DEEP);
  static readonly Surface: string = hexColor(T.COLORS_SURFACE);
  static readonly SurfaceRaised: string = hexColor(T.COLORS_SURFACE_RAISED);
  static readonly SurfaceGlass: string = rgbaColor(T.COLORS_SURFACE_GLASS);
  static readonly BorderSubtle: string = rgbaColor(T.COLORS_BORDER_SUBTLE);
  static readonly BorderStrong: string = rgbaColor(T.COLORS_BORDER_STRONG);
  static readonly Primary: string = hexColor(T.COLORS_PRIMARY);
  static readonly PrimaryBright: string = hexColor(T.COLORS_PRIMARY_BRIGHT);
  static readonly PrimarySoft: string = hexColor(T.COLORS_PRIMARY_SOFT);
  static readonly TextPrimary: string = hexColor(T.COLORS_TEXT_PRIMARY);
  static readonly TextSecondary: string = hexColor(T.COLORS_TEXT_SECONDARY);
  static readonly TextMuted: string = hexColor(T.COLORS_TEXT_MUTED);
  static readonly Positive: string = hexColor(T.COLORS_POSITIVE);
  static readonly Warning: string = hexColor(T.COLORS_WARNING);
  static readonly Critical: string = hexColor(T.COLORS_CRITICAL);
  static readonly Unknown: string = hexColor(T.COLORS_UNKNOWN);
  static readonly RegionNodeHi: string = hexColor(T.COLORS_REGION_NODE_HI);
  static readonly RegionNodeLo: string = rgbaColor(T.COLORS_REGION_NODE_LO);
  static readonly ArcActive: string = rgbaColor(T.COLORS_ARC_ACTIVE);
  static readonly ArcQuiet: string = rgbaColor(T.COLORS_ARC_QUIET);
  static readonly AtmosphereInner: string = rgbaColor(T.COLORS_ATMOSPHERE_INNER);
  static readonly AtmosphereOuter: string = rgbaColor(T.COLORS_ATMOSPHERE_OUTER);
  // semantic.focus = colors.primaryBright（别名；无独立色值）
  static readonly FocusRing: string = VColors.PrimaryBright;
}

/** 语义状态色（icon+label+color 三通道；色仅为第三通道）。 */
export function statusColor(status: string): string {
  if (status === 'critical' || status === 'blocked' || status === 'expiring_soon') {
    return VColors.Critical;
  }
  if (status === 'warning' || status === 'verifying') {
    return VColors.Warning;
  }
  if (status === 'completed' || status === 'active' || status === 'ok' || status === 'verified' ||
    status === 'migrated') {
    return VColors.Positive;
  }
  return VColors.TextMuted;
}

/** 状态中文标签（文案语义与 copy-zh / 契约一致；vNext 演示固定文案）。 */
export function statusLabelZh(status: string): string {
  if (status === 'active') {
    return '使用中';
  }
  if (status === 'expiring_soon') {
    return '即将到期';
  }
  if (status === 'expired') {
    return '已过期';
  }
  if (status === 'blocked') {
    return '阻止';
  }
  if (status === 'critical') {
    return '必须处理';
  }
  if (status === 'warning') {
    return '需要确认';
  }
  if (status === 'verifying') {
    return '待验证';
  }
  if (status === 'completed') {
    return '已完成';
  }
  if (status === 'not_started') {
    return '未开始';
  }
  if (status === 'waiting') {
    return '等待中';
  }
  if (status === 'migrated') {
    return '已迁移';
  }
  if (status === 'plan') {
    return '计划';
  }
  return '未知';
}

/** 数值间距 token（vp）。 */
export class VSpacing {
  static readonly Xs: number = T.SPACING_XS;
  static readonly Sm: number = T.SPACING_SM;
  static readonly Md: number = T.SPACING_MD;
  static readonly Lg: number = T.SPACING_LG;
  static readonly Xl: number = T.SPACING_XL;
  static readonly Xxl: number = T.SPACING_XXL;
  static readonly Xxxl: number = T.SPACING_XXXL;
  static readonly PagePadding: number = T.SPACING_PAGE_PADDING;
  static readonly PagePaddingNarrow: number = T.SPACING_PAGE_PADDING_NARROW;
  static readonly SectionGap: number = T.SPACING_SECTION_GAP;
  static readonly GridGap: number = T.SPACING_GRID_GAP;
  static readonly GridGapWide: number = T.SPACING_GRID_GAP_WIDE;
}

/** 圆角 token（vp）。 */
export class VRadius {
  static readonly Sm: number = T.RADIUS_SM;
  static readonly Md: number = T.RADIUS_MD;
  static readonly Lg: number = T.RADIUS_LG;
  static readonly Xl: number = T.RADIUS_XL;
}

/** 字号 token。 */
export class VType {
  static readonly PageTitle: number = T.TYPOGRAPHY_PAGE_TITLE_SIZE;
  static readonly SectionTitle: number = T.TYPOGRAPHY_SECTION_TITLE_SIZE;
  static readonly Body: number = T.TYPOGRAPHY_BODY_SIZE;
  static readonly Secondary: number = T.TYPOGRAPHY_SECONDARY_SIZE;
  static readonly Meta: number = T.TYPOGRAPHY_META_SIZE;
  static readonly Label: number = T.TYPOGRAPHY_LABEL_SIZE;
  static readonly DisplayGlobe: number = T.TYPOGRAPHY_DISPLAY_GLOBE_SIZE;
  static readonly Mono: number = T.TYPOGRAPHY_MONO_SIZE;
}

/** 动效 token。 */
export class VMotion {
  static readonly Fast: number = T.MOTION_FAST;
  static readonly Normal: number = T.MOTION_NORMAL;
  static readonly GlobeFocus: number = T.MOTION_GLOBE_FOCUS;
  static readonly IdleRotationDegPerSec: number = T.GLOBE_IDLE_ROTATION_DEG_PER_SEC;
  static readonly MaxZoom: number = T.GLOBE_MAX_ZOOM;
  static readonly MinZoom: number = T.GLOBE_MIN_ZOOM;
}

/** Globe 几何 token。 */
export class VGlobeTokens {
  static readonly PreferredDiameterMin: number = T.GLOBE_PREFERRED_SPHERE_DIAMETER_MIN;
  static readonly AnchorDiameter: number = T.GLOBE_REGION_ANCHOR_DIAMETER;
  static readonly AnchorActiveDiameter: number = T.GLOBE_REGION_ANCHOR_ACTIVE_DIAMETER;
  static readonly TouchTarget: number = T.COMPONENTS_TOUCH_TARGET_ANDROID;
}
