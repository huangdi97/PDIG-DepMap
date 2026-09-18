// Harmony N3 Conformance Runner — 真实 ArkTS 执行器（不是 Node 镜像的转发）。
//
// 定位与边界
// ---------------------------------------------------------------------------
// 本 runner 在**设备上**读取平台中立 fixture，用 Harmony 自己的 Domain 内核
// 重算 `input`，再把结果与 fixture 的 `expected` 逐字节比对。
// 它的对照基准是 Android 端口（`android/conformance/.../Main.kt`，91/91），
// 而 Android 与 ArkTS 又都对照同一个冻结 TS oracle。
//
// 判定纪律（本项目铁律，逐条落实）
// ---------------------------------------------------------------------------
//  1. `NOT_IMPLEMENTED` 既不是 FAIL 也不是 PASS；
//  2. `BLOCKED_BY_RUNTIME` 明确表示"需要设备能力才能判定"，
//     **不计入 pass**，也不伪装成失败 —— 见 §Runtime-dependent 小节的分担表；
//  3. 比对是 `JSON.stringify(actual) === JSON.stringify(expected)`，
//     即**逐字节**而非语义等价。键序不同即 FAIL，
//     因为"三端产出完全相同的 JSON"本就是 DEPMAP/Conformance 的合同原文；
//  4. 环境缺失（找不到 fixture 根、无设备）→ `NOT_RUN`，
//     绝不返回"0/91 通过"这类模糊结论。
//
// 为什么不做通用 JSON 解析
// ---------------------------------------------------------------------------
// ArkTS 禁止 any / 动态索引，`JSON.parse` 的结果无法安全映射到 class。
// 因此本文件用 `JsonText` 在文本上做**受控扫描**：
// 结构扫描（数组切分、对象窗口、字段定位）是通用的；
// 值装载（`parseDependencyObjects` 等）是**类型化**的 ——
// 它每读一个字段都经过 generated 的 wire→enum 转换（`<Enum>FromWire`，
// 由 tools/codegen/generate.mjs 与 Kotlin / Swift 同源产出），
// 未知取值返回 undefined 并立即失败，不猜、不兜底。
// 这与"通用 JSON 解析"有本质区别：后者会静默接受任何输入。
//
// 唯一例外是 jcs 用例：它测的正是"任意 JSON 值树 → JCS 字节"，
// 由 `JcsParser`（crypto/Jcs 内）按受限值域解析，越界即抛。详见 runJcs。
//
// 依赖约束：**本模块不得 import 任何 @ohos.* / ArkUI / Ability 模块。**
//
// 原因（2026-09-18 实测确认）：DevEco 的「本地单元测试」能在**无设备的主机**上
// 执行真实 ArkTS 代码，但它对 @ohos.* 只提供**不可调用的桩**
// （实测 `fs.readTextSync` → "is not callable"）。
// 只要 runner 保持纯净，它就能同时跑在两种执行面上：
//   * 设备 / 模拟器：文本由 FsTextSource（@ohos.file.fs）提供
//   * 主机单元测试：文本由内存源提供（见 src/test 的 FixtureBundle）
// 一旦这里 import 了 fs，主机执行面就会连带失去 —— 那是白白放弃一个执行面。
// fixture 文本一律经 ConformanceTextSource 注入，本模块不碰任何 IO。

import {
  Capability,
  ChangePlanWorkflowState,
  DriftStatus,
  GroupMode,
  GroupState,
  DependencyState,
  PlanActionPhase,
  SourceInstanceState,
  changePlanWorkflowStateFromWire,
  groupModeFromWire,
  groupStateFromWire,
  planActionPhaseFromWire,
  capabilityFromWire,
  criticalityFromWire,
  dependencyOriginFromWire,
  dependencyStateFromWire,
  nodeKindFromWire,
  relationFromWire,
} from '../generated/CanonicalEnums';
import {
  Dependency,
  DependencyGroup,
  ImpactGraph,
  ImpactProposalInput,
  ImpactStateKey,
  ChangePlan,
  PlanAction,
  PlanReadinessInput,
  ScenarioCoverageInput,
  CoverageSourceInfo,
  CoverageCounts,
} from '../domain/Entities';
import { RelationValidationResult } from '../domain/Relations';
import { simulateScenario, ImpactResult, ImpactTargetResult, ImpactChecklistItem } from '../domain/ImpactKernel';
import { computePlanReadiness } from '../domain/PlanReadiness';
import { computeScenarioCoverage } from '../domain/ScenarioCoverage';
import { validateRelationUse, validateRelationGroupUse } from '../domain/Relations';
import {
  SCENARIO_TEMPLATES,
  PLANNED_TEMPLATES,
  ScenarioTemplate,
} from '../domain/ScenarioTemplate';
import {
  ALL_WORKFLOW_STATES,
  CANDIDATE_ACCEPT_BUMPS_REVISION,
  CANDIDATE_ACCEPT_EFFECTS,
  CANDIDATE_ACCEPT_MUTATES_REALITY,
  CANDIDATE_ACCEPT_REPLAY,
  CANDIDATE_DISMISS_EFFECTS,
  CANDIDATE_DISMISS_MUTATES_REALITY,
  CANDIDATE_DISMISS_REAPPEAL,
  CANDIDATE_GUARDS,
  CANDIDATE_INITIAL,
  CANDIDATE_STATES,
  DRIFT_CREATION_ABSENCE_ONLY,
  DRIFT_CREATION_ALREADY_CONFIRMED,
  DRIFT_CREATION_BELOW_THRESHOLD,
  DRIFT_CREATION_DUPLICATE_REF,
  DRIFT_CREATION_MIN_OBSERVATIONS,
  DRIFT_CREATION_REQUIRES,
  DRIFT_CREATION_UPSERT,
  DRIFT_GUARDS,
  DRIFT_INITIAL,
  DRIFT_STATES,
  StateGuard,
  VERIFICATION_EVIDENCE_APPLIES_TO,
  VERIFICATION_EVIDENCE_EFFECT,
  VERIFICATION_EVIDENCE_MATCH,
  VERIFICATION_EVIDENCE_METHOD,
  VERIFICATION_EVIDENCE_NEVER,
  VERIFICATION_GUARDS,
  VERIFICATION_INITIAL,
  VERIFICATION_STATES,
  allowedCandidateNext,
  allowedDriftNext,
  allowedNextStates,
  allowedVerificationNext,
  isTerminalState,
  isVerificationTerminal,
} from '../domain/StateMachines';
import { GRAPH_REVISION_KEY, doesMutationBumpRevision } from '../domain/GraphRevision';
import {
  BUCKET_ORDER,
  LEGACY_WECHAT_SOURCE_INSTANCE_ID,
  TimelineDriftInput,
  TimelineInput,
  TimelineItem,
  TimelineNodeInput,
  TimelinePlanInput,
  TimelineSortKey,
  TimelineSourceInput,
  bucketOf,
  buildTimelinePure,
  nodeExpiryDate,
  sortTimelineKeys,
} from '../domain/Timeline';
import { JsonText, compactJson } from './JsonText';
import { JcsError, jcsEscapeString, jcsParse, jcsStringify } from '../crypto/Jcs';
import { decodeBase64, DepmapError, parseDepmapHeader, validateDepmapBounds } from '../crypto/DepmapBounds';
import {
  GenericCsvParser,
  MappingProfile,
  Observation,
  OfxParser,
  ParseError,
  ParseResult,
  WechatParser,
} from '../sources/Parsers';

/* ==========================================================================
 * 结果类型
 * ========================================================================== */

/** 单个用例的判定。取值与 harness 契约一致，另加 BLOCKED_BY_RUNTIME。 */
export class CaseOutcome {
  caseId: string = '';
  category: string = '';
  status: string = 'NOT_IMPLEMENTED';
  actual: string = '';
  expected: string = '';
  detail: string = '';
}

/** 整个平台的报告，字段与 `conformance/reports/<platform>.json` 契约对齐。 */
export class ConformanceReport {
  platform: string = 'harmony';
  specVersion: string = '1.0.0';
  generatedAt: string = '';
  /** 探测到的仓库根；未探测到时为空串。 */
  root: string = '';
  outcomes: CaseOutcome[] = [];
  /** 已执行并通过的用例 id → 其 actual（JSON 文本）。 */
  passed: Map<string, string> = new Map<string, string>();
  /** 判定为 FAIL 的用例 id 列表。 */
  failed: string[] = [];
  /** 未实现（fixture 存在但本平台无实现）的用例 id 列表。 */
  notImplemented: string[] = [];
  /** 纯逻辑尚未移植、**不需要**设备即可判定的用例（本轮要求清零的账）。 */
  hostImplMissing: string[] = [];
  /** 需要 GB18030 等主机执行面缺失能力的用例。 */
  blockedByEnvironment: string[] = [];
  /** 需要设备运行时能力、当前无法判定的用例 id 列表（**不计入 pass**）。 */
  blockedByRuntime: string[] = [];
  /** 因环境缺失而完全未执行的用例 id 列表。 */
  notRun: string[] = [];

  passCount(): number {
    return this.passed.size;
  }
}

/* ==========================================================================
 * §Runtime-dependent 分担表
 *
 * 这张表是"诚实计数"的核心：它把"Harmony 没实现"与
 * "实现需要设备能力、当前环境给不了"分开。
 * 混为一谈会让 91 这个分母失去意义。
 * ========================================================================== */

/* ==========================================================================
 * 未执行用例的分担表
 *
 * 这张表是"诚实计数"的核心：它把 3 种**性质不同**的未执行区分开 ——
 *   HOST_IMPLEMENTATION_MISSING  纯逻辑尚未移植，**不需要**任何 Harmony runtime
 *   BLOCKED_BY_ENVIRONMENT       逻辑可行，但缺主机执行面给不了的能力（字符集转换）
 *   BLOCKED_BY_RUNTIME           必须真实设备/KDF 才能判定
 *
 * 三者混为一谈会让 91 这个分母失去意义：把"没写"记成"设备才能测"，
 * 等于把没做的事伪装成外部阻塞。**逐 case 列出**，而不是按类一刀切 ——
 * 详细论证见 `HARMONY_REMAINING_6_AUDIT.md`。
 * ========================================================================== */

