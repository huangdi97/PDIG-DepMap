/**
 * Globe 2.5D 程序化渲染几何（纯数学、平台无关、确定性）。
 * 无 3D 引擎：正交投影 + 深度着色（与 Desktop GlobeMath.kt 同语义）。
 * 仅依赖 bundled 坐标集与程序化大气；零网络、零新增依赖。
 */

/** 3D 向量（y 向上，z 朝初始观察者）。 */
export class Vec3 {
  x: number;
  y: number;
  z: number;
  constructor(x: number, y: number, z: number) {
    this.x = x;
    this.y = y;
    this.z = z;
  }
}

/** 相机：yaw 绕竖直轴（经度视角）、pitch 绕水平轴（纬度视角）、zoom 缩放。 */
export class GlobeCamera {
  yawDeg: number;
  pitchDeg: number;
  zoom: number;
  constructor(yawDeg: number, pitchDeg: number, zoom: number) {
    this.yawDeg = yawDeg;
    this.pitchDeg = pitchDeg;
    this.zoom = zoom;
  }
}

/** 投影结果：zDepth ∈ [-1,1]，>0 表示朝向前方半球（可见）。 */
export class Projected {
  x: number;
  y: number;
  zDepth: number;
  constructor(x: number, y: number, zDepth: number) {
    this.x = x;
    this.y = y;
    this.zDepth = zDepth;
  }
}

function rad(d: number): number {
  return d / 180.0 * Math.PI;
}

/** 经纬度 → 单位球向量。 */
export function latLonToVec(latDeg: number, lonDeg: number): Vec3 {
  const phi = rad(latDeg);
  const lambda = rad(lonDeg);
  return new Vec3(
    Math.cos(phi) * Math.cos(lambda),
    Math.sin(phi),
    Math.cos(phi) * Math.sin(lambda),
  );
}

function rotateY(p: Vec3, yawDeg: number): Vec3 {
  const a = rad(yawDeg);
  const c = Math.cos(a);
  const s = Math.sin(a);
  return new Vec3(c * p.x + s * p.z, p.y, -s * p.x + c * p.z);
}

function rotateX(p: Vec3, pitchDeg: number): Vec3 {
  const a = rad(pitchDeg);
  const c = Math.cos(a);
  const s = Math.sin(a);
  return new Vec3(p.x, c * p.y - s * p.z, s * p.y + c * p.z);
}

export function rotatePoint(p: Vec3, cam: GlobeCamera): Vec3 {
  return rotateX(rotateY(p, cam.yawDeg), cam.pitchDeg);
}

/** 正交投影：zDepth>0 可见；zoom 改变半径。 */
export function project(v: Vec3, cam: GlobeCamera, radiusPx: number, centerX: number, centerY: number): Projected {
  const r = rotatePoint(v, cam);
  return new Projected(centerX + radiusPx * r.x, centerY - radiusPx * r.y, r.z);
}

/** 使目标经纬度对准屏幕中心的相机。 */
export function focusCamera(latDeg: number, lonDeg: number): GlobeCamera {
  const p = latLonToVec(latDeg, lonDeg);
  const yaw = Math.atan2(-p.x, p.z) / Math.PI * 180.0;
  const afterYaw = rotateY(p, yaw);
  const pitch = Math.atan2(afterYaw.y, Math.sqrt(afterYaw.x * afterYaw.x + afterYaw.z * afterYaw.z)) / Math.PI * 180.0;
  return new GlobeCamera(yaw, pitch, 1.0);
}

/** 深度因子（0=最远不可见 → 1=正对观察者）。 */
export function depthFactor(zDepth: number): number {
  const v = (zDepth + 1.0) / 2.0;
  if (v < 0) {
    return 0;
  }
  if (v > 1) {
    return 1;
  }
  return v;
}

/** 锚点命中测试：屏幕距离 ≤ 阈值 且位于前半球。 */
export function isAnchorHit(anchorScreen: Projected, pointerX: number, pointerY: number, hitRadiusPx: number): boolean {
  if (anchorScreen.zDepth <= 0) {
    return false;
  }
  const dx = anchorScreen.x - pointerX;
  const dy = anchorScreen.y - pointerY;
  return dx * dx + dy * dy <= hitRadiusPx * hitRadiusPx;
}

/** 球面上两点间的大圆插值采样（弧线分段渲染）。 */
export function greatCircleSamples(
  fromLat: number, fromLon: number, toLat: number, toLon: number, steps: number): Vec3[] {
  const result: Vec3[] = [];
  if (steps < 2) {
    return result;
  }
  const p1 = latLonToVec(fromLat, fromLon);
  const p2 = latLonToVec(toLat, toLon);
  let dot = p1.x * p2.x + p1.y * p2.y + p1.z * p2.z;
  if (dot < -1) {
    dot = -1;
  }
  if (dot > 1) {
    dot = 1;
  }
  const omega = Math.acos(dot);
  const sinOmega = Math.sin(omega);
  for (let i = 0; i <= steps; i++) {
    const t = i / steps;
    if (sinOmega < 0.0001) {
      result.push(normalize(new Vec3(
        p1.x + (p2.x - p1.x) * t,
        p1.y + (p2.y - p1.y) * t,
        p1.z + (p2.z - p1.z) * t,
      )));
    } else {
      const a = Math.sin((1 - t) * omega) / sinOmega;
      const b = Math.sin(t * omega) / sinOmega;
      result.push(new Vec3(a * p1.x + b * p2.x, a * p1.y + b * p2.y, a * p1.z + b * p2.z));
    }
  }
  return result;
}

function normalize(p: Vec3): Vec3 {
  const len = Math.sqrt(p.x * p.x + p.y * p.y + p.z * p.z);
  const safe = len < 0.000001 ? 1 : len;
  return new Vec3(p.x / safe, p.y / safe, p.z / safe);
}

/** 经纬网格线（每 stepDeg 度一条；lat 上界 ±60，避免极点密集）。 */
export class Graticule {
  parallels: number[][];
  meridians: number[][];
  constructor(stepDeg: number) {
    this.parallels = [];
    this.meridians = [];
    let lat = -60;
    while (lat <= 60) {
      this.parallels.push([lat, 0]);
      lat += stepDeg;
    }
    let lon = -180;
    while (lon < 180) {
      this.meridians.push([0, lon]);
      lon += stepDeg;
    }
  }
}

/** 缩放钳制（globe.minZoom 0.7 – maxZoom 1.9）。 */
export function clampZoom(z: number, minZ: number, maxZ: number): number {
  if (z < minZ) {
    return minZ;
  }
  if (z > maxZ) {
    return maxZ;
  }
  return z;
}

/** 纬度视角 clamp ±60°（INTERACTION_CONTRACT.md §2）。 */
export function clampPitch(p: number): number {
  if (p < -60) {
    return -60;
  }
  if (p > 60) {
    return 60;
  }
  return p;
}

/** easeInOutCubic（MOTION_CONTRACT easing）。 */
export function easeInOutCubic(t: number): number {
  if (t < 0.5) {
    return 4 * t * t * t;
  }
  return 1 - Math.pow(-2 * t + 2, 3) / 2;
}
