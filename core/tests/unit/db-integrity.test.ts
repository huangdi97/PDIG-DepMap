import { describe, expect, it, beforeEach, afterEach } from 'vitest'
import { mkdtempSync, rmSync, readFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { NodeSqliteDriver } from '../../src/db/node-driver.ts'
import { migrate, currentSchemaVersion } from '../../src/schema/migrations.ts'
import { NodeRepository } from '../../src/repositories/node-repository.ts'
import { DependencyRepository } from '../../src/repositories/dependency-repository.ts'
import { ImportFlow } from '../../src/services/import-pipeline.ts'
import { checkGraphIntegrity, exportGraph } from '../../src/services/graph-serialize.ts'

/**
 * PHASE S — Database RC Audit + PHASE AE — Import Transactionality。
 *
 * FK 设计决策（有意为之，见 docs/FAIL_CLOSED_AUDIT.md）：
 * v1 不声明 FOREIGN KEY，原因：retirement 语义要求节点永不物理删除、
 * 逻辑引用由 service 层（nodes.getExisting）校验；完整性通过
 * checkGraphIntegrity 孤儿检测持续保证（本文件验证）。
 */

const FX = (name: string) =>
  new Uint8Array(
    readFileSync(join(dirname(fileURLToPath(import.meta.url)), '..', 'fixtures', name)),
  )

describe('DB Integrity + Import Transactionality (RC PHASE S/AE)', () => {
  let dir: string
  let driver: NodeSqliteDriver

  beforeEach(() => {
    dir = mkdtempSync(join(tmpdir(), 'depmap-dbint-'))
    driver = new NodeSqliteDriver(join(dir, 't.db'))
    driver.open()
    migrate(driver)
  })

  afterEach(() => {
    driver.close()
    rmSync(dir, { recursive: true, force: true })
  })

  it('foreign_keys pragma 开启（驱动层），migration 版本正确', () => {
    const row = driver.prepare('PRAGMA foreign_keys').get() as Record<string, unknown>
    expect(Number(row['foreign_keys'])).toBe(1)
    expect(currentSchemaVersion(driver)).toBe(1)
  })

  it('正常流：无孤儿依赖/孤儿组/悬空组成员', () => {
    const nodes = new NodeRepository(driver)
    const deps = new DependencyRepository(driver)
    const card = nodes.create({ kind: 'payment_instrument', name: '招行卡', last4: '4417' })
    const wechat = nodes.create({ kind: 'account', name: '微信' })
    deps.confirm({
      from: card.id,
      relation: 'funding_source',
      to: wechat.id,
      capability: 'payment',
    })
    const report = checkGraphIntegrity(driver)
    expect(report.orphanDependencies).toHaveLength(0)
    expect(report.orphanGroups).toHaveLength(0)
    expect(report.danglingGroupMembers).toHaveLength(0)
  })

  it('孤儿检测：直接 SQL 注入悬空引用可被检出', () => {
    // 悬空依赖（from_node 不存在）
    driver
      .prepare(
        `INSERT INTO dependencies (id, from_node, relation, to_node, capability, criticality, state, origin, confirmed_at, last_verified_at, evidence_refs_json, created_at, updated_at)
         VALUES ('orphan1', 'ghost', 'funding_source', 'also-ghost', 'payment', 'unknown', 'active', 'manual', 't', 't', '[]', 't', 't')`,
      )
      .run()
    const report = checkGraphIntegrity(driver)
    expect(report.orphanDependencies).toEqual(['orphan1'])
  })

  it('组成员完整性：组引用已删除边 → danglingGroupMembers 检出', () => {
    driver
      .prepare(
        `INSERT INTO dependency_groups (id, group_key, target_node_id, capability, mode, member_edge_ids_json, state, confirmed_at, last_verified_at, created_at, updated_at)
         VALUES ('g1', 'gk', 'n1', 'payment', 'ANY', '["missing-edge"]', 'active', 't', 't', 't', 't')`,
      )
      .run()
    const report = checkGraphIntegrity(driver)
    expect(report.danglingGroupMembers).toEqual(['g1'])
  })

  it('transaction rollback：异常后无半写入', () => {
    expect(() =>
      driver.transaction(() => {
        driver
          .prepare(
            `INSERT INTO nodes (id, kind, name, owner, archived, fields_json, created_at, updated_at) VALUES ('n1', 'account', 'x', 'self', 0, '{}', 't', 't')`,
          )
          .run()
        throw new Error('boom')
      }),
    ).toThrowError(/boom/)
    const row = driver.prepare(`SELECT * FROM nodes WHERE id = 'n1'`).get()
    expect(row).toBeUndefined()
  })

  it('PHASE AE：finalize 中途失败（注入 evidence 冲突）→ 无半成品指纹/建议/evidence', () => {
    const nodes = new NodeRepository(driver)
    const tencent = nodes.create({ kind: 'service', name: '腾讯视频' })
    nodes.create({
      kind: 'payment_instrument',
      name: '招行经典白',
      issuer: '招商银行',
      last4: '4417',
    })

    // 预埋一个 evidence 行且与本次导入将产生的 key 相同，制造唯一约束冲突
    const wechatNode = nodes.create({
      kind: 'account',
      templateId: 'builtin.account.wechat',
      name: '微信支付',
    })
    driver
      .prepare(
        `INSERT INTO evidence (id, proposal_key, source_type, parser_id, parser_version, last_import_session_id, first_observed_at, last_observed_at, observation_count, created_at, updated_at)
         VALUES ('pre', 'X', 'x', 'x', 1, 'x', 'x', 'x', 0, 'x', 'x')`,
      )
      .run()
    void wechatNode

    // 修补 merchant proposal key 使其冲突：直接向 evidence 表插入与将要生成的相同 key
    const flow = new ImportFlow(driver)
    flow.begin(FX('recurring-monthly.csv'))
    flow.resolveMerchant('腾讯视频', tencent.id)

    // 计算将生成的 merchant key 并预插入 evidence（触发 UNIQUE 冲突）
    const merchantKey = `预置|merchant_agreement|${tencent.id}|payment`
    // ImportFlow 生成 key 用 wechat 节点 id；此处先 finalize 一次空跑拿 wechat id 不可行——
    // 改为在 finalize 的事务内通过 BEFORE INSERT 触发器注入失败
    driver.exec(
      `CREATE TRIGGER fail_evidence BEFORE INSERT ON dependency_proposals
       WHEN (SELECT COUNT(*) FROM evidence) > 0
       BEGIN SELECT RAISE(ABORT, 'injected failure'); END`,
    )
    expect(() => flow.finalize()).toThrowError(/injected failure/)

    // 回滚验证：指纹/建议/证据均无写入
    expect(
      Number(
        (
          driver.prepare(`SELECT COUNT(*) AS c FROM observation_fingerprints`).get() as Record<
            string,
            unknown
          >
        )['c'],
      ),
    ).toBe(0)
    expect(
      Number(
        (
          driver.prepare(`SELECT COUNT(*) AS c FROM dependency_proposals`).get() as Record<
            string,
            unknown
          >
        )['c'],
      ),
    ).toBe(0)
    expect(
      Number(
        (
          driver.prepare(`SELECT COUNT(*) AS c FROM dependency_groups`).get() as Record<
            string,
            unknown
          >
        )['c'],
      ),
    ).toBe(0)
    // 预置的 1 条 evidence 仍在（事务回滚不误删已有数据）
    expect(
      Number(
        (driver.prepare(`SELECT COUNT(*) AS c FROM evidence`).get() as Record<string, unknown>)[
          'c'
        ],
      ),
    ).toBe(1)
    void merchantKey

    // 移除触发器后重试：导入成功且 fingerprint 只在成功后入库（retry 安全）
    driver.exec('DROP TRIGGER fail_evidence')
    const outcome = flow.finalize()
    expect(outcome.newUniqueCount).toBe(8)
    expect(
      Number(
        (
          driver.prepare(`SELECT COUNT(*) AS c FROM observation_fingerprints`).get() as Record<
            string,
            unknown
          >
        )['c'],
      ),
    ).toBe(8)
    // export 在重试后可用且一致
    const exported = exportGraph(driver)
    expect(exported.counts['nodes']).toBe(3) // tencent + 招行卡 + wechat（pipeline 复用手建节点）
  })
})