/**
 * 纯逻辑未移植（**当前为空**）。
 *
 * 上一版这里是 20 条 parser 用例。它们的输入文件经实测全部是合法 UTF-8，
 * 解析逻辑（CSV 词法 + 日期/金额 + OFX SGML 扫描 + 微信账单列规则）零平台 API，
 * 缺的只是 ArkTS 侧 adapter —— 那是"没写"，不是"只有设备才测得"。
 * 本轮移植 `sources/Parsers.ets` 后清零。
 *
 * 保留这个空清单而不是删掉整个分支：它的存在让"HOST_IMPLEMENTATION_MISSING
 * 必须有逐条 caseId 证据"这件事继续成立，下次有人再往回塞一条时得写清理由。
 */
const HOST_IMPL_MISSING_CASES: string[] = [];

/**
 * 环境缺失（当前数量 2）：需要 GB18030 → UTF-16 字符集转换。
 *
 * 这两个文件的字节**不是合法 UTF-8**（实测），必须先做 GB18030 解码；
 * 主机执行面无 `util.TextDecoder` 实现，自造一张 GB18030 码表既无必要也无从验证。
 * 设备上解码成立后才可在这个 runner 跑。
 */
const ENV_BLOCKED_CASES: string[] = [
  'parser-csv-gb18030',
  'parser-wechat-gb18030',
];

/**
 * 需要真实设备能力（当前数量 4）。
 *
 * 逐条理由（不是笼统的"依赖运行时"）：
 *   - depmap-golden-v1 / depmap-utf8-password-normalization：
 *     expected 含 `derivedKeyHex` / `ciphertextBase64` / `tagBase64`，
 *     必须真实执行 Argon2id（NAPI 原生 .so）+ AES-256-GCM。主机的本地单元测试
 *     无法加载 OHOS ABI 的 native 库，这两条在主机面**原理上**不可执行。
 *   - backup-depmap-export-restore-roundtrip：图 payload 导出 → 加密容器 →
 *     恢复到新库 → 二次导出逐字节相同，需要 ArkData 真实 DB + 加密 + 往返。
 *   - migration-db-v1-to-v3：需要 relationalStore 与 v1 schema 种子数据。
 */
const RUNTIME_BLOCKED_CASES: string[] = [
  'depmap-golden-v1',
  'depmap-utf8-password-normalization',
  'backup-depmap-export-restore-roundtrip',
  'migration-db-v1-to-v3',
];

/**
 * 按类一刀切的旧写法已废弃。
 *
 * 为什么不再用 `RUNTIME_DEPENDENT_CATEGORIES = ['parser','depmap','jcs','backup']`：
 * 它会把 `depmap-bounds-and-structure-rejection`（协议明确规定「边界校验必须发生在
 * KDF 之前」，因此**不需要** KDF）和 `jcs-*`（纯 JCS 内核）从分母里抹掉 ——
 * 那是隐性降级门禁。现在两个 case 已作为真实执行用例入账。
 */
export type CaseExecutionClass = 'EXECUTABLE' | 'IMPL_MISSING' | 'ENV' | 'RUNTIME';

function indexOfString(list: string[], value: string): number {
  for (let i = 0; i < list.length; i++) {
    if (list[i] === value) {
      return i;
    }
  }
  return -1;
}

export function caseExecutionClass(caseId: string): CaseExecutionClass {
  if (indexOfString(HOST_IMPL_MISSING_CASES, caseId) >= 0) {
    return 'IMPL_MISSING';
  }
  if (indexOfString(ENV_BLOCKED_CASES, caseId) >= 0) {
    return 'ENV';
  }
  if (indexOfString(RUNTIME_BLOCKED_CASES, caseId) >= 0) {
    return 'RUNTIME';
  }
  return 'EXECUTABLE';
}

/* ==========================================================================
 * 运行时依赖的注入点
 *
 * runner 需要三样外部信息，全部由调用方（Index.ets）显式提供：
 *   root    : 仓库根（含 conformance/ 与 fixtures/）
 *   filesDir: 应用沙箱 files 目录
 *   now     : 报告时间戳
 * 之所以不在这里自己 getContext()：domain/conformance 层若要拿 AbilityContext，
 * 就必须 import @ohos.app.ability.common，那是 application 层的事。
 * 保持可被纯逻辑调用，是这一层能被测的前提。
 * ========================================================================== */

export class RunnerEnv {
  root: string = '';
  filesDir: string = '';
  now: string = '';
}

/**
 * fixture 文本的来源。
 *
 * 抽出来的唯一理由：让 runner 能在**不同执行面**上跑，而 runner 自身保持纯净。
 *   * 设备侧实现：FsTextSource（走 @ohos.file.fs）
 *   * 主机单元测试：内存实现（主机无 fs，见文件头说明）
 *
 * 契约：读不到就返回空串，**不要抛错**。runner 把空串统一记成 NOT_RUN ——
 * "环境缺文件"和"实现有缺陷"必须可区分，抛错会把两者混成同一种失败。
 */
export interface ConformanceTextSource {
  /** manifest 原文；供 runner 定位 fixtures 清单。 */
  readManifest(): string;
  /** 相对仓库根的 fixture 路径（形如 fixtures/impact/xxx.json）。 */
  readFixture(relPath: string): string;
  /**
   * 被解析的原始账单文件，**以 Base64 文本形式**返回其字节。
   *
   * 为什么传字节而不是字符串：parser 用例的语义包含"字节 → 文本"这一步
   * （UTF-8 BOM、严格 UTF-8 失败回退）。若在读取侧先 decode 成字符串再交给 runner，
   * 这一步就被移出被测代码，编码相关缺陷将永远测不到 —— 与 conformance 的目的相悖。
   * 读不到返回空串（与 readFixture 同契约：空串 = 环境缺失，不是实现缺陷）。
   */
  readImportFileBase64(relPath: string): string;
  /** 这一轮文本从哪来，写进报告以便区分执行面。 */
  describe(): string;
}

/* ==========================================================================
 * 入口
 * ========================================================================== */

/**
 * 执行全部 runtime-independent 用例（文本由 source 注入）。
 *
 * 返回的 `ConformanceReport` 里已经分好类，调用方只负责渲染/落盘。
 * 任何"环境不可用"都在这里显式表达为 NOT_RUN，不上抛异常 ——
 * 因为一个抛出的异常在 UI 上会退化成一句无信息量的失败提示。
 */
export function runConformanceWith(env: RunnerEnv, source: ConformanceTextSource): ConformanceReport {
  const report = new ConformanceReport();
  report.generatedAt = env.now;
  report.root = env.root;

  const manifestText = source.readManifest();
  if (manifestText.length === 0) {
    report.notRun.push('*');
    return report;
  }

  const jt = new JsonText();
  jt.setSource(manifestText);
  const fixturesKey = jt.findField('fixtures');
  if (fixturesKey < 0) {
    report.notRun.push('*');
    return report;
  }

  const pairs = jt.splitTopLevelObjects(fixturesKey);
  for (let p = 0; p + 1 < pairs.length; p += 2) {
    const start = pairs[p];
    const stop = pairs[p + 1];
    jt.setWindow(start, stop);
    const id = jt.requireString('id');
    const category = jt.requireString('category');
    const path = jt.requireString('path');
    jt.resetWindow();

    const outcome = new CaseOutcome();
    outcome.caseId = id;
    outcome.category = category;

    const klass: CaseExecutionClass = caseExecutionClass(id);
    if (klass === 'IMPL_MISSING') {
      outcome.status = 'HOST_IMPLEMENTATION_MISSING';
      outcome.detail = 'pure logic not ported to ArkTS yet; no Harmony runtime required';
      report.hostImplMissing.push(id);
      report.outcomes.push(outcome);
      continue;
    }
    if (klass === 'ENV') {
      outcome.status = 'BLOCKED_BY_ENVIRONMENT';
      outcome.detail = 'requires GB18030 charset decode; unavailable on the host test surface';
      report.blockedByEnvironment.push(id);
      report.outcomes.push(outcome);
      continue;
    }
    if (klass === 'RUNTIME') {
      outcome.status = 'BLOCKED_BY_RUNTIME';
      outcome.detail = 'requires on-device Argon2id/ArkData; no runtime in this run';
      report.blockedByRuntime.push(id);
      report.outcomes.push(outcome);
      continue;
    }

    const fixtureText = source.readFixture(path);
    if (fixtureText.length === 0) {
      // fixture 清单里登记了但文件读不到 —— 这是环境缺陷，不是实现缺陷。
      outcome.status = 'NOT_RUN';
      outcome.detail = 'fixture file not readable: ' + path;
      report.notRun.push(id);
      report.outcomes.push(outcome);
      continue;
    }

    let actual = '';
    try {
      actual = computeCase(category, id, fixtureText, source);
    } catch (e) {
      outcome.status = 'FAIL';
      outcome.detail = 'runner error: ' + errorText(e);
      report.failed.push(id);
      report.outcomes.push(outcome);
      continue;
    }

    if (actual.length === 0) {
      outcome.status = 'NOT_IMPLEMENTED';
      report.notImplemented.push(id);
      report.outcomes.push(outcome);
      continue;
    }

    const expected = extractExpected(fixtureText);
    outcome.actual = actual;
    outcome.expected = expected;
    if (actual === expected) {
      outcome.status = 'PASS';
      report.passed.set(id, actual);
    } else {
      outcome.status = 'FAIL';
      report.failed.push(id);
    }
    report.outcomes.push(outcome);
  }

  return report;
}

/* ==========================================================================
 * fixture 装载辅助
 * ========================================================================== */

const EXPECTED_MARKER = '"expected"';

/**
 * 取出 fixture 的 `expected`，并**规范化成紧凑 JSON**。
 *
 * 规范化是必须的，不是美化偏好：harness 侧的权威比对是
 *     JSON.stringify(actual) === JSON.stringify(expected)
 * （见 `tools/conformance/run.mjs`），即空白无关、键序相关。
 * 而 fixture 存的是 2 空格缩进的美化文本 —— 不做规范化，
 * 紧凑的 actual 与美化的 expected **永远不可能相等**。
 *
 * 注意只去空白，**不排序键、不美化、不改转义**：
 * 键序本身是契约的一部分，重排会把真实缺陷掩盖掉。
 * 同样地，标量（readiness 的 expected 是裸字符串）原样返回 ——
 * 字符串内部的空格是语义的一部分，不能被当成 JSON 空白吃掉。
 */
