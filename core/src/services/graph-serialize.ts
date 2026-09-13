import type { SqliteDriver } from '../db/driver.ts'
import {
  SCHEMA_VERSION,
  LEGACY_WECHAT_SOURCE_INSTANCE_ID,
  LEGACY_WECHAT_ADAPTER_ID,
  LEGACY_WECHAT_ADAPTER_VERSION,
} from '../schema/migrations.ts'
import { unknownToString } from '../repositories/meta-repository.ts'

/**
 * 逻辑图序列化 —— `.depmap` 备份 payload 层（GOAL MVP02 §15）。
 *
 * 关键分离：**crypto 容器 formatVersion（V1）与 payload schemaVersion（3）互相独立**。
 * - export：读取全部持久化实体（含 meta/source_instances；graph_revision 随 meta 行）→ payload JSON (schemaVersion=3)
 * - import：decrypt（容器层不变）→ payload schemaVersion 校验 →
 *   v1 payload 先 in-memory migrate 到 v2 → 完整校验 → 单事务原子替换；失败回滚
 * - 等价性：export → import → export 深度一致（idempotency 测试）
 */

export const GRAPH_PAYLOAD_KIND = 'depmap-logical-graph'
export const GRAPH_PAYLOAD_VERSION = 3

interface PayloadTable {
  table: string
  columns: string[]
}

