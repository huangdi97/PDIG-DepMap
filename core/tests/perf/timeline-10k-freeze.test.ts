import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import {
  buildTimeline,
  ChangePlanRepository,
  getGraphRevision,
  migrate,
  NodeRepository,
  NodeSqliteDriver,
  SourceInstanceRepository,
} from '../../src/index.ts'

/**
 * MVP03 FREEZE §48 —— Timeline 性能：10k items heavy smoke（1k 已有常规 smoke）。
 * 只检查明显退化，不做 premature optimization。
 */

describe('MVP03 Freeze Timeline 10k smoke', () => {
  let dir: string
  let driver: NodeSqliteDriver

  beforeEach(() => {
    dir = mkdtempSync(join(tmpdir(), 'depmap-tl10k-'))
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
    migrate(driver)
  })

  afterEach(() => {
    driver.close()
    rmSync(dir, { recursive: true, force: true })
  })

  it('10k TimelineItems 投影 + 排序确定性 < 10s', { timeout: 120_000 }, () => {
    const nodes = new NodeRepository(driver)
    const instances = new SourceInstanceRepository(driver)
    const plans = new ChangePlanRepository(driver)
    // 5000 过期来源 + 2500 计划 × 2 项 = 10k items
    for (let i = 0; i < 5000; i++) {
      const inst = instances.create({
        adapterId: 'generic_csv',
        adapterVersion: 1,
        sourceKind: 'statement_file',
        label: `来源 ${i}`,
      })
      instances.touchIngested(inst.id, '2026-01-01T00:00:00.000Z')
    }
    for (let i = 0; i < 2500; i++) {
      const card = nodes.create({ kind: 'payment_instrument', name: `卡 ${i}` })
      plans.create({
        templateId: 'replace_payment_card',
        scenario: 'replace_payment_card',
        title: `计划 ${i}`,
        targetNodeId: card.id,
        effectiveDate: `2026-${String((i % 12) + 1).padStart(2, '0')}-15`,
        graphRevision: getGraphRevision(driver),
        actions: [
          {
            id: `p${i}-v`,
            title: '验证',
            detail: '',
            phase: 'verify',
            done: false,
            doneAt: null,
            verification: {
              method: 'future_observation',
              status: 'pending',
              verifiedAt: null,
              evidenceRefs: [],
            },
          },
        ],
      })
    }
    const start = Date.now()
    const t1 = buildTimeline(driver, '2026-09-13T00:00:00.000Z')
    const elapsed = Date.now() - start
    expect(t1.length).toBeGreaterThanOrEqual(10_000)
    expect(buildTimeline(driver, '2026-09-13T00:00:00.000Z')).toEqual(t1) // 确定性
    expect(elapsed).toBeLessThan(10_000)
    console.log(`perf-freeze: 10k timeline items: ${elapsed} ms`)
  })
})
