import type { SqliteDriver } from '../db/driver.ts'
import { SCHEMA_VERSION } from '../schema/migrations.ts'

/**
 * 逻辑图序列化 —— `.depmap` 备份的 payload 层（CANONICAL §11.2 备份范围）。
 *
 * 语义：
 * - export：读取全部持久化实体（含 meta）→ canonical JSON payload
 * - import：先完整校验 payload，再在单一事务内原子替换全部数据；失败回滚，
 *   不留半恢复状态（RC PHASE AL backup/restore 要求）
 * - 等价性：export → import → export 深度一致（idempotency 测试）
 */

export const GRAPH_PAYLOAD_KIND = 'depmap-logical-graph'
export const GRAPH_PAYLOAD_VERSION = 1

interface PayloadTable {
  table: string
  columns: string[]
}

const PAYLOAD_TABLES: PayloadTable[] = [
  { table: 'meta', columns: ['key', 'value'] },
  {
    table: 'nodes',
    columns: [
      'id',
      'kind',
      'template_id',
      'name',
      'issuer',
      'last4',
      'owner',
      'archived',
      'fields_json',
      'vault_ref',
      'wallet_ref',
      'created_at',
      'updated_at',
    ],
  },
  {
    table: 'dependencies',
    columns: [
      'id',
      'from_node',
      'relation',
      'to_node',
      'capability',
      'criticality',
      'group_id',
      'state',
      'origin',
      'confirmed_at',
      'last_verified_at',
      'retired_at',
      'evidence_refs_json',
      'created_at',
      'updated_at',
    ],
  },
  {
    table: 'dependency_groups',
    columns: [
      'id',
      'group_key',
      'target_node_id',
      'capability',
      'mode',
      'member_edge_ids_json',
      'state',
      'confirmed_at',
      'last_verified_at',
      'created_at',
      'updated_at',
    ],
  },
  {
    table: 'dependency_proposals',
    columns: [
      'id',
      'key',
      'from_node',
      'relation',
      'to_node',
      'capability',
      'proposal_type',
      'source',
      'parser_id',
      'parser_version',
      'confidence_score',
      'path_json',
      'evidence_id',
      'decision',
      'decided_at',
      'criticality_decision',
      'observation_count',
      'rejected_at',
      'rejected_at_observation_count',
      'created_at',
      'updated_at',
    ],
  },
  {
    table: 'dependency_group_proposals',
    columns: [
      'id',
      'key',
      'target_node_id',
      'capability',
      'mode',
      'member_dependency_keys_json',
      'decision',
      'decided_at',
      'rejected_at',
      'rejected_at_observation_count',
      'created_at',
      'updated_at',
    ],
  },
  {
    table: 'evidence',
    columns: [
      'id',
      'proposal_key',
      'source_type',
      'parser_id',
      'parser_version',
      'last_import_session_id',
      'first_observed_at',
      'last_observed_at',
      'observation_count',
      'created_at',
      'updated_at',
    ],
  },
  {
    table: 'observation_fingerprints',
    columns: ['fingerprint', 'source', 'fingerprint_version', 'import_session_id', 'first_seen_at'],
  },
  {
    table: 'import_sessions',
    columns: [
      'id',
      'source_type',
      'parser_id',
      'parser_version',
      'started_at',
      'completed_at',
      'raw_count',
      'new_unique_count',
      'duplicate_count',
      'proposal_count',
      'error_count',
    ],
  },
]

/** 删除顺序：无 FK 约束，但按逻辑依赖顺序清理保证中途状态可读。 */
const DELETE_ORDER = [
  'dependency_groups',
  'dependencies',
  'dependency_group_proposals',
  'dependency_proposals',
  'evidence',
  'observation_fingerprints',
  'import_sessions',
  'nodes',
  'meta',
]

export interface GraphExportResult {
  payloadJson: string
  counts: Record<string, number>
}

export function exportGraph(driver: SqliteDriver): GraphExportResult {
  const payload: Record<string, unknown> = {
    payloadKind: GRAPH_PAYLOAD_KIND,
    payloadVersion: GRAPH_PAYLOAD_VERSION,
    schemaVersion: SCHEMA_VERSION,
  }
  const counts: Record<string, number> = {}
  for (const t of PAYLOAD_TABLES) {
    const rows = driver.prepare(`SELECT * FROM ${t.table}`).all()
    counts[t.table] = rows.length
    payload[t.table] = rows
  }
  return { payloadJson: JSON.stringify(payload), counts }
}

export interface GraphImportResult {
  imported: Record<string, number>
}

