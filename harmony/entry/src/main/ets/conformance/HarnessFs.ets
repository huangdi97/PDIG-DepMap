// Harmony N3 Conformance — conformance 根目录探测。
//
// 为什么需要它：HAP 内 `context.filesDir` 的实际形态是
//   /data/storage/el2/base/haps/entry/files
// 从它向上冒泡可以走到应用沙箱根（/data/storage/el2/base）。
// 但**应用沙箱内没有仓库**，fixture 必须由外部推进来（见 getHarnessHint()）。
//
// 本模块的职责只有一个：把"fixture 在哪"这件事变成可判定的事实，
// 而不是一件靠猜测的事。探测失败时返回 undefined，由调用方如实记 NOT_RUN；
// **绝不**退回到某个默认路径去读一个可能不相关的目录。
//
// 依赖约束：只依赖 @ohos.file.fs。不得依赖 ArkUI / Ability / NAPI / cryptoFramework。

import fs from '@ohos.file.fs';

/**
 * 候选相对路径。
 *
 * 顺序即优先级，逐条都有明确用途：
 *   1–3  `filesDir` 冒泡出来的祖先 —— 设备侧把 fixture 推入沙箱时的落点；
 *   4–5  桌面 / 内部存储 —— 开发机上测试最常见的投放位置；
 *   6–8  Debug 场景（`hdc file send` 到应用私有目录）。
 */
const RELATIVE_CANDIDATES: string[] = [
  '/conformance/CONFORMANCE_MANIFEST.json',
  '/files/cf/conformance/CONFORMANCE_MANIFEST.json',
  '/../cf/conformance/CONFORMANCE_MANIFEST.json',
  '/storage/media/100/local/files/Docs/conformance/CONFORMANCE_MANIFEST.json',
  '/sdcard/conformance/CONFORMANCE_MANIFEST.json',
  '/sdcard/cf/conformance/CONFORMANCE_MANIFEST.json',
  '/conformance/CONFORMANCE_MANIFEST.json',
  '/data/local/tmp/conformance/CONFORMANCE_MANIFEST.json',
];

/** filesDir 向上冒泡的层数上限（沙箱层级有限，冒过头只会徒增 IO）。 */
const MAX_ASCENT = 6;

function tryDir(dir: string): string | undefined {
  const manifestPath = dir + '/conformance/CONFORMANCE_MANIFEST.json';
  try {
    if (fs.accessSync(manifestPath)) {
      return dir;
    }
  } catch (e) {
    // 不可访问 = 该候选不成立，继续下一个。不抛错：探测本身就允许失败。
  }
  return undefined;
}

function ascendAndProbe(startDir: string): string | undefined {
  let cur = startDir;
  for (let i = 0; i < MAX_ASCENT; i++) {
    const hit = tryDir(cur);
    if (hit !== undefined) {
      return hit;
    }
    const slash = cur.lastIndexOf('/');
    if (slash <= 0) {
      break;
    }
    cur = cur.substring(0, slash);
  }
  return undefined;
}

/**
 * 探测仓库根（含 `conformance/CONFORMANCE_MANIFEST.json` 与同级的 `fixtures/`）。
 *
 * 返回值语义：
 *   返回根路径 → 找到可信的 fixture 根，可以继续跑；
 *   返回 undefined → **未找到**。调用方必须据此记 NOT_RUN，
 *   不得伪造一个"空 fixture 集"然后报 0/91 失败 —— 那是把环境缺失伪装成产品缺陷。
 */
export function locateConformanceRoot(filesDir: string): string | undefined {
  if (filesDir.length > 0) {
    const viaAscent = ascendAndProbe(filesDir);
    if (viaAscent !== undefined) {
      return viaAscent;
    }
    for (let i = 0; i < RELATIVE_CANDIDATES.length; i++) {
      const rel = RELATIVE_CANDIDATES[i];
      const base = filesDir + rel.substring(0, rel.lastIndexOf('/conformance/'));
      const hit = tryDir(normalize(base));
      if (hit !== undefined) {
        return hit;
      }
    }
  }
  // 绝对候选（不依赖 filesDir）：仅在设备上有这些路径时才会命中。
  const absolutes: string[] = [
    '/storage/media/100/local/files/Docs',
    '/sdcard',
    '/sdcard/cf',
    '/data/local/tmp',
  ];
  for (let i = 0; i < absolutes.length; i++) {
    const hit = tryDir(absolutes[i]);
    if (hit !== undefined) {
      return hit;
    }
  }
  return undefined;
}

/** 折叠 `..` 段（例如 `/a/b/../c` → `/a/c`）。 */
function normalize(p: string): string {
  const parts = p.split('/');
  const stack: string[] = [];
  for (let i = 0; i < parts.length; i++) {
    const seg = parts[i];
    if (seg.length === 0 || seg === '.') {
      continue;
    }
    if (seg === '..') {
      if (stack.length > 0) {
        stack.pop();
      }
      continue;
    }
    stack.push(seg);
  }
  return '/' + stack.join('/');
}

/**
 * 未找到 fixture 时给运维的**可执行**提示。
 *
 * 写清楚具体命令而不是"请检查配置"：下一个遇到它的人应该能直接复制粘贴。
 */
export function harnessHint(filesDir: string): string {
  return 'conformance fixtures not found. Push the repo harness into the app sandbox, e.g.:\n' +
    '  hdc shell mkdir -p ' + filesDir + '/cf\n' +
    '  hdc file send <repo>/conformance ' + filesDir + '/cf/conformance\n' +
    '  hdc file send <repo>/fixtures    ' + filesDir + '/cf/fixtures';
}
