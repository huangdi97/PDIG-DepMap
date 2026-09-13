import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import {
  currentSchemaVersion,
  DependencyRepository,
  migrate,
  MIGRATIONS,
  NodeRepository,
  NodeSqliteDriver,
  SCHEMA_V1_STATEMENTS,
  SCHEMA_V2_STATEMENTS,
  SCHEMA_VERSION,
} from '../../src/index.ts'

/**
 * MVP03 MIG3 —— Schema v2 → v3 专项迁移 Gate（§56）。
 * 语义：v3 只新增表（change_plans / reality_drifts / discovery_candidates），
 * 既有 Dependency/Group/Evidence/SourceInstance 数据与 ID 不变；DEPMAP container 无关。
 */

/** 构造一个「停在 v2」的数据库（按 MIGRATIONS v1+v2 语句手工应用）。 */
function makeV2Database(driver: NodeSqliteDriver): void {
  for (const stmt of [...SCHEMA_V1_STATEMENTS, ...SCHEMA_V2_STATEMENTS]) {
    driver.exec(stmt)
  }
  driver
    .prepare(`INSERT INTO meta (key, value) VALUES ('schema_version', '2')`)
    .run()
}

describe('Schema migration v2 → v3 (MVP03 MIG3)', () => {
  let dir: string
  let driver: NodeSqliteDriver

  beforeEach(() => {
    dir = mkdtempSync(join(tmpdir(), 'depmap-mig3-'))
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
  })

  afterEach(() => {
    driver.close()
    rmSync(dir, { recursive: true, force: true })
  })

  function seedV2Reality(): { depId: string; nodeId: string } {
    makeV2Database(driver)
    const nodes = new NodeRepository(driver)
    const deps = new DependencyRepository(driver)
    const node = nodes.create({ kind: 'payment_instrument', name: '招行 4417', last4: '4417' })
    const account = nodes.create({ kind: 'account', templateId: 'builtin.account.wechat', name: '微信支付' })
    const { dependency } = deps.confirm({
      from: node.id,
      relation: 'funding_source',
      to: account.id,
      capability: 'payment',
      criticality: 'required',
    })
    return { depId: dependency.id, nodeId: node.id }
  }

  it('MIG3-001: fresh v3 —— migrate() 到 3 且三张新表存在', () => {
    expect(migrate(driver)).toBe(SCHEMA_VERSION)
    expect(currentSchemaVersion(driver)).toBe(3)
    for (const t of ['change_plans', 'reality_drifts', 'discovery_candidates']) {
      const row = driver.prepare(`SELECT COUNT(*) AS c FROM ${t}`).get() as { c: number }
      expect(Number(row.c)).toBe(0)
    }
  })

  it('MIG3-002: v2 → v3 —— 既有 Dependency/Node 数据与 ID 不变，新增表可用', () => {
    const { depId, nodeId } = seedV2Reality()
    expect(migrate(driver)).toBe(3)
    const deps = new DependencyRepository(driver)
    const nodes = new NodeRepository(driver)
    expect(deps.getById(depId)?.id).toBe(depId)
    expect(deps.getById(depId)?.state).toBe('active')
    expect(nodes.getById(nodeId)?.name).toBe('招行 4417')
    // 新表可写入（由各 repository 的常规路径验证；此处直接证明表存在）
    driver.prepare(`INSERT INTO change_plans (id, scenario, title, workflow_state, baseline_graph_revision, last_analyzed_graph_revision, params_json, action_items_json, created_at, updated_at)
      VALUES ('p1', 's', 't', 'draft', 0, 0, '{}', '[]', '2026-01-01', '2026-01-01')`).run()
    expect(driver.prepare(`SELECT COUNT(*) AS c FROM change_plans`).get()).toBeTruthy()
  })

  it('MIG3-003: v2 → v3 → restart —— 重开再 migrate 安全（幂等）', () => {
    seedV2Reality()
    expect(migrate(driver)).toBe(3)
    driver.close()
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
    for (let i = 0; i < 50; i++) {
      expect(migrate(driver)).toBe(3)
    }
    expect(currentSchemaVersion(driver)).toBe(3)
  })

  it('MIG3-004: failure rollback —— v3 迁移失败 → 停在 v2，随后正常迁移成功', () => {
    seedV2Reality()
    // 注入：预创建同名表使 CREATE TABLE 失败 → v3 事务回滚
    driver.exec(`CREATE TABLE change_plans (poison TEXT)`)
    expect(() => migrate(driver)).toThrow()
    expect(currentSchemaVersion(driver)).toBe(2)
    // 清理毒表后重试成功
    driver.exec(`DROP TABLE change_plans`)
    expect(migrate(driver)).toBe(3)
  })

  it('MIG3-005: v2→v3 ×50 幂等 + 有数据零漂移', () => {
    const { depId } = seedV2Reality()
    expect(migrate(driver)).toBe(3)
    const deps = new DependencyRepository(driver)
    for (let i = 0; i < 50; i++) {
      expect(migrate(driver)).toBe(3)
      expect(deps.getById(depId)?.state).toBe('active')
    }
  })

  it('MIG3-006: MIGRATIONS 声明含 version 3；未来版本拒绝', () => {
    expect(MIGRATIONS.map((m) => m.version)).toEqual([1, 2, 3])
    migrate(driver)
    driver.prepare(`UPDATE meta SET value = '99' WHERE key = 'schema_version'`).run()
    expect(() => migrate(driver)).toThrow(/newer than supported/)
  })
})
