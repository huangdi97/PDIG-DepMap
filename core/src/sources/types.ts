import type { NormalizedPaymentObservation, SourceInstance } from '../domain/source.ts'
import type { Observation, Relation } from '../domain/types.ts'

/**
 * EvidenceSourceAdapter 正式契约（SOURCE_ADAPTER_CONTRACT.md / GOAL MVP02 §6）。
 *
 * 规则：
 * - canHandle deterministic；相同 adapter version + input + mapping → deterministic 输出
 * - Adapter 不创建 Dependency / Group / required；不写 Graph
 * - 不长期保存 raw statement；不输出秘密；不调用业务网络
 * - 必须声明 coverageMode 与 authoritativeFor（MVP02 三个文件 Adapter = event_stream / []）
 * - Parsing/Fingerprint 语义变化必须升级 adapterVersion
 */

export type SourceInput = {
  data: Uint8Array
  fileName?: string
  /** GenericCsvAdapter 专用：显式字段映射（禁止 AI 自动映射） */
  mapping?: MappingProfile
}

export interface SourceContext {
  sourceInstance: SourceInstance
}

/** Mapping Profile：显式字段映射（GOAL MVP02 §16）。 */
export interface MappingProfile {
  columns: {
    transactionId?: string
    dateTime: string
    amount?: string
    debit?: string
    credit?: string
    description?: string
    counterparty?: string
    currency?: string
    balance?: string
    transactionType?: string
    paymentMethod?: string
  }
  options: {
    delimiter: ',' | ';' | '\t' | '|'
    encoding?: 'utf-8' | 'gb18030'
    dateFormats: string[]
    decimalSeparator: '.' | ','
    /** signed：金额列自带符号；debit_credit：借/贷两列；outward_positive：支出为正数 */
    amountSignMode: 'signed' | 'debit_credit' | 'outward_positive'
    hasHeaderRow: boolean
    /** 方向判定：signed 模式下正数默认方向 */
    positiveDirection?: 'in' | 'out'
  }
}

/** source-specific 支付路径建议（仅提议；不写图）。 */
export interface RouteSuggestion {
  from: string
  relation: Relation
  to: string
}

export interface EvidenceSourceAdapter {
  readonly id: string
  readonly version: number
  readonly sourceKind:
    'statement_file' | 'platform_export' | 'open_banking' | 'manual' | 'discovery'
  readonly coverageMode: 'event_stream' | 'partial_snapshot' | 'complete_snapshot' | 'user_selected'
  readonly authoritativeFor: readonly string[]

  /** 置信度 0..1；deterministic。 */
  canHandle(input: SourceInput): Promise<number>
  /** 解析为源观测（legacy Observation 形状，含 merchantRaw/status 供复用周期识别）。 */
  parse(input: SourceInput, context: SourceContext): Promise<Observation[]>
  /** 统一归一化输出。Unknown 保持 unknown，禁止猜。 */
  normalize(
    observations: readonly Observation[],
    context: SourceContext,
  ): Promise<NormalizedPaymentObservation[]>
  /** 可选：source-specific 支付路径建议（WeChat funding 逻辑唯一合法位置）。 */
  suggestRoutes?(
    normalized: readonly NormalizedPaymentObservation[],
    context: SourceContext,
    resolved: ReadonlyMap<string, string>,
  ): RouteSuggestion[]
  /** 可选：最近一次 parse 的坏行/错误（不进入 Observation 流；仅计数与行号）。 */
  lastParseErrors?: () => Array<{ line: number; reason: string }>
}

/** MVP02：三个文件 Adapter 的公共元数据断言（测试用）。 */
export function assertFileAdapterContract(
  adapter: Pick<EvidenceSourceAdapter, 'coverageMode' | 'authoritativeFor' | 'sourceKind'>,
): void {
  if (adapter.coverageMode !== 'event_stream') {
    throw new Error(`MVP02 file adapters must be event_stream, got ${adapter.coverageMode}`)
  }
  if (adapter.authoritativeFor.length !== 0) {
    throw new Error('MVP02 file adapters must have empty authoritativeFor')
  }
  if (adapter.sourceKind !== 'statement_file') {
    throw new Error(`MVP02 file adapters must be statement_file, got ${adapter.sourceKind}`)
  }
}
