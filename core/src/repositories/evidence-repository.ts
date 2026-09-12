import type { Evidence } from '../domain/types.ts'
import type { SqliteDriver } from '../db/driver.ts'
import { newId, nowIso } from '../utils/ids.ts'
import { unknownToString } from './meta-repository.ts'

function rowToEvidence(row: Record<string, unknown>): Evidence {
  return {
    id: String(row.id),
    proposalKey: String(row.proposal_key),
    sourceInstanceId: String(row.source_instance_id),
    adapterId: String(row.adapter_id),
    adapterVersion: Number(row.adapter_version),
    evidenceKind: unknownToString(row.evidence_kind ?? 'transaction_stream', 'transaction_stream'),
    sourceType: String(row.source_type),
    parserId: String(row.parser_id),
    parserVersion: Number(row.parser_version),
    lastImportSessionId: String(row.last_import_session_id),
    firstObservedAt: String(row.first_observed_at),
    lastObservedAt: String(row.last_observed_at),
    observationCount: Number(row.observation_count),
    createdAt: String(row.created_at),
    updatedAt: String(row.updated_at),
  }
}

export interface AccumulateEvidenceInput {
  proposalKey: string
  /** Schema v2：Evidence 按 SourceInstance 分流（同一 proposal 多 provenance） */
  sourceInstanceId: string
  adapterId: string
  adapterVersion: number
  evidenceKind?: string | undefined
  sourceType: string
  parserId: string
  parserVersion: number
  importSessionId: string
  /** 本批新 unique observations 的最早 observedAt */
  firstObservedAt: string
  /** 本批新 unique observations 的最晚 observedAt */
  lastObservedAt: string
  newObservations: number
}

/**
 * Evidence v2 —— 每个 (proposalKey, sourceInstanceId) 一条累计摘要流。
 * 同一现实候选的不同数据源保持独立计数，禁止粗暴相加驱动决策（GOAL MVP02 §9/§24）。
 * 仍是累计摘要而非账本：first=min / last=max / count 只加不减。
 */
export class EvidenceRepository {
  private readonly driver: SqliteDriver

  constructor(driver: SqliteDriver) {
    this.driver = driver
  }

  getByProposalKey(proposalKey: string): Evidence | null {
    const row = this.driver
      .prepare(`SELECT * FROM evidence WHERE proposal_key = ?`)
      .get(proposalKey)
    return row ? rowToEvidence(row) : null
  }

  /** 指定 proposal 的全部证据流（多源 provenance）。 */
  listByProposalKey(proposalKey: string): Evidence[] {
    return this.driver
      .prepare(`SELECT * FROM evidence WHERE proposal_key = ? ORDER BY created_at, id`)
      .all(proposalKey)
      .map(rowToEvidence)
  }

  getByProposalKeyAndInstance(proposalKey: string, sourceInstanceId: string): Evidence | null {
    const row = this.driver
      .prepare(`SELECT * FROM evidence WHERE proposal_key = ? AND source_instance_id = ?`)
      .get(proposalKey, sourceInstanceId)
    return row ? rowToEvidence(row) : null
  }

  getById(id: string): Evidence | null {
    const row = this.driver.prepare(`SELECT * FROM evidence WHERE id = ?`).get(id)
    return row ? rowToEvidence(row) : null
  }

  accumulate(input: AccumulateEvidenceInput): { evidence: Evidence; created: boolean } {
    return this.driver.transaction(() => {
      const now = nowIso()
      const existing = this.getByProposalKeyAndInstance(input.proposalKey, input.sourceInstanceId)
      if (!existing) {
        const id = newId()
        this.driver
          .prepare(
            `INSERT INTO evidence (id, proposal_key, source_instance_id, adapter_id, adapter_version, evidence_kind, source_type, parser_id, parser_version, last_import_session_id, first_observed_at, last_observed_at, observation_count, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
          )
          .run(
            id,
            input.proposalKey,
            input.sourceInstanceId,
            input.adapterId,
            input.adapterVersion,
            input.evidenceKind ?? 'transaction_stream',
            input.sourceType,
            input.parserId,
            input.parserVersion,
            input.importSessionId,
            input.firstObservedAt,
            input.lastObservedAt,
            input.newObservations,
            now,
            now,
          )
        return { evidence: this.getById(id) as Evidence, created: true }
      }
      const first =
        input.firstObservedAt < existing.firstObservedAt
          ? input.firstObservedAt
          : existing.firstObservedAt
      const last =
        input.lastObservedAt > existing.lastObservedAt
          ? input.lastObservedAt
          : existing.lastObservedAt
      this.driver
        .prepare(
          `UPDATE evidence SET last_import_session_id = ?, first_observed_at = ?, last_observed_at = ?, observation_count = observation_count + ?, parser_version = ?, adapter_version = ?, updated_at = ?
           WHERE id = ?`,
        )
        .run(
          input.importSessionId,
          first,
          last,
          input.newObservations,
          input.parserVersion,
          input.adapterVersion,
          now,
          existing.id,
        )
      return { evidence: this.getById(existing.id) as Evidence, created: false }
    })
  }
}
