import type { SourceInstance, SourceKind, CreateSourceInstanceInput } from '../domain/source.ts'
import type { SqlRow, SqliteDriver } from '../db/driver.ts'
import { nowIso } from '../utils/ids.ts'
import { optionalString, requireString } from './meta-repository.ts'

function rowToInstance(row: SqlRow): SourceInstance {
  return {
    id: requireString(row.id, 'id'),
    adapterId: requireString(row.adapter_id, 'adapter_id'),
    adapterVersion: Number(row.adapter_version),
    sourceKind: requireString(row.source_kind, 'source_kind') as SourceKind,
    providerId: optionalString(row.provider_id) ?? undefined,
    accountNodeId: optionalString(row.account_node_id) ?? undefined,
    label: requireString(row.label, 'label'),
    country: optionalString(row.country) ?? undefined,
    jurisdiction: optionalString(row.jurisdiction) ?? undefined,
    currencies: JSON.parse(String(row.currencies_json ?? '[]')) as string[],
    state: requireString(row.state, 'state') as SourceInstance['state'],
    createdAt: requireString(row.created_at, 'created_at'),
    updatedAt: requireString(row.updated_at, 'updated_at'),
    lastIngestedAt: optionalString(row.last_ingested_at) ?? undefined,
  }
}

/**
 * SourceInstance —— 用户具体的数据源实例（Adapter = 如何解析，Instance = 哪个来源）。
 * 不含任何账户秘密（无 number/credential 字段）。
 */
export class SourceInstanceRepository {
  private readonly driver: SqliteDriver

  constructor(driver: SqliteDriver) {
    this.driver = driver
  }

  create(input: CreateSourceInstanceInput): SourceInstance {
    const id = input.id ?? crypto.randomUUID()
    const now = nowIso()
    this.driver
      .prepare(
        `INSERT INTO source_instances (id, adapter_id, adapter_version, source_kind, provider_id, account_node_id, label, country, jurisdiction, currencies_json, state, created_at, updated_at, last_ingested_at)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'active', ?, ?, NULL)`,
      )
      .run(
        id,
        input.adapterId,
        input.adapterVersion,
        input.sourceKind,
        input.providerId ?? null,
        input.accountNodeId ?? null,
        input.label,
        input.country ?? null,
        input.jurisdiction ?? null,
        JSON.stringify(input.currencies ?? []),
        now,
        now,
      )
    return this.getById(id) as SourceInstance
  }

  getById(id: string): SourceInstance | null {
    const row = this.driver.prepare(`SELECT * FROM source_instances WHERE id = ?`).get(id)
    return row ? rowToInstance(row) : null
  }

  getExisting(id: string): SourceInstance {
    const instance = this.getById(id)
    if (!instance) throw new Error(`source instance not found: ${id}`)
    return instance
  }

  listByAdapter(adapterId: string): SourceInstance[] {
    return this.driver
      .prepare(`SELECT * FROM source_instances WHERE adapter_id = ? ORDER BY created_at, id`)
      .all(adapterId)
      .map(rowToInstance)
  }

  listAll(): SourceInstance[] {
    return this.driver
      .prepare(`SELECT * FROM source_instances ORDER BY created_at, id`)
      .all()
      .map(rowToInstance)
  }

  /** retired 实例保留全部 provenance（指纹/evidence 不迁移不删除）。 */
  retire(id: string): SourceInstance {
    const existing = this.getExisting(id)
    if (existing.state === 'retired') return existing
    this.driver
      .prepare(`UPDATE source_instances SET state = 'retired', updated_at = ? WHERE id = ?`)
      .run(nowIso(), id)
    return this.getById(id) as SourceInstance
  }

  touchIngested(id: string, at: string): void {
    this.getExisting(id)
    this.driver
      .prepare(`UPDATE source_instances SET last_ingested_at = ?, updated_at = ? WHERE id = ?`)
      .run(at, nowIso(), id)
  }

  /**
   * 绑定 / 更新 SourceInstance 的账户节点。
   *
   * 必要性：migration 创建的 legacy WeChat SourceInstance 在迁移时无法知道微信账户
   * 节点 id（当时节点可能尚不存在），因此 account_node_id 为空。WeChatStatementAdapter
   * 的 funding_source 路由以 accountNodeId 为起点，必须在导入前完成绑定。
   * 只写 account_node_id，不触碰 state / provenance。
   */
  bindAccountNode(id: string, accountNodeId: string): SourceInstance {
    this.getExisting(id)
    this.driver
      .prepare(`UPDATE source_instances SET account_node_id = ?, updated_at = ? WHERE id = ?`)
      .run(accountNodeId, nowIso(), id)
    return this.getById(id) as SourceInstance
  }

  countAll(): number {
    const row = this.driver.prepare(`SELECT COUNT(*) AS c FROM source_instances`).get()
    return Number(row?.c ?? 0)
  }
}
