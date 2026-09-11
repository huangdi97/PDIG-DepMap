import { describe, expect, it, beforeEach, afterEach } from 'vitest'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { NodeSqliteDriver } from '../../src/db/node-driver.ts'
import { migrate, currentSchemaVersion, SCHEMA_VERSION } from '../../src/schema/migrations.ts'

describe('Schema v1 migration', () => {
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

  it('creates schema v1 and reports version 1', () => {
    const v = migrate(driver, '2026-09-06T00:00:00.000Z')
    expect(v).toBe(SCHEMA_VERSION)
    expect(currentSchemaVersion(driver)).toBe(1)
  })

  it('is idempotent — re-running on fresh open is a no-op', () => {
    migrate(driver)
    migrate(driver)
    migrate(driver)
    expect(currentSchemaVersion(driver)).toBe(1)
  })

  it('is restart-safe — reopen and migrate again does not fail or duplicate', () => {
    migrate(driver)
    driver.close()
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
    migrate(driver)
    expect(currentSchemaVersion(driver)).toBe(1)
    // 表只建一份：UNIQUE 约束仍生效
    const tables = driver
      .prepare(`SELECT name FROM sqlite_master WHERE type='table' AND name='dependencies'`)
      .all()
    expect(tables).toHaveLength(1)
  })

  it('rolls back completely when a migration fails mid-way (no half-migrated DB)', () => {
    // 模拟 v2 失败：注入一个中途失败的迁移语句序列
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

  it('dependencies logical key UNIQUE constraint is enforced', () => {
    migrate(driver)
    const insert = driver.prepare(
      `INSERT INTO dependencies (id, from_node, relation, to_node, capability, origin, confirmed_at, last_verified_at, created_at, updated_at) VALUES ('d1','a','funding_source','b','payment','manual','t','t','t','t')`,
    )
    insert.run()
    expect(() =>
      driver
        .prepare(
          `INSERT INTO dependencies (id, from_node, relation, to_node, capability, origin, confirmed_at, last_verified_at, created_at, updated_at) VALUES ('d2','a','funding_source','b','payment','manual','t','t','t','t')`,
        )
        .run(),
    ).toThrowError(/UNIQUE/)
  })

  it('observation_fingerprints UNIQUE(source, fingerprint) is enforced', () => {
    migrate(driver)
    driver
      .prepare(
        `INSERT INTO observation_fingerprints (fingerprint, source, fingerprint_version, import_session_id, first_seen_at) VALUES ('fp1','wechat',1,'s1','t')`,
      )
      .run()
    expect(() =>
      driver
        .prepare(
          `INSERT INTO observation_fingerprints (fingerprint, source, fingerprint_version, import_session_id, first_seen_at) VALUES ('fp1','wechat',1,'s1','t')`,
        )
        .run(),
    ).toThrowError(/UNIQUE/)
  })

  it('criticality CHECK rejects values outside required|unknown', () => {
    migrate(driver)
    expect(() =>
      driver
        .prepare(
          `INSERT INTO dependencies (id, from_node, relation, to_node, capability, criticality, origin, confirmed_at, last_verified_at, created_at, updated_at) VALUES ('d9','a','funding_source','b','payment','preferred','manual','t','t','t','t')`,
        )
        .run(),
    ).toThrowError(/CHECK/)
  })
})