export class GraphImportError extends Error {}

/** import 前完整校验（结构 + 表集合 + schemaVersion），失败绝不触碰 DB。 */
function validatePayload(payloadJson: string): { payload: Record<string, unknown> } {
  let parsed: unknown
  try {
    parsed = JSON.parse(payloadJson)
  } catch {
    throw new GraphImportError('payload is not valid JSON')
  }
  if (parsed === null || typeof parsed !== 'object' || Array.isArray(parsed)) {
    throw new GraphImportError('payload must be an object')
  }
  const payload = parsed as Record<string, unknown>
  if (payload['payloadKind'] !== GRAPH_PAYLOAD_KIND) {
    throw new GraphImportError(`payloadKind must be ${GRAPH_PAYLOAD_KIND}`)
  }
  if (payload['payloadVersion'] !== GRAPH_PAYLOAD_VERSION) {
    throw new GraphImportError(`unsupported payloadVersion: ${String(payload['payloadVersion'])}`)
  }
  const schemaVersion = payload['schemaVersion']
  if (typeof schemaVersion !== 'number' || !Number.isSafeInteger(schemaVersion)) {
    throw new GraphImportError('schemaVersion must be an integer')
  }
  if (schemaVersion > SCHEMA_VERSION) {
    throw new GraphImportError(
      `payload schemaVersion (${schemaVersion}) newer than supported (${SCHEMA_VERSION})`,
    )
  }
  for (const t of PAYLOAD_TABLES) {
    const rows = payload[t.table]
    if (!Array.isArray(rows)) {
      throw new GraphImportError(`payload.${t.table} must be an array`)
    }
    for (const row of rows) {
      if (row === null || typeof row !== 'object' || Array.isArray(row)) {
        throw new GraphImportError(`payload.${t.table} rows must be objects`)
      }
      for (const col of t.columns) {
        if (!(col in (row as Record<string, unknown>))) {
          throw new GraphImportError(`payload.${t.table} row missing column ${col}`)
        }
      }
    }
  }
  return { payload }
}

/** 原子导入：校验通过后单事务替换全部数据；任何失败回滚，不留半恢复状态。 */
export function importGraph(driver: SqliteDriver, payloadJson: string): GraphImportResult {
  const { payload } = validatePayload(payloadJson)
  return driver.transaction(() => {
    const imported: Record<string, number> = {}
    for (const table of DELETE_ORDER) {
      driver.exec(`DELETE FROM ${table}`)
    }
    for (const t of PAYLOAD_TABLES) {
      const rows = payload[t.table] as Array<Record<string, unknown>>
      if (rows.length === 0) {
        imported[t.table] = 0
        continue
      }
      const cols = t.columns
      const placeholders = cols.map(() => '?').join(', ')
      const stmt = driver.prepare(
        `INSERT INTO ${t.table} (${cols.join(', ')}) VALUES (${placeholders})`,
      )
      for (const row of rows) {
        stmt.run(...cols.map((c) => toSqlValue(row[c])))
      }
      imported[t.table] = rows.length
    }
    return { imported }
  })
}

function toSqlValue(v: unknown): null | number | string {
  if (v === null || v === undefined) return null
  if (typeof v === 'number' || typeof v === 'string') return v
  if (typeof v === 'boolean') return v ? 1 : 0
  throw new GraphImportError(`unsupported payload value type: ${typeof v}`)
}

/** 孤儿检测（PHASE S）：逻辑引用指向不存在的实体。 */
export interface OrphanReport {
  orphanDependencies: string[]
  orphanGroups: string[]
  danglingGroupMembers: string[]
}

export function checkGraphIntegrity(driver: SqliteDriver): OrphanReport {
  const orphanDependencies = driver
    .prepare(
      `SELECT d.id FROM dependencies d WHERE d.from_node NOT IN (SELECT id FROM nodes) OR d.to_node NOT IN (SELECT id FROM nodes)`,
    )
    .all()
    .map((r) => String(r.id))
  const orphanGroups = driver
    .prepare(
      `SELECT g.id FROM dependency_groups g WHERE g.target_node_id NOT IN (SELECT id FROM nodes)`,
    )
    .all()
    .map((r) => String(r.id))
  const danglingGroupMembers = driver
    .prepare(
      `SELECT g.id FROM dependency_groups g WHERE EXISTS (SELECT 1 FROM json_each(g.member_edge_ids_json) je WHERE je.value NOT IN (SELECT id FROM dependencies))`,
    )
    .all()
    .map((r) => String(r.id))
  return { orphanDependencies, orphanGroups, danglingGroupMembers }
}
