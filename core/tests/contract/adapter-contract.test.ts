import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { mkdtempSync, readFileSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import {
  assignFingerprintsV2,
  assertFileAdapterContract,
  GenericCsvAdapter,
  migrate,
  NodeSqliteDriver,
  OfxQfxAdapter,
  WeChatStatementAdapter,
  type EvidenceSourceAdapter,
  type MappingProfile,
  type SourceContext,
  type SourceInput,
} from '../../src/index.ts'

/**
 * Engineering Baseline V1 — EvidenceSourceAdapterContract（共享契约套件）。
 *
 * 所有 statement_file / event_stream Adapter 必须通过同一组契约：
 *   C1 元数据契约（id/version/sourceKind/coverageMode/authoritativeFor）
 *   C2 canHandle 确定性且 ∈ [0,1]
 *   C3 normalize 输出契约（长度一致 / sourceInstanceId 锚定 / 金额方向合法 / ISO 日期）
 *   C4 parse+normalize 确定性 ×10
 *   C5 纯净性：parse/normalize/suggestRoutes 零 Reality 写入（表行数不变）
 *   C6 SourceInstance 隔离：同内容不同实例 → 指纹命名空间互不串扰
 *
 * 未来新增 Adapter：必须把用例加入 ADAPTER_CASES 才允许 Merge（TEST_STRATEGY 契约层）。
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
      amountSignMode: 'signed',
      positiveDirection: 'out',
      hasHeaderRow: true,
    },
  }
}

/** 被测 Adapter 清单：新增 Adapter 在此注册。 */
const ADAPTER_CASES: Array<{
  label: string
  make: (driver: NodeSqliteDriver) => EvidenceSourceAdapter
  input: () => SourceInput
}> = [
  {
    label: 'WeChatStatementAdapter',
    make: (driver) => new WeChatStatementAdapter(driver),
    input: () => ({ data: FX('normal-wechat.csv'), fileName: 'normal-wechat.csv' }),
  },
  {
    label: 'GenericCsvAdapter',
    make: () => new GenericCsvAdapter(),
    input: () => ({ data: FX('csv-us-credit-card.csv'), mapping: usProfile() }),
  },
  {
    label: 'OfxQfxAdapter',
    make: () => new OfxQfxAdapter(),
    input: () => ({ data: FX('qfx-basic.qfx'), fileName: 'qfx-basic.qfx' }),
  },
]

/** 纯净性检查覆盖的全部 Reality / 状态表（迁移 v2 后的实际表名）。 */
const REALITY_TABLES = [
  'nodes',
  'dependencies',
  'dependency_groups',
  'dependency_proposals',
  'dependency_group_proposals',
  'evidence',
  'observation_fingerprints',
  'proposal_evidence_refs',
  'import_sessions',
  'source_instances',
] as const

function tableCounts(driver: NodeSqliteDriver): Map<string, number> {
  const out = new Map<string, number>()
  for (const t of REALITY_TABLES) {
    const row = driver.prepare(`SELECT COUNT(*) AS n FROM ${t}`).get() as { n: number }
    out.set(t, Number(row.n))
  }
  return out
}

describe('EvidenceSourceAdapterContract（共享契约，Engineering Baseline V1）', () => {
  let dir: string
  let driver: NodeSqliteDriver

  beforeEach(() => {
    dir = mkdtempSync(join(tmpdir(), 'depmap-contract-'))
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
    migrate(driver)
  })

  afterEach(() => {
    driver.close()
    rmSync(dir, { recursive: true, force: true })
  })

  for (const ac of ADAPTER_CASES) {
    describe(`[${ac.label}]`, () => {
      it('C1: 元数据契约 — event_stream / statement_file / authoritativeFor=[] 且 guard 放行', () => {
        const a = ac.make(driver)
        expect(a.sourceKind).toBe('statement_file')
        expect(a.coverageMode).toBe('event_stream')
        expect(a.authoritativeFor).toEqual([])
        expect(typeof a.id).toBe('string')
        expect(a.id.length).toBeGreaterThan(0)
        expect(Number.isInteger(a.version)).toBe(true)
        expect(() => assertFileAdapterContract(a)).not.toThrow()
      })

      it('C2: canHandle 确定性（两次一致）且 ∈ [0,1]，对自家格式 > 0', async () => {
        const a = ac.make(driver)
        const input = ac.input()
        const s1 = await a.canHandle(input)
        const s2 = await a.canHandle(input)
        expect(s1).toBe(s2)
        expect(s1).toBeGreaterThan(0)
        expect(s1).toBeLessThanOrEqual(1)
      })

      it('C3: normalize 输出契约 — 长度一致 / 实例锚定 / 金额方向日期合法', async () => {
        const a = ac.make(driver)
        const input = ac.input()
        const parsed = await a.parse(input, ctx('s1'))
        const normalized = await a.normalize(parsed, ctx('s1'))
        expect(normalized.length).toBe(parsed.length)

        for (const n of normalized) {
          expect(n.sourceInstanceId).toBe('s1')
          expect(n.adapterId).toBe(a.id)
          expect(Number.isFinite(n.amount)).toBe(true)
          expect(n.amount).toBeGreaterThanOrEqual(0)
          expect(['in', 'out', 'neutral']).toContain(n.direction)
          // 严格日历日期：YYYY-MM-DD 前缀 + 可被 Date 解析（禁止 JS 自动纠正非法日期）
          expect(n.occurredAt).toMatch(/^\d{4}-\d{2}-\d{2}/)
          expect(Number.isNaN(Date.parse(n.occurredAt))).toBe(false)
          if (n.currency !== undefined) {
            expect(n.currency).toMatch(/^[A-Z]{3}$/)
          }
        }
      })

      it('C4: parse+normalize 确定性 ×10 — 同输入完全一致', async () => {
        const a = ac.make(driver)
        let baseline: string | null = null
        for (let i = 0; i < 10; i++) {
          const parsed = await a.parse(ac.input(), ctx('s1'))
          const normalized = await a.normalize(parsed, ctx('s1'))
          const sig = JSON.stringify({ parsed, normalized })
          if (baseline === null) baseline = sig
          else expect(sig).toBe(baseline)
        }
        expect(baseline).not.toBeNull()
      })

      it('C5: 纯净性 — parse/normalize/suggestRoutes 零 Reality 写入', async () => {
        const a = ac.make(driver)
        const before = tableCounts(driver)
        const input = ac.input()
        const parsed = await a.parse(input, ctx('s1'))
        const normalized = await a.normalize(parsed, ctx('s1'))
        if (a.suggestRoutes) {
          a.suggestRoutes(normalized, ctx('s1'), new Map())
        }
        a.lastParseErrors?.()
        const after = tableCounts(driver)
        expect([...after.entries()]).toEqual([...before.entries()])
      })

      it('C6: SourceInstance 隔离 — 同内容不同实例，指纹命名空间互不串扰', async () => {
        const a = ac.make(driver)
        const input = ac.input()
        const n1 = await a.normalize(await a.parse(input, ctx('inst-A')), ctx('inst-A'))
        const n2 = await a.normalize(await a.parse(input, ctx('inst-B')), ctx('inst-B'))
        // 业务字段一致，仅实例锚定不同
        expect(n1.length).toBe(n2.length)
        const strip = (rows: typeof n1) =>
          rows.map((r) => ({ ...r, sourceInstanceId: '<stripped>' }))
        expect(strip(n1)).toEqual(strip(n2))

        // 同一批业务行在两个实例下指纹全不同（scope 含 instance id）
        const fpA = assignFingerprintsV2('fp-secret-contract', n1).map((f) => f.fingerprint)
        const fpB = assignFingerprintsV2('fp-secret-contract', n2).map((f) => f.fingerprint)
        expect(fpA.length).toBe(fpB.length)
        if (fpA.length > 0) {
          const setA = new Set(fpA)
          for (const f of fpB) expect(setA.has(f)).toBe(false)
        }
      })
    })
  }

  it('C0: 契约套件必须覆盖全部三个已实现 Adapter（防漏注册）', () => {
    expect(ADAPTER_CASES.map((c) => c.label).sort()).toEqual([
      'GenericCsvAdapter',
      'OfxQfxAdapter',
      'WeChatStatementAdapter',
    ])
  })
})
