// DO NOT EDIT 之外的语义 — 语义必须与 TS 冻结基准一致
// 端口自 core/src/services/timeline.ts（MVP03 §42–§45）中**纯投影**的部分。
//
// 分层约束：只依赖 `./PlanReadiness`（复用同一套"今天"口径）与
// `../generated/CanonicalEnums`，**不依赖任何 repository / DB**。
//
// 与本 TS 基准的一处**有意差异**（必须显式记录，不得被当作遗漏）：
//   原 buildTimeline() 从 ChangePlanRepository / RealityDriftRepository /
//   NodeRepository / SourceInstanceRepository 收集条目 —— 那是 data 层，
//   Harmony domain 层不得依赖 relationalStore。
//
//   2026-09-18 更新：这里**已经**提供完整的纯投影 `buildTimelinePure()`，
//   但它接收的是**调用方传入的**条目集合（IoArray<PlanRow> / drift / source 等），
//   而不是自己去读库。也就是说：
//     * 「给定同一批条目 → 同一 atypical now → 同一序列」这个不变式在 domain 层被保证；
//     * 「条目从哪来」仍属 application/data 层。
//   conformance runner 侧的 timeline 三个用例，是按 caseId **重建 fixture 场景**
//   后喂给本函数（见 HARMONY_N3_CONFORMANCE_REPORT.md §7.5.1 —— 那三个 fixture
//   的 input 本身不足以决定 expected，三端都是这么做的）。
//
// 语义定位（原文照录）：纯投影（derived read model）：
// 不是 Reality、不持久化、可随时重建。
// 只包含数字基础设施事项；排序 deterministic（bucket → priority → scheduledAt → id）。

import {
  ActionVerificationStatus,
  DriftStatus,
  PlanActionPhase,
  SourceInstanceState,
  TimelineBucket,
  TimelineItemKind,
  TimelineSourceType,
} from '../generated/CanonicalEnums';

const DAY_MS = 86400000;

/** bucket 的确定性次序。排序与 UI 分组都以此为准，不得各自定义。 */
export const BUCKET_ORDER: string[] = [
  TimelineBucket.ATTENTION,
  TimelineBucket.OVERDUE,
  TimelineBucket.TODAY,
  TimelineBucket.V7D,
  TimelineBucket.V30D,
  TimelineBucket.V90D,
  TimelineBucket.LATER,
];

/**
 * 判定一个时间点落在哪个 bucket。
 *
 * 两个 fail-safe 分支都归入 `attention`（而非某个"未来"档）：
 *   - scheduledAt 为 null（没有时间点，需要人看）
 *   - scheduledAt 不可解析（脏数据，需要人看）
 * 这与"缺少观察不能提升结论"一致：无法判定时，往**更需要人工注意**的方向收，
 * 而不是往"还有时间"的方向放。
 *
 * 边界一律用 UTC 日界（setUTCHours(0,0,0,0)）——
 * 若改用本地时区，同一份数据在不同时区的设备上会得到不同 bucket，
 * 跨端 conformance 就再也比不齐。
 */
export function bucketOf(scheduledAt: string | null, nowMs: number): string {
  if (scheduledAt === null || scheduledAt.length === 0) {
    return TimelineBucket.ATTENTION;
  }
  const t = Date.parse(scheduledAt);
  if (Number.isNaN(t)) {
    return TimelineBucket.ATTENTION;
  }
  const todayStart = startOfUtcDay(nowMs);
  if (t < todayStart) {
    return TimelineBucket.OVERDUE;
  }
  if (t < todayStart + DAY_MS) {
    return TimelineBucket.TODAY;
  }
  if (t < todayStart + 8 * DAY_MS) {
    return TimelineBucket.V7D;
  }
  if (t < todayStart + 31 * DAY_MS) {
    return TimelineBucket.V30D;
  }
  if (t < todayStart + 91 * DAY_MS) {
    return TimelineBucket.V90D;
  }
  return TimelineBucket.LATER;
}