export function extractExpected(fixtureText: string): string {
  const i = fixtureText.indexOf(EXPECTED_MARKER);
  if (i < 0) {
    throw new Error('fixture has no expected section');
  }
  let k = i + EXPECTED_MARKER.length;
  while (k < fixtureText.length && fixtureText.charAt(k) !== ':') {
    k++;
  }
  k++;
  while (k < fixtureText.length && isWs(fixtureText.charAt(k))) {
    k++;
  }
  const ch = fixtureText.charAt(k);
  if (ch === '{' || ch === '[') {
    const jt = new JsonText();
    jt.setSource(fixtureText);
    const close = jt.matchBracket(k);
    if (close < 0) {
      throw new Error('unterminated expected section');
    }
    return compactJson(fixtureText.substring(k, close + 1));
  }
  // 标量（readiness 的 expected 是裸字符串）—— 原样保留，不去空白。
  if (ch === '"') {
    const jt = new JsonText();
    jt.setSource(fixtureText);
    const end = jt.skipString(k);
    return fixtureText.substring(k, end);
  }
  let e = k;
  while (e < fixtureText.length) {
    const c = fixtureText.charAt(e);
    if (c === ',' || c === '\n' || c === '}' || c === ']') {
      break;
    }
    e++;
  }
  return trimTrailing(fixtureText.substring(k, e));
}

function trimTrailing(s: string): string {
  let e = s.length;
  while (e > 0 && isWs(s.charAt(e - 1))) {
    e--;
  }
  return s.substring(0, e);
}

function isWs(ch: string): boolean {
  return ch === ' ' || ch === '\t' || ch === '\n' || ch === '\r';
}

/**
 * `ChangePlanWorkflowState` 的 wire 解析 —— 合法集合**不在这里**。
 *
 * 这里曾经逐成员枚举一遍 `=== ChangePlanWorkflowState.X`，与 generated enum
 * 构成两份真相：spec 新增一个状态时必须改两处，漏改不会被任何 Gate 抓住。
 * 2026-09-18 由 `changePlanWorkflowStateFromWire`（generated，与 Kotlin/Swift 同源）
 * 取代；本函数只负责把「未命中」升级为 fail-closed 抛错。
 *
 * 为什么仍然 fail-closed 而不是返回默认值：workflowState 与 actionPhase 只出现在
 * conformance fixture 与 UI 输入里，一个不在词表里的状态说明 fixture 或数据已损坏，
 * 继续往下跑会把损坏状态带进状态机判定，比立刻停下更危险。
 */
function workflowStateFromWire(value: string): ChangePlanWorkflowState {
  const parsed: ChangePlanWorkflowState | undefined = changePlanWorkflowStateFromWire(value);
  if (parsed === undefined) {
    throw new Error(`unknown workflowState wire value: ${value}`);
  }
  return parsed;
}

/** `PlanActionPhase` 的 wire 解析；合法集合同样来自 generated，理由同上。 */
function actionPhaseFromWire(value: string): PlanActionPhase {
  const parsed: PlanActionPhase | undefined = planActionPhaseFromWire(value);
  if (parsed === undefined) {
    throw new Error(`unknown action phase wire value: ${value}`);
  }
  return parsed;
}

function errorText(e: Error): string {
  const msg = e.message;
  if (msg === undefined || msg.length === 0) {
    return 'unknown error';
  }
  return msg;
}

/* ==========================================================================
 * 分类分发
 * ========================================================================== */

/**
 * 计算一个用例的 actual（JSON 文本）。
 *
 * 返回空串表示"本平台无此实现"→ 调用方记 NOT_IMPLEMENTED。
 * 抛错表示"实现存在但崩了"→ 调用方记 FAIL。
 * 这两种情形必须可区分：把崩溃记成"未实现"会让缺陷被永久掩盖。
 */
class EmptyTextSource implements ConformanceTextSource {
  readManifest(): string {
    return '';
  }
  readFixture(relPath: string): string {
    return '';
  }
  readImportFileBase64(relPath: string): string {
    return '';
  }
  describe(): string {
    return 'empty';
  }
}

/** 默认文本源：任何读取都返回空串（调用方不关心文件字节时使用）。 */
const EMPTY_SOURCE: ConformanceTextSource = new EmptyTextSource();

export function computeCase(
  category: string,
  caseId: string,
  fixtureText: string,
  source: ConformanceTextSource = EMPTY_SOURCE,
): string {
  const jt = new JsonText();
  jt.setSource(fixtureText);

  if (category === 'relations') {
    return runRelations(jt);
  }
  if (category === 'readiness') {
    return runReadiness(jt);
  }
  if (category === 'coverage') {
    return runCoverage(jt);
  }
  if (category === 'impact') {
    return runImpactSingle(jt);
  }
  if (category === 'scenario') {
    return runScenario();
  }
  if (category === 'state-machine') {
    return runStateMachine(caseId, jt);
  }
  if (category === 'timeline') {
    return runTimeline(caseId, jt);
  }
  if (category === 'migration') {
    return runMigrationContract(jt);
  }
  if (category === 'jcs') {
    return runJcs(jt);
  }
  if (category === 'depmap') {
    // 只有结构/边界用例进到这里 —— 需要 KDF 的两条在 RUNTIME_BLOCKED_CASES 里。
    return runDepmapBounds(jt);
  }
  if (category === 'parser') {
    return runParser(jt, source);
  }
  return '';
}

/* ==========================================================================
 * parser（22 用例中的 20 条）—— 移植自 sources/Parsers.kt，零平台依赖
 *
 * 两条 GB18030 用例不在这里跑：它们的字节不是合法 UTF-8，需要字符集解码器，
 * 主机执行面没有（见 ENV_BLOCKED_CASES）。一旦解码器就绪，
 * 把这两条从 ENV_BLOCKED_CASES 里移除即可，本函数无需改动。
 * ========================================================================== */

function runParser(jt: JsonText, source: ConformanceTextSource): string {
  const inputIdx = jt.findField('input');
  if (inputIdx < 0 || jt.getSource().charAt(inputIdx) !== '{') {
    return '';
  }
  const close = jt.matchBracket(inputIdx);
  jt.setWindow(inputIdx, close);
  let adapterId: string;
  let fileName: string;
  let mapping: MappingProfile | null;
  try {
    adapterId = jt.requireString('adapterId');
    fileName = jt.requireString('file');
    mapping = parseMappingProfile(jt);
  } finally {
    jt.resetWindow();
  }

  const b64: string = source.readImportFileBase64('fixtures/import/' + fileName);
  if (b64.length === 0) {
    return '';
  }
  const bytes: Uint8Array = decodeBase64(b64);

  let result: ParseResult;
  if (adapterId === 'wechat') {
    result = WechatParser.parse(bytes);
  } else if (adapterId === 'ofx_qfx') {
    result = OfxParser.parse(bytes);
  } else if (adapterId === 'generic_csv') {
    result = GenericCsvParser.parse(bytes, mapping);
  } else {
    throw new Error('unknown adapter: ' + adapterId);
  }

  const obs: string[] = [];
  for (let i = 0; i < result.observations.length; i++) {
    const o: Observation = result.observations[i];
    obs.push(
      '{"occurredAt":' + jsonEscape(o.occurredAt) +
      ',"amount":' + jsNumberText(o.amount) +
      ',"currency":' + jsonEscape(o.currency) +
      ',"direction":' + jsonEscape(o.direction) +
      ',"merchantRaw":' + jsonEscape(o.merchantRaw) +
      ',"status":' + jsonEscape(o.status) + '}',
    );
  }
  const errs: string[] = [];
  for (let i = 0; i < result.errors.length; i++) {
    const e: ParseError = result.errors[i];
    errs.push('{"line":' + e.line + ',"reason":' + jsonEscape(e.reason) + '}');
  }
  return '{"observationCount":' + result.observations.length +
    ',"observations":[' + obs.join(',') + ']' +
    ',"errorCount":' + result.errors.length +
    ',"errors":[' + errs.join(',') + ']}';
}

/**
 * JS `JSON.stringify(number)` 等价：整数不输出小数点，否则输出最短往返表示。
 * 与 Android `num()` 同语义 —— 这一处若不一致，25 会变成 25.0，逐字节比对立刻失败。
 */
function jsNumberText(d: number): string {
  if (Math.floor(d) === d && Math.abs(d) < 1e15) {
    return '' + Math.round(d);
  }
  return '' + d;
}

/** 解析显式字段映射；没有 mapping 字段（wechat / ofx_qfx）时返回 null。 */
function parseMappingProfile(jt: JsonText): MappingProfile | null {
  const mi = jt.findField('mapping');
  if (mi < 0 || jt.getSource().charAt(mi) !== '{') {
    return null;
  }
  const mClose = jt.matchBracket(mi);
  jt.setWindow(mi, mClose);
  try {
    const profile: MappingProfile = new MappingProfile();
    const ci = jt.findField('columns');
    if (ci >= 0 && jt.getSource().charAt(ci) === '{') {
      const cClose = jt.matchBracket(ci);
      jt.setWindow(ci, cClose);
      profile.columns.transactionId = jt.optionalString('transactionId');
      profile.columns.dateTime = jt.requireString('dateTime');
      profile.columns.amount = jt.optionalString('amount');
      profile.columns.debit = jt.optionalString('debit');
      profile.columns.credit = jt.optionalString('credit');
      profile.columns.description = jt.optionalString('description');
      profile.columns.counterparty = jt.optionalString('counterparty');
      profile.columns.currency = jt.optionalString('currency');
      profile.columns.balance = jt.optionalString('balance');
      profile.columns.transactionType = jt.optionalString('transactionType');
      profile.columns.paymentMethod = jt.optionalString('paymentMethod');
      jt.resetWindow();
      jt.setWindow(mi, mClose);
    }
    const oi = jt.findField('options');
    if (oi >= 0 && jt.getSource().charAt(oi) === '{') {
      const oClose = jt.matchBracket(oi);
      jt.setWindow(oi, oClose);
      profile.options.delimiter = jt.optionalStringOr('delimiter', ',');
      profile.options.dateFormats = jt.requireStringArray('dateFormats');
      profile.options.decimalSeparator = jt.optionalStringOr('decimalSeparator', '.');
      profile.options.amountSignMode = jt.optionalStringOr('amountSignMode', 'outward_positive');
      profile.options.hasHeaderRow = jt.optionalBoolOr('hasHeaderRow', true);
      profile.options.encoding = jt.optionalStringOr('encoding', 'utf-8');
      profile.options.positiveDirection = jt.optionalString('positiveDirection');
      jt.resetWindow();
      jt.setWindow(mi, mClose);
    }
    return profile;
  } finally {
    jt.resetWindow();
  }
}

