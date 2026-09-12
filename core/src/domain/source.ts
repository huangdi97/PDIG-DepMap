/**
 * MVP02 Global Source Abstraction — domain v2 类型（GOAL §6/§7/§11/§26）。
 *
 * 语义铁律不变：
 * - Observation ≠ Reality；Adapter 不创建 Dependency/Group/required
 * - event_stream 的 absence 不产生任何现实否定（GOAL §25）
 * - sourceTxnId 仅存在于会话内存/指纹输入，不明文持久化
 */

// ---------------------------------------------------------------------------
// SourceKind / CoverageMode
// ---------------------------------------------------------------------------

export type SourceKind =
  'statement_file' | 'platform_export' | 'open_banking' | 'manual' | 'discovery'

/**
 * coverageMode 决定 absence 的语义：
 * - event_stream：账单中没出现 ≠ 现实不存在（MVP02 三个文件 Adapter 均为此）
 * - partial/complete_snapshot / user_selected：后续轮次才引入
 */
export type CoverageMode =
  'event_stream' | 'partial_snapshot' | 'complete_snapshot' | 'user_selected'

// ---------------------------------------------------------------------------
// SourceInstance — 用户具体的数据源实例
// ---------------------------------------------------------------------------

export interface SourceInstance {
  id: string
  adapterId: string
  adapterVersion: number
  sourceKind: SourceKind

  providerId?: string | undefined
  accountNodeId?: string | undefined
  label: string

  country?: string | undefined
  jurisdiction?: string | undefined
  currencies?: string[] | undefined

  state: 'active' | 'retired'
  createdAt: string
  updatedAt: string
  lastIngestedAt?: string | undefined
}

/** MVP02 的三个文件 Adapter 共用的 deterministic legacy/默认实例工厂参数。 */
export interface CreateSourceInstanceInput {
  id?: string
  adapterId: string
  adapterVersion: number
  sourceKind: SourceKind
  providerId?: string | undefined
  accountNodeId?: string | undefined
  label: string
  country?: string | undefined
  jurisdiction?: string | undefined
  currencies?: string[] | undefined
}

// ---------------------------------------------------------------------------
// NormalizedPaymentObservation — 所有 Adapter 的统一输出（GOAL §17）
// ---------------------------------------------------------------------------

export type ObservationDirectionV2 = 'in' | 'out' | 'neutral'

export interface NormalizedPaymentObservation {
  sourceInstanceId: string
  adapterId: string
  /** 仅存在于会话内存/指纹输入，绝不持久化明文 */
  sourceTxnId?: string | undefined
  occurredAt: string
  amount: number
  currency?: string | undefined
  direction: ObservationDirectionV2
  description?: string | undefined
  counterparty?: string | undefined
  balance?: number | undefined
  transactionType?: string | undefined
  /** 如“招商银行信用卡(4417)”——仅作为 Node Resolution 提示 */
  paymentMethodHint?: string | undefined
  /** 商户/交易对方原始描述（Node Resolution 输入） */
  merchantRaw?: string | undefined
  status?: string | undefined
  note?: string | undefined
}

// ---------------------------------------------------------------------------
// VerificationBasis — Reality 是如何被验证的（GOAL §11）
// ---------------------------------------------------------------------------

export type VerificationBasis =
  | { type: 'user_confirmed'; verifiedAt: string }
  | {
      type: 'authoritative_source'
      sourceInstanceId: string
      factType: string
      verifiedAt: string
    }

/** MVP02：三个 Adapter 均为 event_stream → 一切 Reality 只能 user_confirmed。 */
export function userConfirmedBasis(verifiedAt: string): VerificationBasis {
  return { type: 'user_confirmed', verifiedAt }
}
