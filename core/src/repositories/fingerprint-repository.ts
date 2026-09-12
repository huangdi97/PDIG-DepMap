import type { SqliteDriver } from '../db/driver.ts'
import { nowIso } from '../utils/ids.ts'

export interface InsertFingerprintsResult {
  /** 本次新插入（未见过）的 fingerprint 集合 */
  fresh: string[]
  /** 已存在（重复导入）的数量 */
  duplicates: number
}

export interface ScopedFingerprintRecord {
  fingerprint: string
  sourceInstanceId: string
  /** legacy 展示字段（v1 的 source 列保留） */
  source?: string
  fingerprintVersion: number
  importSessionId: string
  firstSeenAt?: string
}

/**
 * ObservationFingerprint v2 —— 按 SourceInstance 隔离。
 * UNIQUE(source_instance_id, fingerprint_version, fingerprint)。
 * 不保存 raw transaction id / merchant / amount。
 */
export class FingerprintRepository {
  private readonly driver: SqliteDriver

  constructor(driver: SqliteDriver) {
    this.driver = driver
  }

  exists(sourceInstanceId: string, fingerprint: string, fingerprintVersion = 1): boolean {
    const row = this.driver
      .prepare(
        `SELECT 1 AS x FROM observation_fingerprints WHERE source_instance_id = ? AND fingerprint_version = ? AND fingerprint = ?`,
      )
      .get(sourceInstanceId, fingerprintVersion, fingerprint)
    return row !== undefined
  }

  /** 会话批量插入：先在内存去重，再逐个插入（UNIQUE 兜底）。 */
  insertBatch(records: ScopedFingerprintRecord[]): InsertFingerprintsResult {
    return this.driver.transaction(() => {
      const fresh: string[] = []
      let duplicates = 0
      for (const rec of records) {
        if (this.exists(rec.sourceInstanceId, rec.fingerprint, rec.fingerprintVersion)) {
          duplicates += 1
          continue
        }
        this.driver
          .prepare(
            `INSERT INTO observation_fingerprints (fingerprint, source_instance_id, source, fingerprint_version, import_session_id, first_seen_at)
             VALUES (?, ?, ?, ?, ?, ?)`,
          )
          .run(
            rec.fingerprint,
            rec.sourceInstanceId,
            rec.source ?? '',
            rec.fingerprintVersion,
            rec.importSessionId,
            rec.firstSeenAt ?? nowIso(),
          )
        fresh.push(rec.fingerprint)
      }
      return { fresh, duplicates }
    })
  }

  countAll(): number {
    const row = this.driver.prepare(`SELECT COUNT(*) AS c FROM observation_fingerprints`).get()
    return Number(row?.c ?? 0)
  }

  countByInstance(sourceInstanceId: string): number {
    const row = this.driver
      .prepare(`SELECT COUNT(*) AS c FROM observation_fingerprints WHERE source_instance_id = ?`)
      .get(sourceInstanceId)
    return Number(row?.c ?? 0)
  }
}
