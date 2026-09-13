import type { SqliteDriver } from '../db/driver.ts'

/**
 * GraphRevision（MVP03 §9–§11）—— Confirmed Reality Graph 的语义版本。
 *
 * 仅 Reality mutation 提升（在同一次 DB 事务内）：
 *   Dependency created / retired / reactivated / criticality 被用户修改
 *   DependencyGroup confirmed / retired / membership 确认变化
 *
 * 以下一律不提升：
 *   Observation / ImportSession / Fingerprint / Evidence / Proposal（创建与 confidence）/
 *   RealityDrift / DiscoveryCandidate / Timeline projection / UI 状态。
 *
 * 存储复用 meta 表（键 `graph_revision`，初始 0），不新建表、不做 event sourcing。
 */

const KEY = 'graph_revision'

export function getGraphRevision(driver: SqliteDriver): number {
  const row = driver.prepare(`SELECT value FROM meta WHERE key = ?`).get(KEY)
  if (!row) return 0
  const value = Number(row.value)
  return Number.isFinite(value) ? value : 0
}

/**
 * +1 并返回新值。**必须与 Reality mutation 处于同一次 `driver.transaction` 内调用**
 * （mutation 失败回滚时 revision 一并回滚；NodeSqliteDriver 嵌套事务经 SAVEPOINT 保证）。
 */
export function bumpGraphRevision(driver: SqliteDriver): number {
  const next = getGraphRevision(driver) + 1
  driver
    .prepare(
      `INSERT INTO meta (key, value) VALUES (?, ?)
       ON CONFLICT(key) DO UPDATE SET value = excluded.value`,
    )
    .run(KEY, String(next))
  return next
}
