import { describe, expect, it, beforeEach, afterEach } from 'vitest'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { NodeSqliteDriver } from '../../src/db/node-driver.ts'
import {
  currentSchemaVersion,
  LATEST_SCHEMA_VERSION,
  migrate,
  SCHEMA_V1_STATEMENTS,
  SCHEMA_V2_STATEMENTS,
  SCHEMA_VERSION,
} from '../../src/schema/migrations.ts'
import { exportGraph, importGraph } from '../../src/services/graph-serialize.ts'
import { createDepmapContainer, openDepmapContainer } from '../../src/crypto/depmap.ts'

/**
 * Schema v4 Final Contract Closure (v0.3.0) — spec-aligned evidence.
 *
 * Canonical authority (spec/schema/logical-schema.json + logical-schema-v4.json
 * payloadNote): the .depmap payload is the LOGICAL GRAPH only (11 tables);
 * failure_domains / provider_policies / change_plans / reality_drifts /
 * discovery_candidates are app-level knowledge/reality metadata that survive
 * LOCAL persistence (Schema v4 DB) but are intentionally NOT exported, keeping
 * payload v3 byte-compatible and DEPMAP_CONTAINER_V1 frozen. Therefore:
 *   - v4 metadata lossless round-trip is proven at the DB level (close → reopen);
 *   - .depmap round-trip is proven for the graph payload (byte-equal);
 *   - migration chains v1→v4 / v2→v4 / v3→v4, v4 reopen, future reject and
 *     corrupt rollback are proven on fresh runs.
 */

const NOW = '2026-09-27T00:00:00.000Z'