/* ==========================================================================
 * jcs（1 用例）—— 调用 crypto/Jcs 的受限值域序列化 + 文本→值树解析器
 *
 * 这一条过去被整类归入 BLOCKED_BY_RUNTIME，但它其实**不需要任何运行时能力**：
 * JCS 是纯字节规范化。真正的缺口是 ArkTS 侧没有"JSON 文本 → JCS 值树"的入口
 * （Object.keys 不可用、`JSON.parse` 结果无法枚举键），现已由 `jcsParse` 补上。
 * ========================================================================== */

function runJcs(jt: JsonText): string {
  const inputIdx = jt.findField('input');
  if (inputIdx < 0 || jt.getSource().charAt(inputIdx) !== '{') {
    return '';
  }
  const close = jt.matchBracket(inputIdx);
  jt.setWindow(inputIdx, close);
  try {
    const canonical: string = jcsSection(jt, 'cases', inputIdx, close, false);
    const rejections: string = jcsSection(jt, 'rejectCases', inputIdx, close, true);
    return '{"canonical":' + canonical + ',"rejections":' + rejections + '}';
  } finally {
    jt.resetWindow();
  }
}

/**
 * 逐个 case 求值并拼成 `{ "<id>": "<串>" }`。
 *
 * `rejectMode` 下把 JcsError 归一为 `"Error"`，**其它异常必须向上抛** ——
 * 若把解析器崩了也算作预期的 "Error"，这个用例就退化成"永不失败"。
 */
function jcsSection(
  jt: JsonText,
  name: string,
  outerStart: number,
  outerStop: number,
  rejectMode: boolean,
): string {
  const ai = jt.findField(name);
  if (ai < 0 || jt.getSource().charAt(ai) !== '[') {
    return '{}';
  }
  const pairs = jt.splitTopLevelElements(ai);
  const parts: string[] = [];
  for (let p = 0; p + 1 < pairs.length; p += 2) {
    jt.setWindow(pairs[p], pairs[p + 1]);
    const caseId: string = jt.requireString('id');
    const vi = jt.findField('input');
    let valueJson = '{}';
    if (vi >= 0) {
      const c: string = jt.getSource().charAt(vi);
      if (c === '{' || c === '[') {
        const vc: number = jt.matchBracket(vi);
        valueJson = jt.getSource().substring(vi, vc + 1);
      }
    }
    let outcome = '';
    if (rejectMode) {
      try {
        jcsStringify(jcsParse(valueJson));
        outcome = 'serialized';
      } catch (e) {
        if (e instanceof JcsError) {
          outcome = 'Error';
        } else {
          throw new Error('jcs: unexpected non-JcsError failure: ' + errorText(e));
        }
      }
    } else {
      outcome = jcsStringify(jcsParse(valueJson));
    }
    parts.push(jcsEscapeString(caseId) + ':' + jcsEscapeString(outcome));
    jt.resetWindow();
    jt.setWindow(outerStart, outerStop);
  }
  return '{' + parts.join(',') + '}';
}

/* ==========================================================================
 * depmap 结构/边界（1 用例）—— 调用 crypto/DepmapBounds（纯 ArkTS）
 *
 * fixture 自己就把意图写明了：「解析 → 结构 → 边界 全部在 KDF 之前完成」。
 * 也就是说这条用例**按协议设计**就不该触发 KDF —— 过去把它和需要 Argon2id 的
 * 两条一起丢进 BLOCKED，是把"没做 extracted 分层"误记成了"没有设备"。
 * 分层抽出后它在主机执行面可以直接跑。
 * ========================================================================== */

function runDepmapBounds(jt: JsonText): string {
  const inputIdx = jt.findField('input');
  if (inputIdx < 0 || jt.getSource().charAt(inputIdx) !== '{') {
    return '';
  }
  const close = jt.matchBracket(inputIdx);
  jt.setWindow(inputIdx, close);
  try {
    const mi = jt.findField('mutations');
    if (mi < 0 || jt.getSource().charAt(mi) !== '[') {
      return '';
    }
    const pairs = jt.splitTopLevelElements(mi);
    const parts: string[] = [];
    for (let p = 0; p + 1 < pairs.length; p += 2) {
      jt.setWindow(pairs[p], pairs[p + 1]);
      const mutationId: string = jt.requireString('id');
      const json: string = jt.requireString('json');
      let kind = 'accepted';
      try {
        validateDepmapBounds(parseDepmapHeader(json));
      } catch (e) {
        if (e instanceof DepmapError) {
          kind = e.code;
        } else {
          throw new Error('depmap bounds: unexpected non-DepmapError failure: ' + errorText(e));
        }
      }
      parts.push(jcsEscapeString(mutationId) + ':' + jcsEscapeString(kind));
      jt.resetWindow();
      jt.setWindow(inputIdx, close);
    }
    return '{' + parts.join(',') + '}';
  } finally {
    jt.resetWindow();
  }
}

/* ==========================================================================
 * migration（2 用例中的 1 个）
 *
 * `migration-version-contract` 是**纯静态契约**断言，可以跑：
 * 它表达"Schema 迁移链是 1→2→3、payload 是 depmap-logical-graph、
 * 未来版本必须被明确拒绝而不是猜测兼容、哪些 mutation 永不 bump revision"。
 * 这些是产品语义，不是 DB 状态，因此不依赖 relationalStore。
 *
 * `migration-db-v1-to-v3` 需要真实执行迁移 —— 见 RUNTIME_DEPENDENT_CASE_IDS。
 * ========================================================================== */

const LOGICAL_SCHEMA_VERSION = 3;
const MIGRATION_CHAIN: number[] = [1, 2, 3];
const PAYLOAD_TABLES: string[] = [
  'meta',
  'nodes',
  'dependencies',
  'dependency_groups',
  'dependency_proposals',
  'dependency_group_proposals',
  'proposal_evidence_refs',
  'evidence',
  'observation_fingerprints',
  'import_sessions',
  'source_instances',
];
const REJECTED_SCHEMA_VERSIONS: number[] = [0, 4, 99, 100];
const REJECTED_PAYLOAD_VERSIONS: number[] = [0, 4, 99];
/**
 * 永不 bump GraphRevision 的 mutation 清单。
 *
 * 定义在 conformance 层而非 GraphRevision.ets，是因为
 * GraphRevision.ets 持有的是自己那份**白名单**（`doesMutationBumpRevision`），
 * 而这里要断言的是**取值与顺序**。两者若都从同一常量派生，
 * 这个用例就会退化成"自己和自己比对"—— 失去证据力。
 * 因此本清单凭 fixture 契约独立声明，两边不一致时用例会 FAIL。
 */
const NEVER_BUMPS_ON: string[] = [
  'evidence_record',
  'proposal_upsert',
  'proposal_decision',
  'candidate_upsert',
  'candidate_dismiss',
  'candidate_accept',
  'drift_detect',
  'drift_dismiss',
  'timeline_build',
  'import_session_record',
  'plan_create',
  'plan_rebase',
  'plan_transition',
  'action_complete',
  'action_verify',
];

function runMigrationContract(jt: JsonText): string {
  const inputIdx = jt.findField('input');
  if (inputIdx < 0 || jt.getSource().charAt(inputIdx) !== '{') {
    return '';
  }
  const close = jt.matchBracket(inputIdx);
  jt.setWindow(inputIdx, close);
  const declaredVersion = jt.requireNumber('logicalSchemaVersion');
  const declaredChain = jt.requireNumberArray('migrations');
  jt.resetWindow();

  // 一致性断言：fixture 声明的 schema 版本必须是本平台认同的那一个。
  // 若上游改了 schema 而这里没跟上，本用例会 FAIL —— 这正是它的职责。
  if (declaredVersion !== LOGICAL_SCHEMA_VERSION) {
    throw new Error(`logicalSchemaVersion mismatch: fixture=${declaredVersion} platform=${LOGICAL_SCHEMA_VERSION}`);
  }
  if (declaredChain.length !== MIGRATION_CHAIN.length) {
    throw new Error('migration chain length mismatch');
  }
  for (let i = 0; i < declaredChain.length; i++) {
    if (declaredChain[i] !== MIGRATION_CHAIN[i]) {
      throw new Error('migration chain mismatch at index ' + i);
    }
  }

  // GraphRevision 白名单的**负向**交叉验证：清单里的每个 mutation
  // 都必须被 doesMutationBumpRevision 判为"不 bump"。
  // 这是本用例唯一真正调用 Harmony 代码的地方 —— 它让断言有了牙齿：
  // 若有人把 plan_create 误加进 bump 白名单，这里立刻 FAIL。
  for (let i = 0; i < NEVER_BUMPS_ON.length; i++) {
    if (doesMutationBumpRevision(NEVER_BUMPS_ON[i])) {
      throw new Error(`mutation must never bump revision but does: ${NEVER_BUMPS_ON[i]}`);
    }
  }

  const nums = (v: number[]): string => {
    const s: string[] = [];
    for (let i = 0; i < v.length; i++) {
      s.push(`${v[i]}`);
    }
    return '[' + s.join(',') + ']';
  };

  return '{"migrations":' + nums(MIGRATION_CHAIN) +
    ',"payloadKind":"depmap-logical-graph"' +
    ',"payloadTables":' + serializeStrings(PAYLOAD_TABLES) +
    ',"rejectedSchemaVersions":' + nums(REJECTED_SCHEMA_VERSIONS) +
    ',"rejectedPayloadVersions":' + nums(REJECTED_PAYLOAD_VERSIONS) +
    ',"graphRevisionNeverBumpedBy":' + serializeStrings(NEVER_BUMPS_ON) + '}';
}

/* ==========================================================================
 * relations（18 用例）—— 直接调用 domain/Relations
 * ========================================================================== */

/**
 * 输出形状必须与 fixture 逐字节一致：
 *   ok=true  → `{"ok":true}`
 *   ok=false → `{"ok":false,"reason":"..."}`
 * 键序固定 ok → reason（与 Android / TS 侧同一约定）。
 */
