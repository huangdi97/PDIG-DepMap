import { createHmac, createHash } from 'node:crypto'
import type { Observation } from '../domain/types.ts'

/**
 * ObservationFingerprint v2 —— source-scoped（GOAL MVP02 §8）。
 *
 * - 优先稳定交易号：HMAC-SHA256(fpSecret, adapterId:sourceInstanceId:sourceTxnId)
 * - 无稳定 ID：canonicalRow fallback（按 SourceInstance 隔离，同文件完全重复行追加 ordinal #N）
 * - UNIQUE(source_instance_id, fingerprint_version, fingerprint)：
 *   同 txn id 跨 SourceInstance 不冲突；不同 fingerprintVersion 可并存
 * - 不保存 raw transaction id / merchant / amount / description
 * - 策略偏 precision：允许 false negative，尽量避免 false positive
 */

export const FINGERPRINT_VERSION = 1

export function computeFingerprintWithTxnId(
  fpSecret: string,
  adapterId: string,
  sourceInstanceId: string,
  sourceTxnId: string,
): string {
  return createHmac('sha256', fpSecret)
    .update(`${adapterId}:${sourceInstanceId}:${sourceTxnId}`)
    .digest('hex')
}

export function normalizeDescription(description: string): string {
  return description
    .trim()
    .replace(/\s+/g, ' ')
    .replace(/[（(]/g, '(')
    .replace(/[）)]/g, ')')
    .toLowerCase()
}

interface FingerprintObservationInput {
  adapterId: string
  sourceInstanceId: string
  sourceTxnId?: string | null
  occurredAt: string
  amount: number
  direction: string
  description?: string
  counterparty?: string
  currency?: string
  transactionType?: string
}

function canonicalRowOf(obs: FingerprintObservationInput, signedAmount: number): string {
  return [
    obs.occurredAt,
    signedAmount.toFixed(2),
    obs.currency ?? '',
    normalizeDescription(obs.description ?? ''),
    normalizeDescription(obs.counterparty ?? ''),
    obs.transactionType ?? '',
  ].join('|')
}

export interface FingerprintAssignment {
  fingerprint: string
  /** 该观测是否来自稳定交易号（false = canonical row 回退） */
  stable: boolean
}

/** 统一入口：接受 v2 Normalized 观测或 legacy Observation 形状。 */
export function assignFingerprintsV2(
  fpSecret: string,
  observations: readonly FingerprintObservationInput[],
): FingerprintAssignment[] {
  const ordinalCounters = new Map<string, number>()
  return observations.map((obs) => {
    if (obs.sourceTxnId !== null && obs.sourceTxnId !== undefined && obs.sourceTxnId !== '') {
      return {
        fingerprint: computeFingerprintWithTxnId(
          fpSecret,
          obs.adapterId,
          obs.sourceInstanceId,
          obs.sourceTxnId,
        ),
        stable: true,
      }
    }
    const signedAmount = obs.direction === 'out' ? -obs.amount : obs.amount
    const base = canonicalRowOf(obs, signedAmount)
    const scoped = `${obs.adapterId}:${obs.sourceInstanceId}:${base}`
    const n = (ordinalCounters.get(scoped) ?? 0) + 1
    ordinalCounters.set(scoped, n)
    const canonical = n === 1 ? scoped : `${scoped}#${n}`
    return {
      fingerprint: createHash('sha256').update(canonical).digest('hex'),
      stable: false,
    }
  })
}

/** legacy 兼容入口：MVP01 Observation 形状（scope 由调用方给出）。 */
export function assignFingerprints(
  fpSecret: string,
  observations: readonly Observation[],
  scope: { adapterId: string; sourceInstanceId: string },
): FingerprintAssignment[] {
  return assignFingerprintsV2(
    fpSecret,
    observations.map((o) => ({
      adapterId: scope.adapterId,
      sourceInstanceId: scope.sourceInstanceId,
      sourceTxnId: o.sourceTxnId,
      occurredAt: o.occurredAt,
      amount: o.amount,
      direction: o.direction,
      description: o.description,
      counterparty: o.merchantRaw,
      currency: o.currency,
      transactionType: o.status,
    })),
  )
}