/** UTC 当日 00:00 的毫秒时间戳。 */
export function startOfUtcDay(nowMs: number): number {
  const d = new Date(nowMs);
  d.setUTCHours(0, 0, 0, 0);
  return d.getTime();
}

export function bucketRank(bucket: string): number {
  for (let i = 0; i < BUCKET_ORDER.length; i++) {
    if (BUCKET_ORDER[i] === bucket) {
      return i;
    }
  }
  // 未知 bucket 排到最后：不隐藏它，但也不让它插到已知项前面。
  return BUCKET_ORDER.length;
}

/**
 * Timeline 条目的**可排序投影**。
 *
 * 与 TS TimelineItem 的字段一一对应，但去掉任何仓库耦合。
 * scheduledAt 用 null 表示"无时间点"，与 bucketOf 的语义一致。
 */
export class TimelineSortKey {
  id: string = '';
  kind: string = TimelineItemKind.NEEDS_ATTENTION;
  bucket: string = TimelineBucket.ATTENTION;
  /** 越小的 priority 越靠前（原文语义）。 */
  priority: number = 0;
  scheduledAt: string | null = null;
}

/**
 * deterministic 排序：bucket → priority → scheduledAt → id。
 *
 * ⚠ 2026-09-18 修正两处与 TS 基准**方向相反**的实现（由 timeline conformance
 * 的 fixture 反推确认，不是凭注释推断）：
 *
 *   1. **priority 是降序**。原文 `return b.priority - a.priority`：
 *      数值越**大**越靠前。本函数此前写成升序，且注释还写着"越小的越靠前"——
 *      注释与基准同时错了。证据：`timeline-attention-signals` 的 expected
 *      在同一 attention 桶内给出 priorities=[3,1,1]（drift 3 在 freshness 1 之前）。
 *   2. **scheduledAt 为 null 时按空串参与比较**（`?? ''`），因此
 *      **排在同时段有时间的项之前**。本函数此前把 null 当成 +Infinity
 *      （排在最后），方向相反。
 *
 * 最后用 id 兜底**不是**装饰：前三个键都可能相等（同一 bucket、
 * 同优先级、同一时间点），没有 id 兜底时排序结果依赖输入顺序，
 * 同一份数据在两次读取中可能给出不同序列 —— 那会让快照比较失效。
 *
 * ISO 日期串的字典序与其时间序一致，所以直接比较字符串即可
 * （不解析成数字，避免非法日期被 Date.parse 变成 NaN 而打乱比较）。
 */
export function sortTimelineKeys(items: TimelineSortKey[]): TimelineSortKey[] {
  const out: TimelineSortKey[] = [];
  for (let i = 0; i < items.length; i++) {
    out.push(items[i]);
  }
  out.sort((a: TimelineSortKey, b: TimelineSortKey): number => {
    const ra = bucketRank(a.bucket);
    const rb = bucketRank(b.bucket);
    if (ra !== rb) {
      return ra - rb;
    }
    if (a.priority !== b.priority) {
      return b.priority - a.priority;   // 降序：数值越大越靠前
    }
    const sa = a.scheduledAt === null ? '' : a.scheduledAt;
    const sb = b.scheduledAt === null ? '' : b.scheduledAt;
    if (sa !== sb) {
      return sa < sb ? -1 : 1;          // null → '' 排在前面
    }
    if (a.id < b.id) {
      return -1;
    }
    if (a.id > b.id) {
      return 1;
    }
    return 0;
  });
  return out;
}

/**
 * 从 node.fields 中取 expiryDate。
 *
 * 只有字符串且非空才算有到期日 —— 数字/对象/空串一律视为"没有"。
 * 这里**不**做隐式转换：把 `0` 或 `""` 当成有效日期会造出凭空的时间线事项。
 */
export function nodeExpiryDate(fields: Map<string, string>): string | null {
  const raw = fields.get('expiryDate');
  if (raw === undefined || raw.length === 0) {
    return null;
  }
  return raw;
}

