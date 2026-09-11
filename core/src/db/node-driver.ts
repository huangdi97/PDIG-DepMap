import { DatabaseSync } from 'node:sqlite'
import type { SqlRow, SqlValue, SqliteDriver, SqliteStatement } from './driver.ts'

/**
 * Node 环境驱动（node:sqlite）— 仅供 Core 测试与 CLI 工具使用。
 * 平台端由 Kotlin/Swift/ArkTS Adapter 提供同语义实现。
 */

class NodeStatement implements SqliteStatement {
  private readonly stmt: ReturnType<DatabaseSync['prepare']>

  constructor(stmt: ReturnType<DatabaseSync['prepare']>) {
    this.stmt = stmt
  }

  run(...params: SqlValue[]): { changes: number | bigint } {
    const res = this.stmt.run(...(params as never[]))
    return { changes: res.changes }
  }

  get(...params: SqlValue[]): SqlRow | undefined {
    return this.stmt.get(...(params as never[])) as SqlRow | undefined
  }

  all(...params: SqlValue[]): SqlRow[] {
    return this.stmt.all(...(params as never[])) as SqlRow[]
  }
}

export class NodeSqliteDriver implements SqliteDriver {
  private db: DatabaseSync | null = null
  private txDepth = 0

  private readonly path: string

  constructor(path: string) {
    this.path = path
  }

  open(): void {
    this.db = new DatabaseSync(this.path)
    this.db.exec('PRAGMA journal_mode = WAL;')
    this.db.exec('PRAGMA foreign_keys = ON;')
  }

  private requireDb(): DatabaseSync {
    if (!this.db) throw new Error('driver not open; call open() first')
    return this.db
  }

  exec(sql: string): void {
    this.requireDb().exec(sql)
  }

  prepare(sql: string): SqliteStatement {
    return new NodeStatement(this.requireDb().prepare(sql))
  }

  transaction<T>(fn: () => T): T {
    const db = this.requireDb()
    if (this.txDepth === 0) {
      db.exec('BEGIN')
      this.txDepth = 1
      try {
        const out = fn()
        db.exec('COMMIT')
        this.txDepth = 0
        return out
      } catch (e) {
        db.exec('ROLLBACK')
        this.txDepth = 0
        throw e
      }
    } else {
      const sp = `sp_${this.txDepth}`
      this.txDepth += 1
      db.exec(`SAVEPOINT ${sp}`)
      try {
        const out = fn()
        db.exec(`RELEASE ${sp}`)
        this.txDepth -= 1
        return out
      } catch (e) {
        db.exec(`ROLLBACK TO ${sp}`)
        db.exec(`RELEASE ${sp}`)
        this.txDepth -= 1
        throw e
      }
    }
  }

  close(): void {
    this.db?.close()
    this.db = null
  }
}
