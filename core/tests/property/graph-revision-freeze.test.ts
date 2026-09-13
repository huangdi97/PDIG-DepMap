import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import fc from 'fast-check'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import {
  DependencyGroupRepository,
  DependencyProposalRepository,
  DependencyRepository,
  DiscoveryService,
  getGraphRevision,
  migrate,
  NodeRepository,
  NodeSqliteDriver,
  RealityDriftService,
} from '../../src/index.ts'

/**
 * MVP03 FREEZE FR-GR-012 / GOAL §16 —— graphRevision property invariant。
 * 随机混合 Reality / non-Reality 操作、失败注入与重放：
 *   revision ≡ 0 + (成功 commit 的 Reality mutation 次数)，monotonic / deterministic / rollback-safe。
 * 失败 seed 由 fast-check 自动保留（configureGlobal seed=20260913）。
 */
fc.configureGlobal({ seed: 20260913 })

describe('graphRevision property（FR-GR-012）', () => {
  let dir: string
  let driver: NodeSqliteDriver

  beforeEach(() => {
    dir = mkdtempSync(join(tmpdir(), 'depmap-frgr-'))
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
    migrate(driver)
  })

  afterEach(() => {
    driver.close()
    rmSync(dir, { recursive: true, force: true })
  })

  it(
    'P-FR-GR: 随机 Reality/non-Reality/failure/replay 序列 → revision ≡ Reality mutation 计数',
    { timeout: 120_000 },
    () => {
      const realityOpArb = fc.constantFrom(
        'confirm',
        'retire',
        'reactivate',
        'group',
        'criticality-change',
      ) as fc.Arbitrary<string>
      const nonRealityOpArb = fc.constantFrom(
        'proposal',
        'evidence',
        'drift-detect',
        'candidate',
        'verify-suggest',
      ) as fc.Arbitrary<string>
      const opsArb = fc.array(
        fc.oneof(
          realityOpArb.map((op) => ({ kind: 'reality' as const, op })),
          nonRealityOpArb.map((op) => ({ kind: 'non-reality' as const, op })),
          fc.constant({ kind: 'failure' as const, op: 'failed-tx' }),
          fc.constant({ kind: 'replay' as const, op: 'confirm-again' }),
        ),
        { minLength: 1, maxLength: 40 },
      )

      fc.assert(
        fc.property(opsArb, (ops) => {
          // 每个 property 用独立 DB
          const d = mkdtempSync(join(tmpdir(), 'depmap-frgr-case-'))
          const drv = new NodeSqliteDriver(join(d, 'test.db'))
          drv.open()
          migrate(drv)
          try {
            const nodes = new NodeRepository(drv)
            const deps = new DependencyRepository(drv)
            const groups = new DependencyGroupRepository(drv)
            const proposals = new DependencyProposalRepository(drv)
            const drifts = new RealityDriftService(drv)
            const discovery = new DiscoveryService(drv)

            const anchors = [
              nodes.create({ kind: 'payment_instrument', name: 'A' }).id,
              nodes.create({ kind: 'payment_instrument', name: 'B' }).id,
              nodes.create({ kind: 'account', name: 'W' }).id,
            ]
            let expected = 0
            let confirmed = false
            let retired = false
            let groupConfirmed = false

            for (const step of ops) {
              const before = getGraphRevision(drv)
              try {
                switch (step.kind) {
                  case 'reality': {
                    if (step.op === 'confirm') {
                      const r = deps.confirm({
                        from: anchors[0]!,
                        relation: 'funding_source',
                        to: anchors[2]!,
                        capability: 'payment',
                      })
                      if (r.reactivated || !confirmed) {
                        // INSERT 或 reactivate 才 +1；active verify 重放不加
                        if (!confirmed || retired) expected += 1
                        confirmed = true
                        retired = false
                      }
                    } else if (step.op === 'retire') {
                      const dep = deps.findByLogicalKey(
                        anchors[0]!,
                        'funding_source',
                        anchors[2]!,
                        'payment',
                      )
                      if (dep && dep.state === 'active') {
                        deps.retire(dep.id)
                        expected += 1
                        retired = true
                      }
                    } else if (step.op === 'reactivate') {
                      const dep = deps.findByLogicalKey(
                        anchors[0]!,
                        'funding_source',
                        anchors[2]!,
                        'payment',
                      )
                      if (dep && dep.state === 'retired') {
                        deps.confirm({
                          from: anchors[0]!,
                          relation: 'funding_source',
                          to: anchors[2]!,
                          capability: 'payment',
                        })
                        expected += 1
                        retired = false
                        confirmed = true
                      }
                    } else if (step.op === 'group') {
                      if (confirmed && !retired && !groupConfirmed) {
                        const d2 = deps.findByLogicalKey(
                          anchors[0]!,
                          'funding_source',
                          anchors[2]!,
                          'payment',
                        )!
                        groups.confirm(
                          {
                            targetNodeId: anchors[2]!,
                            capability: 'payment',
                            mode: 'ANY',
                            memberEdgeIds: [d2.id],
                          },
                          [`k-${d2.id}`],
                        )
                        expected += 1
                        groupConfirmed = true
                      }
                    } else if (step.op === 'criticality-change') {
                      const dep = deps.findByLogicalKey(
                        anchors[0]!,
                        'funding_source',
                        anchors[2]!,
                        'payment',
                      )
                      if (dep && dep.criticality !== 'required') {
                        deps.updateCriticality(dep.id, 'required')
                        expected += 1
                      }
                    }
                    break
                  }
                  case 'non-reality': {
                    if (step.op === 'proposal') {
                      proposals.upsert({
                        from: anchors[1]!,
                        relation: 'merchant_agreement',
                        to: anchors[2]!,
                        capability: 'payment',
                        proposalType: 'merchant_agreement',
                        source: 't',
                        parserId: 't',
                        parserVersion: 1,
                        confidenceScore: 0.999,
                      })
                    } else if (step.op === 'drift-detect') {
                      drifts.detectFromEvidence({
                        targetNodeId: anchors[2]!,
                        capability: 'payment',
                        signals: [
                          {
                            fromNodeId: anchors[1]!,
                            observations: 2,
                            evidenceRef: `e-${Math.random()}`,
                          },
                        ],
                      })
                    } else if (step.op === 'candidate') {
                      discovery.upsertCandidate({
                        candidateKind: 'service',
                        displayLabel: 'X',
                        normalizedKey: `svc:${step.op}`,
                        sourceInstanceId: 's1',
                      })
                    }
                    // evidence / verify-suggest 在此简化为无 DB 写（语义已在专项测试覆盖）
                    break
                  }
                  case 'failure': {
                    expect(() =>
                      drv.transaction(() => {
                        deps.confirm({
                          from: anchors[0]!,
                          relation: 'funding_source',
                          to: nodes.create({ kind: 'account', name: 'tmp' }).id,
                          capability: 'payment',
                        })
                        throw new Error('injected')
                      }),
                    ).toThrow('injected')
                    break
                  }
                  case 'replay': {
                    // confirm-again：active verify 重放 → 不 bump
                    if (confirmed && !retired) {
                      deps.confirm({
                        from: anchors[0]!,
                        relation: 'funding_source',
                        to: anchors[2]!,
                        capability: 'payment',
                      })
                    }
                    break
                  }
                }
              } catch {
                // 非 failed-tx 用例中的意外异常也算操作未生效（revision 不应变化）
              }
              const after = getGraphRevision(drv)
              // monotonic + 精确等于 Reality mutation 计数
              expect(after).toBe(expected)
              expect(after).toBeGreaterThanOrEqual(before)
            }
          } finally {
            drv.close()
            rmSync(d, { recursive: true, force: true })
          }
        }),
        { numRuns: 30 },
      )
    },
  )
})