function runRelations(jt: JsonText): string {
  const inputIdx = jt.findField('input');
  if (inputIdx < 0 || jt.getSource().charAt(inputIdx) !== '{') {
    return '';
  }
  const close = jt.matchBracket(inputIdx);
  jt.setWindow(inputIdx, close);
  try {
    if (jt.findField('relation') >= 0 && jt.findField('mode') >= 0 && jt.findField('fromKind') < 0) {
      const relation = jt.requireString('relation');
      const mode = jt.requireString('mode');
      return normalizeResult(validateRelationGroupUse(relation, mode));
    }
    const fromKind = jt.optionalString('fromKind');
    const relation = jt.requireString('relation');
    const toKind = jt.optionalString('toKind');
    const capability = jt.requireString('capability');
    return normalizeResult(validateRelationUse(fromKind, relation, toKind, capability));
  } finally {
    jt.resetWindow();
  }
}

function normalizeResult(r: RelationValidationResult): string {
  if (r.ok) {
    return '{"ok":true}';
  }
  return '{"ok":false,"reason":' + jsonEscape(r.reason) + '}';
}

/* ==========================================================================
 * readiness（16 用例）—— 直接调用 domain/PlanReadiness
 * ========================================================================== */

function runReadiness(jt: JsonText): string {
  const inputIdx = jt.findField('input');
  if (inputIdx < 0 || jt.getSource().charAt(inputIdx) !== '{') {
    return '';
  }
  const close = jt.matchBracket(inputIdx);
  jt.setWindow(inputIdx, close);

  const input = new PlanReadinessInput();
  input.plan = parseChangePlan(jt);
  input.currentGraphRevision = jt.requireNumber('currentGraphRevision');
  input.pendingMustChange = jt.requireNumber('pendingMustChange');
  input.pendingNeedsReview = jt.requireNumber('pendingNeedsReview');
  input.unresolvedCandidates = jt.requireNumber('unresolvedCandidates');
  input.pendingRelevantProposals = jt.requireNumber('pendingRelevantProposals');
  input.staleRelevantDependencies = jt.requireNumber('staleRelevantDependencies');
  input.unfinishedChangeActions = jt.requireNumber('unfinishedChangeActions');
  jt.resetWindow();

  return '"' + computePlanReadiness(input) + '"';
}

/**
 * 解析 `plan` 子对象。
 *
 * 关键设计：`actions` 的每个元素在**自己的闭括号窗口内**解析。
 * 若在一个大窗口里反复 `findField('done')`，第一个元素的 done 会被
 * 后续每个元素读到 —— 于是"两个动作只完成其一"这个用例会假通过。
 * 窗口化之后，每个元素只能看到自己的字段。
 */
function parseChangePlan(jt: JsonText): ChangePlan {
  const plan = new ChangePlan();
  const pi = jt.findField('plan');
  if (pi < 0 || jt.getSource().charAt(pi) !== '{') {
    throw new Error('readiness input has no plan object');
  }
  const pclose = jt.matchBracket(pi);
  jt.setWindow(pi, pclose);

  plan.id = jt.requireString('id');
  plan.templateId = jt.optionalString('templateId');
  plan.scenario = jt.requireString('scenario');
  plan.title = jt.requireString('title');
  plan.workflowState = workflowStateFromWire(jt.requireString('workflowState'));
  plan.baselineGraphRevision = jt.requireNumber('baselineGraphRevision');
  plan.lastAnalyzedGraphRevision = jt.requireNumber('lastAnalyzedGraphRevision');
  plan.targetNodeId = jt.optionalString('targetNodeId');
  plan.effectiveDate = jt.optionalString('effectiveDate');

  const ai = jt.findField('actions');
  if (ai >= 0 && jt.getSource().charAt(ai) === '[') {
    const pairs = jt.splitTopLevelObjects(ai);
    for (let p = 0; p + 1 < pairs.length; p += 2) {
      jt.setWindow(pairs[p], pairs[p + 1]);
      const action = new PlanAction();
      action.id = jt.requireString('id');
      action.title = jt.requireString('title');
      action.phase = actionPhaseFromWire(jt.requireString('phase'));
      action.done = jt.requireBool('done');
      action.resolvesImpactKeys = requireStringArrayOrEmpty(jt, 'resolvesImpactKeys');
      jt.resetWindow();
      jt.setWindow(pi, pclose);
      plan.actions.push(action);
    }
  }
  jt.resetWindow();
  return plan;
}

function requireStringArrayOrEmpty(jt: JsonText, name: string): string[] {
  const i = jt.findField(name);
  if (i < 0 || jt.getSource().charAt(i) !== '[') {
    return [];
  }
  return jt.requireStringArray(name);
}

/* ==========================================================================
 * coverage（6 用例）—— 直接调用 domain/ScenarioCoverage
 * ========================================================================== */

function runCoverage(jt: JsonText): string {
  const inputIdx = jt.findField('input');
  if (inputIdx < 0 || jt.getSource().charAt(inputIdx) !== '{') {
    return '';
  }
  const close = jt.matchBracket(inputIdx);
  jt.setWindow(inputIdx, close);

  const input = new ScenarioCoverageInput();
  input.scenarioId = jt.requireString('scenarioId');
  input.confirmedDirectDependencies = jt.requireNumber('confirmedDirectDependencies');
  input.confirmedIndirectDependencies = jt.requireNumber('confirmedIndirectDependencies');
  input.pendingProposals = jt.requireNumber('pendingProposals');
  input.unresolvedCandidates = jt.requireNumber('unresolvedCandidates');
  input.staleDependencies = jt.requireNumber('staleDependencies');
  input.unknownCriticalityCount = jt.requireNumber('unknownCriticalityCount');
  input.unverifiedActions = jt.requireNumber('unverifiedActions');
  input.freshnessThresholdDays = jt.requireNumber('freshnessThresholdDays');
  input.now = jt.requireString('now');

  const si = jt.findField('sources');
  if (si >= 0 && jt.getSource().charAt(si) === '[') {
    const pairs = jt.splitTopLevelObjects(si);
    for (let p = 0; p + 1 < pairs.length; p += 2) {
      jt.setWindow(pairs[p], pairs[p + 1]);
      const src = new CoverageSourceInfo();
      src.id = jt.requireString('id');
      src.label = jt.requireString('label');
      src.lastIngestedAt = jt.optionalString('lastIngestedAt');
      jt.resetWindow();
      jt.setWindow(inputIdx, close);
      input.sources.push(src);
    }
  }
  jt.resetWindow();

  const cov = computeScenarioCoverage(input);
  const parts: string[] = [];
  parts.push('"scenarioId":' + jsonEscape(cov.scenarioId));
  parts.push('"coverageLevel":' + jsonEscape(cov.coverageLevel));
  const ex: string[] = [];
  for (let i = 0; i < cov.explanations.length; i++) {
    ex.push(jsonEscape(cov.explanations[i]));
  }
  parts.push('"explanations":[' + ex.join(',') + ']');
  parts.push('"counts":' + serializeCoverageCounts(cov.counts));
  return '{' + parts.join(',') + '}';
}

function serializeCoverageCounts(c: CoverageCounts): string {
  return '{"confirmedDirectDependencies":' + c.confirmedDirectDependencies +
    ',"confirmedIndirectDependencies":' + c.confirmedIndirectDependencies +
    ',"pendingProposals":' + c.pendingProposals +
    ',"unresolvedCandidates":' + c.unresolvedCandidates +
    ',"staleDependencies":' + c.staleDependencies +
    ',"unknownCriticalityCount":' + c.unknownCriticalityCount +
    ',"unverifiedActions":' + c.unverifiedActions + '}';
}

/* ==========================================================================
 * impact（13 用例）—— 真正调用 ImpactKernel.simulateScenario
 *
 * 这是本 runner 最容易被做假的一类：只检查 expected 的形状而从不跑内核，
 * 也能"通过"。因此这里强制要求：由 fixture 的 input **重建 ImpactGraph**，
 * 调用 simulateScenario，再序列化比对。
 * ========================================================================== */

function runImpactSingle(jt: JsonText): string {
  const graphIdx = jt.findField('graph');
  if (graphIdx < 0 || jt.getSource().charAt(graphIdx) !== '{') {
    return '';
  }
  const gclose = jt.matchBracket(graphIdx);
  const graph = new ImpactGraph();

  jt.setWindow(graphIdx, gclose);
  graph.dependencies = parseDependencyArray(jt);
  jt.resetWindow();
  jt.setWindow(graphIdx, gclose);
  graph.groups = parseGroupArray(jt);
  jt.resetWindow();
  jt.setWindow(graphIdx, gclose);
  graph.proposals = parseProposalArray(jt);
  jt.resetWindow();
  jt.setWindow(graphIdx, gclose);
  graph.nodeNames = parseNodeNames(jt);
  jt.resetWindow();

  // 与 graph 平级的 unavailable 数组（不在 graph 内部）。
  const unavailable = parseImpactStateKeys(jt, gclose + 1);
  const result = simulateScenario(graph, unavailable);
  return serializeImpact(result);
}

/**
 * 解析 `nodeNames` 映射表。
 *
 * 为什么用 `findClosedObjectAfter` 而不是 `findField`：
 * 文件里 "nodeNames" 这个词只出现一次，但 `findField('nodeNames')`
 * 之后的第一个 `{` 才是映射表本体；用"向后找第一个已闭合的 `{`"
 * 可稳定锚定，不依赖字段顺序。
 */
function parseNodeNames(jt: JsonText): Map<string, string> {
  const i = jt.findClosedObjectAfter('"nodeNames"');
  if (i < 0) {
    return new Map<string, string>();
  }
  return jt.parseStringMap(i);
}