/* ==========================================================================
 * 纯投影（in-memory collection）
 *
 * 2026-09-18 新增。本文件开头曾声明"条目收集属 data 层，尚未落地"，
 * 于是 3 个 timeline conformance 用例一直记 NOT_IMPLEMENTED。
 *
 * 这一节补的是**同一个投影**的纯函数版本：输入不是 SqliteDriver，
 * 而是一组已经读进内存的行。规则逐条照录
 * `core/src/services/timeline.ts` 的 buildTimeline()，一条不多一条不少。
 *
 * 为什么这不是"另写一份实现"：规则本身（哪些行产生哪个 kind / bucket /
 * priority / status）就是被 conformance 断言的对象，也就是要移植的**产品语义**。
 * 仓库读取方式（SQL vs 内存数组）不影响这些语义 —— 换读取方式不等于换语义。
 *
 * 与 TS 基准的一处**有意差异**（必须显式记录）：
 *   TS 版从 DB 现读 graph_revision 与各表；本版由调用方把
 *   `currentGraphRevision` 与各行一起传入。判定规则完全相同。
 * ========================================================================== */

/**
 * 迁移时植入的 legacy 来源实例 id。
 *
 * 与 `core/src/schema/migrations.ts` 的 LEGACY_WECHAT_SOURCE_INSTANCE_ID、
 * 以及 Android `Migrations.kt` 的同名常量保持一致。
 * 它会**永远**是一个 stale 来源（从未导入过数据），
 * 因此每个 DB 都至少产生一条 freshness_review —— 这是契约行为，不是缺陷。
 */
export const LEGACY_WECHAT_SOURCE_INSTANCE_ID: string = 'legacy-wechat-statement';

/** 单条 timeline 项。字段与 TS TimelineItem 对应，去掉仓库耦合。 */
export class TimelineItem {
  id: string = '';
  kind: string = TimelineItemKind.NEEDS_ATTENTION;
  bucket: string = TimelineBucket.ATTENTION;
  /** 越大越靠前（见 sortTimelineKeys 的修正说明）。 */
  priority: number = 0;
  scheduledAt: string | null = null;
  sourceType: string = '';
  sourceId: string = '';
  status: string = '';
  title: string = '';
  subtitle: string = '';
}

/** 动作的 verification 视图（只取投影用得到的字段）。 */
export class TimelineVerificationInput {
  status: string = '';
  method: string = '';
}

export class TimelineActionInput {
  id: string = '';
  title: string = '';
  phase: string = '';
  verification: TimelineVerificationInput | null = null;
}

export class TimelinePlanInput {
  id: string = '';
  title: string = '';
  scenario: string = '';
  workflowState: string = '';
  lastAnalyzedGraphRevision: number = 0;
  effectiveDate: string | null = null;
  actions: TimelineActionInput[] = [];
}

export class TimelineDriftInput {
  id: string = '';
  kind: string = '';
  detectedAt: string = '';
  status: string = '';
  targetNodeId: string = '';
}

export class TimelineNodeInput {
  id: string = '';
  name: string = '';
  archived: boolean = false;
  fields: Map<string, string> = new Map<string, string>();
}

export class TimelineSourceInput {
  id: string = '';
  label: string = '';
  state: string = '';
  lastIngestedAt: string | null = null;
}

/** 投影的全部输入：一次读取的只读快照。 */
export class TimelineInput {
  currentGraphRevision: number = 0;
  plans: TimelinePlanInput[] = [];
  drifts: TimelineDriftInput[] = [];
  nodes: TimelineNodeInput[] = [];
  sources: TimelineSourceInput[] = [];
}

/**
 * 计划是否 stale（派生，不是存储状态）。
 *
 * 与 PlanReadiness 的 isPlanStale 同口径 —— 这里**重复**一份常量逻辑而非
 * 引入依赖，是为了让 Timeline 保持只依赖 CanonicalEnums。
 * 两处若不一致，UI 会说"需重校验"而 Timeline 不提示，属正确性缺陷；
 * 目前由 DomainSelfCheck 的 timeline / readiness 两项共同覆盖。
 */
