import type { SqliteDriver } from '../db/driver.ts'
import { ChangePlanRepository } from '../repositories/change-plan-repository.ts'
import { RealityDriftRepository } from '../repositories/reality-drift-repository.ts'
import { NodeRepository } from '../repositories/node-repository.ts'
import { SourceInstanceRepository } from '../repositories/source-instance-repository.ts'
import { getGraphRevision } from '../repositories/graph-revision.ts'
import { isPlanStale } from '../domain/change-plan.ts'

/**
 * Timeline / Upcoming（MVP03 §42–§45）。
 *
 * 纯投影（derived read model）：不是 Reality、不持久化、可随时重建。
 * 只包含数字基础设施事项；排序 deterministic（bucket → priority → scheduledAt → id）。
 */

export type TimelineKind =
  | 'needs_attention'
  | 'upcoming_change'
  | 'verification_pending'
  | 'freshness_review'
  | 'drift_review'
  | 'expiration'

export type TimelineBucket = 'attention' | 'overdue' | 'today' | '7d' | '30d' | '90d' | 'later'

export type TimelineSourceType =
  'change_plan' | 'reality_drift' | 'action_verification' | 'node_expiry' | 'source_freshness'

export interface TimelineItem {
  id: string
  kind: TimelineKind
  title: string
  subtitle: string
  scheduledAt: string | null
  bucket: TimelineBucket
  priority: number
  sourceType: TimelineSourceType
  sourceId: string
  actionTarget: string | null
  status: string
}

export interface TimelineOptions {
  /** 源新鲜度阈值（天），超过即产生 freshness_review。 */
  freshnessThresholdDays?: number
}

const BUCKET_ORDER: TimelineBucket[] = [
  'attention',
  'overdue',
  'today',
  '7d',
  '30d',
  '90d',
  'later',
]

const DAY_MS = 86_400_000

function bucketOf(scheduledAt: string | null, now: number): TimelineBucket {
  if (scheduledAt === null) return 'attention'
  const t = Date.parse(scheduledAt)
  if (Number.isNaN(t)) return 'attention'
  const startOfToday = new Date(now)
  startOfToday.setUTCHours(0, 0, 0, 0)
  const todayStart = startOfToday.getTime()
  if (t < todayStart) return 'overdue'
  if (t < todayStart + DAY_MS) return 'today'
  if (t < todayStart + 8 * DAY_MS) return '7d'
  if (t < todayStart + 31 * DAY_MS) return '30d'
  if (t < todayStart + 91 * DAY_MS) return '90d'
  return 'later'
}

function nodeExpiryDate(fields: Record<string, unknown> | undefined): string | null {
  const raw = fields?.['expiryDate']
  return typeof raw === 'string' && raw.length > 0 ? raw : null
}