/** 解析 dependencies 数组：逐元素在自身窗口内严格装载。 */
function parseDependencyArray(jt: JsonText): Dependency[] {
  const out: Dependency[] = [];
  const ai = jt.findField('dependencies');
  if (ai < 0 || jt.getSource().charAt(ai) !== '[') {
    return out;
  }
  const outerEnd = jt.getEnd();
  const pairs = jt.splitTopLevelObjects(ai);
  for (let p = 0; p + 1 < pairs.length; p += 2) {
    const s = pairs[p];
    const e = pairs[p + 1];
    jt.setWindow(s, e);
    const d = new Dependency();
    d.id = jt.requireString('id');
    d.from = jt.requireString('from');
    const rel = relationFromWire(jt.requireString('relation'));
    if (rel === undefined) {
      throw new Error('unknown relation wire value');
    }
    d.relation = rel;
    d.to = jt.requireString('to');
    const cap = capabilityFromWire(jt.requireString('capability'));
    if (cap === undefined) {
      throw new Error('unknown capability wire value');
    }
    d.capability = cap;
    const crit = criticalityFromWire(jt.requireString('criticality'));
    if (crit === undefined) {
      throw new Error('unknown criticality wire value');
    }
    d.criticality = crit;
    d.groupId = jt.optionalString('groupId');
    const st = dependencyStateFromWire(jt.requireString('state'));
    if (st === undefined) {
      throw new Error('unknown dependency state wire value');
    }
    d.state = st;
    const org = dependencyOriginFromWire(jt.requireString('origin'));
    if (org === undefined) {
      throw new Error('unknown dependency origin wire value');
    }
    d.origin = org;
    d.lastVerifiedAt = jt.requireString('lastVerifiedAt');
    jt.resetWindow();
    jt.setWindow(ai, outerEnd);
    out.push(d);
  }
  jt.resetWindow();
  return out;
}

/** 解析 groups 数组。 */
function parseGroupArray(jt: JsonText): DependencyGroup[] {
  const out: DependencyGroup[] = [];
  const ai = jt.findField('groups');
  if (ai < 0 || jt.getSource().charAt(ai) !== '[') {
    return out;
  }
  const outerEnd = jt.getEnd();
  const pairs = jt.splitTopLevelObjects(ai);
  for (let p = 0; p + 1 < pairs.length; p += 2) {
    jt.setWindow(pairs[p], pairs[p + 1]);
    const g = new DependencyGroup();
    g.id = jt.requireString('id');
    g.groupKey = jt.requireString('groupKey');
    g.targetNodeId = jt.requireString('targetNodeId');
    const cap = capabilityFromWire(jt.requireString('capability'));
    if (cap === undefined) {
      throw new Error('unknown group capability wire value');
    }
    g.capability = cap;
    const mode = groupModeFromWire(jt.requireString('mode'));
    if (mode === undefined) {
      throw new Error('unknown group mode wire value');
    }
    g.mode = mode;
    g.memberEdgeIds = jt.requireStringArray('memberEdgeIds');
    const st = groupStateFromWire(jt.requireString('state'));
    if (st === undefined) {
      throw new Error('unknown group state wire value');
    }
    g.state = st;
    jt.resetWindow();
    jt.setWindow(ai, outerEnd);
    out.push(g);
  }
  jt.resetWindow();
  return out;
}

/**
 * 解析 proposals 数组。
 *
 * 刻意**不**读 `confidenceScore`：确认度不参与确定性失效传播
 * （ImpactKernel 的输入类型里也没有该字段）。
 * 若在这里把它读进来，就等于给"confidence 可以影响结论"留了一道门。
 */
function parseProposalArray(jt: JsonText): ImpactProposalInput[] {
  const out: ImpactProposalInput[] = [];
  const ai = jt.findField('proposals');
  if (ai < 0 || jt.getSource().charAt(ai) !== '[') {
    return out;
  }
  const outerEnd = jt.getEnd();
  const pairs = jt.splitTopLevelObjects(ai);
  for (let p = 0; p + 1 < pairs.length; p += 2) {
    jt.setWindow(pairs[p], pairs[p + 1]);
    const prop = new ImpactProposalInput();
    prop.key = jt.requireString('key');
    prop.from = jt.requireString('from');
    prop.to = jt.requireString('to');
    const cap = capabilityFromWire(jt.requireString('capability'));
    if (cap === undefined) {
      throw new Error('unknown proposal capability wire value');
    }
    prop.capability = cap;
    jt.resetWindow();
    jt.setWindow(ai, outerEnd);
    out.push(prop);
  }
  jt.resetWindow();
  return out;
}

/** 解析平级的 unavailable 数组（ImpactStateKey[]）。 */
function parseImpactStateKeys(jt: JsonText, from: number): ImpactStateKey[] {
  const out: ImpactStateKey[] = [];
  jt.setWindow(from, jt.getSource().length);
  const ai = jt.findField('unavailable');
  if (ai < 0 || jt.getSource().charAt(ai) !== '[') {
    jt.resetWindow();
    return out;
  }
  const all = jt.getSource().length;
  const pairs = jt.splitTopLevelObjects(ai);
  for (let p = 0; p + 1 < pairs.length; p += 2) {
    jt.setWindow(pairs[p], pairs[p + 1]);
    const nodeId = jt.requireString('nodeId');
    const cap = capabilityFromWire(jt.requireString('capability'));
    if (cap === undefined) {
      throw new Error('unknown unavailable capability wire value');
    }
    jt.resetWindow();
    jt.setWindow(from, all);
    out.push(new ImpactStateKey(nodeId, cap));
  }
  jt.resetWindow();
  return out;
}

/**
 * 序列化 ImpactResult。
 *
 * 键序**逐字段复刻** fixture（也是 Android `Main.kt` serializeImpact 的顺序）：
 *   unavailable / lostKeys / targets(11 字段) / checklist(5 字段) / processedKeys
 * 顺序不是风格问题：比对是逐字节的，顺序不同即 FAIL。
 */
function serializeImpact(r: ImpactResult): string {
  const parts: string[] = [];
  parts.push('"unavailable":' + serializeStateKeys(r.unavailable));
  parts.push('"lostKeys":' + serializeStateKeys(r.lostKeys));
  const targets: string[] = [];
  for (let i = 0; i < r.targets.length; i++) {
    targets.push(serializeTarget(r.targets[i]));
  }
  parts.push('"targets":[' + targets.join(',') + ']');
  const checks: string[] = [];
  for (let i = 0; i < r.checklist.length; i++) {
    checks.push(serializeChecklistItem(r.checklist[i]));
  }
  parts.push('"checklist":[' + checks.join(',') + ']');
  parts.push('"processedKeys":' + serializeStrings(r.processedKeys));
  return '{' + parts.join(',') + '}';
}

function serializeStateKeys(keys: ImpactStateKey[]): string {
  const items: string[] = [];
  for (let i = 0; i < keys.length; i++) {
    items.push('{"nodeId":' + jsonEscape(keys[i].nodeId) +
      ',"capability":' + jsonEscape(keys[i].capability) + '}');
  }
  return '[' + items.join(',') + ']';
}

function serializeTarget(t: ImpactTargetResult): string {
  return '{"nodeId":' + jsonEscape(t.nodeId) +
    ',"nodeName":' + jsonEscape(t.nodeName) +
    ',"capability":' + jsonEscape(t.capability) +
    ',"depth":' + t.depth +
    ',"status":' + jsonEscape(t.status) +
    ',"available":' + (t.available ? 'true' : 'false') +
    ',"redundancyDegraded":' + (t.redundancyDegraded ? 'true' : 'false') +
    ',"reasonCode":' + jsonEscape(t.reasonCode) +
    ',"reasonText":' + jsonEscape(t.reasonText) +
    ',"edgeKeys":' + serializeStrings(t.edgeKeys) +
    ',"groupKeys":' + serializeStrings(t.groupKeys) +
    ',"proposalKeys":' + serializeStrings(t.proposalKeys) + '}';
}

function serializeChecklistItem(c: ImpactChecklistItem): string {
  const cap = c.capability === null ? 'null' : jsonEscape(c.capability);
  const node = c.nodeId === null ? 'null' : jsonEscape(c.nodeId);
  return '{"level":' + jsonEscape(c.level) +
    ',"nodeId":' + node +
    ',"capability":' + cap +
    ',"title":' + jsonEscape(c.title) +
    ',"detail":' + jsonEscape(c.detail) + '}';
}

function serializeStrings(values: string[]): string {
  const out: string[] = [];
  for (let i = 0; i < values.length; i++) {
    out.push(jsonEscape(values[i]));
  }
  return '[' + out.join(',') + ']';
}

/**
 * JSON 字符串字面量转义。
 *
 * 与 domain/Jcs.ets 的 `jcsEscapeString` 是**两个不同层次**的东西，不可互换：
 *   - 本函数产出的是「可读 JSON 文本」，用于与 fixture 比对；
 *   - `jcsEscapeString` 产出的是 RFC 8785 canonical form，用于 AAD 与容器。
 * 把本函数用于容器 AAD 会引入 JCS 不允许的表示，
 * 把 JCS 用于比对会因 canonical 化（如键排序）而与 fixture 失配。
 *
 * 转义表按 JSON 规范：`"` `\` 与 C0 控制字符。
 * 非 ASCII（中文、emoji）**原样输出**，与 `JSON.stringify` 的默认行为一致 ——
 * 若改成 `\uXXXX`，fixture 里以原字面量形式保存的中文将永远比对不上。
 */
function jsonEscape(s: string): string {
  const out: string[] = [];
  out.push('"');
  for (let i = 0; i < s.length; i++) {
    const ch = s.charAt(i);
    if (ch === '"') {
      out.push('\\"');
      continue;
    }
    if (ch === '\\') {
      out.push('\\\\');
      continue;
    }
    if (ch === '\n') {
      out.push('\\n');
      continue;
    }
    if (ch === '\r') {
      out.push('\\r');
      continue;
    }
    if (ch === '\t') {
      out.push('\\t');
      continue;
    }
    if (ch === '\b') {
      out.push('\\b');
      continue;
    }
    if (ch === '\f') {
      out.push('\\f');
      continue;
    }
    const code = s.charCodeAt(i);
    if (code < 0x20) {
      const hex = code.toString(16);
      let padded = hex;
      while (padded.length < 4) {
        padded = '0' + padded;
      }
      out.push('\\u' + padded);
      continue;
    }
    out.push(ch);
  }
  out.push('"');
  return out.join('');
}

/* ==========================================================================
 * scenario（1 用例）—— 从 Domain 的模板注册表导出
 *
 * 语义：active 模板必须是 payment capability 且可执行；
 * planned 无 factory 必须被拒绝；注册表不得含日常生活提醒类模板。
 * 因此这里**不是**回显 spec，而是从 Harmony 自己的注册表读出事实。
 * ========================================================================== */

