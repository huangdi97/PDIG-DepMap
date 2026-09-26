// Harmony N3 Conformance — 自检入口（runner 的唯一可达入边）。
//
// 为什么需要这一层，而不是让页面直接调 runConformance：
//   1. **可达性**：hvigor 的 CompileArkTS 只编译从 ability / page 可达的模块。
//      页面 import 本模块 → 本模块 import ConformanceRunner → 后者 import
//      JsonText / HarnessFs / 各 Domain 内核。这条链一旦成立，
//      runner 的编译就被 `check-compiled-reachability.mjs` 的 A/B/C/D 判据覆盖；
//      若断在这里，runner 就是个"源文件存在但从未编译"的孤儿。
//   2. **分层**：`getContext(this)` 属于 application 层职责。
//      runner 自己不去拿 AbilityContext（那会让 conformance 层依赖
//      @ohos.app.ability.common，从而无法在无 UI 环境下被调用）。
//      页面把 filesDir / now 显式传进来，runner 保持纯逻辑。
//
// 依赖约束：本模块可依赖 @ohos.app.ability.common（它是页面侧的桥），
// 但 **不得** 依赖关系型数据库 / HUKS / NAPI —— 那会让"conformance 可跑"
// 这件事依赖于尚未落地的能力。

import { common } from '@kit.AbilityKit';
import { ConformanceReport, RunnerEnv } from './ConformanceRunner';
import { runConformanceFs } from './FsTextSource';
import { harnessHint } from './HarnessFs';

/**
 * 一行摘要。
 *
 * 措辞刻意保守：本函数**不是** verdict，它只报告本次实际执行到的计数。
 * 之所以不返回 "PASS"/"FAIL"，是因为这里的运行环境是设备沙箱 ——
 * 找不到 fixture 根时如实说明，而不是让读者以为"跑了 0 个用例 = 全失败"。
 */
export function conformanceSummaryLine(): string {
  const ctx = getContext() as common.UIAbilityContext;
  const env = new RunnerEnv();
  env.filesDir = ctx.filesDir;
  env.now = new Date().toISOString();
  env.root = '';

  const report = runConformanceFs(env);

  if (report.notRun.length > 0 && report.passCount() === 0 && report.failed.length === 0) {
    // 环境未就绪：给出可直接执行的投放命令，而不是一句"未找到"。
    return 'conformance: NOT_RUN (0 executed) — ' + harnessHint(env.filesDir);
  }

  return 'conformance: pass=' + report.passCount() +
    ' fail=' + report.failed.length +
    ' notImplemented=' + report.notImplemented.length +
    ' blockedByRuntime=' + report.blockedByRuntime.length +
    ' (of 128)';
}

/** 面向报告的明细行；供后续落盘 `conformance/reports/harmony.json` 使用。 */
export function conformanceDetailLines(): string[] {
  const ctx = getContext() as common.UIAbilityContext;
  const env = new RunnerEnv();
  env.filesDir = ctx.filesDir;
  env.now = new Date().toISOString();
  env.root = '';

  const report = runConformanceFs(env);
  const lines: string[] = [];
  lines.push('platform=harmony specVersion=' + report.specVersion);
  lines.push('generatedAt=' + report.generatedAt);
  for (let i = 0; i < report.outcomes.length; i++) {
    const o = report.outcomes[i];
    lines.push(o.status + ' ' + o.category + '/' + o.caseId +
      (o.detail.length > 0 ? ' — ' + o.detail : ''));
  }
  return lines;
}

/**
 * 供设备侧取证脚本调用的稳定入口。
 *
 * 返回报告对象本身（而非字符串），因为比对细节（actual 与 expected 的差异）
 * 无法被压进一行摘要里 —— 把失败原因丢掉，等于让人拿不到可定位的证据。
 */
export function conformanceRun(): ConformanceReport {
  const ctx = getContext() as common.UIAbilityContext;
  const env = new RunnerEnv();
  env.filesDir = ctx.filesDir;
  env.now = new Date().toISOString();
  env.root = '';
  return runConformanceFs(env);
}
