import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import {
  ChangePlanService,
  DependencyRepository,
  getGraphRevision,
  instantiateScenario,
  migrate,
  NodeRepository,
  NodeSqliteDriver,
  type PlanAction,
} from '../../src/index.ts'

/**
 * MVP03 VF-001..007 —— Action Verification Gate（done ≠ verified）。
 */

describe('Action Verification（MVP03 VF）', () => {
  let dir: string
  let driver: NodeSqliteDriver
  let service: ChangePlanService
  let planId: string
  let newCard: string
  let spotify: string

  beforeEach(() => {
    dir = mkdtempSync(join(tmpdir(), 'depmap-vf-'))
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
    migrate(driver)
    const nodes = new NodeRepository(driver)
    const card = nodes.create({ kind: 'payment_instrument', name: '招行 4417' })
    newCard = nodes.create({ kind: 'payment_instrument', name: '建行 8821' }).id
    spotify = nodes.create({ kind: 'service', name: 'Spotify' }).id
    service = new ChangePlanService(driver)
    const actions: PlanAction[] = [
      {
        id: 'act-move',
        title: '把 Spotify 换到新卡',
        detail: '在 Spotify 中把扣款卡换成新卡。',
        phase: 'change',
        done: false,
        doneAt: null,
        verification: {
          method: 'future_observation',
          status: 'pending',
          verifiedAt: null,
          evidenceRefs: [],
          expectedFromNodeId: newCard,
          expectedToNodeId: spotify,
        },
      },
      {
        id: 'act-destroy',
        title: '销毁旧卡',
        detail: '',
        phase: 'change',
        done: false,
        doneAt: null,
        verification: null,
      },
    ]
    const plan = service.plans.create({
      templateId: 'replace_payment_card',
      scenario: 'replace_payment_card',
      title: '更换招行 4417',
      targetNodeId: card.id,
      actions,
      graphRevision: getGraphRevision(driver),
    })
    planId = plan.id
  })

  afterEach(() => {
    driver.close()
    rmSync(dir, { recursive: true, force: true })
  })

  it('VF-001: Action done ≠ verified', () => {
    service.completeAction(planId, 'act-move')
    const action = service.plans.getExisting(planId).actions.find((a) => a.id === 'act-move')!
    expect(action.done).toBe(true)
    expect(action.verification?.status).toBe('pending') // 未验证
    expect(action.verification?.verifiedAt).toBeNull()
  })

  it('VF-002: manual confirm → verified', () => {
    service.verifyActionManually(planId, 'act-destroy')
    const action = service.plans.getExisting(planId).actions.find((a) => a.id === 'act-destroy')!
    expect(action.verification?.status).toBe('verified')
    expect(action.verification?.method).toBe('manual_confirmation')
    expect(action.verification?.verifiedAt).not.toBeNull()
  })

  it('VF-003: future evidence 命中 → evidence_suggested（不自动 verified）', () => {
    const { matchedActionIds } = service.applyEvidenceSignal(planId, {
      fromNodeId: newCard,
      toNodeId: spotify,
      evidenceRef: 'inst-new#1',
    })
    expect(matchedActionIds).toEqual(['act-move'])
    const action = service.plans.getExisting(planId).actions.find((a) => a.id === 'act-move')!
    expect(action.verification?.status).toBe('evidence_suggested')
    expect(action.verification?.verifiedAt).toBeNull()
  })

  it('VF-004: evidence suggestion 不修改 Reality（revision / deps 不变）', () => {
    const before = getGraphRevision(driver)
    service.applyEvidenceSignal(planId, {
      fromNodeId: newCard,
      toNodeId: spotify,
      evidenceRef: 'i#1',
    })
    expect(getGraphRevision(driver)).toBe(before)
    expect(new DependencyRepository(driver).countAll()).toBe(0)
  })

  it('VF-005: duplicate evidence 不重复 verification 记录', () => {
    service.applyEvidenceSignal(planId, {
      fromNodeId: newCard,
      toNodeId: spotify,
      evidenceRef: 'i#1',
    })
    service.applyEvidenceSignal(planId, {
      fromNodeId: newCard,
      toNodeId: spotify,
      evidenceRef: 'i#1',
    })
    const action = service.plans.getExisting(planId).actions.find((a) => a.id === 'act-move')!
    expect(action.verification?.evidenceRefs).toEqual(['i#1'])
  })

  it('VF-006: wrong source 不产生 suggestion', () => {
    const { matchedActionIds } = service.applyEvidenceSignal(planId, {
      fromNodeId: 'wrong-card',
      toNodeId: spotify,
      evidenceRef: 'i#9',
    })
    expect(matchedActionIds).toEqual([])
    const action = service.plans.getExisting(planId).actions.find((a) => a.id === 'act-move')!
    expect(action.verification?.status).toBe('pending')
    expect(action.verification?.evidenceRefs).toEqual([])
  })

  it('VF-007: verified 状态 restart 后稳定', () => {
    service.verifyActionManually(planId, 'act-move')
    driver.close()
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
    migrate(driver)
    const reloaded = new ChangePlanService(driver).plans.getExisting(planId)
    expect(reloaded.actions.find((a) => a.id === 'act-move')?.verification?.status).toBe('verified')
  })

  it('FREEZE: verified/failed 状态不可被 evidence suggestion 覆盖（watch 匹配也不行）', () => {
    // 构造带 watch 字段且已 verified 的动作
    const service2 = new ChangePlanService(driver)
    const actions: PlanAction[] = [
      { id: 'done-v', title: '已验证动作', detail: '', phase: 'verify', done: true, doneAt: null,
        verification: { method: 'future_observation', status: 'verified', verifiedAt: '2026-01-01T00:00:00Z',
          evidenceRefs: [], expectedFromNodeId: newCard, expectedToNodeId: spotify } },
      { id: 'fail-v', title: '失败动作', detail: '', phase: 'verify', done: false, doneAt: null,
        verification: { method: 'future_observation', status: 'failed', verifiedAt: null,
          evidenceRefs: [], expectedFromNodeId: newCard, expectedToNodeId: spotify } },
    ]
    const p2 = service2.plans.create({
      scenario: 'replace_payment_card', title: 't2', targetNodeId: 'x', actions, graphRevision: getGraphRevision(driver),
    })
    const { matchedActionIds } = service2.applyEvidenceSignal(p2.id, {
      fromNodeId: newCard, toNodeId: spotify, evidenceRef: 'e#9',
    })
    expect(matchedActionIds).toEqual([])
    const stored = service2.plans.getExisting(p2.id).actions
    expect(stored.find((a) => a.id === 'done-v')?.verification?.status).toBe('verified')
    expect(stored.find((a) => a.id === 'fail-v')?.verification?.status).toBe('failed')
  })

  it('evidence_suggested 后用户确认 → verified（两段式，禁止自动 verified）', () => {
    service.applyEvidenceSignal(planId, {
      fromNodeId: newCard,
      toNodeId: spotify,
      evidenceRef: 'i#1',
    })
    // 模拟 UI：用户看到「新数据表明这项变更可能已经生效，请确认」后确认
    const plan = service.plans.getExisting(planId)
    void plan
    service.verifyActionManually(planId, 'act-move')
    const action = service.plans.getExisting(planId).actions.find((a) => a.id === 'act-move')!
    expect(action.verification?.status).toBe('verified')
    // instantiateScenario 导入保持被引用（模板与计划同一链路）
    expect(instantiateScenario).toBeTypeOf('function')
  })
})
