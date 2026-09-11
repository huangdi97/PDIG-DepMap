import type { Observation } from '../../domain/types.ts'

/**
 * 简单 MVP 周期识别 (GOAL §15 / CANONICAL §6.6)
 *
 * 按已解析的服务候选分组，用时间间隔 + 金额稳定度寻找：
 * - monthly   28–31 天
 * - quarterly 88–92 天
 * - yearly    360–370 天
 * 只输出 confidence，不产生任何 Dependency / required。
 */

export type RecurrencePeriod = 'monthly' | 'quarterly' | 'yearly'

export interface RecurrenceResult {
  merchantKey: string
  period: RecurrencePeriod
  confidence: number
  occurrences: number
  firstObservedAt: string
  lastObservedAt: string
  /** 金额众数（仅用于展示，不参与影响判定） */
  typicalAmount: number
  /** 涉及的支付方式集合（原始字符串，供路径识别参考） */
  paymentMethods: string[]
}

const PERIOD_RANGES: Array<{ period: RecurrencePeriod; minDays: number; maxDays: number }> = [
  { period: 'monthly', minDays: 28, maxDays: 31 },
  { period: 'quarterly', minDays: 88, maxDays: 92 },
  { period: 'yearly', minDays: 360, maxDays: 370 },
]

function dayDiff(a: Date, b: Date): number {
  return Math.round(Math.abs(b.getTime() - a.getTime()) / 86400000)
}

function median(values: number[]): number {
  const sorted = [...values].sort((x, y) => x - y)
  const mid = Math.floor(sorted.length / 2)
  const a = sorted[mid - 1]
  const b = sorted[mid]
  if (a === undefined || b === undefined) return 0
  return sorted.length % 2 === 0 ? (a + b) / 2 : b
}

/**
 * 对单一商户（merchantKey）的“成功支出”观测做周期识别。
 * 只统计 direction=out 且非退款状态的观测。
 */
export function detectRecurrence(
  merchantKey: string,
  observations: Observation[],
): RecurrenceResult | null {
  const paid = observations
    .filter(
      (o) => o.direction === 'out' && !o.status.includes('退款') && !o.status.includes('撤销'),
    )
    .map((o) => ({ date: new Date(o.occurredAt), obs: o }))
    .sort((a, b) => a.date.getTime() - b.date.getTime())

  if (paid.length < 2) return null

  const gaps: number[] = []
  for (let i = 1; i < paid.length; i++) {
    const prev = paid[i - 1]
    const cur = paid[i]
    if (!prev || !cur) continue
    gaps.push(dayDiff(prev.date, cur.date))
  }

  for (const range of PERIOD_RANGES) {
    const inRange = gaps.filter((g) => g >= range.minDays && g <= range.maxDays).length
    const ratio = inRange / gaps.length
    if (ratio < 0.6) continue

    // confidence：间隔命中率 × 次数因子 × 金额稳定度
    const countFactor = Math.min(1, (paid.length - 1) / 4) // ≥5 次给满
    const amounts = paid.map((p) => p.obs.amount)
    const med = median(amounts)
    const stability =
      med > 0 ? Math.max(0, 1 - Math.max(...amounts.map((a) => Math.abs(a - med))) / med) : 0
    const confidence = Math.round((0.5 * ratio + 0.3 * countFactor + 0.2 * stability) * 100) / 100

    const paymentMethods = [
      ...new Set(paid.map((p) => p.obs.paymentMethodRaw).filter((m) => m !== '')),
    ].sort()
    const first = paid[0]
    const last = paid[paid.length - 1]
    if (!first || !last) return null
    return {
      merchantKey,
      period: range.period,
      confidence,
      occurrences: paid.length,
      firstObservedAt: first.obs.occurredAt,
      lastObservedAt: last.obs.occurredAt,
      typicalAmount: med,
      paymentMethods,
    }
  }
  return null
}
