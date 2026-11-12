/**
 * VNextGlobe —— ArkUI Canvas 程序化 2.5D 交互球体（Global Infrastructure Navigator）。
 * 契约：components/globe.md + INTERACTION_CONTRACT.md §1-§3。
 *  - 状态机：GLOBAL → REGION_HOVER → REGION_SELECTED → REGION_DETAIL（Escape 逐级回退）；
 *  - drag 旋转（pitch clamp ±60°）/ pinch 缩放（0.7–1.9）/ 点击聚焦（620ms ease）/ 返回全局；
 *  - idle rotation 0.8°/s（交互或选中或 reduce motion 时暂停）；
 *  - 触控热区 ≥48vp（透明扩大，视觉锚点保持 14/22）；
 *  - 非视觉替代（Region List）由父级提供，本组件不是唯一导航。
 */
import { ArcPair, RegionPresentation } from './UIVNextModels';
import { GlobeCamera, clampPitch, clampZoom, easeInOutCubic, focusCamera } from './GlobeMath';
import { VColors, VMotion } from './PdigV2Theme';
import { GlobeMetrics, anchorScreen, globeMetrics, hitTolerance, renderGlobe } from './VNextGlobeDraw';

const YAW_PER_TICK = VMotion.IdleRotationDegPerSec * 0.2;   // 200ms tick → 0.16°
const FOCUS_STEPS = 31;                                      // 620ms / 20ms
const FOCUS_TICK_MS = 20;

@Component
export struct VNextGlobe {
  @Prop regions: RegionPresentation[];
  @Prop arcs: ArcPair[];
  @Prop reduceMotion: boolean;
  @Prop @Watch('onSelectedChange') selectedRegion: string = '';
  onSelectRegion: (code: string) => void = (code: string): void => { };
  onOpenDetail: (code: string) => void = (code: string): void => { };
  onBackToGlobal: () => void = (): void => { };
  private settings: RenderingContextSettings = new RenderingContextSettings(true);
  private ctx: CanvasRenderingContext2D = new CanvasRenderingContext2D(this.settings);
  private idleTimer: number = -1;
  private focusTimer: number = -1;
  private canvasW: number = 0;
  private canvasH: number = 0;
  @State yawDeg: number = 0;
  @State pitchDeg: number = 30;
  @State zoom: number = 1;
  @State hovered: string = '';
  @State interacting: boolean = false;
  private dragStartX: number = 0;
  private dragStartY: number = 0;
  private movedDuringDrag: boolean = false;
  private pinchStartDist: number = 0;
  private pinchStartZoom: number = 1;

