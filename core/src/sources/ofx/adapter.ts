import type { Observation } from '../../domain/types.ts'
import type { NormalizedPaymentObservation } from '../../domain/source.ts'
import type { EvidenceSourceAdapter, SourceContext, SourceInput } from '../types.ts'

/**
 * OfxQfxAdapter —— 本地 OFX v1/QFX statement 解析（GOAL MVP02 §18）。
 * 不联网、不长期保存原文件；输出统一 NormalizedPaymentObservation；
 * 未知/缺失字段保守（unknown 保持 unknown）；不自动创建 Dependency。
 */

interface OfxTransaction {
  fitid?: string
  dtposted?: string
  trnamt?: string
  trntype?: string
  name?: string
  memo?: string
  currency?: string
}

/** OFX v1 是 SGML 风格：<TAG>VALUE 成对出现，可能带 </TAG>。 */
export function parseOfxTransactions(text: string): {
  transactions: OfxTransaction[]
  malformed: number
} {
  const transactions: OfxTransaction[] = []
  let malformed = 0
  const blocks = text.split(/<STMTTRN>/i).slice(1)
  if (blocks.length === 0) return { transactions: [], malformed: 0 }
  for (const block of blocks) {
    const end = block.search(/<\/STMTTRN>|<STMTTRN>/i)
    const body = end === -1 ? block : block.slice(0, end)
    const tag = (name: string): string | undefined => {
      const m = new RegExp(`<${name}>\\s*([^<\\r\\n]+)`, 'i').exec(body)
      return m === null ? undefined : m[1]?.trim()
    }
    const fitid = tag('FITID')
    const trnamt = tag('TRNAMT')
    // 无 FITID 且无 TRNAMT 视为坏块
    if (!fitid && !trnamt) {
      malformed += 1
      continue
    }
    transactions.push({
      fitid,
      dtposted: tag('DTPOSTED'),
      trnamt,
      trntype: tag('TRNTYPE'),
      name: tag('NAME'),
      memo: tag('MEMO'),
      currency: tag('CURDEF') ?? tag('CURRENCY'),
    })
  }
  return { transactions, malformed }
}

/** OFX DTPOSTED YYYYMMDD[HHMMSS.XXX[gmt offset]] → ISO。 */
export function parseOfxDate(raw: string): string | null {
  const m = /^(\d{4})(\d{2})(\d{2})(?:(\d{2})(\d{2})(\d{2}))?/.exec(raw.trim())
  if (!m) return null
  const y = Number(m[1])
  const mo = Number(m[2])
  const d = Number(m[3])
  const h = m[4] ? Number(m[4]) : 0
  const mi = m[5] ? Number(m[5]) : 0
  const s = m[6] ? Number(m[6]) : 0
  if (mo < 1 || mo > 12 || h > 23 || mi > 59 || s > 59) return null
  const utc = new Date(Date.UTC(y, mo - 1, d, h, mi, s))
  if (utc.getUTCFullYear() !== y || utc.getUTCMonth() !== mo - 1 || utc.getUTCDate() !== d) {
    return null
  }
  const p = (v: number, w = 2) => String(v).padStart(w, '0')
  return `${p(y, 4)}-${p(mo)}-${p(d)}T${p(h)}:${p(mi)}:${p(s)}+00:00`
}

export function parseOfxAmount(raw: string): number | null {
  const n = Number(raw.trim())
  if (!Number.isFinite(n)) return null
  return Math.round(n * 100) / 100
}

export class OfxQfxAdapter implements EvidenceSourceAdapter {
  readonly id = 'ofx_qfx'
  readonly version = 1
  readonly sourceKind = 'statement_file' as const
  readonly coverageMode = 'event_stream' as const
  readonly authoritativeFor: readonly string[] = []

  private lastErrors: Array<{ line: number; reason: string }> = []
  private balanceOf: ((obs: Observation) => number | undefined) | null = null

  async canHandle(input: SourceInput): Promise<number> {
    await Promise.resolve()
    const head = new TextDecoder('utf-8', { fatal: false }).decode(input.data.slice(0, 512))
    if (/OFXHEADER/i.test(head) || /<OFX>/i.test(head)) return 0.95
    return 0
  }

  async parse(input: SourceInput, _context: SourceContext): Promise<Observation[]> {
    await Promise.resolve()
    const text = new TextDecoder('utf-8', { fatal: false }).decode(input.data)
    const { transactions, malformed } = parseOfxTransactions(text)
    this.lastErrors = []
    for (let i = 0; i < malformed; i++) {
      this.lastErrors.push({ line: i + 1, reason: 'malformed STMTTRN block (no FITID/TRNAMT)' })
    }

    const observations: Observation[] = []
    const balances: Array<number | undefined> = []
    for (const txn of transactions) {
      const occurredAt = txn.dtposted ? parseOfxDate(txn.dtposted) : null
      if (occurredAt === null) {
        this.lastErrors.push({
          line: observations.length + 1,
          reason: `bad DTPOSTED: ${txn.dtposted?.slice(0, 20) ?? 'missing'}`,
        })
        balances.push(undefined)
        continue
      }
      const amount = txn.trnamt !== undefined ? parseOfxAmount(txn.trnamt) : null
      if (amount === null) {
        this.lastErrors.push({
          line: observations.length + 1,
          reason: `bad TRNAMT: ${txn.trnamt?.slice(0, 20) ?? 'missing'}`,
        })
        balances.push(undefined)
        continue
      }
      observations.push({
        source: this.id,
        sourceTxnId: txn.fitid ?? null,
        merchantTxnId: null,
        occurredAt,
        merchantRaw: txn.name ?? txn.memo ?? '',
        description: txn.memo ?? '',
        amount: Math.abs(amount),
        currency: txn.currency ?? 'XXX',
        direction: amount < 0 ? 'out' : 'in',
        paymentMethodRaw: '',
        status: txn.trntype ?? '',
        note: '',
      })
      balances.push(undefined)
    }
    this.balanceOf = (obs) => balances[observations.indexOf(obs)]
    void input
    return observations
  }

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
      status: o.status || undefined,
    }))
  }
}
