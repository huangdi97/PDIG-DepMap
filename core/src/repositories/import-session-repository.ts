import type { ImportSession } from '../domain/types.ts'
import type { SqliteDriver } from '../db/driver.ts'
import { newId, nowIso } from '../utils/ids.ts'
import { optionalString } from './meta-repository.ts'

function rowToSession(row: Record<string, unknown>): ImportSession {
  return {
    id: String(row.id),
    sourceType: String(row.source_type),
    parserId: String(row.parser_id),
    parserVersion: Number(row.parser_version),
    startedAt: String(row.started_at),
    completedAt: optionalString(row.completed_at as never),
    rawCount: Number(row.raw_count ?? 0),
    newUniqueCount: Number(row.new_unique_count ?? 0),
    duplicateCount: Number(row.duplicate_count ?? 0),
    proposalCount: Number(row.proposal_count ?? 0),
    errorCount: Number(row.error_count ?? 0),
  }
}

export class ImportSessionRepository {
  constructor(private readonly driver: SqliteDriver) {}

  start(input: {
    sourceType: string
    parserId: string
    parserVersion: number
    id?: string
  }): ImportSession {
    const id = input.id ?? newId()
    this.driver
      .prepare(
        `INSERT INTO import_sessions (id, source_type, parser_id, parser_version, started_at) VALUES (?, ?, ?, ?, ?)`,
      )
      .run(id, input.sourceType, input.parserId, input.parserVersion, nowIso())
    return this.getById(id) as ImportSession
  }

  getById(id: string): ImportSession | null {
    const row = this.driver.prepare(`SELECT * FROM import_sessions WHERE id = ?`).get(id)
    return row ? rowToSession(row) : null
  }

  update(id: string, patch: Partial<Omit<ImportSession, 'id' | 'startedAt'>>): ImportSession {
    const existing = this.getById(id)
    if (!existing) throw new Error(`import session not found: ${id}`)
    const merged = { ...existing, ...patch }
    this.driver
      .prepare(
        `UPDATE import_sessions SET completed_at = ?, raw_count = ?, new_unique_count = ?, duplicate_count = ?, proposal_count = ?, error_count = ? WHERE id = ?`,
      )
      .run(
        merged.completedAt ?? null,
        merged.rawCount,
        merged.newUniqueCount,
        merged.duplicateCount,
        merged.proposalCount,
        merged.errorCount,
        id,
      )
    return this.getById(id) as ImportSession
  }

  complete(id: string): ImportSession {
    return this.update(id, { completedAt: nowIso() })
  }
}
