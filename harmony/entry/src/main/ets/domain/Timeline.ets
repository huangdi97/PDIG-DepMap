// DO NOT EDIT 之外的语义 — 语义必须与 TS 冻结基准一致
// 端口自 core/src/services/timeline.ts（MVP03 §42–§45）中**纯投影**的部分。
//
// 分层约束：只依赖 `./PlanReadiness`（复用同一套"今天"口径）与
// `../generated/CanonicalEnums`，**不依赖任何 repository / DB**。
//
// 与本 TS 基准的一处**有意差异**（必须显式记录，不得被当作遗漏）：
//   原 buildTimeline() 需要 ChangePlanRepository / RealityDriftRepository /
//   NodeRepository / SourceInstanceRepository 才能收集条目 —— 那是 data 层。
//   Harmony domain 层不得依赖 relationalStore，因此这里保留的是
//   Timeline 的**分类与排序内核**（bucket 判定 + deterministic 排序），
//   它决定了"同一 DB 状态与同一 now 必然得到同一序列"这一不变式。
//   条目的收集属于 application/data 层，尚未落地。
//
// 语义定位（原文照录）：纯投影（derived read model）：
// 不是 Reality、不持久化、可随时重建。
// 只包含数字基础设施事项；排序 deterministic（bucket → priority → scheduledAt → id）。

import { TimelineBucket, TimelineItemKind } from '../generated/CanonicalEnums';

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
 * 最后用 id 兜底**不是**装饰：前三个键都可能相等（同一 bucket、
 * 同优先级、同一时间点），没有 id 兜底时排序结果依赖输入顺序，
 * 同一份数据在两次读取中可能给出不同序列 —— 那会让快照比较失效。
 *
 * scheduledAt 为 null 视为"无穷远"（排在同时段有时间的项之后）。
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
      return a.priority - b.priority;
    }
    const ta = a.scheduledAt === null ? Number.POSITIVE_INFINITY : Date.parse(a.scheduledAt);
    const tb = b.scheduledAt === null ? Number.POSITIVE_INFINITY : Date.parse(b.scheduledAt);
    const sa = Number.isNaN(ta) ? Number.POSITIVE_INFINITY : ta;
    const sb = Number.isNaN(tb) ? Number.POSITIVE_INFINITY : tb;
    if (sa !== sb) {
      return sa - sb;
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
