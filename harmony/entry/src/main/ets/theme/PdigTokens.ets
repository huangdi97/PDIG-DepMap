/**
 * PDIG Harmony 设计 token —— Quiet Infrastructure 冻结表现层常量。
 *
 * 值来源（冻结，勿自行漂移）：
 *   - docs/uiux/PDIG_UIUX_DIRECTION_FREEZE.md §5 Palette / §6 Typography / §7 Spacing
 *   - docs/uiux/PDIG_DESIGN_SYSTEM.md §1 Color / §2 Typography / §3 Spacing / §4 Radius
 *
 * 约束：
 *   - 页面禁止散用颜色/字号/间距字面量，一律引用本文件。
 *   - 当前仅实现 light 色板；dark 色板需在设备上对比度验证后补充。
 *   - 本文件只承载表现层常量，不含任何业务逻辑。
 */

// ── Color（light；品牌 indigo 冻结不动）──────────────────────────────
export const ColorPrimary: string = '#4C4FD8';      // indigo：主 CTA / 选中 / 链接
export const ColorPrimaryActive: string = '#3B3EB8'; // 按压态
export const ColorPrimarySoft: string = '#EEEEFB';   // 图标底 / 轻 accent
export const ColorBackground: string = '#F5F6FA';    // 页面底色（微冷灰，neutral-dominant）
export const ColorSurface: string = '#FFFFFF';       // 面板 / 内容容器
export const ColorSurfaceElevated: string = '#FFFFFF';
export const ColorSurfaceLow: string = '#EDEFF6';
export const ColorTextPrimary: string = '#1B1D29';
export const ColorTextSecondary: string = '#5A5F73';
export const ColorTextTertiary: string = '#8A90A6';  // 仅元信息 / 占位
export const ColorBorder: string = '#E6E8F0';        // hairline 分隔
export const ColorSuccess: string = '#2BA471';
export const ColorWarning: string = '#D98E04';
export const ColorDanger: string = '#D54941';
export const ColorInfo: string = '#3B6FD8';
export const ColorDisabled: string = '#B9BDCC';
export const ColorFocusRing: string = '#4C4FD8';

// ── Spacing（4pt grid）───────────────────────────────────────────────
export const SpaceXs: number = 4;
export const SpaceSm: number = 8;
export const SpaceMd: number = 12;
export const SpaceLg: number = 16;
export const SpaceXl: number = 20;
export const SpaceXxl: number = 24;

// ── Radius ──────────────────────────────────────────────────────────
export const RadiusSm: number = 6;   // chip / 小徽标
export const RadiusMd: number = 8;   // 卡片 / 输入 / 按钮
export const RadiusLg: number = 12;  // 对话框 / 浮层

// ── Font size（角色）────────────────────────────────────────────────
export const FontMeta: number = 12;     // 元信息 / 状态 label
export const FontBody: number = 14;     // 正文
export const FontSection: number = 16;  // section 标题（16/600）
export const FontTitle: number = 20;    // 页面标题（移动）
export const FontTitleLg: number = 24;  // 页面标题（桌面大屏）

// ── Font weight ─────────────────────────────────────────────────────
export const WeightRegular: number = 400;
export const WeightMedium: number = 500;
export const WeightSemibold: number = 600;
export const WeightBold: number = 700;
