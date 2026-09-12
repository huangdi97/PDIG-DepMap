import type { SqliteDriver, SqlValue } from '../db/driver.ts'

/** meta 表访问：schema_version、fpSecret、app 元数据。 */
export class MetaRepository {
  private readonly driver: SqliteDriver

  constructor(driver: SqliteDriver) {
    this.driver = driver
  }

  get(key: string): string | null {
    const row = this.driver.prepare(`SELECT value FROM meta WHERE key = ?`).get(key)
    return row ? String(row.value) : null
  }

  set(key: string, value: string): void {
    this.driver
      .prepare(
        `INSERT INTO meta (key, value) VALUES (?, ?)
         ON CONFLICT(key) DO UPDATE SET value = excluded.value`,
      )
      .run(key, value)
  }

  /** fpSecret 只在首次创建；存在即返回，不覆盖（HMAC 指纹依赖其稳定）。 */
  getOrCreateFpSecret(generate: () => string): { secret: string; created: boolean } {
    return this.driver.transaction(() => {
      const existing = this.get('fp_secret')
      if (existing !== null) return { secret: existing, created: false }
      const secret = generate()
      this.set('fp_secret', secret)
      return { secret, created: true }
    })
  }
}

export function requireString(v: SqlValue | undefined, field: string): string {
  if (typeof v !== 'string') throw new Error(`missing column: ${field}`)
  return v
}

export function optionalString(v: SqlValue | undefined): string | null {
  return v === null || v === undefined ? null : String(v)
}

/**
 * 把来自 JSON.parse 等 unknown 来源的值安全收敛为 string。
 *
 * 与 String(v) 的区别：只接受 string / number / bigint / boolean 标量；
 * 对象与数组一律返回 fallback，避免把 `[object Object]` 静默写进数据库
 * （@typescript-eslint/no-base-to-string 所防护的正是这类缺陷）。
 */
export function unknownToString(v: unknown, fallback = ''): string {
  if (typeof v === 'string') return v
  if (typeof v === 'number' || typeof v === 'bigint' || typeof v === 'boolean') {
    return String(v)
  }
  return fallback
}

export function optionalNumber(v: SqlValue | undefined): number | null {
  return v === null || v === undefined ? null : Number(v)
}