/** 构建 Timeline 投影（只读；对同一 DB 状态与同一 now 恒定）。 */
export function buildTimeline(
  driver: SqliteDriver,
  nowIso: string,
  options: TimelineOptions = {},
): TimelineItem[] {
  const now = Date.parse(nowIso)
  const items: TimelineItem[] = []
  const freshnessDays = options.freshnessThresholdDays ?? 45

  // 1. ChangePlans：needs_revalidation → attention；effectiveDate → upcoming_change / overdue
  const plans = new ChangePlanRepository(driver)
  const currentRevision = getGraphRevision(driver)
  for (const plan of plans.listAll()) {
    if (plan.workflowState === 'completed' || plan.workflowState === 'cancelled') continue
    if (isPlanStale(plan, currentRevision)) {
      items.push({
        id: `tl-plan-stale-${plan.id}`,
        kind: 'needs_attention',
        title: `计划需要重新检查：${plan.title}`,
        subtitle: '基础设施在此计划创建后发生了变化，需要重新分析。',
        scheduledAt: null,
        bucket: 'attention',
        priority: 3,
        sourceType: 'change_plan',
        sourceId: plan.id,
        actionTarget: plan.id,
        status: 'needs_revalidation',
      })
      continue
    }
    if (plan.effectiveDate) {
      const bucket = bucketOf(plan.effectiveDate, now)
      items.push({
        id: `tl-plan-${plan.id}`,
        kind: 'upcoming_change',
        title: plan.title,
        subtitle: `变更计划（${plan.scenario}）`,
        scheduledAt: plan.effectiveDate,
        bucket: bucket === 'attention' ? 'later' : bucket,
        priority: 2,
        sourceType: 'change_plan',
        sourceId: plan.id,
        actionTarget: plan.id,
        status: plan.workflowState,
      })
    }
    // 2. Verification pending（verify 阶段动作）
    for (const action of plan.actions) {
      if (
        action.phase === 'verify' &&
        action.verification &&
        action.verification.status !== 'verified' &&
        action.verification.status !== 'not_required'
      ) {
        items.push({
          id: `tl-verif-${plan.id}-${action.id}`,
          kind: 'verification_pending',
          title: `待验证：${action.title}`,
          subtitle: `属于计划「${plan.title}」`,
          scheduledAt: plan.effectiveDate,
          bucket:
            bucketOf(plan.effectiveDate, now) === 'attention'
              ? 'later'
              : bucketOf(plan.effectiveDate, now),
          priority: 2,
          sourceType: 'action_verification',
          sourceId: `${plan.id}/${action.id}`,
          actionTarget: plan.id,
          status: action.verification.status,
        })
      }
    }
  }

  // 3. Open RealityDrifts → drift_review（attention）
  const drifts = new RealityDriftRepository(driver)
  for (const drift of drifts.listByStatus('open')) {
    items.push({
      id: `tl-drift-${drift.id}`,
      kind: 'drift_review',
      title: '可能发生了变化',
      subtitle: `${drift.kind}（需确认或忽略）`,
      scheduledAt: drift.detectedAt,
      bucket: 'attention',
      priority: 3,
      sourceType: 'reality_drift',
      sourceId: drift.id,
      actionTarget: drift.targetNodeId,
      status: drift.status,
    })
  }

  // 4. Node expiry（payment_instrument fields.expiryDate）
  const nodes = new NodeRepository(driver)
  for (const node of nodes.list({ archived: false })) {
    const expiry = nodeExpiryDate(node.fields)
    if (!expiry) continue
    items.push({
      id: `tl-expiry-${node.id}`,
      kind: 'expiration',
      title: `${node.name} 即将到期`,
      subtitle: '检查仍依赖此对象的支付路径。',
      scheduledAt: expiry,
      bucket: bucketOf(expiry, now),
      priority: 2,
      sourceType: 'node_expiry',
      sourceId: node.id,
      actionTarget: node.id,
      status: 'scheduled',
    })
  }

  // 5. Source freshness
  const instances = new SourceInstanceRepository(driver)
  for (const inst of instances.listAll()) {
    if (inst.state !== 'active') continue
    const last = inst.lastIngestedAt ?? null
    const ageDays = last ? (now - Date.parse(last)) / DAY_MS : Number.POSITIVE_INFINITY
    if (ageDays > freshnessDays) {
      items.push({
        id: `tl-fresh-${inst.id}`,
        kind: 'freshness_review',
        title: `数据来源需要刷新：${inst.label}`,
        subtitle: last ? `最近更新：${last.slice(0, 10)}` : '从未导入过数据。',
        scheduledAt: null,
        bucket: 'attention',
        priority: 1,
        sourceType: 'source_freshness',
        sourceId: inst.id,
        actionTarget: inst.id,
        status: 'stale',
      })
    }
  }

  return items.sort((a, b) => {
    const ba = BUCKET_ORDER.indexOf(a.bucket)
    const bb = BUCKET_ORDER.indexOf(b.bucket)
    if (ba !== bb) return ba - bb
    if (a.priority !== b.priority) return b.priority - a.priority
    const sa = a.scheduledAt ?? ''
    const sb = b.scheduledAt ?? ''
    if (sa !== sb) return sa < sb ? -1 : 1
    return a.id < b.id ? -1 : a.id > b.id ? 1 : 0
  })
}
