import type { Observation, ObservationDirection } from '../../domain/types.ts'

/**
 * 微信支付账单 Parser — MVP 第一解析器 (GOAL §11 / CANONICAL §6.5)
 *
 * pure / deterministic：
 * - 编码：UTF-8 BOM → UTF-8；严格 UTF-8 失败 → GB18030
 * - 表头说明行：定位 "交易时间..." 列头行
 * - 金额：¥ 前缀、千分位容错
 * - 收/支：收入/支出/中性交易
 * - 退款/撤销：进入 status，不剔除观测事实
 * - malformed row：计入 errors，不抛出整次导入
 * - 不持久化：Observation 仅存在于导入会话内存
 */

export const WECHAT_PARSER_ID = 'wechat'
export const WECHAT_PARSER_VERSION = 1

export interface ParseError {
  line: number
  reason: string
}

export interface WechatParseResult {
  observations: Observation[]
  errors: ParseError[]
  /** 检测到的来源标记（用于 ImportSession 元数据） */
  sourceLabel: string
}

export interface ParsedCsvCell {
  text: string
}

/** 解析单行 CSV（支持引号转义与引号内逗号/换行）。 */
export function parseCsvLine(line: string): string[] {
  const cells: string[] = []
  let cur = ''
  let inQuotes = false
  for (let i = 0; i < line.length; i++) {
    const ch = line[i]
    if (inQuotes) {
      if (ch === '"') {
        if (line[i + 1] === '"') {
          cur += '"'
          i++
        } else {
          inQuotes = false
        }
      } else {
        cur += ch
      }
    } else if (ch === '"') {
      inQuotes = true
    } else if (ch === ',') {
      cells.push(cur)
      cur = ''
    } else {
      cur += ch
    }
  }
  cells.push(cur)
  return cells
}

/** 编码检测与解码：UTF-8 BOM → utf-8；严格 UTF-8 失败 → gb18030。 */
export function decodeBill(raw: Uint8Array): { text: string; encoding: 'utf-8' | 'gb18030' } {
  if (raw.length >= 3 && raw[0] === 0xef && raw[1] === 0xbb && raw[2] === 0xbf) {
    return { text: new TextDecoder('utf-8').decode(raw.slice(3)), encoding: 'utf-8' }
  }
  try {
    const text = new TextDecoder('utf-8', { fatal: true }).decode(raw)
    return { text, encoding: 'utf-8' }
  } catch {
    return { text: new TextDecoder('gb18030').decode(raw), encoding: 'gb18030' }
  }
}

/** 自动识别来源：微信账单特征。 */
export function detect(text: string): number {
  if (text.includes('微信支付账单明细')) return 0.99
  if (text.includes('交易时间') && text.includes('交易对方') && text.includes('收/支')) return 0.9
  return 0
}

const EXPECTED_COLUMNS = [
  '交易时间',
  '交易类型',
  '交易对方',
  '商品',
  '收/支',
  '金额(元)',
  '支付方式',
  '当前状态',
  '交易单号',
  '商户单号',
  '备注',
]

function findHeaderRowIndex(lines: string[]): number {
  for (let i = 0; i < lines.length; i++) {
    const cells = parseCsvLine(lines[i] ?? '')
    if (cells[0]?.trim() === '交易时间') return i
  }
  return -1
}

function parseAmount(raw: string): number | null {
  const s = raw.trim().replace(/[¥￥,\s]/g, '')
  if (s === '') return null
  const n = Number(s)
  if (!Number.isFinite(n)) return null
  return Math.round(n * 100) / 100
}

function parseDirection(raw: string): ObservationDirection | null {
  const s = raw.trim()
  if (s === '收入') return 'in'
  if (s === '支出') return 'out'
  if (s === '/' || s === '中性交易' || s === '') return 'neutral'
  return null
}

/** '2026-01-15 10:23:00' → ISO8601 +08:00（微信账单为中国大陆本地时间）。 */
export function parseWechatTime(raw: string): string | null {
  const m = /^(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})$/.exec(raw.trim())
  if (!m) return null
  const [, y, mo, d, h, mi, s] = m
  const month = Number(mo)
  const day = Number(d)
  const hour = Number(h)
  const minute = Number(mi)
  const second = Number(s)
  if (month < 1 || month > 12) return null
  if (day < 1 || day > 31) return null
  if (hour > 23 || minute > 59 || second > 59) return null
  return `${y}-${mo}-${d}T${h}:${mi}:${s}+08:00`
}

export function parseWechatBill(raw: Uint8Array): WechatParseResult {
  const { text } = decodeBill(raw)
  const lines = text.split(/\r?\n/)
  const observations: Observation[] = []
  const errors: ParseError[] = []

  const headerIdx = findHeaderRowIndex(lines)
  if (headerIdx === -1) {
    return {
      observations,
      errors: [{ line: 0, reason: 'missing column header row (交易时间...)' }],
      sourceLabel: 'wechat',
    }
  }
  // 列头行本身校验（列漂移容错：只要求首列匹配）
  const headerCells = parseCsvLine(lines[headerIdx] ?? '')
  if (headerCells[0]?.trim() !== '交易时间' || headerCells.length < EXPECTED_COLUMNS.length - 3) {
    errors.push({ line: headerIdx + 1, reason: 'unexpected column header layout' })
  }

  for (let i = headerIdx + 1; i < lines.length; i++) {
    const line = lines[i] ?? ''
    if (line.trim() === '') continue
    const cells = parseCsvLine(line)
    if (cells.length < 9) {
      errors.push({ line: i + 1, reason: `expected >=9 columns, got ${cells.length}` })
      continue
    }
    const occurredAt = parseWechatTime(cells[0] ?? '')
    if (occurredAt === null) {
      errors.push({ line: i + 1, reason: `bad transaction time: ${cells[0]?.slice(0, 19)}` })
      continue
    }
    const amount = parseAmount(cells[5] ?? '')
    if (amount === null) {
      errors.push({ line: i + 1, reason: `bad amount: ${cells[5]?.slice(0, 12)}` })
      continue
    }
    const direction = parseDirection(cells[4] ?? '')
    if (direction === null) {
      errors.push({ line: i + 1, reason: `bad direction: ${cells[4]?.slice(0, 8)}` })
      continue
    }
    const sourceTxnId = (cells[8] ?? '').trim()
    const merchantTxnId = (cells[9] ?? '').trim()
    const status = (cells[7] ?? '').trim()

    observations.push({
      source: 'wechat',
      sourceTxnId: sourceTxnId === '' ? null : sourceTxnId,
      merchantTxnId: merchantTxnId === '' ? null : merchantTxnId,
      occurredAt,
      merchantRaw: (cells[2] ?? '').trim(),
      description: (cells[3] ?? '').trim(),
      amount: Math.abs(amount),
      currency: 'CNY',
      direction,
      paymentMethodRaw: (cells[6] ?? '').trim(),
      status,
      note: (cells[10] ?? '').trim(),
    })
  }

  return { observations, errors, sourceLabel: 'wechat' }
}
