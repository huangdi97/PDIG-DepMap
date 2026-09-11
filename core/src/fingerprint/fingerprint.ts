import { createHmac, createHash } from 'node:crypto'
import type { Observation } from '../domain/types.ts'

/**
 * ObservationFingerprint — 只回答“这条原始记录以前处理过没有” (GOAL §12 / CANONICAL §5.5)
 *
 * - 优先稳定交易号：HMAC-SHA256(fpSecret, source + ":" + sourceTxnId)
 * - 无稳定交易号：canonicalRow（dateTime + signedAmount + normalizedDescription +
 *   counterparty + transactionType），同文件内完全重复行追加 occurrence ordinal #N
 * - 不保存 raw transaction id / merchant / amount / description
 * - 策略偏 precision：允许 false negative，尽量避免 false positive
 */

export const FINGERPRINT_VERSION = 1

export function computeFingerprintWithTxnId(
  fpSecret: string,
  source: string,
  sourceTxnId: string,
): string {
  return createHmac('sha256', fpSecret).update(`${source}:${sourceTxnId}`).digest('hex')
}

export function normalizeDescription(description: string): string {
  return description
    .trim()
    .replace(/\s+/g, ' ')
    .replace(/[（(]/g, '(')
    .replace(/[）)]/g, ')')
    .toLowerCase()
}

function canonicalRowOf(obs: Observation, signedAmount: number): string {
  return [
    obs.occurredAt,
    signedAmount.toFixed(2),
    normalizeDescription(obs.description),
    normalizeDescription(obs.merchantRaw),
    obs.direction,
  ].join('|')
}

export interface FingerprintAssignment {
  fingerprint: string
  /** 该观测是否来自稳定交易号（false = canonical row 回退） */
  stable: boolean
}

/**
 * 为一批同会话观测计算指纹。
 * 同一文件内 canonicalRow 完全相同的行：#1、#2 递增 ordinal，保证不互相吞掉，
 * 但跨会话仍可被 UNIQUE(source, fingerprint) 去重（同 ordinal 同指纹）。
 */
export function assignFingerprints(
  fpSecret: string,
  observations: Observation[],
): FingerprintAssignment[] {
  const ordinalCounters = new Map<string, number>()
  return observations.map((obs) => {
    if (obs.sourceTxnId !== null && obs.sourceTxnId !== '') {
      return {
        fingerprint: computeFingerprintWithTxnId(fpSecret, obs.source, obs.sourceTxnId),
        stable: true,
      }
    }
    const signedAmount = obs.direction === 'out' ? -obs.amount : obs.amount
    const base = canonicalRowOf(obs, signedAmount)
    const n = (ordinalCounters.get(base) ?? 0) + 1
    ordinalCounters.set(base, n)
    const canonical = n === 1 ? base : `${base}#${n}`
    return {
      fingerprint: createHash('sha256').update(`${obs.source}|${canonical}`).digest('hex'),
      stable: false,
    }
  })
}