  aboutToAppear(): void {
    this.idleTimer = setInterval((): void => {
      if (this.interacting || this.selectedRegion !== '' || this.reduceMotion) {
        return;
      }
      this.yawDeg = (this.yawDeg + YAW_PER_TICK) % 360;
      this.redraw();
    }, 200);
  }
  aboutToDisappear(): void {
    if (this.idleTimer >= 0) {
      clearInterval(this.idleTimer);
    }
    if (this.focusTimer >= 0) {
      clearInterval(this.focusTimer);
    }
  }
  /** 父级选中地区 → 相机 focus/flyTo（620ms easeInOutCubic；reduce motion 简化）。 */
  onSelectedChange(): void {
    if (this.selectedRegion === '') {
      this.hovered = '';
      return;
    }
    const r = this.regions.find((item: RegionPresentation): boolean => item.regionCode === this.selectedRegion);
    if (r === undefined) {
      return;
    }
    const target = focusCamera(r.latitude, r.longitude);
    target.zoom = Math.max(this.zoom, 1.3);
    this.animateCameraTo(target);
  }
  private animateCameraTo(target: GlobeCamera): void {
    if (this.focusTimer >= 0) {
      clearInterval(this.focusTimer);
    }
    if (this.reduceMotion) {
      // reduceMotion：cameraAnimation simplified —— 无 ease 转场，直接到位。
      this.yawDeg = target.yawDeg;
      this.pitchDeg = target.pitchDeg;
      this.zoom = target.zoom;
      this.redraw();
      return;
    }
    const yaw0 = this.yawDeg;
    const pitch0 = this.pitchDeg;
    const zoom0 = this.zoom;
    let step = 0;
    this.focusTimer = setInterval((): void => {
      step++;
      const eased = easeInOutCubic(step / FOCUS_STEPS);
      this.yawDeg = yaw0 + (target.yawDeg - yaw0) * eased;
      this.pitchDeg = pitch0 + (target.pitchDeg - pitch0) * eased;
      this.zoom = zoom0 + (target.zoom - zoom0) * eased;
      this.redraw();
      if (step >= FOCUS_STEPS) {
        clearInterval(this.focusTimer);
        this.focusTimer = -1;
      }
    }, FOCUS_TICK_MS);
  }
  private camera(): GlobeCamera {
    return new GlobeCamera(this.yawDeg, this.pitchDeg, this.zoom);
  }
  private redraw(): void {
    if (this.canvasW <= 0 || this.canvasH <= 0) {
      return;
    }
    renderGlobe(this.ctx, this.canvasW, this.canvasH, this.camera(),
      this.regions, this.arcs, this.hovered, this.selectedRegion, true);
  }
  private hitRegionAt(px: number, py: number): string {
    const m: GlobeMetrics = globeMetrics(this.canvasW, this.canvasH, this.zoom);
    const cam = this.camera();
    const tol = Math.max(hitTolerance(m.radius), 24); // 触控热区 ≥48vp（直径）
    const tol2 = tol * tol;
    let best: string = '';
    let bestD = Number.MAX_VALUE;
    const list = this.regions;
    for (let i = 0; i < list.length; i++) {
      const p = anchorScreen(list[i], cam, m);
      if (p === undefined) {
        continue;
      }
      const dx = p.x - px;
      const dy = p.y - py;
      const d = dx * dx + dy * dy;
      if (d <= tol2 && d < bestD) {
        best = list[i].regionCode;
        bestD = d;
      }
    }
    return best;
  }
  private touchDistance(t: TouchObject[]): number {
    if (t.length < 2) {
      return 0;
    }
    const dx = t[0].x - t[1].x;
    const dy = t[0].y - t[1].y;
    return Math.sqrt(dx * dx + dy * dy);
  }
  private onTouchEvent(e: TouchEvent): void {
    if (e.type === TouchType.Down) {
      this.interacting = true;
      this.movedDuringDrag = false;
      this.dragStartX = e.touches[0].x;
      this.dragStartY = e.touches[0].y;
      if (e.touches.length >= 2) {
        this.pinchStartDist = this.touchDistance(e.touches);
        this.pinchStartZoom = this.zoom;
      }
      return;
    }
    if (e.type === TouchType.Move) {
      if (e.touches.length >= 2) {
        const dist = this.touchDistance(e.touches);
        if (this.pinchStartDist > 0) {
          this.zoom = clampZoom(this.pinchStartZoom * dist / this.pinchStartDist, VMotion.MinZoom, VMotion.MaxZoom);
          this.movedDuringDrag = true;
          this.redraw();
        }
        return;
      }
      const dx = e.touches[0].x - this.dragStartX;
      const dy = e.touches[0].y - this.dragStartY;
      if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
        this.movedDuringDrag = true;
      }
      this.yawDeg = this.yawDeg - dx * 0.35;
      this.pitchDeg = clampPitch(this.pitchDeg - dy * 0.35);
      this.dragStartX = e.touches[0].x;
      this.dragStartY = e.touches[0].y;
      this.redraw();
      return;
    }
    if (e.type === TouchType.Up) {
      this.interacting = false;
      const t = e.touches.length > 0 ? e.touches[0] : e.changedTouches[0];
      if (!this.movedDuringDrag && t !== undefined) {
        this.handleTap(t.x, t.y);
      }
      return;
    }
    this.interacting = false;
  }
  /** 点击锚点：未选中 → 聚焦选中；已选中 → REGION_DETAIL；点空白 → 回全局。 */
  private handleTap(x: number, y: number): void {
    const hit = this.hitRegionAt(x, y);
    if (hit !== '') {
      if (this.selectedRegion === hit) {
        this.onOpenDetail(hit);
      } else {
        this.onSelectRegion(hit);
      }
      return;
    }
    if (this.selectedRegion !== '') {
      this.onBackToGlobal();
    }
  }
  private onHoverEvent(isHover: boolean, e: HoverEvent): void {
    if (!isHover || e.x < 0 || e.y < 0) {
      this.hovered = '';
    } else {
      const hit = this.hitRegionAt(e.x, e.y);
      this.hovered = hit !== '' ? hit : '';
    }
    this.redraw();
  }
  private onKeyHandler(event: KeyEvent): void {
    if (event.type === KeyType.Down && event.keyCode === KeyCode.KEYCODE_ESCAPE && this.selectedRegion !== '') {
      this.onBackToGlobal();
    }
  }
  private zoomBy(delta: number): void {
    this.interacting = true;
    this.zoom = clampZoom(this.zoom + delta, VMotion.MinZoom, VMotion.MaxZoom);
    this.redraw();
    this.interacting = false;
  }
  @Builder
  zoomButton(label: string, delta: number) {
    Button(label)
      .width(48)
      .height(48)
      .backgroundColor(VColors.SurfaceRaised)
      .fontColor(VColors.PrimaryBright)
      .borderRadius(24)
      .fontSize(20)
      .onClick((): void => {
        this.zoomBy(delta);
      })
  }

  build() {
    Stack({ alignContent: Alignment.TopStart }) {
      Canvas(this.ctx)
        .width('100%')
        .height('100%')
        .id('pdig.globe.canvas')
        .onReady((): void => {
          this.redraw();
        })
        .onAreaChange((oldValue: Area, newValue: Area): void => {
          this.canvasW = typeof newValue.width === 'number' ? newValue.width : Number.parseFloat(String(newValue.width));
          this.canvasH = typeof newValue.height === 'number' ? newValue.height : Number.parseFloat(String(newValue.height));
          this.redraw();
        })
        .onTouch((e: TouchEvent): void => {
          this.onTouchEvent(e);
        })
        .onHover((isHover: boolean, e: HoverEvent): void => {
          this.onHoverEvent(isHover, e);
        })
        .onKeyEvent((event: KeyEvent): void => {
          this.onKeyHandler(event);
        })
        .focusable(true)
        .accessibilityText('交互地球：拖动旋转，捏合缩放，点击地区聚焦')
        .accessibilityLevel('yes')

      if (this.selectedRegion !== '') {
        Button('返回全局')
          .id('pdig.globe.back')
          .height(40)
          .fontSize(12)
          .backgroundColor(VColors.SurfaceRaised)
          .fontColor(VColors.TextPrimary)
          .borderRadius(10)
          .margin({ top: 12, left: 12 })
          .align(Alignment.TopStart)
          .onClick((): void => {
            this.onBackToGlobal();
          })
      }
      Column({ space: 8 }) {
        this.zoomButton('+', 0.2);
        this.zoomButton('−', -0.2);
      }
      .align(Alignment.BottomEnd)
      .margin({ right: 12, bottom: 12 })
    }
    .width('100%')
    .height('100%')
    .clip(true)
  }
}
