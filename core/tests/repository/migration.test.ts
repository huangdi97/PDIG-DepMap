import { describe, expect, it, beforeEach, afterEach } from 'vitest'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { NodeSqliteDriver } from '../../src/db/node-driver.ts'
import {
  migrate,
  currentSchemaVersion,
  SCHEMA_VERSION,
  SCHEMA_V1_STATEMENTS,
  SCHEMA_V2_STATEMENTS,
  MIGRATIONS,
  LEGACY_WECHAT_SOURCE_INSTANCE_ID,
} from '../../src/schema/migrations.ts'

describe('Schema migration (v1→v2, MVP02 T1–T10)', () => {
  let dir: string
  let driver: NodeSqliteDriver

  beforeEach(() => {
    dir = mkdtempSync(join(tmpdir(), 'depmap-mig-'))
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
  })

  afterEach(() => {
    driver.close()
    rmSync(dir, { recursive: true, force: true })
  })

  it('T1: fresh v2 —— migrate() 一次到 schemaVersion=2 且含 source_instances 表', () => {
    const v = migrate(driver, '2026-09-12T00:00:00.000Z')
    expect(v).toBe(SCHEMA_VERSION)
    expect(currentSchemaVersion(driver)).toBe(2)
    const tables = driver
      .prepare(`SELECT name FROM sqlite_master WHERE type='table' AND name='source_instances'`)
      .all()
    expect(tables).toHaveLength(1)
    // deterministic legacy WeChat 实例存在且唯一
    const legacy = driver
      .prepare(`SELECT COUNT(*) AS c FROM source_instances WHERE id = ?`)
      .get(LEGACY_WECHAT_SOURCE_INSTANCE_ID)
    expect(Number((legacy as Record<string, unknown>)['c'])).toBe(1)
  })

  it('幂等 —— 重复执行 no-op（fresh v2 上 ×50）', () => {
    for (let i = 0; i < 50; i++) migrate(driver)
    expect(currentSchemaVersion(driver)).toBe(2)
    const legacy = driver
      .prepare(`SELECT COUNT(*) AS c FROM source_instances WHERE id = ?`)
      .get(LEGACY_WECHAT_SOURCE_INSTANCE_ID)
    expect(Number((legacy as Record<string, unknown>)['c'])).toBe(1)
  })

  it('T2: v1 → v2 —— legacy fingerprint/evidence/proposal 数据保留且归属 legacy 实例', () => {
    // 手工建 v1 并插入数据
    driver.transaction(() => {
      for (const sql of SCHEMA_V1_STATEMENTS) driver.exec(sql)
      driver.prepare(`INSERT INTO meta (key, value) VALUES ('schema_version', '1')`).run()
    })
    driver
      .prepare(
        `INSERT INTO observation_fingerprints (fingerprint, source, fingerprint_version, import_session_id, first_seen_at)
         VALUES ('fp-legacy-1', 'wechat', 1, 's1', '2026-01-01')`,
      )
      .run()
    driver
      .prepare(
        `INSERT INTO evidence (id, proposal_key, source_type, parser_id, parser_version, last_import_session_id, first_observed_at, last_observed_at, observation_count, created_at, updated_at)
         VALUES ('ev1', 'a|funding_source|b|payment', 'wechat_bill', 'wechat', 1, 's1', '2026-01-01', '2026-06-01', 6, 't', 't')`,
      )
      .run()
    driver
      .prepare(
        `INSERT INTO dependency_proposals (id, key, from_node, relation, to_node, capability, proposal_type, source, parser_id, parser_version, confidence_score, path_json, evidence_id, observation_count, created_at, updated_at)
         VALUES ('p1', 'a|funding_source|b|payment', 'a', 'funding_source', 'b', 'payment', 'recurring_payment_route', 'statement', 'wechat', 1, 0.9, '[]', 'ev1', 6, 't', 't')`,
      )
      .run()

    migrate(driver)
    expect(currentSchemaVersion(driver)).toBe(2)

    // fingerprint 归属 legacy 实例且 dedupe 保持
    const fp = driver
      .prepare(
        `SELECT source_instance_id FROM observation_fingerprints WHERE fingerprint = 'fp-legacy-1'`,
      )
      .get()
    expect((fp as Record<string, unknown>)['source_instance_id']).toBe(
      LEGACY_WECHAT_SOURCE_INSTANCE_ID,
    )

    // evidence provenance 迁移
    const ev = driver.prepare(`SELECT * FROM evidence WHERE id = 'ev1'`).get() as Record<
      string,
      unknown
    >
    expect(ev['source_instance_id']).toBe(LEGACY_WECHAT_SOURCE_INSTANCE_ID)
    expect(ev['adapter_id']).toBe('wechat_statement')
    expect(ev['evidence_kind']).toBe('transaction_stream')

    // T6: evidenceId → evidenceRefs
    const refs = driver
      .prepare(
        `SELECT evidence_id FROM proposal_evidence_refs WHERE proposal_key = 'a|funding_source|b|payment'`,
      )
      .all()
    expect(refs).toHaveLength(1)
    const proposal = driver
      .prepare(`SELECT * FROM dependency_proposals WHERE id = 'p1'`)
      .get() as Record<string, unknown>
    expect(proposal['evidence_id']).toBeUndefined() // 列已移除
    expect(proposal['decision']).toBe('pending') // decision 不丢

    // legacy fingerprint 仍可去重（同 fingerprint 再插 → UNIQUE）
    expect(() =>
      driver
        .prepare(
          `INSERT INTO observation_fingerprints (fingerprint, source_instance_id, source, fingerprint_version, import_session_id, first_seen_at)
           VALUES ('fp-legacy-1', ?, 'wechat', 1, 's2', '2026-02-01')`,
        )
        .run(LEGACY_WECHAT_SOURCE_INSTANCE_ID),
    ).toThrowError(/UNIQUE/)
  })

  it('T3: v1 → v2 → restart —— 重开再 migrate 安全（legacy 实例不重复）', () => {
    driver.transaction(() => {
      for (const sql of SCHEMA_V1_STATEMENTS) driver.exec(sql)
      driver.prepare(`INSERT INTO meta (key, value) VALUES ('schema_version', '1')`).run()
    })
    migrate(driver)
    const dbPath = join(dir, 'test.db')
    driver.close()
    driver = new NodeSqliteDriver(dbPath)
    driver.open()
    for (let i = 0; i < 50; i++) migrate(driver)
    expect(currentSchemaVersion(driver)).toBe(2)
    const legacy = driver
      .prepare(`SELECT COUNT(*) AS c FROM source_instances WHERE id = ?`)
      .get(LEGACY_WECHAT_SOURCE_INSTANCE_ID)
    expect(Number((legacy as Record<string, unknown>)['c'])).toBe(1)
  })

  it('T8: 注入迁移失败 → 整体回滚（v1 状态不变，随后正常迁移成功）', () => {
    driver.transaction(() => {
      for (const sql of SCHEMA_V1_STATEMENTS) driver.exec(sql)
      driver.prepare(`INSERT INTO meta (key, value) VALUES ('schema_version', '1')`).run()
    })
    // 毒化的 v2 前两条 + 非法 SQL → 事务回滚
    const poisoned = [...SCHEMA_V2_STATEMENTS.slice(0, 2), 'NOT VALID SQL --']
    expect(() =>
      driver.transaction(() => {
        for (const sql of poisoned) driver.exec(sql)
      }),
    ).toThrowError()
    // v1 表仍在、版本仍 1
    expect(currentSchemaVersion(driver)).toBe(1)
    const table = driver
      .prepare(
        `SELECT name FROM sqlite_master WHERE type='table' AND name='observation_fingerprints'`,
      )
      .get()
    expect(table).toBeDefined()
    // 正常 migrate 仍可完成
    migrate(driver)
    expect(currentSchemaVersion(driver)).toBe(2)
  })

  it('rolls back completely when a transaction fails mid-way', () => {
    expect(() =>
      driver.transaction(() => {
        driver.exec(`CREATE TABLE temp_x (a)`)
        driver.exec(`INSERT INTO temp_x VALUES (1)`)
        throw new Error('boom')
      }),
    ).toThrowError(/boom/)
    const row = driver.prepare(`SELECT name FROM sqlite_master WHERE name='temp_x'`).get()
    expect(row).toBeUndefined()
  })

  it('rejects a database whose schema_version is newer than supported', () => {
    migrate(driver)
    driver.prepare(`UPDATE meta SET value = '99' WHERE key = 'schema_version'`).run()
    expect(() => migrate(driver)).toThrowError(/newer than supported/)
  })

  it('T7: invalid relation/capability rejected（v2 CHECK 延续）', () => {
    migrate(driver)
    expect(() =>
      driver
        .prepare(
          `INSERT INTO dependencies (id, from_node, relation, to_node, capability, origin, confirmed_at, last_verified_at, created_at, updated_at) VALUES ('d1','a','bogus','b','payment','manual','t','t','t','t')`,
        )
        .run(),
    ).toThrowError(/CHECK/)
    expect(() =>
      driver
        .prepare(
          `INSERT INTO dependencies (id, from_node, relation, to_node, capability, origin, confirmed_at, last_verified_at, created_at, updated_at) VALUES ('d2','a','funding_source','b','bogus','manual','t','t','t','t')`,
        )
        .run(),
    ).toThrowError(/CHECK/)
  })

  it('v2 fingerprint UNIQUE(source_instance_id, fingerprint_version, fingerprint)', () => {
    migrate(driver)
    driver
      .prepare(
        `INSERT INTO observation_fingerprints (fingerprint, source_instance_id, source, fingerprint_version, import_session_id, first_seen_at)
         VALUES ('fpX', 'inst-a', 'wechat_statement', 1, 's', 't')`,
      )
      .run()
    // 同实例同指纹 → UNIQUE
    expect(() =>
      driver
        .prepare(
          `INSERT INTO observation_fingerprints (fingerprint, source_instance_id, source, fingerprint_version, import_session_id, first_seen_at)
           VALUES ('fpX', 'inst-a', 'wechat_statement', 1, 's', 't')`,
        )
        .run(),
    ).toThrowError(/UNIQUE/)
    // 不同实例同指纹 → 允许（跨源不冲突）
    driver
      .prepare(
        `INSERT INTO observation_fingerprints (fingerprint, source_instance_id, source, fingerprint_version, import_session_id, first_seen_at)
         VALUES ('fpX', 'inst-b', 'generic_csv', 1, 's', 't')`,
      )
      .run()
    // 同实例不同版本 → 允许
    driver
      .prepare(
        `INSERT INTO observation_fingerprints (fingerprint, source_instance_id, source, fingerprint_version, import_session_id, first_seen_at)
         VALUES ('fpX', 'inst-a', 'wechat_statement', 2, 's', 't')`,
      )
      .run()
  })

  it('dependencies logical key UNIQUE constraint is enforced（v2 延续）', () => {
    migrate(driver)
    driver
      .prepare(
        `INSERT INTO dependencies (id, from_node, relation, to_node, capability, origin, confirmed_at, last_verified_at, created_at, updated_at) VALUES ('d1','a','funding_source','b','payment','manual','t','t','t','t')`,
      )
      .run()
    expect(() =>
      driver
        .prepare(
          `INSERT INTO dependencies (id, from_node, relation, to_node, capability, origin, confirmed_at, last_verified_at, created_at, updated_at) VALUES ('d2','a','funding_source','b','payment','manual','t','t','t','t')`,
        )
        .run(),
    ).toThrowError(/UNIQUE/)
  })

  it('criticality CHECK rejects values outside required|unknown（v2 延续）', () => {
    migrate(driver)
    expect(() =>
      driver
        .prepare(
          `INSERT INTO dependencies (id, from_node, relation, to_node, capability, criticality, origin, confirmed_at, last_verified_at, created_at, updated_at) VALUES ('d9','a','funding_source','b','payment','preferred','manual','t','t','t','t')`,
        )
        .run(),
    ).toThrowError(/CHECK/)
  })

  it('verificationBasis 列存在且默认 user_confirmed（v2）', () => {
    migrate(driver)
    driver
      .prepare(
        `INSERT INTO dependencies (id, from_node, relation, to_node, capability, origin, confirmed_at, last_verified_at, created_at, updated_at) VALUES ('vb','a','funding_source','b','payment','manual','t','t','t','t')`,
      )
      .run()
    const row = driver
      .prepare(`SELECT verification_basis_type FROM dependencies WHERE id = 'vb'`)
      .get()
    expect((row as Record<string, unknown>)['verification_basis_type']).toBe('user_confirmed')
  })

  it('T9: 无孤儿 SourceInstance 引用（v2 模板自带引用完整）', () => {
    migrate(driver)
    // legacy 实例存在；evidence/fingerprints 为空 → 无孤儿
    const orphan = driver
      .prepare(
        `SELECT COUNT(*) AS c FROM evidence WHERE source_instance_id NOT IN (SELECT id FROM source_instances)`,
      )
      .get()
    expect(Number((orphan as Record<string, unknown>)['c'])).toBe(0)
  })

  it('MIGRATIONS 顺序完整（1,2）', () => {
    expect(MIGRATIONS.map((m) => m.version)).toEqual([1, 2])
  })

  // -------------------------------------------------------------------------
  // T4 —— ×50 幂等在**已迁移且已装载数据**的库上成立
  //
  // T3 只覆盖「v1 空库 → v2 → ×50」。真实风险是：库已迁移完成、且含
  // legacy 数据与多实例 provenance 时，重复 migrate 仍须严格 no-op。
  // -------------------------------------------------------------------------
  it('T4: 已迁移且有数据的库上 ×50 —— 数据零漂移、legacy 实例不重复', () => {
    // 建 v1 并装载 legacy 数据
    driver.transaction(() => {
      for (const sql of SCHEMA_V1_STATEMENTS) driver.exec(sql)
      driver.prepare(`INSERT INTO meta (key, value) VALUES ('schema_version', '1')`).run()
    })
    driver
      .prepare(
        `INSERT INTO observation_fingerprints (fingerprint, source, fingerprint_version, import_session_id, first_seen_at)
         VALUES ('fp-t4-a', 'wechat', 1, 's1', '2026-01-01'),
                ('fp-t4-b', 'wechat', 1, 's1', '2026-02-01')`,
      )
      .run()
    driver
      .prepare(
        `INSERT INTO evidence (id, proposal_key, source_type, parser_id, parser_version, last_import_session_id, first_observed_at, last_observed_at, observation_count, created_at, updated_at)
         VALUES ('ev-t4', 'a|funding_source|b|payment', 'wechat_bill', 'wechat', 1, 's1', '2026-01-01', '2026-06-01', 6, 't', 't')`,
      )
      .run()
    driver
      .prepare(
        `INSERT INTO dependency_proposals (id, key, from_node, relation, to_node, capability, proposal_type, source, parser_id, parser_version, confidence_score, path_json, evidence_id, observation_count, created_at, updated_at)
         VALUES ('p-t4', 'a|funding_source|b|payment', 'a', 'funding_source', 'b', 'payment', 'recurring_payment_route', 'statement', 'wechat', 1, 0.9, '[]', 'ev-t4', 6, 't', 't')`,
      )
      .run()

    migrate(driver)
    expect(currentSchemaVersion(driver)).toBe(2)

    const snapshot = () => ({
      fps: driver
        .prepare(
          `SELECT fingerprint, source_instance_id FROM observation_fingerprints ORDER BY fingerprint`,
        )
        .all(),
      evidence: driver.prepare(`SELECT * FROM evidence ORDER BY id`).all(),
      proposals: driver
        .prepare(`SELECT id, key, decision FROM dependency_proposals ORDER BY id`)
        .all(),
      refs: driver
        .prepare(
          `SELECT proposal_key, evidence_id, position FROM proposal_evidence_refs ORDER BY proposal_key, position`,
        )
        .all(),
      instances: driver.prepare(`SELECT id, adapter_id FROM source_instances ORDER BY id`).all(),
      version: currentSchemaVersion(driver),
    })
    const before = JSON.stringify(snapshot())

    for (let i = 0; i < 50; i++) migrate(driver)

    // 严格 no-op：连续 50 次迁移后状态逐字段不变
    expect(JSON.stringify(snapshot())).toBe(before)
    // legacy 实例唯一
    const legacyCount = driver
      .prepare(`SELECT COUNT(*) AS c FROM source_instances WHERE id = ?`)
      .get(LEGACY_WECHAT_SOURCE_INSTANCE_ID)
    expect(Number((legacyCount as Record<string, unknown>)['c'])).toBe(1)
  })

  it('T4b: 已迁移库在进程重启后 ×50 —— 仍严格 no-op', () => {
    migrate(driver)
    const dbPath = join(dir, 'test.db')
    driver.close()
    driver = new NodeSqliteDriver(dbPath)
    driver.open()

    const before = JSON.stringify({
      instances: driver.prepare(`SELECT id FROM source_instances ORDER BY id`).all(),
      version: currentSchemaVersion(driver),
    })
    for (let i = 0; i < 50; i++) migrate(driver)
    const after = JSON.stringify({
      instances: driver.prepare(`SELECT id FROM source_instances ORDER BY id`).all(),
      version: currentSchemaVersion(driver),
    })
    expect(after).toBe(before)
  })

  // -------------------------------------------------------------------------
  // T5 —— legacy 去重语义在迁移与后续写入中保持
  //
  // T2 只断言「再插同 fingerprint 会 UNIQUE 失败」。真实要求更强：
  // 迁移后的去重仍按 (source_instance_id, fingerprint_version, fingerprint)
  // 作用域生效，且 legacy 行的 fingerprint_version / source 展示字段未被改写。
  // -------------------------------------------------------------------------
  it('T5: legacy 去重语义保持 —— 同实例同版本重复拒绝、不同版本/不同实例放行', () => {
    driver.transaction(() => {
      for (const sql of SCHEMA_V1_STATEMENTS) driver.exec(sql)
      driver.prepare(`INSERT INTO meta (key, value) VALUES ('schema_version', '1')`).run()
    })
    driver
      .prepare(
        `INSERT INTO observation_fingerprints (fingerprint, source, fingerprint_version, import_session_id, first_seen_at)
         VALUES ('fp-t5', 'wechat', 1, 's1', '2026-01-01')`,
      )
      .run()

    migrate(driver)

    // legacy 行的非作用域字段未被迁移改写（展示字段 source 保留）
    const row = driver
      .prepare(
        `SELECT source, fingerprint_version, source_instance_id FROM observation_fingerprints WHERE fingerprint = 'fp-t5'`,
      )
      .get() as Record<string, unknown>
    expect(row['source']).toBe('wechat')
    expect(Number(row['fingerprint_version'])).toBe(1)
    expect(row['source_instance_id']).toBe(LEGACY_WECHAT_SOURCE_INSTANCE_ID)

    const insert = (fp: string, inst: string, ver: number) =>
      driver
        .prepare(
          `INSERT INTO observation_fingerprints (fingerprint, source_instance_id, source, fingerprint_version, import_session_id, first_seen_at)
           VALUES (?, ?, 'wechat', ?, 's2', '2026-03-01')`,
        )
        .run(fp, inst, ver)

    // 同实例 + 同版本 + 同 fingerprint → 拒绝（legacy 去重仍在生效）
    expect(() => insert('fp-t5', LEGACY_WECHAT_SOURCE_INSTANCE_ID, 1)).toThrowError(/UNIQUE/)

    // 同实例但 fingerprint_version 不同 → 放行（版本属于作用域的一部分）
    expect(() => insert('fp-t5', LEGACY_WECHAT_SOURCE_INSTANCE_ID, 2)).not.toThrow()

    // 不同 SourceInstance → 放行（命名空间隔离）
    const other = driver
      .prepare(
        `INSERT INTO source_instances (id, adapter_id, adapter_version, source_kind, label, currencies_json, state, created_at, updated_at)
         VALUES ('si-t5-other', 'generic_csv', 1, 'statement_file', 'other', '[]', 'active', 't', 't')`,
      )
      .run()
    expect(other.changes).toBe(1)
    expect(() => insert('fp-t5', 'si-t5-other', 1)).not.toThrow()

    // 唯一性由表级 UNIQUE 约束定义（非独立索引）→ 检查重建后的表 DDL 列顺序
    const ddl = driver
      .prepare(
        `SELECT sql FROM sqlite_master WHERE type='table' AND name='observation_fingerprints'`,
      )
      .get() as Record<string, unknown>
    const tableSql = typeof ddl['sql'] === 'string' ? ddl['sql'] : ''
    expect(tableSql).toMatch(
      /UNIQUE\s*\(\s*source_instance_id\s*,\s*fingerprint_version\s*,\s*fingerprint\s*\)/,
    )
    // v1 的 UNIQUE(source, fingerprint) 必须已被移除（否则去重口径错误）
    expect(tableSql).not.toMatch(/UNIQUE\s*\(\s*source\s*,\s*fingerprint\s*\)/)
  })
})