const PAYLOAD_TABLES: PayloadTable[] = [
  { table: 'meta', columns: ['key', 'value'] },
  {
    table: 'source_instances',
    columns: [
      'id',
      'adapter_id',
      'adapter_version',
      'source_kind',
      'provider_id',
      'account_node_id',
      'label',
      'country',
      'jurisdiction',
      'currencies_json',
      'state',
      'created_at',
      'updated_at',
      'last_ingested_at',
    ],
  },
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
      'verification_basis_type',
      'verification_basis_json',
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
      'verification_basis_type',
      'verification_basis_json',
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
      'decision',
      'decided_at',
      'criticality_decision',
      'observation_count',
      'rejected_at',
      'rejected_at_stream_counts_json',
      'created_at',
      'updated_at',
    ],
  },
  { table: 'proposal_evidence_refs', columns: ['proposal_key', 'evidence_id', 'position'] },
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
      'source_instance_id',
      'adapter_id',
      'adapter_version',
      'evidence_kind',
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
    columns: [
      'fingerprint',
      'source_instance_id',
      'source',
      'fingerprint_version',
      'import_session_id',
      'first_seen_at',
    ],
  },
  {
    table: 'import_sessions',
    columns: [
      'id',
      'source_type',
      'parser_id',
      'parser_version',
      'source_instance_id',
      'adapter_id',
      'adapter_version',
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

const DELETE_ORDER = [
  'proposal_evidence_refs',
  'dependency_groups',
  'dependencies',
  'dependency_group_proposals',
  'dependency_proposals',
  'evidence',
  'observation_fingerprints',
  'import_sessions',
  'source_instances',
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

export class GraphImportError extends Error {}

function tableRows(
  payload: Record<string, unknown>,
  table: string,
  requiredColumns: string[],
): Array<Record<string, unknown>> {
  const rows = payload[table]
  if (!Array.isArray(rows)) {
    throw new GraphImportError(`payload.${table} must be an array`)
  }
  for (const row of rows) {
    if (row === null || typeof row !== 'object' || Array.isArray(row)) {
      throw new GraphImportError(`payload.${table} rows must be objects`)
    }
    for (const col of requiredColumns) {
      if (!(col in (row as Record<string, unknown>))) {
        throw new GraphImportError(`payload.${table} row missing column ${col}`)
      }
    }
  }
  return rows as Array<Record<string, unknown>>
}

/**
 * v1 payload → v2 payload（in-memory migrate，GOAL MVP02 §15）：
 * - 生成 deterministic legacy WeChat SourceInstance
 * - fingerprints/evidence/import_sessions 归属 legacy 实例
 * - evidence_id → proposal_evidence_refs
 * - rejected_at_observation_count → rejected_at_stream_counts_json（空快照，
 *   legacy 单流计数以 evidence 流计数为准）
 * - verification basis 补 user_confirmed
 */
export function migratePayloadV1toV2(payloadJson: string): string {
  let parsed: unknown
  try {
    parsed = JSON.parse(payloadJson)
  } catch {
    throw new GraphImportError('payload is not valid JSON')
  }
  if (parsed === null || typeof parsed !== 'object' || Array.isArray(parsed)) {
    throw new GraphImportError('payload must be an object')
  }
  const v1 = parsed as Record<string, unknown>
  if (v1['payloadKind'] !== GRAPH_PAYLOAD_KIND) {
    throw new GraphImportError(`payloadKind must be ${GRAPH_PAYLOAD_KIND}`)
  }
  if (v1['payloadVersion'] !== 1) {
    throw new GraphImportError(
      `migratePayloadV1toV2 expects payloadVersion 1, got ${String(v1['payloadVersion'])}`,
    )
  }

  const legacyNow = '1970-01-01T00:00:00.000Z'
  const legacyInstance = {
    id: LEGACY_WECHAT_SOURCE_INSTANCE_ID,
    adapter_id: LEGACY_WECHAT_ADAPTER_ID,
    adapter_version: LEGACY_WECHAT_ADAPTER_VERSION,
    source_kind: 'statement_file',
    provider_id: null,
    account_node_id: null,
    label: 'Legacy WeChat Statement Source',
    country: null,
    jurisdiction: null,
    currencies_json: '["CNY"]',
    state: 'active',
    created_at: legacyNow,
    updated_at: legacyNow,
    last_ingested_at: null,
  }

  const sourceInstances = [legacyInstance]
  const fingerprints = (
    (v1['observation_fingerprints'] as Array<Record<string, unknown>>) ?? []
  ).map((r) => ({
    fingerprint: r['fingerprint'],
    source_instance_id: LEGACY_WECHAT_SOURCE_INSTANCE_ID,
    source: unknownToString(r['source']),
    fingerprint_version: r['fingerprint_version'] ?? 1,
    import_session_id: r['import_session_id'],
    first_seen_at: r['first_seen_at'],
  }))
  const evidence = ((v1['evidence'] as Array<Record<string, unknown>>) ?? []).map((r) => ({
    id: r['id'],
    proposal_key: r['proposal_key'],
    source_instance_id: LEGACY_WECHAT_SOURCE_INSTANCE_ID,
    adapter_id: LEGACY_WECHAT_ADAPTER_ID,
    adapter_version: Number(r['parser_version'] ?? 1),
    evidence_kind: 'transaction_stream',
    source_type: r['source_type'],
    parser_id: r['parser_id'],
    parser_version: r['parser_version'],
    last_import_session_id: r['last_import_session_id'],
    first_observed_at: r['first_observed_at'],
    last_observed_at: r['last_observed_at'],
    observation_count: r['observation_count'],
    created_at: r['created_at'],
    updated_at: r['updated_at'],
  }))
  const proposals = ((v1['dependency_proposals'] as Array<Record<string, unknown>>) ?? []).map(
    (r) => {
      const out: Record<string, unknown> = { ...r }
      delete out['evidence_id']
      out['rejected_at_stream_counts_json'] = null
      return out
    },
  )
  const evidenceRefs = ((v1['dependency_proposals'] as Array<Record<string, unknown>>) ?? [])
    .filter((r) => r['evidence_id'] !== null && r['evidence_id'] !== undefined)
    .map((r, i) => ({ proposal_key: r['key'], evidence_id: r['evidence_id'], position: i }))
  const sessions = ((v1['import_sessions'] as Array<Record<string, unknown>>) ?? []).map((r) => ({
    ...r,
    source_instance_id: LEGACY_WECHAT_SOURCE_INSTANCE_ID,
    adapter_id: LEGACY_WECHAT_ADAPTER_ID,
    adapter_version: LEGACY_WECHAT_ADAPTER_VERSION,
  }))
  const deps = ((v1['dependencies'] as Array<Record<string, unknown>>) ?? []).map((r) => ({
    ...r,
    verification_basis_type: r['verification_basis_type'] ?? 'user_confirmed',
    verification_basis_json: r['verification_basis_json'] ?? null,
  }))
  const groups = ((v1['dependency_groups'] as Array<Record<string, unknown>>) ?? []).map((r) => ({
    ...r,
    verification_basis_type: r['verification_basis_type'] ?? 'user_confirmed',
    verification_basis_json: r['verification_basis_json'] ?? null,
  }))

  const v2: Record<string, unknown> = {
    ...v1,
    payloadVersion: 2, // v1→v2 迁移器固定输出 v2 形状（后续由 migratePayloadV2toV3 升 v3）
    schemaVersion: 2,
  }
  v2['source_instances'] = sourceInstances
  v2['observation_fingerprints'] = fingerprints
  v2['evidence'] = evidence
  v2['dependency_proposals'] = proposals
  v2['proposal_evidence_refs'] = evidenceRefs
  v2['import_sessions'] = sessions
  v2['dependencies'] = deps
  v2['dependency_groups'] = groups
  return JSON.stringify(v2)
}

export interface GraphImportResult {
  imported: Record<string, number>
}

/**
 * v2 payload → v3 payload（in-memory migrate，MVP03 §57）：
 * - payloadVersion 2 → 3；schemaVersion → 3
 * - graph_revision 随 meta 行传递；v2 快照无 revision 概念 → 显式置 0
 * 纯函数：不触碰 DB。
 */
export function migratePayloadV2toV3(payloadJson: string): string {
  let parsed: unknown
  try {
    parsed = JSON.parse(payloadJson)
  } catch {
    throw new GraphImportError('payload is not valid JSON')
  }
  if (parsed === null || typeof parsed !== 'object' || Array.isArray(parsed)) {
    throw new GraphImportError('payload must be an object')
  }
  const v2 = parsed as Record<string, unknown>
  if (v2['payloadVersion'] !== 2) {
    throw new GraphImportError(
      `migratePayloadV2toV3 expects payloadVersion 2, got ${String(v2['payloadVersion'])}`,
    )
  }
  const v3: Record<string, unknown> = {
    ...v2,
    payloadVersion: GRAPH_PAYLOAD_VERSION,
    schemaVersion: SCHEMA_VERSION,
  }
  const meta = Array.isArray(v2['meta']) ? (v2['meta'] as Array<Record<string, unknown>>) : []
  if (!meta.some((row) => row['key'] === 'graph_revision')) {
    v3['meta'] = [...meta, { key: 'graph_revision', value: '0' }]
  }
  return JSON.stringify(v3)
}

/** import 前完整校验（kind/version/表集合/schemaVersion），失败绝不触碰 DB。 */
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
    throw new GraphImportError(
      `unsupported payloadVersion: ${String(payload['payloadVersion'])} (expected ${GRAPH_PAYLOAD_VERSION})`,
    )
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
    tableRows(payload, t.table, t.columns)
  }
  return { payload }
}

/** 原子导入：校验通过后单事务替换全部数据；任何失败回滚，不留半恢复状态。 */
export function importGraph(driver: SqliteDriver, payloadJson: string): GraphImportResult {
  // v1 payload → in-memory migrate 到 v2（容器 V1 解密不变，仅 payload 层升级）
  const normalized = migrateIfNeeded(payloadJson)
  const { payload } = validatePayload(normalized)
  return driver.transaction(() => {
    const imported: Record<string, number> = {}
    for (const table of DELETE_ORDER) {
      driver.exec(`DELETE FROM ${table}`)
    }
    for (const t of PAYLOAD_TABLES) {
      const rows = tableRows(payload, t.table, t.columns)
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

function migrateIfNeeded(payloadJson: string): string {
  let parsed: unknown
  try {
    parsed = JSON.parse(payloadJson)
  } catch {
    throw new GraphImportError('payload is not valid JSON')
  }
  if (parsed !== null && typeof parsed === 'object' && !Array.isArray(parsed)) {
    const v = (parsed as Record<string, unknown>)['payloadVersion']
    if (v === 1) return migratePayloadV2toV3(migratePayloadV1toV2(payloadJson))
    if (v === 2) return migratePayloadV2toV3(payloadJson)
  }
  return payloadJson
}

function toSqlValue(v: unknown): null | number | string {
  if (v === null || v === undefined) return null
  if (typeof v === 'number' || typeof v === 'string') return v
  if (typeof v === 'boolean') return v ? 1 : 0
  throw new GraphImportError(`unsupported payload value type: ${typeof v}`)
}

/** 孤儿检测（PHASE S 延续 + MVP02 SourceInstance refs）。 */
export interface OrphanReport {
  orphanDependencies: string[]
  orphanGroups: string[]
  danglingGroupMembers: string[]
  orphanEvidence: string[]
  orphanFingerprints: string[]
}

export function checkGraphIntegrity(driver: SqliteDriver): OrphanReport {
  const orphanDependencies = driver
    .prepare(
      `SELECT d.id FROM dependencies d WHERE d.from_node NOT IN (SELECT id FROM nodes) OR d.to_node NOT IN (SELECT id FROM nodes)`,
    )
    .all()
    .map((r) => String((r as Record<string, unknown>).id))
  const orphanGroups = driver
    .prepare(
      `SELECT g.id FROM dependency_groups g WHERE g.target_node_id NOT IN (SELECT id FROM nodes)`,
    )
    .all()
    .map((r) => String((r as Record<string, unknown>).id))
  const danglingGroupMembers = driver
    .prepare(
      `SELECT g.id FROM dependency_groups g WHERE EXISTS (SELECT 1 FROM json_each(g.member_edge_ids_json) je WHERE je.value NOT IN (SELECT id FROM dependencies))`,
    )
    .all()
    .map((r) => String((r as Record<string, unknown>).id))
  const orphanEvidence = driver
    .prepare(
      `SELECT e.id FROM evidence e WHERE e.source_instance_id NOT IN (SELECT id FROM source_instances)`,
    )
    .all()
    .map((r) => String((r as Record<string, unknown>).id))
  const orphanFingerprints = driver
    .prepare(
      `SELECT f.rowid FROM observation_fingerprints f WHERE f.source_instance_id NOT IN (SELECT id FROM source_instances)`,
    )
    .all()
    .map((r) => String((r as Record<string, unknown>).rowid))
  return {
    orphanDependencies,
    orphanGroups,
    danglingGroupMembers,
    orphanEvidence,
    orphanFingerprints,
  }
}