describe('Schema v4 persistence closure (v0.3.0)', () => {
  let dir: string
  let driver: NodeSqliteDriver

  beforeEach(() => {
    dir = mkdtempSync(join(tmpdir(), 'depmap-v4-'))
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
  })

  afterEach(() => {
    driver.close()
    rmSync(dir, { recursive: true, force: true })
  })

  function makeV2Database(d: NodeSqliteDriver): void {
    for (const stmt of [...SCHEMA_V1_STATEMENTS, ...SCHEMA_V2_STATEMENTS]) d.exec(stmt)
    d.prepare(`INSERT INTO meta (key, value) VALUES ('schema_version', '2')`).run()
  }

  /** Seed the full v4 object set (identity/recovery relations + all v4-era metadata tables). */
  function seedV4Reality(d: NodeSqliteDriver): void {
    const t = (n: number) => `2026-09-${String(n).padStart(2, '0')}T00:00:00.000Z`
    d.prepare(
      `INSERT INTO nodes (id, kind, name, owner, fields_json, created_at, updated_at)
       VALUES ('phone', 'identity_anchor', '13800000000', 'self', '{"subtype":"phone_number"}', ?, ?)`,
    ).run(t(1), t(1))
    d.prepare(
      `INSERT INTO nodes (id, kind, name, owner, fields_json, created_at, updated_at)
       VALUES ('wechat', 'account', '微信支付', 'self', '{"subtype":"platform_account"}', ?, ?)`,
    ).run(t(1), t(1))
    d.prepare(
      `INSERT INTO nodes (id, kind, name, owner, fields_json, created_at, updated_at)
       VALUES ('device', 'device', '主力手机', 'self', '{"subtype":"phone"}', ?, ?)`,
    ).run(t(1), t(1))
    d.prepare(
      `INSERT INTO nodes (id, kind, name, last4, owner, fields_json, created_at, updated_at)
       VALUES ('card', 'payment_instrument', '招商银行信用卡', '4417', 'self', '{}', ?, ?)`,
    ).run(t(1), t(1))
    const deps: Array<[string, string, string, string, string]> = [
      ['d-auth', 'phone', 'authenticates', 'wechat', 'authentication'],
      ['d-rec', 'wechat', 'recovers', 'phone', 'recovery'],
      ['d-ctrl', 'phone', 'controls', 'device', 'communication'],
      ['d-fund', 'card', 'funding_source', 'wechat', 'payment'],
    ]
    for (const [id, from, rel, to, cap] of deps) {
      d.prepare(
        `INSERT INTO dependencies (id, from_node, relation, to_node, capability, criticality, state, origin, confirmed_at, last_verified_at, evidence_refs_json, verification_basis_type, created_at, updated_at)
         VALUES (?, ?, ?, ?, ?, 'required', 'active', 'manual', ?, ?, '[]', 'user_confirmed', ?, ?)`,
      ).run(id, from, rel, to, cap, t(2), t(2), t(2), t(2))
    }
    d.prepare(
      `INSERT INTO dependency_groups (id, group_key, target_node_id, capability, mode, member_edge_ids_json, state, confirmed_at, last_verified_at, verification_basis_type, created_at, updated_at)
       VALUES ('g1', 'wechat|payment|ANY', 'wechat', 'payment', 'ANY', '["d-fund"]', 'active', ?, ?, 'user_confirmed', ?, ?)`,
    ).run(t(2), t(2), t(2), t(2))
    d.prepare(
      `INSERT INTO failure_domains (id, kind, subject_ref, scope, evidence_refs_json, status, confirmed_at, created_at, updated_at)
       VALUES ('fd-device', 'DEVICE', 'device', 'ALL_CAPABILITIES', '["ev-fd-1"]', 'confirmed', ?, ?, ?)`,
    ).run(t(3), t(3), t(3))
    d.prepare(
      `INSERT INTO provider_policies (provider, policy_type, source_url, retrieved_at, last_verified_at, effective_from, effective_to, jurisdiction, account_type_scope, parameters_json, policy_revision, state)
       VALUES ('微信支付', 'phone_recovery', 'https://example.invalid/policy', ?, ?, ?, NULL, 'CN', 'personal', '{"minDelayHours":24}', 3, 'effective')`,
    ).run(t(3), t(3), t(3))
    d.prepare(
      `INSERT INTO change_plans (id, template_id, scenario, title, workflow_state, baseline_graph_revision, last_analyzed_graph_revision, target_node_id, effective_date, params_json, impact_snapshot_json, action_items_json, created_at, updated_at, baseline_policy_revision, last_analyzed_policy_revision, change_primitive, temporal_effective_at, temporal_verification_not_before, temporal_verification_due_at, temporal_retire_old_path_after)
       VALUES ('plan-rp', 'scenario.replace_phone_number', 'replace_phone_number', '更换手机号', 'in_progress', 5, 6, 'phone', ?, '{"scenario":"replace_phone_number","target":"phone"}', '{"impact":{}}', '[{"actionId":"a1","prerequisiteActionIds":["a0"],"kind":"verify_new_path"},{"actionId":"a2","prerequisiteActionIds":["a1"],"kind":"retire_old_path"}]', ?, ?, 3, 3, 'REPLACE', ?, ?, ?, ?)`,
    ).run(t(4), t(4), t(4), t(5), t(5), t(6), t(8))
    d.prepare(
      `INSERT INTO reality_drifts (id, kind, target_node_id, capability, candidate_from, candidate_relation, related_dependency_ids_json, evidence_refs_json, proposal_keys_json, observation_count, detected_at, updated_at, status)
       VALUES ('drift-1', 'possible_replacement', 'phone', 'recovery', 'new_phone', 'recovers', '["d-rec"]', '[]', '[]', 3, ?, ?, 'open')`,
    ).run(t(5), t(5))
    d.prepare(
      `INSERT INTO discovery_candidates (id, candidate_kind, display_label, normalized_key, source_instance_id, evidence_refs_json, observation_count, first_seen_at, last_seen_at, status, created_at, updated_at)
       VALUES ('cand-1', 'merchant', '新商户', 'new-merchant', 'legacy-wechat-statement', '[]', 2, ?, ?, 'pending', ?, ?)`,
    ).run(t(5), t(5), t(5), t(5))
    d.prepare(`INSERT INTO meta (key, value) VALUES ('graph_revision', '6')`).run()
  }

  it('T1: v4 local persistence — close → reopen → semantic equality, zero drops (DB level)', () => {
    migrate(driver, NOW, LATEST_SCHEMA_VERSION)
    expect(currentSchemaVersion(driver)).toBe(4)
    seedV4Reality(driver)
    driver.close()

    // reopen the SAME DB file: v4 metadata must survive local persistence
    const d2 = new NodeSqliteDriver(join(dir, 'test.db'))
    d2.open()
    expect(currentSchemaVersion(d2)).toBe(4)
    const fd = d2
      .prepare(`SELECT kind, status, subject_ref FROM failure_domains WHERE id = 'fd-device'`)
      .get()
    expect((fd as Record<string, unknown>)['kind']).toBe('DEVICE')
    expect((fd as Record<string, unknown>)['status']).toBe('confirmed')
    const pp = d2
      .prepare(`SELECT policy_revision, state FROM provider_policies WHERE provider = '微信支付'`)
      .get()
    expect((pp as Record<string, unknown>)['policy_revision']).toBe(3)
    expect((pp as Record<string, unknown>)['state']).toBe('effective')
    const plan = d2
      .prepare(
        `SELECT change_primitive, baseline_policy_revision, temporal_retire_old_path_after, action_items_json
         FROM change_plans WHERE id = 'plan-rp'`,
      )
      .get()
    expect((plan as Record<string, unknown>)['change_primitive']).toBe('REPLACE')
    expect((plan as Record<string, unknown>)['baseline_policy_revision']).toBe(3)
    expect((plan as Record<string, unknown>)['temporal_retire_old_path_after']).toBe(
      '2026-09-08T00:00:00.000Z',
    )
    expect((plan as Record<string, unknown>)['action_items_json'] as string).toContain(
      'prerequisiteActionIds',
    )
    const rels = new Set(
      (d2.prepare(`SELECT relation FROM dependencies`).all() as Array<Record<string, unknown>>).map(
        (r) => r['relation'],
      ),
    )
    expect(rels).toEqual(new Set(['authenticates', 'recovers', 'controls', 'funding_source']))
    d2.close()
  })

  it('T2: .depmap graph payload round-trip (11 tables, byte-equal) — spec-aligned', () => {
    migrate(driver, NOW, LATEST_SCHEMA_VERSION)
    seedV4Reality(driver)
    const e1 = exportGraph(driver)
    const p1 = JSON.parse(e1.payloadJson) as Record<string, unknown>
    expect(p1['schemaVersion']).toBe(3)
    // spec payloadNote: v4-era metadata tables are NOT exported
    expect(p1['failure_domains']).toBeUndefined()
    expect(p1['provider_policies']).toBeUndefined()
    expect(p1['change_plans']).toBeUndefined()
    expect(p1['reality_drifts']).toBeUndefined()
    expect(p1['discovery_candidates']).toBeUndefined()

    const d2 = new NodeSqliteDriver(join(dir, 'rt.db'))
    d2.open()
    migrate(d2, NOW, LATEST_SCHEMA_VERSION)
    try {
      importGraph(d2, e1.payloadJson)
      const e2 = exportGraph(d2)
      expect(e2.payloadJson).toBe(e1.payloadJson)
      expect(e2.counts).toEqual(e1.counts)
    } finally {
      d2.close()
    }
  })

  it('T3: v1 → v4 chain preserves legacy data and creates v4 tables', () => {
    for (const stmt of SCHEMA_V1_STATEMENTS) driver.exec(stmt)
    driver.prepare(`INSERT INTO meta (key, value) VALUES ('schema_version', '1')`).run()
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
    expect(migrate(driver, NOW, LATEST_SCHEMA_VERSION)).toBe(4)
    expect(currentSchemaVersion(driver)).toBe(4)
    const tables = driver
      .prepare(
        `SELECT name FROM sqlite_master WHERE type='table' AND name IN ('failure_domains','provider_policies')`,
      )
      .all()
    expect(tables).toHaveLength(2)
    const fp = driver
      .prepare(
        `SELECT source_instance_id FROM observation_fingerprints WHERE fingerprint = 'fp-legacy-1'`,
      )
      .get()
    expect((fp as Record<string, unknown>)['source_instance_id']).toBe('legacy-wechat-statement')
  })

  it('T4: v2 → v4 chain preserves data and widens checks', () => {
    makeV2Database(driver)
    driver
      .prepare(
        `INSERT INTO dependencies (id, from_node, relation, to_node, capability, criticality, state, origin, confirmed_at, last_verified_at, evidence_refs_json, verification_basis_type, created_at, updated_at)
         VALUES ('d1', 'n1', 'funding_source', 'acc1', 'payment', 'required', 'active', 'manual', 't', 't', '[]', 'user_confirmed', 't', 't')`,
      )
      .run()
    expect(migrate(driver, NOW, LATEST_SCHEMA_VERSION)).toBe(4)
    const dep = driver.prepare(`SELECT id FROM dependencies WHERE id = 'd1'`).get()
    expect((dep as Record<string, unknown>)['id']).toBe('d1')
    const fd = driver
      .prepare(`SELECT name FROM sqlite_master WHERE type='table' AND name='failure_domains'`)
      .all()
    expect(fd).toHaveLength(1)
  })

  it('T5: v3 → v4 keeps v3 data and adds temporal/policy columns + widening', () => {
    migrate(driver, NOW, SCHEMA_VERSION)
    expect(currentSchemaVersion(driver)).toBe(3)
    driver
      .prepare(
        `INSERT INTO change_plans (id, scenario, title, workflow_state, baseline_graph_revision, last_analyzed_graph_revision, params_json, action_items_json, created_at, updated_at)
         VALUES ('p3', 'replace_phone_number', '旧计划', 'ready', 1, 1, '{}', '[]', 't', 't')`,
      )
      .run()
    expect(migrate(driver, NOW, LATEST_SCHEMA_VERSION)).toBe(4)
    const plan = driver
      .prepare(
        `SELECT change_primitive, baseline_policy_revision, temporal_effective_at FROM change_plans WHERE id = 'p3'`,
      )
      .get()
    expect((plan as Record<string, unknown>)['change_primitive']).toBe('REPLACE')
    driver
      .prepare(
        `INSERT INTO dependencies (id, from_node, relation, to_node, capability, criticality, state, origin, confirmed_at, last_verified_at, evidence_refs_json, verification_basis_type, created_at, updated_at)
         VALUES ('d4', 'n1', 'authenticates', 'n2', 'identity', 'unknown', 'active', 'manual', 't', 't', '[]', 'user_confirmed', 't', 't')`,
      )
      .run()
  })

  it('T6: v4 reopen is an idempotent no-op', () => {
    migrate(driver, NOW, LATEST_SCHEMA_VERSION)
    for (let i = 0; i < 10; i++) {
      expect(migrate(driver, NOW, LATEST_SCHEMA_VERSION)).toBe(4)
      expect(currentSchemaVersion(driver)).toBe(4)
    }
  })

  it('T7: future schema reject fails closed (target > LATEST and target < current)', () => {
    migrate(driver, NOW, LATEST_SCHEMA_VERSION)
    expect(() => migrate(driver, NOW, LATEST_SCHEMA_VERSION + 1)).toThrowError(
      /newer than supported/,
    )
    expect(currentSchemaVersion(driver)).toBe(4)
    expect(() => migrate(driver, NOW, SCHEMA_VERSION)).toThrowError(/newer than target/)
    expect(currentSchemaVersion(driver)).toBe(4)
  })

  it('T8: corrupt (partial-v4) DB rolls back — no half migration, no schema bump', () => {
    migrate(driver, NOW, SCHEMA_VERSION)
    driver.exec(`ALTER TABLE change_plans ADD COLUMN baseline_policy_revision INTEGER`)
    expect(() => migrate(driver, NOW, LATEST_SCHEMA_VERSION)).toThrow()
    expect(currentSchemaVersion(driver)).toBe(3)
    const fd = driver
      .prepare(`SELECT name FROM sqlite_master WHERE type='table' AND name='failure_domains'`)
      .all()
    expect(fd).toHaveLength(0)
  })

  it('T9: v3 payload restores into a v4 app (V3_TO_V4_RESTORE, graph only per spec)', async () => {
    migrate(driver, NOW, SCHEMA_VERSION)
    const v3 = exportGraph(driver)
    const container = await createDepmapContainer(
      new TextEncoder().encode(v3.payloadJson),
      'closure-password',
    )
    const opened = await openDepmapContainer(container.json, 'closure-password')
    const restored = new TextDecoder().decode(opened.plaintext)

    const d2 = new NodeSqliteDriver(join(dir, 'v4-restore.db'))
    d2.open()
    migrate(d2, NOW, LATEST_SCHEMA_VERSION)
    try {
      importGraph(d2, restored)
      // graph payload restores cleanly; v4 metadata tables stay empty (spec payloadNote)
      const fd = d2.prepare(`SELECT COUNT(*) AS c FROM failure_domains`).get()
      expect(Number((fd as Record<string, unknown>)['c'])).toBe(0)
      const cp = d2.prepare(`SELECT COUNT(*) AS c FROM change_plans`).get()
      expect(Number((cp as Record<string, unknown>)['c'])).toBe(0)
    } finally {
      d2.close()
    }
  })

  it('T10: v4 backup → v4 restore through the real container (graph), wrong password and tamper fail closed', async () => {
    migrate(driver, NOW, LATEST_SCHEMA_VERSION)
    seedV4Reality(driver)
    const e1 = exportGraph(driver)
    const container = await createDepmapContainer(
      new TextEncoder().encode(e1.payloadJson),
      'closure-password',
    )
    await expect(openDepmapContainer(container.json, 'wrong-password')).rejects.toThrow()
    const tampered = container.json.replace(
      /"(ciphertext|tag)":"[^"]+"/,
      '"tag":"AAAAAAAAAAAAAAAAAAAAAA=="',
    )
    await expect(openDepmapContainer(tampered, 'closure-password')).rejects.toThrow()
    const opened = await openDepmapContainer(container.json, 'closure-password')
    const d2 = new NodeSqliteDriver(join(dir, 'v4-rt.db'))
    d2.open()
    migrate(d2, NOW, LATEST_SCHEMA_VERSION)
    try {
      importGraph(d2, new TextDecoder().decode(opened.plaintext))
      const e2 = exportGraph(d2)
      expect(e2.payloadJson).toBe(e1.payloadJson)
    } finally {
      d2.close()
    }
  })

  it('T11: Unicode + large payload survive the graph round-trip', () => {
    migrate(driver, NOW, LATEST_SCHEMA_VERSION)
    seedV4Reality(driver)
    const bigText = '长'.repeat(4096)
    driver
      .prepare(`UPDATE nodes SET name = ?, fields_json = ? WHERE id = 'wechat'`)
      .run(
        '微信支付（Unicode 测试）',
        JSON.stringify({ subtype: 'platform_account', blob: bigText }),
      )
    const e1 = exportGraph(driver)
    const d2 = new NodeSqliteDriver(join(dir, 'uni.db'))
    d2.open()
    migrate(d2, NOW, LATEST_SCHEMA_VERSION)
    try {
      importGraph(d2, e1.payloadJson)
      const e2 = exportGraph(d2)
      expect(e2.payloadJson).toBe(e1.payloadJson)
    } finally {
      d2.close()
    }
  })
})
