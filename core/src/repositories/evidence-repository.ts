import type { Evidence } from '../domain/types.ts'
import type { SqliteDriver } from '../db/driver.ts'
import { newId, nowIso } from '../utils/ids.ts'

function rowToEvidence(row: Record<string, unknown>): Evidence {
  return {
    id: String(row.id),
    proposalKey: String(row.proposal_key),
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
 * Evidence 是累计摘要，不是账本 (AGENTS §12 / CANONICAL §5.7)：
 * - 只累计新 unique observations
 * - firstObservedAt = min(old,new)，lastObservedAt = max(old,new)
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

  getById(id: string): Evidence | null {
    const row = this.driver.prepare(`SELECT * FROM evidence WHERE id = ?`).get(id)
    return row ? rowToEvidence(row) : null
  }

  accumulate(input: AccumulateEvidenceInput): { evidence: Evidence; created: boolean } {
    return this.driver.transaction(() => {
      const now = nowIso()
      const existing = this.getByProposalKey(input.proposalKey)
      if (!existing) {
        const id = newId()
        this.driver
          .prepare(
            `INSERT INTO evidence (id, proposal_key, source_type, parser_id, parser_version, last_import_session_id, first_observed_at, last_observed_at, observation_count, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
          )
          .run(
            id,
            input.proposalKey,
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
          `UPDATE evidence SET last_import_session_id = ?, first_observed_at = ?, last_observed_at = ?, observation_count = observation_count + ?, parser_version = ?, updated_at = ?
           WHERE id = ?`,
        )
        .run(
          input.importSessionId,
          first,
          last,
          input.newObservations,
          input.parserVersion,
          now,
          existing.id,
        )
      return { evidence: this.getById(existing.id) as Evidence, created: false }
    })
  }
}
