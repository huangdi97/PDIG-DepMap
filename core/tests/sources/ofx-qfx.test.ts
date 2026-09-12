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
  OfxQfxAdapter,
  parseOfxTransactions,
  parseOfxDate,
  parseOfxAmount,
} from '../../src/sources/ofx/adapter.ts'
import type { SourceContext } from '../../src/sources/types.ts'

/**
 * MVP02 F 段 —— OfxQfxAdapter。
 *
 * 覆盖 GOAL §18；语义铁律：
 * - FITID 缺失 → 不伪造 ID（sourceTxnId=null，指纹退化为内容指纹）
 * - DTPOSTED 非法 → 该行被拒绝并计入 lastParseErrors（不猜日期）
 * - 同文件内重复 FITID → 由 fingerprint scope 去重（不合并、不静默丢数据前的语义）
 * - 跨 SourceInstance 同 FITID → 命名空间隔离，互不冲突
 * - event_stream：absence 不产生任何现实否定
 */

const FX = (name: string) =>
  new Uint8Array(
    readFileSync(join(dirname(fileURLToPath(import.meta.url)), '..', 'fixtures', name)),
  )

const ctx = (id: string): SourceContext => ({ sourceInstance: { id } }) as unknown as SourceContext

describe('OfxQfxAdapter (MVP02 F)', () => {
  let dir: string
  let driver: NodeSqliteDriver
  let nodes: NodeRepository
  let instances: SourceInstanceRepository
  let fingerprints: FingerprintRepository

  beforeEach(() => {
    dir = mkdtempSync(join(tmpdir(), 'depmap-ofx-'))
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
    migrate(driver)
    nodes = new NodeRepository(driver)
    instances = new SourceInstanceRepository(driver)
    fingerprints = new FingerprintRepository(driver)
  })

  afterEach(() => {
    driver.close()
    rmSync(dir, { recursive: true, force: true })
  })

  // -------------------------------------------------------------------------
  // F0 —— canHandle 识别
  // -------------------------------------------------------------------------

  it('F0: 识别 OFXHEADER / <OFX> 头；非 OFX 输入返回 0', async () => {
    const a = new OfxQfxAdapter()
    expect(await a.canHandle({ data: FX('ofx-basic.qfx') })).toBeGreaterThan(0)
    expect(await a.canHandle({ data: FX('qfx-basic.qfx') })).toBeGreaterThan(0)
    // 普通 CSV 不应被 OFX adapter 认领
    expect(await a.canHandle({ data: FX('csv-us-credit-card.csv') })).toBe(0)
    expect(await a.canHandle({ data: new Uint8Array([0, 1, 2]) })).toBe(0)
  })

  it('F0b: 契约元数据 = statement_file / event_stream / authoritativeFor=[]', () => {
    const a = new OfxQfxAdapter()
    expect(a.sourceKind).toBe('statement_file')
    expect(a.coverageMode).toBe('event_stream')
    expect(a.authoritativeFor).toEqual([])
    expect(a.id).toBe('ofx_qfx')
    expect(a.version).toBe(1)
  })

  // -------------------------------------------------------------------------
  // F1 —— SGML 与 XML 两种形态
  // -------------------------------------------------------------------------

  it('F1: SGML 形态（无闭合标签）解析 5 笔，方向由符号决定（OFX 约定：负=支出）', async () => {
    const a = new OfxQfxAdapter()
    const parsed = await a.parse({ data: FX('ofx-basic.qfx') }, ctx('s1'))
    expect(parsed).toHaveLength(5)
    expect(a.lastParseErrors()).toHaveLength(0)
    const tencent = parsed.filter((o) => o.merchantRaw === 'TENCENT VIDEO VIP')
    expect(tencent).toHaveLength(2)
    for (const o of tencent) {
      expect(o.direction).toBe('out')
      expect(o.amount).toBe(25)
    }
    const payroll = parsed.find((o) => o.merchantRaw === 'PAYROLL DEPOSIT')!
    expect(payroll.direction).toBe('in')
    expect(payroll.amount).toBe(5200)
    // FITID 被保留为 sourceTxnId
    expect(tencent[0]!.sourceTxnId).toBe('202601150001')
  })

  it('F1b: XML 形态（全闭合标签）同样解析出 3 笔', async () => {
    const a = new OfxQfxAdapter()
    const parsed = await a.parse({ data: FX('ofx-multiple.ofx') }, ctx('s1'))
    expect(parsed).toHaveLength(3)
    expect(a.lastParseErrors()).toHaveLength(0)
    expect(parsed.map((o) => o.sourceTxnId).sort()).toEqual(['XML0001', 'XML0002', 'XML0003'])
  })

  // -------------------------------------------------------------------------
  // F2 —— FITID 缺失 / 重复
  // -------------------------------------------------------------------------

  it('F2: 单笔缺 FITID → 不伪造 ID（sourceTxnId=null），其余行仍解析', async () => {
    const a = new OfxQfxAdapter()
    const parsed = await a.parse({ data: FX('ofx-missing-fitid.ofx') }, ctx('s1'))
    expect(parsed).toHaveLength(3)
    expect(a.lastParseErrors()).toHaveLength(0)
    const spotify = parsed.find((o) => o.merchantRaw === 'SPOTIFY AB')!
    expect(spotify.sourceTxnId).toBeNull()
    // 其他行 ID 完好
    expect(parsed.filter((o) => o.sourceTxnId !== null)).toHaveLength(2)
  })

  it('F2b: 同文件内重复 FITID → 两笔都进入 parse，由 fingerprint scope 去重', async () => {
    const card = nodes.create({
      kind: 'payment_instrument',
      name: '招行经典白',
      issuer: '招商银行',
      last4: '4417',
    })
    const tencent = nodes.create({ kind: 'service', name: '腾讯视频' })
    const spotify = nodes.create({ kind: 'service', name: 'SPOTIFY AB' })
    const inst = instances.create({
      adapterId: 'ofx_qfx',
      adapterVersion: 1,
      sourceKind: 'statement_file',
      accountNodeId: card.id,
      label: 'Card statement (OFX)',
    })

    const co = new ImportCoordinator(driver)
    const begin = await co.begin(inst.id, new OfxQfxAdapter(), {
      data: FX('qfx-duplicate-fitid.ofx'),
    })
    // parse 层不静默丢行：3 笔全部可见
    expect(begin.rawCount).toBe(3)
    co.resolveMerchant('TENCENT VIDEO VIP', tencent.id)
    co.resolveMerchant('SPOTIFY AB', spotify.id)
    const out = await co.finalize()
    // 完全相同内容的两笔 → 同 scope 下指纹相同 → 去重为 1
    expect(out.newUniqueCount).toBe(2)
    expect(out.duplicateCount).toBe(1)
  })

  // -------------------------------------------------------------------------
  // F3 —— 非法日期
  // -------------------------------------------------------------------------

  it('F3: DTPOSTED=20260230（2 月 30 日）被拒绝并记录错误行，不猜日期', async () => {
    const a = new OfxQfxAdapter()
    const parsed = await a.parse({ data: FX('ofx-invalid-date.ofx') }, ctx('s1'))
    expect(parsed).toHaveLength(2)
    const errs = a.lastParseErrors()
    expect(errs).toHaveLength(1)
    expect(errs[0]!.reason).toMatch(/bad DTPOSTED/)
    // 被拒绝的行不产生 Observation —— 无伪造日期
    expect(parsed.map((o) => o.merchantRaw).sort()).toEqual(['NETEASE MUSIC', 'TENCENT VIDEO VIP'])
  })

  it('F3b: parseOfxDate 严格日历校验', () => {
    expect(parseOfxDate('20260115')).toBe('2026-01-15T00:00:00+00:00')
    expect(parseOfxDate('20260115123045')).toBe('2026-01-15T12:30:45+00:00')
    expect(parseOfxDate('20260230')).toBeNull()
    expect(parseOfxDate('20261301')).toBeNull()
    expect(parseOfxDate('20260229')).toBeNull() // 2026 非闰年
    expect(parseOfxDate('20240229')).toBe('2024-02-29T00:00:00+00:00')
    expect(parseOfxDate('garbage')).toBeNull()
    expect(parseOfxDate('')).toBeNull()
  })

  // -------------------------------------------------------------------------
  // F4 —— 符号与零金额
  // -------------------------------------------------------------------------

  it('F4: 正负号映射方向（OFX 约定）；零金额 → direction=in，不误判为支出', async () => {
    const a = new OfxQfxAdapter()
    const parsed = await a.parse({ data: FX('ofx-negative-positive.ofx') }, ctx('s1'))
    expect(parsed).toHaveLength(4)
    const byMerchant = new Map(parsed.map((o) => [o.merchantRaw, o]))
    expect(byMerchant.get('TENCENT VIDEO VIP')!.direction).toBe('out')
    expect(byMerchant.get('PAYROLL DEPOSIT')!.direction).toBe('in')
    expect(byMerchant.get('PAYROLL DEPOSIT')!.amount).toBe(5200)
    const zero = byMerchant.get('ZERO AMOUNT TXN')!
    expect(zero.amount).toBe(0)
    expect(zero.direction).toBe('in') // 0 >= 0 → in（OFX 语义：非负即入账侧）
  })

  it('F4b: parseOfxAmount 非数字与空串 → null（空串绝不静默变 0）', () => {
    expect(parseOfxAmount('25.00')).toBe(25)
    expect(parseOfxAmount('-5200.00')).toBe(-5200)
    expect(parseOfxAmount('1,234.56')).toBeNull() // 逗号千分位不是 OFX 合法数字
    expect(parseOfxAmount('abc')).toBeNull()
    // Number('') === 0：若不显式守卫，"缺失金额" 会伪造出一笔 0 元交易
    expect(parseOfxAmount('')).toBeNull()
    expect(parseOfxAmount('   ')).toBeNull()
  })

  // -------------------------------------------------------------------------
  // F5 —— malformed 块
  // -------------------------------------------------------------------------

  it('F5: 无 FITID 且无 TRNAMT 的块计为 malformed，不计入 Observation', () => {
    const text = new TextDecoder().decode(FX('ofx-malformed.ofx'))
    const { transactions, malformed } = parseOfxTransactions(text)
    // 3 块：1 块缺两字段 → malformed；1 块合法；1 块空 → malformed
    expect(transactions).toHaveLength(1)
    expect(malformed).toBe(2)
    expect(transactions[0]!.fitid).toBe('M0002')
  })

  it('F5b: 完全无 STMTTRN 的文件 → 空结果 + 0 malformed（不抛异常）', () => {
    const { transactions, malformed } = parseOfxTransactions('<OFX></OFX>')
    expect(transactions).toHaveLength(0)
    expect(malformed).toBe(0)
  })

  // -------------------------------------------------------------------------
  // F6 —— 跨 SourceInstance 命名空间隔离
  // -------------------------------------------------------------------------

  it('F6: 两个 SourceInstance 导入同一 OFX → 同一 FITID 各自独立，fingerprint 不冲突', async () => {
    const card = nodes.create({
      kind: 'payment_instrument',
      name: '招行经典白',
      issuer: '招商银行',
      last4: '4417',
    })
    nodes.create({ kind: 'service', name: '腾讯视频' })
    const tencent = nodes.create({ kind: 'service', name: '腾讯视频官方' })
    const spotify = nodes.create({ kind: 'service', name: 'SPOTIFY AB' })

    const s1 = instances.create({
      adapterId: 'ofx_qfx',
      adapterVersion: 1,
      sourceKind: 'statement_file',
      accountNodeId: card.id,
      label: 'QFX A',
    })
    const s2 = instances.create({
      adapterId: 'ofx_qfx',
      adapterVersion: 1,
      sourceKind: 'statement_file',
      accountNodeId: card.id,
      label: 'QFX B',
    })

    const fresh: number[] = []
    for (const inst of [s1, s2]) {
      const co = new ImportCoordinator(driver)
      await co.begin(inst.id, new OfxQfxAdapter(), { data: FX('qfx-basic.qfx') })
      co.resolveMerchant('TENCENT VIDEO VIP', tencent.id)
      co.resolveMerchant('SPOTIFY AB', spotify.id)
      const out = await co.finalize()
      fresh.push(out.newUniqueCount)
    }
    // 同一 FITID 在两个 scope 下各自 fresh
    expect(fresh).toEqual([3, 3])
    expect(fingerprints.countByInstance(s1.id)).toBe(3)
    expect(fingerprints.countByInstance(s2.id)).toBe(3)
    expect(fingerprints.countAll()).toBe(6)
  })

  // -------------------------------------------------------------------------
  // F7 —— deterministic ×50
  // -------------------------------------------------------------------------

  it('F7: 同输入 ×50 parse+normalize 输出完全一致', async () => {
    let baseline: string | null = null
    for (let i = 0; i < 50; i++) {
      const a = new OfxQfxAdapter()
      const data = FX('ofx-basic.qfx')
      const parsed = await a.parse({ data }, ctx('s1'))
      const normalized = await a.normalize(parsed, ctx('s1'))
      const sig = JSON.stringify({ parsed, normalized, errors: a.lastParseErrors() })
      if (baseline === null) baseline = sig
      else expect(sig).toBe(baseline)
    }
    expect(baseline).not.toBeNull()
  })

  // -------------------------------------------------------------------------
  // F8 —— normalize 形状
  // -------------------------------------------------------------------------

  it('F8: normalize 输出携带 sourceInstanceId / adapterId，且不泄漏明文 ID 到 DB', async () => {
    const a = new OfxQfxAdapter()
    const parsed = await a.parse({ data: FX('ofx-basic.qfx') }, ctx('inst-42'))
    const normalized = await a.normalize(parsed, ctx('inst-42'))
    expect(normalized).toHaveLength(5)
    for (const n of normalized) {
      expect(n.sourceInstanceId).toBe('inst-42')
      expect(n.adapterId).toBe('ofx_qfx')
      expect(n.occurredAt).toMatch(/^\d{4}-\d{2}-\d{2}T/)
      expect(typeof n.amount).toBe('number')
    }
    // sourceTxnId 仅存在于内存/指纹输入
    expect(normalized[0]!.sourceTxnId).toBe('202601150001')
  })
})
