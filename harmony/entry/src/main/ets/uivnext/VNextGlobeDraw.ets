/**
 * Globe 2.5D 画布渲染器（纯绘制函数，无组件状态）。
 * 视觉契约：components/globe.md §4 —— 只允许 token 颜色（canvas / canvasDeep / surface /
 * surfaceRaised / atmosphereInner→Outer / borderSubtle / regionNodeHi/Lo / arcActive/Quiet）；
 * 弧线仅来自真实跨区关系或地区资产摘要；禁止装饰性连线。
 */
import { ArcPair, RegionPresentation } from './UIVNextModels';
import { VColors } from './PdigV2Theme';
import {
  GlobeCamera, Graticule, Projected, Vec3, depthFactor, greatCircleSamples, latLonToVec, project,
} from './GlobeMath';

/** 舞台度量：圆心 + 半径（zoom 改变半径；直径下限 520 由父级 stage 尺寸保证）。 */
export class GlobeMetrics {
  centerX: number;
  centerY: number;
  radius: number;
  constructor(centerX: number, centerY: number, radius: number) {
    this.centerX = centerX;
    this.centerY = centerY;
    this.radius = radius;
  }
}

export function globeMetrics(width: number, height: number, zoom: number): GlobeMetrics {
  const d = Math.min(width, height);
  const radius = d * 0.36 * zoom;
  return new GlobeMetrics(width / 2, height / 2, radius);
}

export function anchorScreen(region: RegionPresentation, cam: GlobeCamera, m: GlobeMetrics): Projected | undefined {
  const p = project(latLonToVec(region.latitude, region.longitude), cam, m.radius, m.centerX, m.centerY);
  if (p.zDepth <= 0) {
    return undefined;
  }
  return p;
}

export function hitTolerance(radius: number): number {
  const raw = radius * 0.05;
  if (raw < 10) {
    return 10;
  }
  if (raw > 18) {
    return 18;
  }
  return raw;
}

function drawGraticule(ctx: CanvasRenderingContext2D, m: GlobeMetrics, cam: GlobeCamera, grid: Graticule): void {
  ctx.strokeStyle = VColors.BorderSubtle;
  ctx.lineWidth = 1;
  const parallels = grid.parallels;
  for (let i = 0; i < parallels.length; i++) {
    const lat = parallels[i][0];
    const path = greatCircleSamples(lat, 0, lat + 20, 0, 24);
    drawPathFaded(ctx, m, cam, path, 0.9);
  }
  const meridians = grid.meridians;
  for (let i = 0; i < meridians.length; i++) {
    const lon = meridians[i][1];
    drawPathFaded(ctx, m, cam, greatCircleSamples(0, lon, 10, lon, 24), 0.9);
    drawPathFaded(ctx, m, cam, greatCircleSamples(0, lon, 10, lon + 10, 24), 0.9);
  }
}

/** 弧线段：仅绘制前后半球交界、按深度淡出。 */
function drawPathFaded(ctx: CanvasRenderingContext2D, m: GlobeMetrics, cam: GlobeCamera,
  path: Vec3[], fade: number): void {
  if (path.length < 2) {
    return;
  }
  let prev = project(path[0], cam, m.radius, m.centerX, m.centerY);
  let drawing = prev.zDepth > -0.05;
  for (let i = 1; i < path.length; i++) {
    const cur = project(path[i], cam, m.radius, m.centerX, m.centerY);
    const visible = cur.zDepth > -0.05;
    if (visible || drawing) {
      const d = depthFactor(cur.zDepth);
      const alpha = (d * 0.9 + 0.1) * fade;
      ctx.globalAlpha = alpha;
      ctx.beginPath();
      ctx.moveTo(prev.x, prev.y);
      ctx.lineTo(cur.x, cur.y);
      ctx.stroke();
    }
    drawing = visible;
    prev = cur;
  }
  ctx.globalAlpha = 1;
}

/** 跨区弧线（active/quiet 按选中态）。 */
function drawArcs(ctx: CanvasRenderingContext2D, m: GlobeMetrics, cam: GlobeCamera,
  arcs: ArcPair[], regions: RegionPresentation[], selected: string): void {
  for (let i = 0; i < arcs.length; i++) {
    const arc = arcs[i];
    const a = regions.find((r: RegionPresentation): boolean => r.regionCode === arc.a);
    const b = regions.find((r: RegionPresentation): boolean => r.regionCode === arc.b);
    if (a === undefined || b === undefined) {
      continue;
    }
    const isSelected = selected === arc.a || selected === arc.b;
    ctx.strokeStyle = isSelected ? VColors.ArcActive : VColors.ArcQuiet;
    ctx.lineWidth = isSelected ? 1.8 : 1.2;
    const path = greatCircleSamples(a.latitude, a.longitude, b.latitude, b.longitude, 48);
    drawPathFaded(ctx, m, cam, path, 1);
  }
}

