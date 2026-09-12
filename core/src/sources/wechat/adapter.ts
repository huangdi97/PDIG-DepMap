import type { Observation } from '../../domain/types.ts'
import type { NormalizedPaymentObservation } from '../../domain/source.ts'
import { parseWechatBill } from '../../parser/wechat/parser.ts'
import { parsePaymentMethod, findBankCardNode } from '../../resolver/resolver.ts'
import type { SqliteDriver } from '../../db/driver.ts'
import { NodeRepository } from '../../repositories/node-repository.ts'
import type {
  EvidenceSourceAdapter,
  RouteSuggestion,
  SourceContext,
  SourceInput,
} from '../types.ts'

/**
 * WeChatStatementAdapter —— 微信账单作为普通 Adapter（MVP02 Adapter 化）。
 * Domain 层不再出现 wechat 特例；funding_source 推断逻辑全部留在本 Adapter。
 */
export class WeChatStatementAdapter implements EvidenceSourceAdapter {
  readonly id = 'wechat_statement'
  readonly version = 1
  readonly sourceKind = 'statement_file' as const
  readonly coverageMode = 'event_stream' as const
  readonly authoritativeFor: readonly string[] = []

  private readonly nodes: NodeRepository
  private lastErrors: Array<{ line: number; reason: string }> = []

  constructor(driver: SqliteDriver) {
    this.nodes = new NodeRepository(driver)
  }

  async canHandle(input: SourceInput): Promise<number> {
    // 契约要求 Promise 返回（SOURCE_ADAPTER_CONTRACT）；实现为同步纯解析，
    // 显式 await 以保持 async 语义、避免被误当作可同步调用。
    await Promise.resolve()
    const text = new TextDecoder('utf-8', { fatal: false }).decode(input.data.slice(0, 4096))
    if (text.includes('微信支付账单明细')) return 0.99
    if (text.includes('交易时间') && text.includes('交易对方') && text.includes('收/支')) return 0.9
    return 0
  }

  async parse(input: SourceInput, _context: SourceContext): Promise<Observation[]> {
    await Promise.resolve()
    const result = parseWechatBill(input.data)
    this.lastErrors = result.errors
    return result.observations
  }

  lastParseErrors(): Array<{ line: number; reason: string }> {
    return this.lastErrors
  }

  async normalize(
    observations: readonly Observation[],
    context: SourceContext,
  ): Promise<NormalizedPaymentObservation[]> {
    await Promise.resolve()
    return observations.map((o) => ({
      sourceInstanceId: context.sourceInstance.id,
      adapterId: this.id,
      sourceTxnId: o.sourceTxnId ?? undefined,
      occurredAt: o.occurredAt,
      amount: o.amount,
      currency: o.currency,
      direction: o.direction,
      description: o.description,
      counterparty: o.merchantRaw,
      merchantRaw: o.merchantRaw,
      transactionType: o.status,
      paymentMethodHint: o.paymentMethodRaw,
      status: o.status,
      note: o.note,
    }))
  }

  /**
   * WeChat 特有：账单支付方式直接给出资金卡 → funding_source 建议；
   * merchant_agreement 建议从微信账户指向服务节点。
   * from 节点 = sourceInstance.accountNodeId（微信账户节点，由 coordinator/facade 绑定）。
   */
  suggestRoutes(
    normalized: readonly NormalizedPaymentObservation[],
    context: SourceContext,
    resolved: ReadonlyMap<string, string>,
  ): RouteSuggestion[] {
    const wechatAccountId = context.sourceInstance.accountNodeId
    if (!wechatAccountId) return []
    const routes: RouteSuggestion[] = []
    const cardSeen = new Set<string>()
    const serviceSeen = new Set<string>()

    for (const obs of normalized) {
      const serviceNodeId = resolved.get(obs.merchantRaw ?? '')
      // merchant_agreement 是 “账户 → 服务” 的现实关系，与观测条数无关：
      // 同一服务必须只产生一条建议，否则同 key UPSERT 会按观测数重复累计 evidence
      // （6 次腾讯视频扣款会变成 observationCount=36，直接扭曲重提阈值与置信度）。
      if (serviceNodeId && !serviceSeen.has(serviceNodeId)) {
        serviceSeen.add(serviceNodeId)
        routes.push({ from: wechatAccountId, relation: 'merchant_agreement', to: serviceNodeId })
      }
      const hint = obs.paymentMethodHint
      if (hint) {
        const info = parsePaymentMethod(hint)
        if (info.kind === 'bank_card' && !cardSeen.has(hint)) {
          const cardNode = findBankCardNode(info, this.nodes.list({ archived: false }))
          if (cardNode) {
            cardSeen.add(hint)
            routes.push({
              from: cardNode.id,
              relation: 'funding_source',
              to: wechatAccountId,
            })
          }
        }
      }
    }
    return routes
  }
}
