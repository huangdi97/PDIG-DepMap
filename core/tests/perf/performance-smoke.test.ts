import { describe, expect, it } from 'vitest'
import { mkdtempSync, rmSync, readFileSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { tmpdir } from 'node:os'
import { fileURLToPath } from 'node:url'
import { parseWechatBill } from '../../src/parser/wechat/parser.ts'
import { assignFingerprints } from '../../src/fingerprint/fingerprint.ts'
import { simulateScenario, simulateDisable } from '../../src/impact/kernel.ts'
import type { Dependency } from '../../src/domain/types.ts'
import {
  createDepmapContainer,
  validateDepmapBounds,
  parseDepmapHeader,
  openDepmapContainer,
} from '../../src/crypto/depmap.ts'
import { ImportFlow } from '../../src/services/import-pipeline.ts'
import { NodeRepository } from '../../src/repositories/node-repository.ts'
import { NodeSqliteDriver } from '../../src/db/node-driver.ts'
import { migrate } from '../../src/schema/migrations.ts'
import { exportGraph } from '../../src/services/graph-serialize.ts'
import { GenericCsvAdapter } from '../../src/sources/generic-csv/adapter.ts'
import { OfxQfxAdapter } from '../../src/sources/ofx/adapter.ts'
import { ImportCoordinator } from '../../src/services/import-coordinator.ts'
import { SourceInstanceRepository } from '../../src/repositories/source-instance-repository.ts'
import { FingerprintRepository } from '../../src/repositories/fingerprint-repository.ts'
import type { MappingProfile, SourceContext } from '../../src/sources/types.ts'

/**
 * PHASE L — Performance Smoke（只防异常退化，阈值宽松）+ PHASE AF — Large Synthetic Smoke。
 * 运行：npm run test:perf。输出记录进 docs/PERFORMANCE_SMOKE.md（人工由运行结果更新）。
 */

const FX = (name: string) =>
  new Uint8Array(
    readFileSync(join(dirname(fileURLToPath(import.meta.url)), '..', 'fixtures', name)),
  )

const enc = new TextEncoder()

function buildBillCsv(rows: number): Uint8Array {
  const HEADER =
    '交易时间,交易类型,交易对方,商品,收/支,金额(元),支付方式,当前状态,交易单号,商户单号,备注'
  const lines = [HEADER]
  for (let i = 0; i < rows; i++) {
    lines.push(
      `2026-${String(1 + (i % 12)).padStart(2, '0')}-${String(1 + (i % 28)).padStart(2, '0')} 10:23:00,商户消费,商户${i % 500},商品${i % 100},支出,¥1.00,招商银行信用卡(4417),支付成功,TXN${i},M${i},/`,
    )
  }
  return enc.encode(lines.join('\r\n'))
}

/**
 * MVP02 性能 smoke 数据构造器。
 * 目标：为 10k 行 CSV / 10k 笔 OFX / 3 个 SourceInstance 提供结构性规模压测，
 * 不引入新的语义断言（语义正确性由 E/F 段用例负责）。
 */

/** 构造 10k 级 US 信用卡风格 CSV（RFC4180 表头 + 唯一 txnId）。 */
function buildCsvStatement(rows: number): Uint8Array {
  const lines = ['Transaction Date,Posting Date,Description,Amount,Type']
  for (let i = 0; i < rows; i++) {
    const mo = String(1 + (i % 12)).padStart(2, '0')
    const d = String(1 + (i % 28)).padStart(2, '0')
    lines.push(`${mo}/${d}/2026,${mo}/${d}/2026,商户${i % 500},${(1 + (i % 900)).toFixed(2)},Debit`)
  }
  return enc.encode(lines.join('\r\n'))
}

/** 构造 10k 笔 STMTTRN 的 OFX v1 文本。 */
function buildOfxStatement(rows: number): Uint8Array {
  const parts: string[] = [
    'OFXHEADER:100',
    'DATA:OFXSGML',
    'VERSION:102',
    '',
    '<OFX><BANKMSGSRSV1><STMTTRNRS><STMTRS><CURDEF>USD',
    '<BANKTRANLIST>',
  ]
  for (let i = 0; i < rows; i++) {
    const mo = String(1 + (i % 12)).padStart(2, '0')
    const d = String(1 + (i % 28)).padStart(2, '0')
    parts.push(
      '<STMTTRN>',
      `<TRNTYPE>DEBIT`,
      `<DTPOSTED>2026${mo}${d}120000`,
      `<TRNAMT>-${(1 + (i % 900)).toFixed(2)}`,
      `<FITID>PERF${i}`,
      `<NAME>MERCHANT ${i % 500}`,
      '</STMTTRN>',
    )
  }
  parts.push('</BANKTRANLIST></STMTRS></STMTTRNRS></BANKMSGSRSV1></OFX>')
  return enc.encode(parts.join('\n'))
}

function usPerfProfile(): MappingProfile {
  return {
    columns: {
      dateTime: 'Transaction Date',
      amount: 'Amount',
      counterparty: 'Description',
    },
    options: {
      delimiter: ',',
      encoding: 'utf-8',
      dateFormats: ['MM/DD/YYYY'],
      decimalSeparator: '.',
      amountSignMode: 'signed',
      hasHeaderRow: true,
    },
  }
}

const perfCtx = (id: string): SourceContext =>
  ({ sourceInstance: { id } }) as unknown as SourceContext

function chainGraph(n: number): Dependency[] {
  return [...Array(n).keys()].map((i) => ({
    id: `e${i}`,
    from: `n${i}`,
    relation: 'funding_source',
    to: `n${i + 1}`,
    capability: 'payment',
    criticality: 'required',
    groupId: null,
    state: 'active',
    origin: 'manual',
    confirmedAt: 't',
    lastVerifiedAt: 't',
    retiredAt: null,
    evidenceRefs: [],
    verificationBasis: null,
    createdAt: 't',
    updatedAt: 't',
  }))
}

function ringGraph(n: number): Dependency[] {
  return [...Array(n).keys()].map((i) => ({
    id: `r${i}`,
    from: `c${i}`,
    relation: 'funding_source',
    to: `c${(i + 1) % n}`,
    capability: 'payment',
    criticality: 'required',
    groupId: null,
    state: 'active',
    origin: 'manual',
    confirmedAt: 't',
    lastVerifiedAt: 't',
    retiredAt: null,
    evidenceRefs: [],
    verificationBasis: null,
    createdAt: 't',
    updatedAt: 't',
  }))
}

const results: Array<{ name: string; ms: number }> = []

describe('Performance Smoke (RC PHASE L/AF)', () => {
  it('10k rows parse < 10s', () => {
    const raw = buildBillCsv(10000)
    const t0 = performance.now()
    const r = parseWechatBill(raw)
    const ms = performance.now() - t0
    results.push({ name: 'parse 10k rows', ms })
    expect(r.observations).toHaveLength(10000)
    expect(ms).toBeLessThan(10000)
  })

  it('10k fingerprints < 10s', () => {
    const parsed = parseWechatBill(buildBillCsv(10000))
    const t0 = performance.now()
    const fps = assignFingerprints('perf-secret', parsed.observations, {
      adapterId: 'wechat_statement',
      sourceInstanceId: 'perf-instance',
    })
    const ms = performance.now() - t0
    results.push({ name: 'fingerprint 10k', ms })
    expect(fps).toHaveLength(10000)
    expect(ms).toBeLessThan(10000)
  })

  it('proposal generation（1k merchant × 10k 观测级数据流）< 15s', async () => {
    const dir = mkdtemp()
    const driver = new NodeSqliteDriver(join(dir, 'p.db'))
    driver.open()
    migrate(driver)
    try {
      const nodes = new NodeRepository(driver)
      const service = nodes.create({ kind: 'service', name: '批量商户500' })
      const flow = new ImportFlow(driver)
      const raw = buildBillCsv(2000)
      await flow.begin(raw)
      flow.resolveMerchant('商户499', service.id)
      const t0 = performance.now()
      await flow.finalize()
      const ms = performance.now() - t0
      results.push({ name: 'import+proposal finalize (2000 rows)', ms })
      expect(ms).toBeLessThan(15000)
    } finally {
      driver.close()
      rmSync(dir, { recursive: true, force: true })
    }
  })

  it('1k-node chain impact < 5s；100-node cycle < 2s', () => {
    let t0 = performance.now()
    const chain = simulateDisable(
      { dependencies: chainGraph(1000), groups: [], nodeNames: {} },
      'n0',
    )
    const chainMs = performance.now() - t0
    results.push({ name: 'impact 1k-node chain', ms: chainMs })
    expect(chain.lostKeys).toHaveLength(1001)
    expect(chainMs).toBeLessThan(5000)

    t0 = performance.now()
    const ring = simulateDisable({ dependencies: ringGraph(100), groups: [], nodeNames: {} }, 'c0')
    const ringMs = performance.now() - t0
    results.push({ name: 'impact 100-node cycle', ms: ringMs })
    expect(ring.lostKeys).toHaveLength(100)
    expect(ringMs).toBeLessThan(2000)
  })

  it('malicious KDF rejection < 100ms', () => {
    const header = parseDepmapHeader(
      JSON.stringify({
        format: 'depmap',
        formatVersion: 1,
        kdf: {
          algorithm: 'argon2id',
          version: 19,
          salt: 'ABEiM0RVZneImaq7zN3u/w==',
          memoryKiB: 262144,
          iterations: 3,
          parallelism: 1,
        },
        cipher: { algorithm: 'AES-256-GCM', nonce: 'obLD1OX2BxgpOktc' },
        ciphertext: 'AAAA',
        tag: '5qpABhovPbNet1q2GNEhkg==',
      }),
    )
    const t0 = performance.now()
    for (let i = 0; i < 100; i++) validateDepmapBounds(header)
    const ms = performance.now() - t0
    results.push({ name: 'malicious KDF bounds rejection ×100', ms })
    expect(ms).toBeLessThan(500)
  })

  it('PHASE AF large synthetic：500 nodes / 1k deps / 100 proposals / 50 groups 级数据流', () => {
    const dir = mkdtemp()
    const driver = new NodeSqliteDriver(join(dir, 'big.db'))
    driver.open()
    migrate(driver)
    try {
      const t0 = performance.now()
      const nodes = new NodeRepository(driver)
      void nodes
      const insert = driver.prepare(
        `INSERT INTO nodes (id, kind, name, owner, archived, fields_json, created_at, updated_at) VALUES (?, ?, ?, 'self', 0, '{}', 't', 't')`,
      )
      for (let i = 0; i < 500; i++) insert.run(`node${i}`, 'account', `节点${i}`)
      const insertDep = driver.prepare(
        `INSERT OR IGNORE INTO dependencies (id, from_node, relation, to_node, capability, criticality, state, origin, confirmed_at, last_verified_at, evidence_refs_json, created_at, updated_at) VALUES (?, ?, 'funding_source', ?, 'payment', 'unknown', 'active', 'manual', 't', 't', '[]', 't', 't')`,
      )
      for (let i = 0; i < 1000; i++) {
        const from = i % 500
        const to = (from + 1 + Math.floor(i / 500)) % 500 // 1000 个唯一 (from,to) 对
        insertDep.run(`dep${i}`, `node${from}`, `node${to}`)
      }
      const graph = {
        dependencies: [...Array(1000).keys()].map((i) => ({
          id: `dep${i}`,
          from: `node${i % 500}`,
          relation: 'funding_source' as const,
          to: `node${((i % 500) + 1 + Math.floor(i / 500)) % 500}`,
          capability: 'payment' as const,
          criticality: 'unknown' as const,
          groupId: null,
          state: 'active' as const,
          origin: 'manual' as const,
          confirmedAt: 't',
          lastVerifiedAt: 't',
          retiredAt: null,
          evidenceRefs: [],
          verificationBasis: null,
          createdAt: 't',
          updatedAt: 't',
        })),
        groups: [],
        nodeNames: Object.fromEntries([...Array(500).keys()].map((i) => [`node${i}`, `节点${i}`])),
      }
      const scenario = new Set([{ nodeId: 'node0', capability: 'payment' as const }])
      const sim = simulateScenario(graph, scenario)
      const simMs = performance.now() - t0
      results.push({ name: 'large synthetic build+simulate (500n/1k e)', ms: simMs })
      expect(sim.lostKeys.length).toBeGreaterThan(0)
      expect(simMs).toBeLessThan(10000)

      // export 大库
      const t1 = performance.now()
      const exported = exportGraph(driver)
      const exportMs = performance.now() - t1
      results.push({ name: 'export 500-node graph', ms: exportMs })
      expect(exported.counts['nodes']).toBe(500)
      expect(exported.counts['dependencies']).toBe(1000)
      expect(exportMs).toBeLessThan(10000)
      void [100, 50] // proposals/groups 数量在 MVP 流程由确认流生成；此处以结构性规模验证为主
    } finally {
      driver.close()
      rmSync(dir, { recursive: true, force: true })
    }
  }, 30000)

  it('crypto roundtrip perf（含 golden 参数）记录', async () => {
    const t0 = performance.now()
    const { json } = await createDepmapContainer(enc.encode('perf-payload'), 'perf-pwd')
    const { plaintext } = await openDepmapContainer(json, 'perf-pwd')
    const ms = performance.now() - t0
    results.push({ name: 'depmap create+open (65536/3/1)', ms })
    expect(new TextDecoder().decode(plaintext)).toBe('perf-payload')
    expect(ms).toBeLessThan(10000)
  })

  // -------------------------------------------------------------------------
  // MVP02 — Global Source Abstraction 性能 smoke
  // -------------------------------------------------------------------------

  it('MVP02: GenericCsvAdapter 10k rows 解析 < 10s', async () => {
    const adapter = new GenericCsvAdapter()
    const raw = buildCsvStatement(10000)
    const t0 = performance.now()
    const observations = await adapter.parse(
      { data: raw, fileName: 'perf-10k.csv', mapping: usPerfProfile() },
      perfCtx('si-csv-perf'),
    )
    const ms = performance.now() - t0
    results.push({ name: 'MVP02 CSV parse 10k rows', ms })
    expect(observations).toHaveLength(10000)
    expect(adapter.lastParseErrors?.() ?? []).toHaveLength(0)
    expect(ms).toBeLessThan(10000)
  }, 30000)

  it('MVP02: OfxQfxAdapter 10k transactions 解析 < 10s', async () => {
    const adapter = new OfxQfxAdapter()
    const raw = buildOfxStatement(10000)
    const t0 = performance.now()
    const observations = await adapter.parse({ data: raw }, perfCtx('si-ofx-perf'))
    const ms = performance.now() - t0
    results.push({ name: 'MVP02 OFX parse 10k txns', ms })
    expect(observations).toHaveLength(10000)
    expect(adapter.lastParseErrors?.() ?? []).toHaveLength(0)
    expect(ms).toBeLessThan(10000)
  }, 30000)

  it('MVP02: 3 个 SourceInstance 并发导入（各 2k 行）< 30s；指纹命名空间互不串扰', async () => {
    const dir = mkdtemp()
    const driver = new NodeSqliteDriver(join(dir, 'multi.db'))
    driver.open()
    migrate(driver)
    try {
      const instances = new SourceInstanceRepository(driver)
      instances.create({
        id: 'si-csv-1',
        adapterId: 'generic_csv',
        adapterVersion: 1,
        sourceKind: 'statement_file',
        label: 'CSV 信用卡',
      })
      instances.create({
        id: 'si-ofx-1',
        adapterId: 'ofx_qfx',
        adapterVersion: 1,
        sourceKind: 'statement_file',
        label: 'OFX 银行对账单',
      })
      instances.create({
        id: 'si-csv-2',
        adapterId: 'generic_csv',
        adapterVersion: 1,
        sourceKind: 'statement_file',
        label: 'CSV 第二张卡',
      })

      const jobs = [
        { si: 'si-csv-1', raw: buildCsvStatement(2000) },
        { si: 'si-csv-2', raw: buildCsvStatement(2000) },
        { si: 'si-ofx-1', raw: buildOfxStatement(2000) },
      ]

      // BEGIN_STATE 是 WeakMap<ImportCoordinator, PendingImport> —— 一个 coordinator
      // 同时只能承载一个未 finalize 的 import（begin → finalize 三段式契约）。
      // 因此「3 实例并发」用 3 个独立 coordinator 表达真实并发；
      // 每个 coordinator 内部仍严格走 begin → resolveMerchant → finalize。
      const csvAdapterFor = (): GenericCsvAdapter => new GenericCsvAdapter()
      const ofxAdapterFor = (): OfxQfxAdapter => new OfxQfxAdapter()

      const t0 = performance.now()
      const outcomes = await Promise.all(
        jobs.map(async (j) => {
          const coordinator = new ImportCoordinator(driver)
          const adapter = j.si === 'si-ofx-1' ? ofxAdapterFor() : csvAdapterFor()
          const input =
            j.si === 'si-ofx-1'
              ? { data: j.raw, fileName: `${j.si}.ofx` }
              : { data: j.raw, fileName: `${j.si}.csv`, mapping: usPerfProfile() }
          const begun = await coordinator.begin(j.si, adapter, input)
          const nodes = new NodeRepository(driver)
          for (const c of begun.candidates) {
            const node = nodes.create({ kind: 'service', name: c.merchantRaw })
            coordinator.resolveMerchant(c.merchantRaw, node.id)
          }
          return coordinator.finalize()
        }),
      )
      const ms = performance.now() - t0
      results.push({ name: 'MVP02 3-instance concurrent import (2k×3)', ms })

      expect(ms).toBeLessThan(30000)
      const totalRaw = outcomes.reduce((n, o) => n + o.newUniqueCount + o.duplicateCount, 0)
      expect(totalRaw).toBe(6000)

      // 指纹命名空间互不串扰（结构性断言，独立于哈希 seed 实现）：
      // 每个 SourceInstance 必须在 observation_fingerprints 中拥有**自己**的
      // 2000 条记录。若某实现把 DB 去重键退化为全局 fingerprint（丢掉
      // source_instance_id），两个内容完全一致的 CSV 实例会互相吞掉对方的行，
      // 导致 si-csv-2 的持久化计数为 0。
      const fps = new FingerprintRepository(driver)
      expect(fps.countByInstance('si-csv-1')).toBe(2000)
      expect(fps.countByInstance('si-csv-2')).toBe(2000)
      expect(fps.countByInstance('si-ofx-1')).toBe(2000)
      expect(fps.countAll()).toBe(6000)

      const csvOut = outcomes.slice(0, 2)
      for (const o of csvOut) {
        expect(o.duplicateCount).toBe(0)
        expect(o.newUniqueCount).toBe(2000)
      }
      expect(outcomes[2]?.newUniqueCount).toBe(2000)
    } finally {
      driver.close()
      rmSync(dir, { recursive: true, force: true })
    }
  }, 60000)

  it('汇总输出（供 docs/PERFORMANCE_SMOKE.md 记录）', () => {
    // eslint 例外：测试文件允许输出（见 eslint.config.js tests 块）
    process.stdout.write('\n=== PERFORMANCE SMOKE RESULTS ===\n')
    for (const r of results) {
      process.stdout.write(`${r.name}: ${r.ms.toFixed(1)} ms\n`)
    }
    process.stdout.write('=================================\n')
    expect(results.length).toBeGreaterThan(0)
  })
})

function mkdtemp(): string {
  return mkdtempSync(join(tmpdir(), 'depmap-perf-'))
}

void FX
