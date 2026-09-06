/** id / 时间工具。测试可注入固定值保证 deterministic。 */

export function newId(): string {
  return crypto.randomUUID()
}

export function nowIso(): string {
  return new Date().toISOString()
}

export interface IdFactory {
  (): string
}

export interface Clock {
  (): string
}
