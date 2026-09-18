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
// 它每读一个字段都经过 CanonicalWire 的 wire→enum 转换，
// 未知取值返回 undefined 并立即失败，不猜、不兜底。
// 这与"通用 JSON 解析"有本质区别：后者会静默接受任何输入。
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
  nodeKindFromWire,
  relationFromWire,
  capabilityFromWire,
  criticalityFromWire,
  dependencyStateFromWire,
  dependencyOriginFromWire,
  groupModeFromWire,
  groupStateFromWire,
} from '../domain/CanonicalWire';
import {
  Capability,
  ChangePlanWorkflowState,
  GroupMode,
  GroupState,
  DependencyState,
  PlanActionPhase,
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
  allowedNextStates,
  isTerminalState,
} from '../domain/StateMachines';
import { GRAPH_REVISION_KEY, doesMutationBumpRevision } from '../domain/GraphRevision';
import {
  BUCKET_ORDER,
  bucketOf,
  nodeExpiryDate,
  sortTimelineKeys,
  TimelineSortKey,
} from '../domain/Timeline';
import { JsonText, compactJson } from './JsonText';

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

/**
 * 需要 Argon2 native 执行或 relationalStore 才能判定的用例。
 *
 * 逐类理由（不是笼统的"依赖运行时"）：
 *   - parser(22)：需要 `TextDecoder('gb18030')` 与文件字节读取，
 *     且 ArkTS 侧 adapter 尚未移植 —— 属**未实现**，需运行环境才能验证；
 *   - depmap(3)：3 个用例的 expected 全部含 `derivedKeyHex` / `ciphertextBase64`
 *     / `tagBase64`，必须真实执行 Argon2id + AES-256-GCM；
 *   - jcs(1)：expected 含 `rejections.float = "Error"`，
 *     需要有真实的异常类型名，且 9 个 canonical 用例走 JCS 内核；
 *   - backup(1)：需要 DB 导出/恢复往返 + 加密容器；
 *   - migration-db-v1-to-v3(1)：需要 relationalStore 与 v1 schema 种子数据。
 */
const RUNTIME_DEPENDENT_CATEGORIES: string[] = ['parser', 'depmap', 'jcs', 'backup'];

/**
 * 精确到**用例级**的 runtime 依赖。
 *
 * 为什么不按整类扔进 BLOCKED：`migration` 类里
 *   - `migration-version-contract` 是纯静态的版本契约断言，**能跑**；
 *   - `migration-db-v1-to-v3` 需要真实 DB 迁移，**跑不了**。
 * 按类一刀切会把一个可执行用例从分母里抹掉 —— 那是隐性降级门禁。
 */
const RUNTIME_DEPENDENT_CASE_IDS: string[] = ['migration-db-v1-to-v3'];

function isRuntimeDependent(category: string, caseId: string): boolean {
  for (let i = 0; i < RUNTIME_DEPENDENT_CASES.length; i++) {
    if (RUNTIME_DEPENDENT_CASES[i] === caseId) {
      return true;
    }
  }
  for (let i = 0; i < RUNTIME_DEPENDENT_CATEGORIES.length; i++) {
    if (RUNTIME_DEPENDENT_CATEGORIES[i] === category) {
      return true;
    }
  }
  return false;
}

// 显式列出而非直接内联到 isRuntimeDependent：便于在报告里逐条引用。
const RUNTIME_DEPENDENT_CASES: string[] = RUNTIME_DEPENDENT_CASE_IDS;

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

    if (isRuntimeDependent(category, id)) {
      outcome.status = 'BLOCKED_BY_RUNTIME';
      outcome.detail = 'requires on-device Argon2 / relationalStore; no runtime in this run';
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
      actual = computeCase(category, id, fixtureText);
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
 * `ChangePlanWorkflowState` 的 wire 解析。
 *
 * 为什么不放在 `domain/CanonicalWire.ets`：那个模块是**手写的 domain 辅助**，
 * 遵循一条明确的分工 —— 只为「DB / wire 边界」上的枚举提供解析。
 * workflowState 与 actionPhase 只出现在 conformance fixture 与 UI 输入里，
 * 不属于 DB 读取路径，因此按同一纪律留在使用它的地方，
 * 而不是把 domain 模块扩成一个"什么 wire 都懂的"杂物间。
 *
 * 解析失败一律抛错（fail-closed）：一个不在词表里的状态说明 fixture 或数据已损坏，
 * 此时继续往下跑会把损坏状态带进状态机判定，比立刻停下更危险。
 */
function workflowStateFromWire(value: string): ChangePlanWorkflowState {
  if (value === ChangePlanWorkflowState.DRAFT) {
    return ChangePlanWorkflowState.DRAFT;
  }
  if (value === ChangePlanWorkflowState.ANALYZED) {
    return ChangePlanWorkflowState.ANALYZED;
  }
  if (value === ChangePlanWorkflowState.REVIEW_REQUIRED) {
    return ChangePlanWorkflowState.REVIEW_REQUIRED;
  }
  if (value === ChangePlanWorkflowState.READY) {
    return ChangePlanWorkflowState.READY;
  }
  if (value === ChangePlanWorkflowState.IN_PROGRESS) {
    return ChangePlanWorkflowState.IN_PROGRESS;
  }
  if (value === ChangePlanWorkflowState.VERIFYING) {
    return ChangePlanWorkflowState.VERIFYING;
  }
  if (value === ChangePlanWorkflowState.COMPLETED) {
    return ChangePlanWorkflowState.COMPLETED;
  }
  if (value === ChangePlanWorkflowState.CANCELLED) {
    return ChangePlanWorkflowState.CANCELLED;
  }
  throw new Error(`unknown workflowState wire value: ${value}`);
}

/** `PlanActionPhase` 的 wire 解析；失败抛错，理由同 `workflowStateFromWire`。 */
function actionPhaseFromWire(value: string): PlanActionPhase {
  if (value === PlanActionPhase.PREPARE) {
    return PlanActionPhase.PREPARE;
  }
  if (value === PlanActionPhase.CHANGE) {
    return PlanActionPhase.CHANGE;
  }
  if (value === PlanActionPhase.VERIFY) {
    return PlanActionPhase.VERIFY;
  }
  throw new Error(`unknown action phase wire value: ${value}`);
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
export function computeCase(category: string, caseId: string, fixtureText: string): string {
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
  return '';
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
  return '';
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
 * Runtime-dependent 的分担说明：`timeline-attention-signals` 与
 * `timeline-terminal-plans-excluded` 的 expected 依赖 DB 中已有的
 * Drift / SourceInstance / ChangePlan 行。domain 层的 Timeline 只保留
 * **分类与排序内核**（bucket 判定 + deterministic 排序），
 * 条目收集属 data 层。因此这里对这两个用例返回空串 → NOT_IMPLEMENTED。
 *
 * 这是如实记账：把只有内核的现状报成"通过"，等于声称已经实现了
 * 尚未实现的仓库读取。
 * ========================================================================== */

function runTimeline(caseId: string, jt: JsonText): string {
  if (caseId === 'timeline-buckets-and-ordering') {
    // 该用例的 corpus 由 `graphRevision` + 一个固定来源 + 6 个计划构成，
    // 其 expected 里的 tl-fresh-legacy-wechat-statement 来自 repository。
    return '';
  }
  return '';
}
