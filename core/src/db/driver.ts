/**
 * 同步 SQLite 驱动接口。
 *
 * Core 的 Repository / Migration 只依赖此接口；平台层（Android SQLCipher /
 * HarmonyOS ArkData / iOS SQLCipher）在各自 Adapter 内提供等价实现，
 * 保证三端 Repository 行为一致（CANONICAL_DESIGN §8.2）。
 */

export type SqlValue = null | number | string | Uint8Array

export interface SqlRow {
  [column: string]: SqlValue | undefined
}

export interface SqliteStatement {
  run(...params: SqlValue[]): { changes: number | bigint }
  get(...params: SqlValue[]): SqlRow | undefined
  all(...params: SqlValue[]): SqlRow[]
}

export interface SqliteDriver {
  exec(sql: string): void
  prepare(sql: string): SqliteStatement
  /**
   * 事务执行；出错回滚并重新抛出，不留下半迁移/半写入状态。
   * 支持嵌套（内层使用 SAVEPOINT）。
   */
  transaction<T>(fn: () => T): T
  /** 关闭连接。 */
  close(): void
}
