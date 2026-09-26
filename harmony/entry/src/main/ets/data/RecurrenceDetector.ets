// 周期识别（纯 ArkTS，零 @ohos；主机单测可执行）。
//
// 移植自 core/src/parser/wechat/recurring.ts 的冻结基准（GOAL §15 / CANONICAL §6.6）：
//   - 只统计 direction=out 且非「退款/撤销」状态的观测
//   - 时间间隔：monthly 28–31 天 / quarterly 88–92 天 / yearly 360–370 天
//   - 命中率 ≥0.6 → confidence = 0.5·间隔命中率 + 0.3·次数因子 + 0.2·金额稳定度
// 只输出 confidence，**不产生任何 Dependency / required**。
//
// ArkTS 约束：无解构；Date.getTime() 参与计算。

import { Observation } from '../sources/Parsers';
import { ObservationDirection } from '../generated/CanonicalEnums';

export class RecurrenceResult {
  merchantKey: string = '';
  period: string = '';
  confidence: number = 0;
  occurrences: number = 0;
  firstObservedAt: string = '';
  lastObservedAt: string = '';
  typicalAmount: number = 0;
  paymentMethods: string[] = [];
}

interface DatedObservation {
  dateMs: number;
  obs: Observation;
}

class PeriodRange {
  period: string;
  minDays: number;
  maxDays: number;

  constructor(period: string, minDays: number, maxDays: number) {
    this.period = period;
    this.minDays = minDays;
    this.maxDays = maxDays;
  }
}

const PERIOD_RANGES: PeriodRange[] = [
  new PeriodRange('monthly', 28, 31),
  new PeriodRange('quarterly', 88, 92),
  new PeriodRange('yearly', 360, 370),
];

function dayDiff(aMs: number, bMs: number): number {
  return Math.round(Math.abs(aMs - bMs) / 86400000);
}

function median(values: number[]): number {
  const sorted = values.slice().sort((x, y) => x - y);
  const mid = Math.floor(sorted.length / 2);
  const a = sorted[mid - 1];
  const b = sorted[mid];
  if (a === undefined || b === undefined) {
    return 0;
  }
  return sorted.length % 2 === 0 ? (a + b) / 2 : b;
}

/** 对单一商户的成功支出观测做周期识别。 */
export function detectRecurrence(merchantKey: string, observations: Observation[]): RecurrenceResult | null {
  const paid: DatedObservation[] = [];
  for (const o of observations) {
    if (o.direction !== ObservationDirection.OUT) {
      continue;
    }
    if (o.status.includes('退款') || o.status.includes('撤销')) {
      continue;
    }
    paid.push({ dateMs: new Date(o.occurredAt).getTime(), obs: o });
  }
  if (paid.length < 2) {
    return null;
  }
  paid.sort((a, b) => a.dateMs - b.dateMs);

  const gaps: number[] = [];
  for (let i = 1; i < paid.length; i++) {
    const prev = paid[i - 1];
    const cur = paid[i];
    if (prev === undefined || cur === undefined) {
      continue;
    }
    gaps.push(dayDiff(prev.dateMs, cur.dateMs));
  }

  for (const range of PERIOD_RANGES) {
    let inRange = 0;
    for (const g of gaps) {
      if (g >= range.minDays && g <= range.maxDays) {
        inRange++;
      }
    }
    const ratio = inRange / gaps.length;
    if (ratio < 0.6) {
      continue;
    }

    const countFactor = Math.min(1, (paid.length - 1) / 4);
    const amounts: number[] = [];
    for (const p of paid) {
      amounts.push(p.obs.amount);
    }
    const med = median(amounts);
    let maxDev = 0;
    for (const a of amounts) {
      const d = Math.abs(a - med);
      if (d > maxDev) {
        maxDev = d;
      }
    }
    const stability = med > 0 ? Math.max(0, 1 - maxDev / med) : 0;
    const confidence = Math.round((0.5 * ratio + 0.3 * countFactor + 0.2 * stability) * 100) / 100;

    const methods: string[] = [];
    const seen = new Set<string>();
    for (const p of paid) {
      const m = p.obs.paymentMethodRaw;
      if (m !== '' && !seen.has(m)) {
        seen.add(m);
        methods.push(m);
      }
    }
    methods.sort();

    const first = paid[0];
    const last = paid[paid.length - 1];
    if (first === undefined || last === undefined) {
      return null;
    }
    const r = new RecurrenceResult();
    r.merchantKey = merchantKey;
    r.period = range.period;
    r.confidence = confidence;
    r.occurrences = paid.length;
    r.firstObservedAt = first.obs.occurredAt;
    r.lastObservedAt = last.obs.occurredAt;
    r.typicalAmount = med;
    r.paymentMethods = methods;
    return r;
  }
  return null;
}