/**
 * 测试夹具：graph payload 等价性。
 *
 * 背景（Canonical v4 / schema-v4-persistence）：
 * `meta.schema_version` 与 `meta.schema_version_updated_at` 属于**本地数据库元数据**，
 * 不属于可移植的逻辑图（见 graph-serialize.importGraph 的注释）。导入到全新库时，
 * 该库会保留自己的迁移时间戳，因此两个独立迁移出来的库不会逐字节相同。
 *
 * 因此：
 * - 其余所有表、所有列必须逐字节一致（强断言）；
 * - 唯一允许不同的字段是本地 schema 元数据，且必须被显式列出。
 */

export function normalizeLocalSchemaMeta(payloadJson: string): string {
  const payload = JSON.parse(payloadJson) as Record<string, unknown>
  const meta = payload['meta']
  if (Array.isArray(meta)) {
    payload['meta'] = meta.map((row) => {
      const record = row as Record<string, unknown>
      if (record['key'] === 'schema_version_updated_at') {
        return { ...record, value: '<local-db-metadata>' }
      }
      return record
    })
  }
  return JSON.stringify(payload)
}

/** 返回两个 payload 之间允许出现的差异（必须是本地 schema 元数据）。 */
export function localMetaDifferences(a: string, b: string): string[] {
  const left = JSON.parse(a) as Record<string, unknown>
  const right = JSON.parse(b) as Record<string, unknown>
  const differences: string[] = []
  const tables = new Set([...Object.keys(left), ...Object.keys(right)])
  for (const table of tables) {
    if (table === 'meta') continue
    if (JSON.stringify(left[table]) !== JSON.stringify(right[table])) differences.push(table)
  }
  const leftMeta = Array.isArray(left['meta'])
    ? (left['meta'] as Array<Record<string, unknown>>)
    : []
  const rightMeta = Array.isArray(right['meta'])
    ? (right['meta'] as Array<Record<string, unknown>>)
    : []
  const keys = new Set([...leftMeta, ...rightMeta].map((row) => String(row['key'])))
  for (const key of keys) {
    const l = leftMeta.find((row) => String(row['key']) === key)
    const r = rightMeta.find((row) => String(row['key']) === key)
    if (JSON.stringify(l) !== JSON.stringify(r)) differences.push(`meta.${key}`)
  }
  return differences.sort()
}
