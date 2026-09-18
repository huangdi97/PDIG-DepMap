/*
 * FsTextSource —— fixture 文本的**设备侧**来源（走 @ohos.file.fs）。
 *
 * 为什么单独一个文件（而不是留在 ConformanceRunner 里）：
 * ConformanceRunner 必须保持零 @ohos 依赖，才能同时跑在
 *   (a) 设备 / 模拟器      —— 用本文件
 *   (b) 主机本地单元测试   —— 用内存源
 * 把 IO 收拢到这一个文件，runner 的纯净性就是一个**结构事实**，
 * 而不是"我们记得不要 import fs"这种靠自觉维持的约定。
 *
 * 依赖：只允许 @ohos.file.fs。不得依赖 ArkUI / Ability / relationalStore /
 * HUKS / NAPI / cryptoFramework。
 */

import fs from '@ohos.file.fs';
import { locateConformanceRoot } from './HarnessFs';
import { ConformanceReport, ConformanceTextSource, RunnerEnv, runConformanceWith } from './ConformanceRunner';

/** 仓库根下的相对路径常量；与 HarnessFs 的探测约定一致。 */
const MANIFEST_REL = 'conformance/CONFORMANCE_MANIFEST.json';

/**
 * 读文本；不存在或不可读返回空串（调用方据空串记 NOT_RUN，不抛错）。
 *
 * 注意 `accessSync` 返回 boolean 而非抛错，两者都要处理：
 * 路径不存在时部分实现返回 false，权限问题则抛 —— 对调用方而言都是"读不到"。
 */
export function readTextIfExists(fullPath: string): string {
  try {
    if (!fs.accessSync(fullPath)) {
      return '';
    }
    return fs.readTextSync(fullPath);
  } catch (e) {
    return '';
  }
}

/** 以「仓库根」为基准的 fs 文本源。 */
export class FsTextSource implements ConformanceTextSource {
  private root: string = '';

  constructor(root: string) {
    this.root = root;
  }

  readManifest(): string {
    return readTextIfExists(this.root + '/' + MANIFEST_REL);
  }

  readFixture(relPath: string): string {
    return readTextIfExists(this.root + '/' + relPath);
  }

  describe(): string {
    return 'fs:' + this.root;
  }
}

/**
 * 设备侧入口：探测仓库根 → 组 fs 文本源 → 交给纯 runner。
 *
 * 探测失败时**不回落**到某个默认路径（那会读到无关目录并产生假结论），
 * 而是返回一个 notRun 报告；具体提示由 ConformanceSelfCheck 负责渲染。
 */
export function runConformanceFs(env: RunnerEnv): ConformanceReport {
  const root = locateConformanceRoot(env.filesDir);
  if (root === undefined) {
    const report = new ConformanceReport();
    report.generatedAt = env.now;
    report.notRun.push('*');
    return report;
  }
  const envWithRoot = new RunnerEnv();
  envWithRoot.root = root;
  envWithRoot.filesDir = env.filesDir;
  envWithRoot.now = env.now;
  return runConformanceWith(envWithRoot, new FsTextSource(root));
}