function planIsStale(plan: TimelinePlanInput, currentGraphRevision: number): boolean {
  const s = plan.workflowState;
  if (s === 'completed' || s === 'cancelled') {
    return false;
  }
  return currentGraphRevision > plan.lastAnalyzedGraphRevision;
}

/**
 * 纯投影：按 buildTimeline() 的 5 条规则收集条目并排序。
 *
 * 规则次序与 TS 基准一致，因为 `continue` 的位置会影响同一计划
 * 是否**同时**产出 needs_attention 与 upcoming_change（基准里是二选一）。
 */
export function buildTimelinePure(
  input: TimelineInput,
  nowIso: string,
  freshnessThresholdDays: number,
): TimelineItem[] {
  const now = Date.parse(nowIso);
  const items: TimelineItem[] = [];

  // 1. ChangePlan：stale → needs_attention（attention）；否则 effectiveDate → upcoming_change
  for (let i = 0; i < input.plans.length; i++) {
    const plan = input.plans[i];
    if (plan.workflowState === 'completed' || plan.workflowState === 'cancelled') {
      continue;
    }
    if (planIsStale(plan, input.currentGraphRevision)) {
      const it = new TimelineItem();
      it.id = 'tl-plan-stale-' + plan.id;
      it.kind = TimelineItemKind.NEEDS_ATTENTION;
      it.title = '计划需要重新检查：' + plan.title;
      it.subtitle = '基础设施在此计划创建后发生了变化，需要重新分析。';
      it.scheduledAt = null;
      it.bucket = TimelineBucket.ATTENTION;
      it.priority = 3;
      it.sourceType = TimelineSourceType.CHANGE_PLAN;
      it.sourceId = plan.id;
      it.status = 'needs_revalidation';
      items.push(it);
      continue;   // stale 的计划不再产出 upcoming_change（基准同此）
    }
    if (plan.effectiveDate !== null && plan.effectiveDate.length > 0) {
      const b = bucketOf(plan.effectiveDate, now);
      const it = new TimelineItem();
      it.id = 'tl-plan-' + plan.id;
      it.kind = TimelineItemKind.UPCOMING_CHANGE;
      it.title = plan.title;
      it.subtitle = '变更计划（' + plan.scenario + '）';
      it.scheduledAt = plan.effectiveDate;
      // attention 桶是"无法判定时间"的落点；计划有时间却落进 attention
      // 说明日期不可解析，此时按"以后再说"处理而不是塞进 attention。
      it.bucket = b === TimelineBucket.ATTENTION ? TimelineBucket.LATER : b;
      it.priority = 2;
      it.sourceType = TimelineSourceType.CHANGE_PLAN;
      it.sourceId = plan.id;
      it.status = plan.workflowState;
      items.push(it);
    }

    // 2. 待验证动作（verify 阶段且 verification 未终结）
    for (let a = 0; a < plan.actions.length; a++) {
      const action = plan.actions[a];
      const v = action.verification;
      if (v === null) {
        continue;
      }
      if (action.phase !== PlanActionPhase.VERIFY) {
        continue;
      }
      if (v.status === ActionVerificationStatus.VERIFIED ||
          v.status === ActionVerificationStatus.NOT_REQUIRED) {
        continue;
      }
      const b = bucketOf(plan.effectiveDate, now);
      const it = new TimelineItem();
      it.id = 'tl-verif-' + plan.id + '-' + action.id;
      it.kind = TimelineItemKind.VERIFICATION_PENDING;
      it.title = '待验证：' + action.title;
      it.subtitle = '属于计划「' + plan.title + '」';
      it.scheduledAt = plan.effectiveDate;
      it.bucket = b === TimelineBucket.ATTENTION ? TimelineBucket.LATER : b;
      it.priority = 2;
      it.sourceType = TimelineSourceType.ACTION_VERIFICATION;
      it.sourceId = plan.id + '/' + action.id;
      it.status = v.status;
      items.push(it);
    }
  }

  // 3. 开放 Drift → drift_review（attention，priority 3）
  for (let i = 0; i < input.drifts.length; i++) {
    const d = input.drifts[i];
    if (d.status !== DriftStatus.OPEN) {
      continue;
    }
    const it = new TimelineItem();
    it.id = 'tl-drift-' + d.id;
    it.kind = TimelineItemKind.DRIFT_REVIEW;
    it.title = '可能发生了变化';
    it.subtitle = d.kind + '（需确认或忽略）';
    it.scheduledAt = d.detectedAt;
    it.bucket = TimelineBucket.ATTENTION;
    it.priority = 3;
    it.sourceType = TimelineSourceType.REALITY_DRIFT;
    it.sourceId = d.id;
    it.status = d.status;
    items.push(it);
  }

  // 4. 节点到期 → expiration（bucket 由到期日决定，priority 2）
  for (let i = 0; i < input.nodes.length; i++) {
    const node = input.nodes[i];
    if (node.archived) {
      continue;
    }
    const expiry = nodeExpiryDate(node.fields);
    if (expiry === null) {
      continue;
    }
    const it = new TimelineItem();
    it.id = 'tl-expiry-' + node.id;
    it.kind = TimelineItemKind.EXPIRATION;
    it.title = node.name + ' 即将到期';
    it.subtitle = '检查仍依赖此对象的支付路径。';
    it.scheduledAt = expiry;
    // 注意：这里**不**做 attention→later 的重映射（基准也没有）。
    it.bucket = bucketOf(expiry, now);
    it.priority = 2;
    it.sourceType = TimelineSourceType.NODE_EXPIRY;
    it.sourceId = node.id;
    it.status = 'scheduled';
    items.push(it);
  }

  // 5. 来源新鲜度 → freshness_review（attention，priority 1）
  for (let i = 0; i < input.sources.length; i++) {
    const inst = input.sources[i];
    if (inst.state !== SourceInstanceState.ACTIVE) {
      continue;
    }
    const last = inst.lastIngestedAt;
    let ageDays = Number.POSITIVE_INFINITY;
    if (last !== null && last.length > 0) {
      const t = Date.parse(last);
      ageDays = Number.isNaN(t) ? Number.POSITIVE_INFINITY : (now - t) / DAY_MS;
    }
    if (ageDays <= freshnessThresholdDays) {
      continue;
    }
    const it = new TimelineItem();
    it.id = 'tl-fresh-' + inst.id;
    it.kind = TimelineItemKind.FRESHNESS_REVIEW;
    it.title = '数据来源需要刷新：' + inst.label;
    it.subtitle = last !== null && last.length > 0
      ? '最近更新：' + last.substring(0, 10)
      : '从未导入过数据。';
    it.scheduledAt = null;
    it.bucket = TimelineBucket.ATTENTION;
    it.priority = 1;
    it.sourceType = TimelineSourceType.SOURCE_FRESHNESS;
    it.sourceId = inst.id;
    it.status = 'stale';
    items.push(it);
  }

  // 排序：复用 sortTimelineKeys，保证"同一输入必然同一序列"。
  const keys: TimelineSortKey[] = [];
  for (let i = 0; i < items.length; i++) {
    const k = new TimelineSortKey();
    k.id = items[i].id;
    k.kind = items[i].kind;
    k.bucket = items[i].bucket;
    k.priority = items[i].priority;
    k.scheduledAt = items[i].scheduledAt;
    keys.push(k);
  }
  const sorted = sortTimelineKeys(keys);
  const out: TimelineItem[] = [];
  for (let i = 0; i < sorted.length; i++) {
    for (let j = 0; j < items.length; j++) {
      if (items[j].id === sorted[i].id) {
        out.push(items[j]);
        break;
      }
    }
  }
  return out;
}
