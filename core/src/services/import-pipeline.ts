import type { SqliteDriver } from '../db/driver.ts'
import { NodeRepository } from '../repositories/node-repository.ts'
import { SourceInstanceRepository } from '../repositories/source-instance-repository.ts'
import {
  LEGACY_WECHAT_ADAPTER_ID,
  LEGACY_WECHAT_ADAPTER_VERSION,
  LEGACY_WECHAT_SOURCE_INSTANCE_ID,
} from '../schema/migrations.ts'
import { WeChatStatementAdapter } from '../sources/wechat/adapter.ts'
import {
  ImportCoordinator,
  type CoordinatorBeginResult,
  type CoordinatorOutcome,
} from './import-coordinator.ts'

export type { MerchantCandidate } from './import-coordinator.ts'
export type { CoordinatorOutcome as ImportOutcome }

export interface BeginImportResult {
  session: CoordinatorBeginResult['session']
  rawCount: number
  candidates: CoordinatorBeginResult['candidates']
  errors: import('../parser/wechat/parser.ts').ParseError[]
}

/**
 * ImportFlow —— MVP01 兼容 facade（GOAL MVP02 §19：WeChat 只是其中一个 Adapter）。
 * 内部委托 ImportCoordinator + WeChatStatementAdapter + deterministic legacy SourceInstance。
 * Domain / coordinator 层不包含任何 wechat 特例。
 */
export class ImportFlow {
  private readonly coordinator: ImportCoordinator
  private readonly nodes: NodeRepository
  private readonly sourceInstances: SourceInstanceRepository
  private readonly adapter: WeChatStatementAdapter
  private begun = false

  constructor(driver: SqliteDriver) {
    this.coordinator = new ImportCoordinator(driver)
    this.nodes = new NodeRepository(driver)
    this.sourceInstances = new SourceInstanceRepository(driver)
    this.adapter = new WeChatStatementAdapter(driver)
  }

  /** 阶段 1（MVP01 兼容签名；现返回 Promise）。 */
  async begin(raw: Uint8Array): Promise<BeginImportResult> {
    if (this.begun) throw new Error('flow already begun')
    this.begun = true
    const instanceId = this.ensureLegacyInstanceWithAccount()
    const result = await this.coordinator.begin(instanceId, this.adapter, { data: raw })
    return {
      session: result.session,
      rawCount: result.rawCount,
      candidates: result.candidates,
      errors: result.errors,
    }
  }

  /** 阶段 2：用户确认商户描述符 → 节点。 */
  resolveMerchant(merchantRaw: string, nodeId: string): void {
    this.coordinator.resolveMerchant(merchantRaw, nodeId)
  }

  /** 阶段 3：指纹落库（legacy 实例 scope）→ recurrence → proposals。 */
  async finalize(): Promise<CoordinatorOutcome> {
    return this.coordinator.finalize()
  }

  /**
   * legacy WeChat SourceInstance：不存在则创建（deterministic id），并**始终**确保
   * 绑定微信账户节点。
   *
   * 注意：migration 插入 legacy 实例时 account_node_id 为空（迁移时微信节点可能还不存在），
   * 因此这里不能只依赖 "不存在则创建" —— 已存在但未绑定账号时必须补绑，
   * 否则 WeChatStatementAdapter 无法产出 funding_source 路由。
   */
  private ensureLegacyInstanceWithAccount(): string {
    const wechatAccount = this.ensureWechatAccount()
    const existing = this.sourceInstances.getById(LEGACY_WECHAT_SOURCE_INSTANCE_ID)
    if (!existing) {
      this.sourceInstances.create({
        id: LEGACY_WECHAT_SOURCE_INSTANCE_ID,
        adapterId: LEGACY_WECHAT_ADAPTER_ID,
        adapterVersion: LEGACY_WECHAT_ADAPTER_VERSION,
        sourceKind: 'statement_file',
        label: 'Legacy WeChat Statement Source',
        currencies: ['CNY'],
        accountNodeId: wechatAccount.id,
      })
      return LEGACY_WECHAT_SOURCE_INSTANCE_ID
    }
    if (existing.accountNodeId !== wechatAccount.id) {
      this.sourceInstances.bindAccountNode(LEGACY_WECHAT_SOURCE_INSTANCE_ID, wechatAccount.id)
    }
    return LEGACY_WECHAT_SOURCE_INSTANCE_ID
  }

  /** 复用已有微信账户节点；缺失则创建（幂等，不产生重复 account 节点）。 */
  private ensureWechatAccount(): { id: string } {
    const existing = this.nodes
      .list({ archived: false })
      .filter((n) => n.kind === 'account' && n.templateId === 'builtin.account.wechat')
    const found = existing[0]
    if (existing.length === 1 && found) return found
    return this.nodes.create({
      kind: 'account',
      templateId: 'builtin.account.wechat',
      name: '微信支付',
    })
  }
}
