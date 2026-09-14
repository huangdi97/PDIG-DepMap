/**
 * RFC 8785 JSON Canonicalization Scheme (JCS) — 受限域实现。
 *
 * DepMap 的 JCS 使用范围：仅包含 string / 安全整数 / 纯对象 / 数组的 JSON
 * 值（.depmap header 与 AAD 不含浮点数）。字符串按 RFC 8785 §3.2.2.2 转义，
 * 对象键按 UTF-16 code unit 排序，整数用最短十进制形式。
 * 超出该值域（浮点、bigint 等）直接抛错，避免静默产生非规范输出。
 */

export class JcsError extends Error {}

function escapeString(s: string): string {
  let out = '"'
  for (const ch of s) {
    const code = ch.codePointAt(0)
    if (code === undefined) continue
    if (ch === '"') out += '\\"'
    else if (ch === '\\') out += '\\\\'
    else if (code === 0x08) out += '\\b'
    else if (code === 0x09) out += '\\t'
    else if (code === 0x0a) out += '\\n'
    else if (code === 0x0c) out += '\\f'
    else if (code === 0x0d) out += '\\r'
    else if (code < 0x20) {
      out += '\\u00' + code.toString(16).padStart(2, '0')
    } else {
      out += ch
    }
  }
  return out + '"'
}

function serialize(value: unknown): string {
  if (value === null) return 'null'
  if (value === true) return 'true'
  if (value === false) return 'false'
  const t = typeof value
  if (t === 'string') return escapeString(value as string)
  if (t === 'number') {
    const n = value as number
    if (!Number.isSafeInteger(n)) {
      throw new JcsError(`JCS restricted domain: only safe integers allowed, got ${n}`)
    }
    return String(n)
  }
  if (t === 'bigint') throw new JcsError('JCS restricted domain: bigint not allowed')
  if (Array.isArray(value)) {
    return '[' + value.map(serialize).join(',') + ']'
  }
  if (t === 'object') {
    const record = value as Record<string, unknown>
    const keys = Object.keys(record).sort()
    return '{' + keys.map((k) => `${escapeString(k)}:${serialize(record[k])}`).join(',') + '}'
  }
  throw new JcsError(`JCS restricted domain: unsupported type ${t}`)
}

/** RFC 8785 JCS（受限值域：string / safe integer / object / array / null / boolean）。 */
export function jcsStringify(value: unknown): string {
  return serialize(value)
}
