import type { ObservationFingerprintRecord } from '../domain/types.ts'
import type { SqliteDriver } from '../db/driver.ts'
import { nowIso } from '../utils/ids.ts'

export interface InsertFingerprintsResult {
  /** 本次新插入（未见过）的 fingerprint 集合 */
  fresh: string[]
  /** 已存在（重复导入）的数量 */
  duplicates: number
}

/**
 * ObservationFingerprint —— 只回答“这条原始记录以前处理过没有”。
 * UNIQUE(source, fingerprint)；不保存 raw transaction id / merchant / amount。
 */
export class FingerprintRepository {
  private readonly driver: SqliteDriver

  constructor(driver: SqliteDriver) {
    this.driver = driver
  }

  exists(source: string, fingerprint: string): boolean {
    const row = this.driver
      .prepare(`SELECT 1 AS x FROM observation_fingerprints WHERE source = ? AND fingerprint = ?`)
      .get(source, fingerprint)
    return row !== undefined
  }

  /** 会话批量插入：先在内存去重，再逐个 INSERT OR IGNORE。 */
  insertBatch(records: ObservationFingerprintRecord[]): InsertFingerprintsResult {
    return this.driver.transaction(() => {
      const fresh: string[] = []
      let duplicates = 0
      for (const rec of records) {
        if (this.exists(rec.source, rec.fingerprint)) {
          duplicates += 1
          continue
        }
        this.driver
          .prepare(
            `INSERT INTO observation_fingerprints (fingerprint, source, fingerprint_version, import_session_id, first_seen_at)
             VALUES (?, ?, ?, ?, ?)`,
          )
          .run(
            rec.fingerprint,
            rec.source,
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
}