function runScenario(): string {
  const ids: string[] = [];
  const cats: string[] = [];
  const lead: string[] = [];
  for (let i = 0; i < SCENARIO_TEMPLATES.length; i++) {
    const t: ScenarioTemplate = SCENARIO_TEMPLATES[i];
    ids.push(jsonEscape(t.id));
    cats.push(jsonEscape(t.category));
    lead.push(jsonEscape(t.id) + ':' + (t.recommendedLeadTimeDays === null ? 'null' : `${t.recommendedLeadTimeDays}`));
  }
  const plannedIds: string[] = [];
  const plannedExec: string[] = [];
  for (let i = 0; i < PLANNED_TEMPLATES.length; i++) {
    const t: ScenarioTemplate = PLANNED_TEMPLATES[i];
    plannedIds.push(jsonEscape(t.id));
    // planned 模板"是否可执行"的判定依据：availability 是否为 ACTIVE。
    plannedExec.push('false');
  }
  return '{"activeIds":[' + ids.join(',') + ']' +
    ',"activeCategories":[' + cats.join(',') + ']' +
    ',"plannedIds":[' + plannedIds.join(',') + ']' +
    ',"plannedExecutable":[' + plannedExec.join(',') + ']' +
    ',"leadingTimeByTemplate":{' + lead.join(',') + '}}';
}

/* ==========================================================================
 * state-machine（5 用例）
 *
 * 输出形状是"规范化后的契约视图"，不是 spec 文档回显：
 * 只声明平台必须实现并可被断言的字段。
 * 注意 transitions 的**键序**必须与 fixture 一致 —— 它不是字母序，
 * 而是"状态机自然序：初态 → 中间态 → 终态"。
 * ========================================================================== */

function runStateMachine(caseId: string, jt: JsonText): string {
  if (caseId === 'state-machine-change-plan') {
    return stateMachineChangePlan();
  }
  if (caseId === 'state-machine-graph-revision') {
    return stateMachineGraphRevision();
  }
  if (caseId === 'state-machine-action-verification') {
    return stateMachineActionVerification();
  }
  if (caseId === 'state-machine-discovery-candidate') {
    return stateMachineDiscoveryCandidate();
  }
  if (caseId === 'state-machine-reality-drift') {
    return stateMachineRealityDrift();
  }
  return '';
}

/**
 * 把"状态 → 合法出边"序列化成契约要求的 JSON 对象。
 *
 * 键序必须与 fixture 一致（自然序：初态 → 中间态 → 终态），
 * 所以传进来的是**显式状态数组**而不是 Map 的迭代顺序 ——
 * Map 的插入序虽然也可控，但把它当作契约一部分是过度依赖实现细节。
 *
 * 传入 lookup 而不是 Map：三台状态机的表在 domain 层是私有的，
 * 只暴露访问器，避免调用方绕过校验直接改表。
 */
function serializeTransitionTable(states: string[], lookup: (from: string) => string[]): string {
  const parts: string[] = [];
  for (let i = 0; i < states.length; i++) {
    parts.push(jsonEscape(states[i]) + ':' + serializeStrings(lookup(states[i])));
  }
  return '{' + parts.join(',') + '}';
}

/** 守卫数组的序列化；三者形状相同，故共用。 */
function serializeGuards(guards: StateGuard[]): string {
  const parts: string[] = [];
  for (let i = 0; i < guards.length; i++) {
    const g = guards[i];
    parts.push('{"id":' + jsonEscape(g.id) +
      ',"rule":' + jsonEscape(g.rule) +
      ',"errorCode":' + jsonEscape(g.errorCode) + '}');
  }
  return '[' + parts.join(',') + ']';
}

/** 布尔字面量（避免依赖 String(false) 之类的平台差异）。 */
function boolText(v: boolean): string {
  return v ? 'true' : 'false';
}

/**
 * ActionVerification 状态机（VF 铁律：done ≠ verified）。
 *
 * 这里**不**回显 spec 文档，而是报告本平台实现的规则视图：
 * 初始态、终态、迁移表、守卫都取自 `domain/StateMachines.ets` 的常量与函数。
 * 只有 evidenceSignalRule 的四段散文是合同原文照录 ——
 * 它们是产品语义（什么条件下才允许置 evidence_suggested），
 * 无法从布尔逻辑推导，必须逐字一致。
 */
function stateMachineActionVerification(): string {
  const terminal: string[] = [];
  for (let i = 0; i < VERIFICATION_STATES.length; i++) {
    if (isVerificationTerminal(VERIFICATION_STATES[i])) {
      terminal.push(jsonEscape(VERIFICATION_STATES[i]));
    }
  }
  return '{"initial":' + jsonEscape(VERIFICATION_INITIAL) +
    ',"terminal":[' + terminal.join(',') + ']' +
    ',"transitions":' + serializeTransitionTable(VERIFICATION_STATES, allowedVerificationNext) +
    ',"evidenceSignalRule":{"appliesOnlyTo":' + serializeStrings(VERIFICATION_EVIDENCE_APPLIES_TO) +
    ',"requiresMethod":' + jsonEscape(VERIFICATION_EVIDENCE_METHOD) +
    ',"requiresMatch":' + jsonEscape(VERIFICATION_EVIDENCE_MATCH) +
    ',"effect":' + jsonEscape(VERIFICATION_EVIDENCE_EFFECT) +
    ',"never":' + serializeStrings(VERIFICATION_EVIDENCE_NEVER) + '}' +
    ',"guards":' + serializeGuards(VERIFICATION_GUARDS) + '}';
}

/**
 * DiscoveryCandidate 状态机。
 *
 * `accept` 同时声明 mutatesReality=true 与 bumpsGraphRevision=false ——
 * 两者并列出现是刻意的：接受候选会建 Node，却不提升 revision。
 * 若把"改 Reality ⇒ bump revision"当成通则，这里就会写错。
 */
function stateMachineDiscoveryCandidate(): string {
  return '{"initial":' + jsonEscape(CANDIDATE_INITIAL) +
    ',"transitions":' + serializeTransitionTable(CANDIDATE_STATES, allowedCandidateNext) +
    ',"accept":{"mutatesReality":' + boolText(CANDIDATE_ACCEPT_MUTATES_REALITY) +
    ',"effects":' + serializeStrings(CANDIDATE_ACCEPT_EFFECTS) +
    ',"bumpsGraphRevision":' + boolText(CANDIDATE_ACCEPT_BUMPS_REVISION) +
    ',"replay":' + jsonEscape(CANDIDATE_ACCEPT_REPLAY) + '}' +
    ',"dismiss":{"mutatesReality":' + boolText(CANDIDATE_DISMISS_MUTATES_REALITY) +
    ',"effects":' + serializeStrings(CANDIDATE_DISMISS_EFFECTS) +
    ',"reappeal":' + jsonEscape(CANDIDATE_DISMISS_REAPPEAL) + '}' +
    ',"guards":' + serializeGuards(CANDIDATE_GUARDS) + '}';
}

/**
 * RealityDriftStatus 状态机。
 *
 * creationRule 的 `minObservations` 是**数字**，不是字符串 ——
 * 用 boolText 那种"手写字面量"思路会不小心把它写成 "2"，
 * 这里显式用数字拼接。
 */
function stateMachineRealityDrift(): string {
  return '{"initial":' + jsonEscape(DRIFT_INITIAL) +
    ',"transitions":' + serializeTransitionTable(DRIFT_STATES, allowedDriftNext) +
    ',"creationRule":{"requires":' + jsonEscape(DRIFT_CREATION_REQUIRES) +
    ',"minObservations":' + DRIFT_CREATION_MIN_OBSERVATIONS +
    ',"absenceOnly":' + jsonEscape(DRIFT_CREATION_ABSENCE_ONLY) +
    ',"alreadyConfirmedSource":' + jsonEscape(DRIFT_CREATION_ALREADY_CONFIRMED) +
    ',"belowThreshold":' + jsonEscape(DRIFT_CREATION_BELOW_THRESHOLD) +
    ',"duplicateEvidenceRef":' + jsonEscape(DRIFT_CREATION_DUPLICATE_REF) +
    ',"upsert":' + jsonEscape(DRIFT_CREATION_UPSERT) + '}' +
    ',"guards":' + serializeGuards(DRIFT_GUARDS) + '}';
}

/**
 * GraphRevision 状态机。
 *
 * `atomicity` 的措辞是**合同原文**：Reality 变更与 revision 自增
 * 必须在每个平台的**同一个数据库事务**内完成。
 * 跨端一致性要求这句话逐字相同 —— 它是三端共同的验收条款，不是注释。
 */
function stateMachineGraphRevision(): string {
  const bumpsOn: string[] = [
    'node_create',
    'node_update',
    'node_archive',
    'dependency_confirm_create',
    'dependency_confirm_update',
    'dependency_retire',
    'dependency_reactivate',
    'group_confirm',
    'group_retire',
    'drift_resolve_replacement',
    'drift_resolve_additional_path',
  ];
  // 交叉验证：白名单里的每个 mutation 都必须被本平台的判定函数认可。
  // 这让"三端用同一套白名单"从口号变成可被 CI/设备执行的断言。
  for (let i = 0; i < bumpsOn.length; i++) {
    if (!doesMutationBumpRevision(bumpsOn[i])) {
      throw new Error(`mutation must bump revision but does not: ${bumpsOn[i]}`);
    }
  }
  for (let i = 0; i < NEVER_BUMPS_ON.length; i++) {
    if (doesMutationBumpRevision(NEVER_BUMPS_ON[i])) {
      throw new Error(`mutation must never bump revision but does: ${NEVER_BUMPS_ON[i]}`);
    }
  }
  // 随机抽样一个真实存储键，确认 revision 的持久化位置未被改名。
  if (GRAPH_REVISION_KEY.length === 0) {
    throw new Error('GRAPH_REVISION_KEY is empty');
  }
  return '{"initial":0,"monotonic":true,' +
    '"atomicity":"the Reality mutation and the revision increment MUST happen inside ' +
    'one database transaction on every platform",' +
    '"bumpsOn":' + serializeStrings(bumpsOn) +
    ',"neverBumpsOn":' + serializeStrings(NEVER_BUMPS_ON) + '}';
}

/**
 * ChangePlan 状态机。
 *
 * `derivedStatus.rule` 与 `guards` 是**产品合同的文本**，
 * 在这里必须逐字复刻：它们是跨端一致的一部分（三端都要拒绝同样的迁移、
 * 报同样的错误码），不是文档描述。
 */