/** 单帧绘制：大气 → 球体深度着色 → 网格 → 弧线 → 锚点 → 标签/tooltip。 */
export function renderGlobe(
  ctx: CanvasRenderingContext2D,
  width: number,
  height: number,
  cam: GlobeCamera,
  regions: RegionPresentation[],
  arcs: ArcPair[],
  hovered: string,
  selected: string,
  showLabels: boolean,
): GlobeMetrics {
  const m = globeMetrics(width, height, cam.zoom);
  ctx.clearRect(0, 0, width, height);

  // L0 Atmosphere（程序化大气辉光）
  const atmosphere = ctx.createRadialGradient(m.centerX, m.centerY, m.radius * 0.4, m.centerX, m.centerY, m.radius * 1.7);
  atmosphere.addColorStop(0, VColors.AtmosphereInner);
  atmosphere.addColorStop(1, VColors.AtmosphereOuter);
  ctx.fillStyle = atmosphere;
  ctx.beginPath();
  ctx.arc(m.centerX, m.centerY, m.radius * 1.7, 0, Math.PI * 2);
  ctx.fill();

  // Sphere（程序化深度着色：canvas → surface → canvasDeep，无纹理）
  const sphere = ctx.createRadialGradient(m.centerX - m.radius * 0.35, m.centerY - m.radius * 0.35,
    m.radius * 0.1, m.centerX, m.centerY, m.radius * 1.4);
  sphere.addColorStop(0, VColors.Canvas);
  sphere.addColorStop(0.55, VColors.Surface);
  sphere.addColorStop(1, VColors.CanvasDeep);
  ctx.fillStyle = sphere;
  ctx.beginPath();
  ctx.arc(m.centerX, m.centerY, m.radius, 0, Math.PI * 2);
  ctx.fill();

  // 经纬网格（前半球，按深度淡出）
  drawGraticule(ctx, m, cam, new Graticule(30));

  // 跨区真实关系弧线
  drawArcs(ctx, m, cam, arcs, regions, selected);

  // 地区锚点（hover/active 高亮 + 光环；quiet 降强调）
  const regionsLen = regions.length;
  for (let i = 0; i < regionsLen; i++) {
    const r = regions[i];
    const p = anchorScreen(r, cam, m);
    if (p === undefined) {
      continue;
    }
    const isSelected = selected === r.regionCode;
    const isHovered = hovered === r.regionCode;
    const d = depthFactor(p.zDepth);
    const base = m.radius * (0.02 + 0.012 * d);
    const anchorR = base * (isSelected ? 1.6 : 1);
    ctx.fillStyle = (isSelected || isHovered) ? VColors.RegionNodeHi : VColors.RegionNodeLo;
    ctx.beginPath();
    ctx.arc(p.x, p.y, anchorR, 0, Math.PI * 2);
    ctx.fill();
    if (isSelected || isHovered) {
      // 聚焦 ring = semantic.focus（primaryBright）
      ctx.strokeStyle = VColors.PrimaryBright;
      ctx.globalAlpha = 0.45;
      ctx.lineWidth = 1.5;
      ctx.beginPath();
      ctx.arc(p.x, p.y, anchorR * 2.1, 0, Math.PI * 2);
      ctx.stroke();
      ctx.globalAlpha = 1;
    }
    // active 标签常显 / hover tooltip
    if (showLabels && isSelected) {
      drawLabel(ctx, p.x, p.y - anchorR - 6, r.displayName, true);
    }
  }

  // Tooltip（name + counts；REGION_HOVER；蒙版下只有聚合计数）
  if (hovered !== '') {
    const r = regions.find((item: RegionPresentation): boolean => item.regionCode === hovered);
    if (r !== undefined) {
      const p = anchorScreen(r, cam, m);
      if (p !== undefined) {
        const tip = `${r.displayName}，${r.cardCount} 张卡，${r.phoneCount} 个号码`;
        drawTooltip(ctx, p.x, p.y - 26, tip);
      }
    }
  }
  return m;
}

function drawLabel(ctx: CanvasRenderingContext2D, x: number, y: number, text: string, primary: boolean): void {
  ctx.font = '600 15px sans-serif';
  ctx.textAlign = 'center';
  ctx.fillStyle = primary ? VColors.TextPrimary : VColors.TextMuted;
  ctx.fillText(text, x, y);
}

/** Tooltip 面板：panelRaised + divider + ink 文字（radius.md 10 / padding 12）。 */
function drawTooltip(ctx: CanvasRenderingContext2D, x: number, y: number, text: string): void {
  ctx.font = '400 12px sans-serif';
  const w = ctx.measureText(text).width + 24;
  const h = 26;
  const bx = x - w / 2;
  const by = y - h - 8;
  ctx.fillStyle = VColors.SurfaceRaised;
  ctx.beginPath();
  ctx.moveTo(bx + 10, by);
  ctx.lineTo(bx + w - 10, by);
  ctx.arc(bx + w - 10, by + 10, 10, -Math.PI / 2, 0);
  ctx.lineTo(bx + w, by + h - 10);
  ctx.arc(bx + w - 10, by + h - 10, 10, 0, Math.PI / 2);
  ctx.lineTo(bx + 10, by + h);
  ctx.arc(bx + 10, by + h - 10, 10, Math.PI / 2, Math.PI);
  ctx.lineTo(bx, by + 10);
  ctx.arc(bx + 10, by + 10, 10, Math.PI, -Math.PI / 2);
  ctx.closePath();
  ctx.fill();
  ctx.strokeStyle = VColors.BorderSubtle;
  ctx.lineWidth = 1;
  ctx.stroke();
  ctx.fillStyle = VColors.TextPrimary;
  ctx.textAlign = 'center';
  ctx.fillText(text, x, by + 17);
}
