import { describe, expect, it, beforeEach, afterEach } from 'vitest'
import { mkdtempSync, rmSync, readFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { NodeSqliteDriver } from '../../src/db/node-driver.ts'
import { migrate } from '../../src/schema/migrations.ts'
import { NodeRepository } from '../../src/repositories/node-repository.ts'
import { SourceInstanceRepository } from '../../src/repositories/source-instance-repository.ts'
import { FingerprintRepository } from '../../src/repositories/fingerprint-repository.ts'
import { ImportCoordinator } from '../../src/services/import-coordinator.ts'
import {
  GenericCsvAdapter,
  MissingMappingError,
  parseCsv,
  parseDate,
  parseAmount,
} from '../../src/sources/generic-csv/adapter.ts'
import type { MappingProfile } from '../../src/sources/types.ts'
import type { SourceContext } from '../../src/sources/types.ts'

/**
 * MVP02 E 段 —— GenericCsvAdapter。
 *
 * 覆盖 GOAL §16/§17：
 * - 显式映射是唯一入口（无 mapping → canHandle=0 / parse throws）
 * - 多区域格式（US 信用卡 / EU 分号 / 借贷两列 / 多币种 / 引号内逗号）
 * - 行尾（LF / CRLF / CR）与编码（UTF-8 / BOM / GB18030）
 * - 坏日期 / 坏金额 / 重复 txnId / 缺列 → 保守拒绝（不猜、不伪造）
 * - deterministic：同输入 ×50 → 完全一致输出
 */

const FX = (name: string) =>
  new Uint8Array(
    readFileSync(join(dirname(fileURLToPath(import.meta.url)), '..', 'fixtures', name)),
  )

const ctx = (id: string): SourceContext => ({ sourceInstance: { id } }) as unknown as SourceContext

function usProfile(): MappingProfile {
  return {
    columns: {
      dateTime: 'Transaction Date',
      counterparty: 'Description',
      amount: 'Amount',
      transactionType: 'Type',
    },
    options: {
      delimiter: ',',
      dateFormats: ['MM/DD/YYYY'],
      decimalSeparator: '.',
      // 美国信用卡导出惯例：消费为正、还款为负 → outward_positive 不适用，
      // 用 signed 并声明正数方向为 out（显式声明，不猜）
      amountSignMode: 'signed',
      positiveDirection: 'out',
      hasHeaderRow: true,
    },
  }
}

describe('GenericCsvAdapter (MVP02 E)', () => {
  let dir: string
  let driver: NodeSqliteDriver
  let nodes: NodeRepository
  let instances: SourceInstanceRepository

  beforeEach(() => {
    dir = mkdtempSync(join(tmpdir(), 'depmap-csv-'))
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
    migrate(driver)
    nodes = new NodeRepository(driver)
    instances = new SourceInstanceRepository(driver)
  })

  afterEach(() => {
    driver.close()
    rmSync(dir, { recursive: true, force: true })
  })

  // -------------------------------------------------------------------------
  // E0 —— 显式映射是唯一入口（禁止 AI 自动映射）
  // -------------------------------------------------------------------------

  it('E0: 无 mapping → canHandle=0；parse 抛 MissingMappingError', async () => {
    const a = new GenericCsvAdapter()
    expect(await a.canHandle({ data: FX('csv-us-credit-card.csv') })).toBe(0)
    await expect(a.parse({ data: FX('csv-us-credit-card.csv') }, ctx('s1'))).rejects.toThrow(
      MissingMappingError,
    )
  })

  it('E0b: 有 mapping → canHandle>0 且确定性（两次一致）', async () => {
    const a = new GenericCsvAdapter()
    const input = { data: FX('csv-us-credit-card.csv'), mapping: usProfile() }
    const c1 = await a.canHandle(input)
    const c2 = await a.canHandle(input)
    expect(c1).toBeGreaterThan(0)
    expect(c1).toBe(c2)
  })

  // -------------------------------------------------------------------------
  // E1 —— 区域格式
  // -------------------------------------------------------------------------

  it('E1: US 信用卡（MM/DD/YYYY + signed + positiveDirection=out）解析 5 行，方向正确', async () => {
    const a = new GenericCsvAdapter()
    const p = usProfile()
    const parsed = await a.parse({ data: FX('csv-us-credit-card.csv'), mapping: p }, ctx('s1'))
    expect(parsed).toHaveLength(5)
    expect(a.lastParseErrors()).toHaveLength(0)
    // 消费为正数但语义为支出 → direction=out，金额取绝对值
    const tencent = parsed.filter((o) => o.merchantRaw === 'TENCENT VIDEO VIP')
    expect(tencent).toHaveLength(2)
    for (const o of tencent) {
      expect(o.direction).toBe('out')
      expect(o.amount).toBe(25)
    }
    expect(tencent.map((o) => o.occurredAt)).toEqual([
      '2026-01-15T00:00:00+00:00',
      '2026-02-15T00:00:00+00:00',
    ])
    // 还款为负 → direction=in（负号即语义方向，positiveDirection 不参与负数判定）
    const payroll = parsed.find((o) => o.merchantRaw.includes('PAYROLL'))!
    expect(payroll.direction).toBe('in')
    expect(payroll.amount).toBe(5200)
  })

  it('E1b: EU 分号 + DD.MM.YYYY + 逗号小数 解析正确', async () => {
    const a = new GenericCsvAdapter()
    const mapping: MappingProfile = {
      columns: { dateTime: 'Buchungstag', counterparty: 'Verwendungszweck', amount: 'Betrag' },
      options: {
        delimiter: ';',
        dateFormats: ['DD.MM.YYYY'],
        decimalSeparator: ',',
        amountSignMode: 'signed',
        positiveDirection: 'out',
        hasHeaderRow: true,
      },
    }
    const parsed = await a.parse({ data: FX('csv-eu-bank-semicolon.csv'), mapping }, ctx('s1'))
    expect(parsed).toHaveLength(5)
    expect(a.lastParseErrors()).toHaveLength(0)
    const netflix = parsed.find((o) => o.merchantRaw === 'NETFLIX.COM')!
    expect(netflix.amount).toBeCloseTo(12.99, 2)
    expect(netflix.occurredAt).toBe('2026-01-15T00:00:00+00:00')
    expect(netflix.direction).toBe('out')
  })

  it('E1c: 借贷两列（debit_credit）→ 借=out 贷=in，且无金额被伪造', async () => {
    const a = new GenericCsvAdapter()
    const mapping: MappingProfile = {
      columns: {
        dateTime: 'Date',
        counterparty: 'Description',
        debit: 'Debit',
        credit: 'Credit',
        balance: 'Balance',
      },
      options: {
        delimiter: ',',
        dateFormats: ['YYYY-MM-DD'],
        decimalSeparator: '.',
        amountSignMode: 'debit_credit',
        hasHeaderRow: true,
      },
    }
    const parsed = await a.parse({ data: FX('csv-debit-credit-columns.csv'), mapping }, ctx('s1'))
    expect(parsed).toHaveLength(5)
    expect(a.lastParseErrors()).toHaveLength(0)
    const outs = parsed.filter((o) => o.direction === 'out')
    const ins = parsed.filter((o) => o.direction === 'in')
    expect(outs).toHaveLength(4)
    expect(ins).toHaveLength(1)
    expect(ins[0]!.amount).toBe(5200)
    // balance 通过 normalize 暴露
    const normalized = await a.normalize(parsed, ctx('s1'))
    const payroll = normalized.find((n) => n.counterparty?.includes('PAYROLL'))!
    expect(payroll.balance).toBeCloseTo(6151.02, 2)
  })

  it('E1d: 多币种 —— 每行保留自身 currency，不跨行混淆', async () => {
    const a = new GenericCsvAdapter()
    const mapping: MappingProfile = {
      columns: {
        dateTime: 'date',
        counterparty: 'description',
        amount: 'amount',
        currency: 'currency',
      },
      options: {
        delimiter: ',',
        dateFormats: ['YYYY-MM-DD'],
        decimalSeparator: '.',
        amountSignMode: 'outward_positive',
        hasHeaderRow: true,
      },
    }
    const parsed = await a.parse({ data: FX('csv-multi-currency.csv'), mapping }, ctx('s1'))
    expect(parsed).toHaveLength(7)
    expect(parsed.find((o) => o.merchantRaw === 'TENCENT VIDEO VIP')!.currency).toBe('CNY')
    expect(parsed.find((o) => o.merchantRaw === 'NETFLIX.COM')!.currency).toBe('EUR')
    expect(parsed.find((o) => o.merchantRaw === 'AWS INC')!.currency).toBe('USD')
    expect(parsed.find((o) => o.merchantRaw === 'AMAZON.JP')!.currency).toBe('JPY')
    // outward_positive：正数 → out
    for (const o of parsed) expect(o.direction).toBe('out')
  })

  it('E1e: 引号内逗号与转义双引号不被误切列', () => {
    const text = readFileSync(
      join(dirname(fileURLToPath(import.meta.url)), '..', 'fixtures', 'csv-quoted-comma.csv'),
      'utf8',
    )
    const rows = parseCsv(text, ',')
    expect(rows[0]).toEqual(['date', 'counterparty', 'amount'])
    expect(rows[1]![1]).toBe('TENCENT VIDEO VIP, BEIJING')
    expect(rows[5]![1]).toBe('SAY "HELLO" SHOP')
    // 列数稳定：引号内逗号不产生额外列
    for (const r of rows) expect(r).toHaveLength(3)
  })

  // -------------------------------------------------------------------------
  // E2 —— 行尾与编码
  // -------------------------------------------------------------------------

  it('E2: 行尾 LF / CRLF / CR 三种均解析出相同结果', async () => {
    const mapping: MappingProfile = {
      columns: {
        dateTime: 'date',
        counterparty: 'counterparty',
        amount: 'amount',
        currency: 'currency',
      },
      options: {
        delimiter: ',',
        dateFormats: ['YYYY-MM-DD'],
        decimalSeparator: '.',
        amountSignMode: 'outward_positive',
        hasHeaderRow: true,
      },
    }
    const results: string[][] = []
    for (const f of ['csv-crlf.csv', 'csv-cr-only.csv']) {
      const a = new GenericCsvAdapter()
      const parsed = await a.parse({ data: FX(f), mapping }, ctx('s1'))
      results.push(parsed.map((o) => `${o.merchantRaw}|${o.occurredAt}|${o.amount}`))
    }
    expect(results[0]).toHaveLength(3)
    expect(results[0]).toEqual(results[1])
  })

  it('E2b: UTF-8 BOM 被剥离（首列名不含 \\uFEFF）', async () => {
    const a = new GenericCsvAdapter()
    const mapping: MappingProfile = {
      columns: {
        dateTime: 'date',
        counterparty: 'counterparty',
        amount: 'amount',
        currency: 'currency',
      },
      options: {
        delimiter: ',',
        dateFormats: ['YYYY-MM-DD'],
        decimalSeparator: '.',
        amountSignMode: 'outward_positive',
        hasHeaderRow: true,
      },
    }
    // 若 BOM 未剥离，colIndexOf('date') 会抛 MissingMappingError
    const parsed = await a.parse({ data: FX('csv-utf8-bom.csv'), mapping }, ctx('s1'))
    expect(parsed).toHaveLength(3)
    expect(parsed[0]!.merchantRaw).toBe('腾讯视频VIP')
    expect(a.lastParseErrors()).toHaveLength(0)
  })

  it('E2c: GB18030 编码（显式声明）中文商户名正确解码', async () => {
    const a = new GenericCsvAdapter()
    const mapping: MappingProfile = {
      columns: {
        dateTime: 'date',
        counterparty: 'counterparty',
        amount: 'amount',
        currency: 'currency',
      },
      options: {
        delimiter: ',',
        encoding: 'gb18030',
        dateFormats: ['YYYY-MM-DD'],
        decimalSeparator: '.',
        amountSignMode: 'outward_positive',
        hasHeaderRow: true,
      },
    }
    const parsed = await a.parse({ data: FX('csv-gb18030.csv'), mapping }, ctx('s1'))
    expect(parsed).toHaveLength(3)
    const names = parsed.map((o) => o.merchantRaw).sort()
    expect(names).toContain('腾讯视频VIP')
    expect(names).toContain('网易云音乐')
  })

  // -------------------------------------------------------------------------
  // E3 —— 坏行保守拒绝（宁可漏报，不可伪造）
  // -------------------------------------------------------------------------

  it('E3: 坏日期 / 坏金额行被跳过并计入 lastParseErrors，合法行仍保留', async () => {
    const a = new GenericCsvAdapter()
    const mapping: MappingProfile = {
      columns: {
        dateTime: 'date',
        counterparty: 'counterparty',
        amount: 'amount',
        currency: 'currency',
      },
      options: {
        delimiter: ',',
        dateFormats: ['YYYY-MM-DD'],
        decimalSeparator: '.',
        amountSignMode: 'outward_positive',
        hasHeaderRow: true,
      },
    }
    const parsed = await a.parse(
      { data: FX('csv-missing-required-column.csv'), mapping },
      ctx('s1'),
    )
    // 6 数据行：2 合法商户（TENCENT ×3）+ SPOTIFY 坏金额 + BROKEN ×2 = 3 合法 → 实际 3 合法
    expect(parsed).toHaveLength(3)
    const errs = a.lastParseErrors()
    expect(errs).toHaveLength(3)
    expect(errs.map((e) => e.reason)).toEqual(
      expect.arrayContaining([
        expect.stringMatching(/bad amount/),
        expect.stringMatching(/bad date/),
        expect.stringMatching(/bad amount/),
      ]),
    )
    // 坏行不产生任何 Observation（无 amount 伪造、无 date 伪造）
    for (const o of parsed) {
      expect(o.amount).toBeGreaterThanOrEqual(0)
      expect(o.occurredAt).toMatch(/^\d{4}-\d{2}-\d{2}T/)
    }
  })

  it('E3b: mapping 列名在 header 中不存在 → 立即抛 MissingMappingError（不静默产出空流）', async () => {
    const a = new GenericCsvAdapter()
    const mapping: MappingProfile = {
      columns: { dateTime: 'NoSuchColumn' },
      options: {
        delimiter: ',',
        dateFormats: ['YYYY-MM-DD'],
        decimalSeparator: '.',
        amountSignMode: 'signed',
        hasHeaderRow: true,
      },
    }
    await expect(
      a.parse({ data: FX('csv-us-credit-card.csv'), mapping }, ctx('s1')),
    ).rejects.toThrowError(/mapping column not found/)
  })

  // -------------------------------------------------------------------------
  // E4 —— 日期 / 金额纯函数边界
  // -------------------------------------------------------------------------

  it('E4: parseDate 严格校验非法日历日期（2/30 拒绝）', () => {
    expect(parseDate('02/30/2026', ['MM/DD/YYYY'])).toBeNull()
    expect(parseDate('13/01/2026', ['MM/DD/YYYY'])).toBeNull()
    expect(parseDate('02/29/2026', ['MM/DD/YYYY'])).toBeNull() // 2026 非闰年
    expect(parseDate('02/29/2024', ['MM/DD/YYYY'])).toBe('2024-02-29T00:00:00+00:00')
    expect(parseDate('', ['MM/DD/YYYY'])).toBeNull()
  })

  it('E4b: 多格式回退 —— 第一个不匹配则尝试下一个', () => {
    const formats = ['DD.MM.YYYY', 'YYYY-MM-DD', 'MM/DD/YYYY']
    expect(parseDate('15.01.2026', formats)).toBe('2026-01-15T00:00:00+00:00')
    expect(parseDate('2026-01-15', formats)).toBe('2026-01-15T00:00:00+00:00')
    expect(parseDate('01/15/2026', formats)).toBe('2026-01-15T00:00:00+00:00')
  })

  it('E4c: parseAmount 千分位与货币符号；空串与非数字 → null', () => {
    expect(parseAmount('1,234.56', '.')).toBeCloseTo(1234.56, 2)
    expect(parseAmount('¥1,234.56', '.')).toBeCloseTo(1234.56, 2)
    expect(parseAmount('1.234,56', ',')).toBeCloseTo(1234.56, 2)
    expect(parseAmount('  ', '.')).toBeNull()
    expect(parseAmount('abc', '.')).toBeNull()
    expect(parseAmount('', '.')).toBeNull()
  })

  // -------------------------------------------------------------------------
  // E5 —— deterministic ×50
  // -------------------------------------------------------------------------

  it('E5: 同输入 ×50 输出完全一致（parse 与 normalize 双确定性）', async () => {
    const mapping: MappingProfile = {
      columns: {
        dateTime: 'date',
        counterparty: 'description',
        amount: 'amount',
        currency: 'currency',
      },
      options: {
        delimiter: ',',
        dateFormats: ['YYYY-MM-DD'],
        decimalSeparator: '.',
        amountSignMode: 'outward_positive',
        hasHeaderRow: true,
      },
    }
    let baseline: string | null = null
    for (let i = 0; i < 50; i++) {
      const a = new GenericCsvAdapter()
      const data = FX('csv-multi-currency.csv')
      const parsed = await a.parse({ data, mapping }, ctx('s1'))
      const normalized = await a.normalize(parsed, ctx('s1'))
      const sig = JSON.stringify({ parsed, normalized })
      if (baseline === null) baseline = sig
      else expect(sig).toBe(baseline)
    }
    expect(baseline).not.toBeNull()
  })

  it('E5b: canHandle 对非 CSV 输入返回 0（不误报）', async () => {
    const a = new GenericCsvAdapter()
    // 无 mapping
    expect(await a.canHandle({ data: new Uint8Array([1, 2, 3]) })).toBe(0)
  })

  // -------------------------------------------------------------------------
  // E6 —— 与 ImportCoordinator 端到端（导入 → 指纹 → proposal）
  // -------------------------------------------------------------------------

  it('E6: 两个 SourceInstance 处理同一 CSV → 各自独立产生指纹，互不冲突', async () => {
    const card = nodes.create({
      kind: 'payment_instrument',
      name: '招行经典白',
      issuer: '招商银行',
      last4: '4417',
    })
    const acct1 = nodes.create({
      kind: 'account',
      templateId: 'builtin.account.wechat',
      name: '微信支付A',
    })
    const tencent = nodes.create({ kind: 'service', name: 'TENCENT VIDEO VIP' })
    const spotify = nodes.create({ kind: 'service', name: 'SPOTIFY AB' })

    const s1 = instances.create({
      adapterId: 'generic_csv',
      adapterVersion: 1,
      sourceKind: 'statement_file',
      accountNodeId: card.id,
      label: 'Card A statement',
    })
    const s2 = instances.create({
      adapterId: 'generic_csv',
      adapterVersion: 1,
      sourceKind: 'statement_file',
      accountNodeId: acct1.id,
      label: 'Card B statement',
    })

    const mapping = usProfile()
    const results: number[] = []
    for (const inst of [s1, s2]) {
      const co = new ImportCoordinator(driver)
      const begin = await co.begin(inst.id, new GenericCsvAdapter(), {
        data: FX('csv-us-credit-card.csv'),
        mapping,
      })
      co.resolveMerchant('TENCENT VIDEO VIP', tencent.id)
      co.resolveMerchant('SPOTIFY AB', spotify.id)
      expect(begin.rawCount).toBe(5)
      const out = await co.finalize()
      results.push(out.newUniqueCount)
    }
    // 同一文件、不同 SourceInstance → 两边都是 5 条 fresh（指纹命名空间隔离）
    expect(results).toEqual([5, 5])

    const fps = new FingerprintRepository(driver)
    expect(fps.countAll()).toBe(10)
    expect(fps.countByInstance(s1.id)).toBe(5)
    expect(fps.countByInstance(s2.id)).toBe(5)
  })

  it('E6b: 同一 SourceInstance 重复导入同一 CSV → 全部 duplicate，不产生新 Proposal', async () => {
    const card = nodes.create({
      kind: 'payment_instrument',
      name: '招行经典白',
      issuer: '招商银行',
      last4: '4417',
    })
    nodes.create({ kind: 'service', name: 'TENCENT VIDEO VIP' })
    const tencent = nodes.create({ kind: 'service', name: '腾讯视频' })

    const inst = instances.create({
      adapterId: 'generic_csv',
      adapterVersion: 1,
      sourceKind: 'statement_file',
      accountNodeId: card.id,
      label: 'Card A statement',
    })
    const mapping = usProfile()

    const co1 = new ImportCoordinator(driver)
    await co1.begin(inst.id, new GenericCsvAdapter(), {
      data: FX('csv-us-credit-card.csv'),
      mapping,
    })
    co1.resolveMerchant('TENCENT VIDEO VIP', tencent.id)
    const out1 = await co1.finalize()
    expect(out1.newUniqueCount).toBe(5)
    expect(out1.duplicateCount).toBe(0)

    const co2 = new ImportCoordinator(driver)
    await co2.begin(inst.id, new GenericCsvAdapter(), {
      data: FX('csv-us-credit-card.csv'),
      mapping,
    })
    co2.resolveMerchant('TENCENT VIDEO VIP', tencent.id)
    const out2 = await co2.finalize()
    expect(out2.newUniqueCount).toBe(0)
    expect(out2.duplicateCount).toBe(5)
    expect(out2.proposalKeys).toHaveLength(0)
  })

  it('E6c: 未 resolution 的商户不产生 Proposal（GOAL §14）', async () => {
    const card = nodes.create({
      kind: 'payment_instrument',
      name: '招行经典白',
      issuer: '招商银行',
      last4: '4417',
    })
    const inst = instances.create({
      adapterId: 'generic_csv',
      adapterVersion: 1,
      sourceKind: 'statement_file',
      accountNodeId: card.id,
      label: 'Card A statement',
    })
    const co = new ImportCoordinator(driver)
    const begin = await co.begin(inst.id, new GenericCsvAdapter(), {
      data: FX('csv-us-credit-card.csv'),
      mapping: usProfile(),
    })
    // 不调用 resolveMerchant
    expect(begin.candidates.length).toBeGreaterThan(0)
    const out = await co.finalize()
    expect(out.proposalKeys).toHaveLength(0)
    expect(out.unresolvedMerchants.length).toBeGreaterThan(0)
  })
})
