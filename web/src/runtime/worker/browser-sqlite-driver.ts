/**
 * 浏览器 SQLite 驱动（Domain Worker 内）。
 *
 * 设计要点：
 * 1. 直接加载 `@sqlite.org/sqlite-wasm` 模块（不用已 deprecated 的 Worker1/Promiser1
 *    协议），初始化一次后 DB 句柄的 exec/prepare 全部同步 —— 与 Core 的
 *    同步 `SqliteDriver` 契约完全一致。
 * 2. 使用内存数据库（`:memory:`），因此不需要 OPFS、不需要 COOP/COEP 跨源隔离。
 *    持久化由加密快照层负责（见 runtime/vault-store.ts）：内存中才有明文图。
 * 3. boolean 归一化为 0/1（SQLite 无 boolean）。
 */
import type { SqlRow, SqlValue, SqliteDriver, SqliteStatement } from '@core/db/driver.ts'

type Oo1Db = {
  exec: (arg: string | Record<string, unknown>) => unknown
  selectObjects?: (sql: string, bind?: unknown[]) => unknown[]
  changes: () => number
  close: () => void
}

export interface SqliteWasmModule {
  oo1: { DB: new (file: string, flags?: string) => unknown }
  version?: { libVersion?: string }
}

export interface BrowserSqliteDriverInit {
  module: SqliteWasmModule
  file?: string
}

function bindable(value: SqlValue): null | number | bigint | string | Uint8Array {
  if (typeof value === 'boolean') return value ? 1 : 0
  return value
}

function normalizeRow(row: Record<string, unknown>): SqlRow {
  const out: SqlRow = {}
  for (const [key, value] of Object.entries(row)) {
    if (value === null || value === undefined) {
      out[key] = null
    } else if (
      typeof value === 'string' ||
      typeof value === 'number' ||
      typeof value === 'bigint' ||
      value instanceof Uint8Array
    ) {
      out[key] = value as SqlValue
    } else {
      out[key] = String(value)
    }
  }
  return out
}

class BrowserStatement implements SqliteStatement {
  constructor(
    private readonly db: Oo1Db,
    private readonly sql: string,
  ) {}

  run(...params: SqlValue[]): { changes: number | bigint } {
    this.db.exec({ sql: this.sql, bind: params.map(bindable) })
    return { changes: this.db.changes() }
  }

  get(...params: SqlValue[]): SqlRow | undefined {
    const rows = this.all(...params)
    return rows[0]
  }

  all(...params: SqlValue[]): SqlRow[] {
    const result = this.db.exec({
      sql: this.sql,
      bind: params.map(bindable),
      rowMode: 'object',
      returnValue: 'resultRows',
    })
    if (!Array.isArray(result)) return []
    return result.map((row) => normalizeRow(row as Record<string, unknown>))
  }
}

export class BrowserSqliteDriver implements SqliteDriver {
  private readonly db: Oo1Db
  private readonly statements = new Map<string, BrowserStatement>()
  private depth = 0
  private closed = false

  constructor(init: BrowserSqliteDriverInit) {
    const DB = init.module.oo1.DB
    this.db = new DB(init.file ?? ':memory:', 'c') as unknown as Oo1Db
    // WAL 不适用于 :memory:；显式设置确定性的基础 pragma。
    this.db.exec('PRAGMA foreign_keys = ON')
  }

  exec(sql: string): void {
    this.assertOpen()
    this.db.exec(sql)
  }

  prepare(sql: string): SqliteStatement {
    this.assertOpen()
    const cached = this.statements.get(sql)
    if (cached) return cached
    const stmt = new BrowserStatement(this.db, sql)
    this.statements.set(sql, stmt)
    return stmt
  }

  transaction<T>(fn: () => T): T {
    this.assertOpen()
    const savepoint = `sp_${this.depth}`
    if (this.depth === 0) {
      this.db.exec('BEGIN')
    } else {
      this.db.exec(`SAVEPOINT ${savepoint}`)
    }
    this.depth++
    try {
      const result = fn()
      this.depth--
      if (this.depth === 0) {
        this.db.exec('COMMIT')
      } else {
        this.db.exec(`RELEASE ${savepoint}`)
      }
      return result
    } catch (error) {
      this.depth--
      try {
        if (this.depth === 0) {
          this.db.exec('ROLLBACK')
        } else {
          this.db.exec(`ROLLBACK TO ${savepoint}`)
          this.db.exec(`RELEASE ${savepoint}`)
        }
      } catch {
        // 回滚失败不掩盖原始错误：原始错误必须向上抛出。
      }
      throw error
    }
  }

  /** 只读计数（投影/诊断用），不走 Core Repository。 */
  countRows(table: string): number {
    const result = this.prepare(`SELECT COUNT(*) AS n FROM ${table}`).get()
    const value = result?.['n']
    return typeof value === 'number' ? value : Number(value ?? 0)
  }

  close(): void {
    if (this.closed) return
    this.closed = true
    this.statements.clear()
    try {
      this.db.close()
    } catch {
      // 已关闭的 DB 再 close 会抛错；锁定时不因此失败。
    }
  }

  private assertOpen(): void {
    if (this.closed) throw new Error('sqlite driver is closed')
  }
}
