import type { Observation } from '../../domain/types.ts'
import type { NormalizedPaymentObservation } from '../../domain/source.ts'
import type { EvidenceSourceAdapter, SourceContext, SourceInput } from '../types.ts'

/**
 * GenericCsvAdapter —— 显式字段映射接入任意结构化 CSV（GOAL MVP02 §16/§17）。
 * 禁止 AI 自动映射：mapping 必须显式传入；canHandle 只做「提供了 mapping」的结构性判断。
 * Unknown 保持 unknown，禁止猜。
 */

export class MissingMappingError extends Error {}

/** RFC4180 风格单行解析（引号内分隔符/换行由上层按完整文件处理）。 */
export function parseCsvCells(line: string, delimiter: string): string[] {
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
    } else if (ch === delimiter) {
      cells.push(cur)
      cur = ''
    } else {
      cur += ch
    }
  }
  cells.push(cur)
  return cells
}

/** 解析完整 CSV（支持引号内换行）。 */
export function parseCsv(text: string, delimiter: string): string[][] {
  const rows: string[][] = []
  let row: string[] = []
  let cur = ''
  let inQuotes = false
  for (let i = 0; i < text.length; i++) {
    const ch = text[i]
    if (inQuotes) {
      if (ch === '"') {
        if (text[i + 1] === '"') {
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
    } else if (ch === delimiter) {
      row.push(cur)
      cur = ''
    } else if (ch === '\r' && text[i + 1] === '\n') {
      row.push(cur)
      rows.push(row)
      row = []
      cur = ''
      i++
    } else if (ch === '\r' || ch === '\n') {
      row.push(cur)
      rows.push(row)
      row = []
      cur = ''
    } else {
      cur += ch
    }
  }
  if (cur !== '' || row.length > 0) {
    row.push(cur)
    rows.push(row)
  }
  return rows
}

/** 按映射 profile 解析日期（显式 dateFormats 列表；全部失败 → null）。 */
export function parseDate(raw: string, formats: string[]): string | null {
  const s = raw.trim()
  for (const fmt of formats) {
    const m = matchFormat(s, fmt)
    if (m) return m
  }
  return null
}

function matchFormat(s: string, fmt: string): string | null {
  // 支持的 tokens：YYYY MM DD HH mm ss；分隔符任意
  const pattern = fmt
    .replace('YYYY', '(\\d{4})')
    .replace('MM', '(\\d{1,2})')
    .replace('DD', '(\\d{1,2})')
    .replace('HH', '(\\d{1,2})')
    .replace('mm', '(\\d{1,2})')
    .replace('ss', '(\\d{1,2})')
    .replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const m = new RegExp(`^${pattern}$`).exec(s)
  if (!m) return null
  const values = m.slice(1).map(Number)
  const order = ['YYYY', 'MM', 'DD', 'HH', 'mm', 'ss'].filter((t) => fmt.includes(t))
  const get = (t: string): number => values[order.indexOf(t)] ?? 0
  const y = get('YYYY')
  const mo = get('MM')
  const d = get('DD')
  const h = get('HH')
  const mi = get('mm')
  const sec = get('ss')
  if (mo < 1 || mo > 12 || h > 23 || mi > 59 || sec > 59) return null
  const utc = new Date(Date.UTC(y, mo - 1, d, h, mi, sec))
  if (
    utc.getUTCFullYear() !== y ||
    utc.getUTCMonth() !== mo - 1 ||
    utc.getUTCDate() !== d ||
    utc.getUTCHours() !== h ||
    utc.getUTCMinutes() !== mi ||
    utc.getUTCSeconds() !== sec
  ) {
    return null
  }
  const p = (v: number, w = 2) => String(v).padStart(w, '0')
  return `${p(y, 4)}-${p(mo)}-${p(d)}T${p(h)}:${p(mi)}:${p(sec)}+00:00`
}

/** 按显式 decimalSeparator 解析金额。 */
export function parseAmount(raw: string, decimalSeparator: '.' | ','): number | null {
  let s = raw.trim().replace(/[\s¥￥$€£]/g, '')
  if (s === '') return null
  if (decimalSeparator === ',') {
    s = s.replace(/\./g, '').replace(',', '.')
  } else {
    s = s.replace(/,/g, '')
  }
  const n = Number(s)
  if (!Number.isFinite(n)) return null
  return Math.round(n * 100) / 100
}

export class GenericCsvAdapter implements EvidenceSourceAdapter {
  readonly id = 'generic_csv'
  readonly version = 1
  readonly sourceKind = 'statement_file' as const
  readonly coverageMode = 'event_stream' as const
  readonly authoritativeFor: readonly string[] = []

  private lastErrors: Array<{ line: number; reason: string }> = []

  async canHandle(input: SourceInput): Promise<number> {
    await Promise.resolve()
    // 显式映射是唯一入口（保守 header suggestion 允许，但最终 mapping 必须明确）
    return input.mapping ? 0.8 : 0
  }

  async parse(input: SourceInput, context: SourceContext): Promise<Observation[]> {
    await Promise.resolve()
    const mapping = input.mapping
    if (!mapping) throw new MissingMappingError('generic_csv requires an explicit MappingProfile')
    this.lastErrors = []

    const text = decodeText(input.data, mapping.options.encoding ?? 'utf-8')
    const rawRows = parseCsv(text, mapping.options.delimiter)
    const rows = mapping.options.hasHeaderRow ? rawRows.slice(1) : rawRows
    const header = rawRows[0] ?? []

    const colIndexOf = (name?: string): number => {
      if (!name) return -1
      const idx = header.indexOf(name)
      if (idx === -1) {
        throw new MissingMappingError(`mapping column not found in header: ${name}`)
      }
      return idx
    }

    const cTxn = colIndexOf(mapping.columns.transactionId)
    const cDate = colIndexOf(mapping.columns.dateTime)
    const cAmount = mapping.columns.amount !== undefined ? colIndexOf(mapping.columns.amount) : -1
    const cDebit = mapping.columns.debit !== undefined ? colIndexOf(mapping.columns.debit) : -1
    const cCredit = mapping.columns.credit !== undefined ? colIndexOf(mapping.columns.credit) : -1
    const cDesc =
      mapping.columns.description !== undefined ? colIndexOf(mapping.columns.description) : -1
    const cCounterparty =
      mapping.columns.counterparty !== undefined ? colIndexOf(mapping.columns.counterparty) : -1
    const cCurrency =
      mapping.columns.currency !== undefined ? colIndexOf(mapping.columns.currency) : -1
    const cBalance =
      mapping.columns.balance !== undefined ? colIndexOf(mapping.columns.balance) : -1
    const cType =
      mapping.columns.transactionType !== undefined
        ? colIndexOf(mapping.columns.transactionType)
        : -1
    const cMethod =
      mapping.columns.paymentMethod !== undefined ? colIndexOf(mapping.columns.paymentMethod) : -1

    const observations: Observation[] = []
    const signMode = mapping.options.amountSignMode

    rows.forEach((cells, rowIdx) => {
      if (cells.every((c) => c.trim() === '')) return
      const line = rowIdx + (mapping.options.hasHeaderRow ? 2 : 1)

      if (cDate === -1 || cDate >= cells.length) {
        this.lastErrors.push({ line, reason: 'missing required date column' })
        return
      }
      const occurredAt = parseDate(cells[cDate] ?? '', mapping.options.dateFormats)
      if (occurredAt === null) {
        this.lastErrors.push({ line, reason: `bad date: ${cells[cDate]?.slice(0, 24)}` })
        return
      }

      let amount: number | null = null
      let direction: 'in' | 'out' | 'neutral' = 'neutral'
      if (signMode === 'debit_credit') {
        const debit =
          cDebit >= 0 ? parseAmount(cells[cDebit] ?? '', mapping.options.decimalSeparator) : null
        const credit =
          cCredit >= 0 ? parseAmount(cells[cCredit] ?? '', mapping.options.decimalSeparator) : null
        if (debit !== null && debit > 0) {
          amount = debit
          direction = 'out'
        } else if (credit !== null && credit > 0) {
          amount = credit
          direction = 'in'
        } else {
          this.lastErrors.push({ line, reason: 'missing debit/credit amount' })
          return
        }
      } else {
        amount =
          cAmount >= 0 ? parseAmount(cells[cAmount] ?? '', mapping.options.decimalSeparator) : null
        if (amount === null) {
          this.lastErrors.push({ line, reason: `bad amount: ${cells[cAmount]?.slice(0, 16)}` })
          return
        }
        if (signMode === 'signed') {
          direction = amount < 0 ? 'out' : 'in'
          amount = Math.abs(amount)
        } else {
          direction = mapping.options.positiveDirection ?? 'out'
        }
      }

      const currency = cCurrency >= 0 ? cells[cCurrency]?.trim() || undefined : undefined
      const balance =
        cBalance >= 0 ? parseAmount(cells[cBalance] ?? '', mapping.options.decimalSeparator) : null

      observations.push({
        source: this.id,
        sourceTxnId: cTxn >= 0 ? cells[cTxn]?.trim() || null : null,
        merchantTxnId: null,
        occurredAt,
        merchantRaw: cCounterparty >= 0 ? (cells[cCounterparty]?.trim() ?? '') : '',
        description: cDesc >= 0 ? (cells[cDesc]?.trim() ?? '') : '',
        amount,
        currency: currency ?? 'XXX',
        direction,
        paymentMethodRaw: cMethod >= 0 ? (cells[cMethod]?.trim() ?? '') : '',
        status: cType >= 0 ? (cells[cType]?.trim() ?? '') : '',
        note: '',
      })
      void balance // balance 归一化输出专用（见 normalize）
    })

    // balance 字段在 Observation 形状中无列位 → 存入私有映射供 normalize 使用
    this.balanceOf = (obs) => {
      const idx = observations.indexOf(obs)
      if (idx === -1 || cBalance < 0) return undefined
      const cells = rows[idx]
      if (!cells) return undefined
      return parseAmount(cells[cBalance] ?? '', mapping.options.decimalSeparator) ?? undefined
    }
    void context
    return observations
  }

  private balanceOf: ((obs: Observation) => number | undefined) | null = null

  lastParseErrors(): Array<{ line: number; reason: string }> {
    return this.lastErrors
  }

  async normalize(
    observations: readonly Observation[],
    context: SourceContext,
  ): Promise<NormalizedPaymentObservation[]> {
    await Promise.resolve()
    return observations.map((o) => ({
      sourceInstanceId: context.sourceInstance.id,
      adapterId: this.id,
      sourceTxnId: o.sourceTxnId ?? undefined,
      occurredAt: o.occurredAt,
      amount: o.amount,
      currency: o.currency === 'XXX' ? undefined : o.currency,
      direction: o.direction,
      description: o.description || undefined,
      counterparty: o.merchantRaw || undefined,
      merchantRaw: o.merchantRaw || undefined,
      balance: this.balanceOf?.(o),
      transactionType: o.status || undefined,
      paymentMethodHint: o.paymentMethodRaw || undefined,
      status: o.status || undefined,
    }))
  }
}

function decodeText(data: Uint8Array, encoding: 'utf-8' | 'gb18030'): string {
  if (data.length >= 3 && data[0] === 0xef && data[1] === 0xbb && data[2] === 0xbf) {
    return new TextDecoder('utf-8').decode(data.slice(3))
  }
  try {
    return new TextDecoder('utf-8', { fatal: true }).decode(data)
  } catch {
    return new TextDecoder(encoding === 'gb18030' ? 'gb18030' : 'utf-8').decode(data)
  }
}
