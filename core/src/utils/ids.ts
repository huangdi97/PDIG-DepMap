/** id / 时间工具。需要 deterministic 的调用点显式传 id/时间戳，不在此注入。 */

export function newId(): string {
  return crypto.randomUUID()
}

export function nowIso(): string {
  return new Date().toISOString()
}