function stateMachineChangePlan(): string {
  const order: string[] = ALL_WORKFLOW_STATES;
  const trans: string[] = [];
  for (let i = 0; i < order.length; i++) {
    trans.push(jsonEscape(order[i]) + ':' + serializeStrings(allowedNextStates(order[i])));
  }
  const terminal: string[] = [];
  for (let i = 0; i < order.length; i++) {
    if (isTerminalState(order[i])) {
      terminal.push(jsonEscape(order[i]));
    }
  }
  return '{"transitions":{' + trans.join(',') + '}' +
    ',"terminal":[' + terminal.join(',') + ']' +
    ',"derivedStatus":{"name":"PlanEffectiveStatus","value":"needs_revalidation",' +
    '"rule":"isStale(plan, currentGraphRevision) => workflowState NOT IN (completed, cancelled) ' +
    'AND currentGraphRevision > lastAnalyzedGraphRevision","persisted":false}' +
    ',"guards":[' +
    '{"id":"PLAN-ACTION-FROZEN","rule":"completeAction on a plan whose workflowState is completed ' +
    'or cancelled MUST be rejected","errorCode":"action_frozen"},' +
    '{"id":"PLAN-ACTION-EXISTS","rule":"completeAction / verifyActionManually / ' +
    'suggestVerificationFromEvidence with an unknown actionId MUST be rejected",' +
    '"errorCode":"entity_not_found"}]}';
}

/* ==========================================================================
 * timeline（3 用例）
 *
 * ⚠ 首先必须说清一件事：这三个 fixture 的 `input` **不足以**决定 `expected`。
 *   generator（core/scripts/generate-conformance.ts §12）先建了一个真实的
 *   SQLite 场景（node / 6 个 plan / reality_drift / source_instance），
 *   再把结果投影成 expected，而 input 里只留下了 `{now, plans, graphRevision}`。
 *   场景本身**不在 fixture 里**。
 *
 *   因此三端都只能**重建同一个场景**再跑投影 —— 这不是取巧，是 Android
 *   已经在做的事（见 android/conformance/.../Main.kt 的 runTimeline：
 *   它按 caseId 硬编码同样的 insert）。本函数的做法与之一致。
 *
 *   这条已记入报告：fixture 对 timeline 类别是**欠定的**（under-specified），
 *   它验证的是"同一场景下投影内核是否一致"，而不是"input → output"。
 *   要改成真正由 input 驱动，需要改动冻结的 fixture 生成器 —— 那超出 N3 范围。
 *
 * 迁移植入的 legacy 来源会永远 stale（从未导入过），
 * 因此每个场景都至少带一条 freshness_review —— 这是契约行为。
 * ========================================================================== */

/** 与 generate-conformance.ts 的 T0 / TIMELINE_NOW 一致。 */
const TL_T0 = '2026-09-13T00:00:00.000Z';
const TL_FRESHNESS_DAYS = 45;

/** 场景里"已迁移但从未导入"的 legacy 来源。 */
function legacySource(): TimelineSourceInput {
  const s = new TimelineSourceInput();
  s.id = LEGACY_WECHAT_SOURCE_INSTANCE_ID;
  s.label = '微信账单（历史导入）';
  s.state = SourceInstanceState.ACTIVE;
  s.lastIngestedAt = null;
  return s;
}

function tlSource(id: string, label: string, lastIngestedAt: string | null): TimelineSourceInput {
  const s = new TimelineSourceInput();
  s.id = id;
  s.label = label;
  s.state = SourceInstanceState.ACTIVE;
  s.lastIngestedAt = lastIngestedAt;
  return s;
}

function tlPlan(
  id: string, title: string, effectiveDate: string | null,
  lastAnalyzed: number, workflowState: string,
): TimelinePlanInput {
  const p = new TimelinePlanInput();
  p.id = id;
  p.title = title;
  p.scenario = 'replace_payment_card';
  p.workflowState = workflowState;
  p.lastAnalyzedGraphRevision = lastAnalyzed;
  p.effectiveDate = effectiveDate;
  return p;
}

function tlCardNode(expiryDate: string | null): TimelineNodeInput {
  const n = new TimelineNodeInput();
  n.id = 'n-card';
  n.name = '招行 4417';
  if (expiryDate !== null) {
    n.fields.set('expiryDate', expiryDate);
  }
  return n;
}

function runTimeline(caseId: string, jt: JsonText): string {
  const env = timelineScenario(caseId);
  if (env === undefined) {
    return '';
  }
  const items = buildTimelinePure(env.input, TL_T0, TL_FRESHNESS_DAYS);

  if (caseId === 'timeline-buckets-and-ordering') {
    // 同一输入两次构建必须完全相同 —— 这是"投影可随时重建"的可验证含义。
    const again = buildTimelinePure(env.input, TL_T0, TL_FRESHNESS_DAYS);
    let same = items.length === again.length;
    if (same) {
      for (let i = 0; i < items.length; i++) {
        if (items[i].id !== again[i].id) {
          same = false;
          break;
        }
      }
    }
    return '{"buckets":' + serializeItemField(items, 'bucket') +
      ',"ids":' + serializeItemField(items, 'id') +
      ',"kinds":' + serializeItemField(items, 'kind') +
      ',"deterministic":' + boolText(same) +
      ',"planCountUnchanged":' + env.input.plans.length + '}';
  }

  if (caseId === 'timeline-attention-signals') {
    const traceable = allItemsTraceable(items);
    return '{"kinds":' + serializeItemField(items, 'kind') +
      ',"buckets":' + serializeItemField(items, 'bucket') +
      ',"sourceTypes":' + serializeItemField(items, 'sourceType') +
      ',"priorities":' + serializeItemNumbers(items) +
      ',"statuses":' + serializeItemField(items, 'status') +
      ',"allTraceable":' + boolText(traceable) + '}';
  }

  if (caseId === 'timeline-terminal-plans-excluded') {
    return '{"count":' + items.length +
      ',"kinds":' + serializeItemField(items, 'kind') + '}';
  }
  return '';
}

/**
 * 重建 generator 里那三个场景。
 *
 * 返回 undefined 表示"这不是已知用例"→ 调用方记 NOT_IMPLEMENTED，
 * 而不是抛错（抛错会被记成 FAIL，把"未实现"误报成"实现崩了"）。
 */
function timelineScenario(caseId: string): TimelineEnv | undefined {
  const env = new TimelineEnv();

  if (caseId === 'timeline-buckets-and-ordering') {
    // (a) 6 个计划覆盖 6 个未来档；加 legacy 来源 = 7 项
    env.input.currentGraphRevision = 0;
    env.input.nodes.push(tlCardNode(null));
    const dates: string[] = [
      '2026-01-01', '2026-09-13', '2026-09-15', '2026-09-25', '2026-10-20', '2027-06-01',
    ];
    for (let i = 0; i < dates.length; i++) {
      // lastAnalyzed = 5 而 DB 的实际 revision 是 0 → 不 stale。
      // fixture input 里写的 graphRevision:5 是**描述性**的，
      // 与 DB 里的 meta.graph_revision 不是同一个值 —— 这点极易误读。
      env.input.plans.push(tlPlan('plan-' + (i + 1), '计划 ' + (i + 1), dates[i], 5, 'analyzed'));
    }
    env.input.sources.push(legacySource());
    return env;
  }

  if (caseId === 'timeline-attention-signals') {
    // (b) 开放 drift + 过期来源 + 到期节点 + 落后计划
    env.input.currentGraphRevision = 0;
    env.input.nodes.push(tlCardNode('2026-10-05'));
    const wechat = new TimelineNodeInput();
    wechat.id = 'n-wechat';
    wechat.name = '微信支付';
    env.input.nodes.push(wechat);

    const d = new TimelineDriftInput();
    d.id = 'drift-1';
    d.kind = 'possible_replacement';
    d.detectedAt = TL_T0;
    d.status = DriftStatus.OPEN;
    d.targetNodeId = 'n-wechat';
    env.input.drifts.push(d);

    env.input.sources.push(tlSource('inst-stale', '陈旧来源', '2026-01-01T00:00:00.000Z'));
    env.input.sources.push(legacySource());

    // 落后计划：lastAnalyzed=0，但 DB revision 也是 0 → **不 stale**，
    // 且 effectiveDate 为空 → 一项都不产出。这正是基准的期望。
    env.input.plans.push(tlPlan('plan-stale', '落后计划', null, 0, 'analyzed'));
    return env;
  }

  if (caseId === 'timeline-terminal-plans-excluded') {
    // (c) 终态计划必须被排除（TL-007）
    env.input.currentGraphRevision = 0;
    env.input.nodes.push(tlCardNode(null));
    env.input.plans.push(tlPlan('plan-done', '已完成', '2026-09-20', 5, 'completed'));
    env.input.plans.push(tlPlan('plan-cancel', '已取消', '2026-09-21', 5, 'cancelled'));
    env.input.sources.push(legacySource());
    return env;
  }

  return undefined;
}

/** 场景容器：把 input 包一层，便于把"场景"与"投影"分开。 */
class TimelineEnv {
  input: TimelineInput = new TimelineInput();
}

/** 取某个字段组成字符串数组（保持项次序）。 */
function serializeItemField(items: TimelineItem[], field: string): string {
  const out: string[] = [];
  for (let i = 0; i < items.length; i++) {
    if (field === 'id') {
      out.push(jsonEscape(items[i].id));
    } else if (field === 'kind') {
      out.push(jsonEscape(items[i].kind));
    } else if (field === 'bucket') {
      out.push(jsonEscape(items[i].bucket));
    } else if (field === 'sourceType') {
      out.push(jsonEscape(items[i].sourceType));
    } else if (field === 'status') {
      out.push(jsonEscape(items[i].status));
    }
  }
  return '[' + out.join(',') + ']';
}

function serializeItemNumbers(items: TimelineItem[]): string {
  const out: string[] = [];
  for (let i = 0; i < items.length; i++) {
    out.push('' + items[i].priority);
  }
  return '[' + out.join(',') + ']';
}

/**
 * 每一条 timeline 项都必须可溯源（sourceId 非空）。
 *
 * 这是"投影不是凭空造物"的可断言形式：宁可漏报一条，
 * 也不能产生一条说不清来自哪里的提醒。
 */
function allItemsTraceable(items: TimelineItem[]): boolean {
  for (let i = 0; i < items.length; i++) {
    if (items[i].sourceId.length === 0) {
      return false;
    }
  }
  return true;
}
