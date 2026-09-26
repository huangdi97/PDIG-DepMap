import { describe, expect, it } from 'vitest'
import type { Capability, Dependency } from '../../src/domain/types.ts'
import { computePathIndependence, buildEdgeToDomainIndex } from '../../src/domain/failure-domain.ts'
import { detectRecoveryCycles } from '../../src/impact/recovery-cycle.ts'
import { generateInfrastructureFindings } from '../../src/domain/infrastructure-findings.ts'
import { validateActionDag } from '../../src/domain/action-dag.ts'
import { evaluateMakeBeforeBreak } from '../../src/domain/make-before-break.ts'
import { classifyTemporalPhase, validateTemporalOrder } from '../../src/domain/temporal-change.ts'
import {
  inferProviderPolicyState,
  interpretProviderCapability,
} from '../../src/services/provider-policy.ts'

const T0 = '2026-09-13T00:00:00.000Z'

function dep(
  id: string,
  from: string,
  to: string,
  capability: Capability = 'recovery',
  state: 'active' | 'retired' = 'active',
): Dependency {
  return {
    id,
    from,
    relation: 'recovers',
    to,
    capability,
    criticality: 'unknown',
    groupId: null,
    state,
    origin: 'manual',
    confirmedAt: T0,
    lastVerifiedAt: T0,
    retiredAt: state === 'retired' ? T0 : null,
    evidenceRefs: [],
    verificationBasis: { type: 'user_confirmed', verifiedAt: T0 },
    createdAt: T0,
    updatedAt: T0,
  }
}

function action(id: string, prerequisites: string[] = [], verified = false) {
  return {
    id,
    title: id,
    detail: '',
    phase: 'change' as const,
    done: true,
    doneAt: T0,
    verification: verified
      ? {
          method: 'manual_confirmation' as const,
          status: 'verified' as const,
          verifiedAt: T0,
          evidenceRefs: [],
        }
      : null,
    resolvesImpactKeys: [],
    prerequisiteActionIds: prerequisites,
  }
}

describe('FailureDomain / PathIndependence (v0.3.0)', () => {
  it('两条路径共享同一台手机 → pathCount=2, independentPathCount=1', () => {
    const d1 = dep('d1', 'phone-a', 'wechat')
    const d2 = dep('d2', 'phone-a', 'wechat')
    const confirmed = new Map([['DEVICE|phone-a', 'fd-device-1']])
    const edgeToDomain = buildEdgeToDomainIndex(
      [d1, d2],
      new Map([['DEVICE|phone-a', ['d1', 'd2']]]),
    )
    const r = computePathIndependence({
      targetNodeId: 'wechat',
      capability: 'recovery',
      dependencies: [d1, d2],
      confirmedDomains: confirmed,
      suspectedDomains: new Map(),
      edgeToDomainKeys: edgeToDomain,
    })
    expect(r.pathCount).toBe(2)
    expect(r.independentPathCount).toBe(1)
    expect(r.sharedFailureDomains).toEqual(['fd-device-1'])
  })

  it('两条路径不同设备 → pathCount=2, independentPathCount=2', () => {
    const d1 = dep('d1', 'phone-a', 'wechat')
    const d2 = dep('d2', 'phone-b', 'wechat')
    const edgeToDomain = buildEdgeToDomainIndex(
      [d1, d2],
      new Map([
        ['DEVICE|phone-a', ['d1']],
        ['DEVICE|phone-b', ['d2']],
      ]),
    )
    const r = computePathIndependence({
      targetNodeId: 'wechat',
      capability: 'recovery',
      dependencies: [d1, d2],
      confirmedDomains: new Map(),
      suspectedDomains: new Map(),
      edgeToDomainKeys: edgeToDomain,
    })
    expect(r.pathCount).toBe(2)
    expect(r.independentPathCount).toBe(2)
    expect(r.sharedFailureDomains).toEqual([])
  })

  it('共享 provider 且未确认 → needs_review（unresolvedAssumptions 非空，不自动 confirmed）', () => {
    const d1 = dep('d1', 'phone-a', 'wechat')
    const d2 = dep('d2', 'phone-b', 'wechat')
    const edgeToDomain = buildEdgeToDomainIndex(
      [d1, d2],
      new Map([['PROVIDER|ChinaMobile', ['d1', 'd2']]]),
    )
    const r = computePathIndependence({
      targetNodeId: 'wechat',
      capability: 'recovery',
      dependencies: [d1, d2],
      confirmedDomains: new Map(),
      suspectedDomains: new Map([['PROVIDER|ChinaMobile', 'fd-provider-1']]),
      edgeToDomainKeys: edgeToDomain,
    })
    expect(r.unresolvedAssumptions).toEqual(['fd-provider-1'])
    expect(r.sharedFailureDomains).toEqual([])
  })

  it('retired 边不参与路径统计', () => {
    const d1 = dep('d1', 'phone-a', 'wechat', 'recovery', 'retired')
    const r = computePathIndependence({
      targetNodeId: 'wechat',
      capability: 'recovery',
      dependencies: [d1],
      confirmedDomains: new Map(),
      suspectedDomains: new Map(),
      edgeToDomainKeys: new Map(),
    })
    expect(r.pathCount).toBe(0)
    expect(r.independentPathCount).toBe(0)
  })
})

describe('RecoveryCycle (v0.3.0)', () => {
  it('simple cycle → confirmed_cycle', () => {
    // account-a recovers via phone-a; phone-a is managed by account-a (self reference)
    const d1 = dep('d1', 'account-a', 'phone-a')
    const d2 = dep('d2', 'phone-a', 'account-a')
    const r = detectRecoveryCycles({
      capability: 'recovery',
      dependencies: [d1, d2],
      hintedEdges: [],
    })
    expect(r.status).toBe('confirmed_cycle')
    expect(r.confirmedCycles.length).toBe(1)
  })

  it('multi-hop cycle → confirmed_cycle', () => {
    const d1 = dep('d1', 'account-a', 'phone-a')
    const d2 = dep('d2', 'phone-a', 'email-a')
    const d3 = dep('d3', 'email-a', 'account-a')
    const r = detectRecoveryCycles({
      capability: 'recovery',
      dependencies: [d1, d2, d3],
      hintedEdges: [],
    })
    expect(r.status).toBe('confirmed_cycle')
    expect(r.confirmedCycles[0]?.length).toBe(3)
  })

  it('no cycle → no_cycle', () => {
    const d1 = dep('d1', 'phone-a', 'account-a')
    const r = detectRecoveryCycles({ capability: 'recovery', dependencies: [d1], hintedEdges: [] })
    expect(r.status).toBe('no_cycle')
  })

  it('unconfirmed edge (proposal-only) → potential_cycle，不 confirmed', () => {
    const d1 = dep('d1', 'account-a', 'phone-a')
    const hinted = [{ from: 'phone-a', to: 'account-a' }]
    const r = detectRecoveryCycles({
      capability: 'recovery',
      dependencies: [d1],
      hintedEdges: hinted,
    })
    expect(r.status).toBe('potential_cycle')
    expect(r.confirmedCycles).toEqual([])
    expect(r.potentialCycles.length).toBeGreaterThan(0)
  })

  it('retired edge 不参与检测', () => {
    const d1 = dep('d1', 'account-a', 'phone-a', 'recovery', 'retired')
    const d2 = dep('d2', 'phone-a', 'account-a', 'recovery', 'retired')
    const r = detectRecoveryCycles({
      capability: 'recovery',
      dependencies: [d1, d2],
      hintedEdges: [],
    })
    expect(r.status).toBe('no_cycle')
  })

  it('mixed capability 不产生 confirmed cycle', () => {
    const d1 = dep('d1', 'account-a', 'phone-a', 'recovery')
    const d2 = dep('d2', 'phone-a', 'account-a', 'access')
    const r = detectRecoveryCycles({
      capability: 'recovery',
      dependencies: [d1, d2],
      hintedEdges: [],
    })
    expect(r.status).toBe('no_cycle')
  })
})

describe('InfrastructureFindings (v0.3.0)', () => {
  it('生成 7 类 findings，且无伪精确总分', () => {
    const findings = generateInfrastructureFindings({
      now: T0,
      criticalPaths: [{ nodeId: 'wechat', capability: 'payment' }],
      singleSourceKeys: [{ nodeId: 'wechat', capability: 'payment', sourceEdgeId: 'd1' }],
      sharedFailureDomains: [
        { what: '两个恢复方式都依赖同一台手机', evidenceRefs: ['ev1'], capability: 'recovery' },
      ],
      recoveryCycles: [{ what: '发现一个恢复循环', evidenceRefs: ['ev2'], capability: 'recovery' }],
      unconfirmedFallbacks: [
        { what: '备用路径尚未确认', evidenceRefs: ['ev3'], capability: 'recovery' },
      ],
      staleRecoveryItems: [
        { what: '恢复信息需要重新确认', evidenceRefs: ['ev4'], capability: 'recovery' },
      ],
      unknownCriticalPaths: [
        { what: '关键路径的影响还不清楚', evidenceRefs: ['ev5'], capability: 'recovery' },
      ],
      pendingVerifications: [
        { what: '有一项变更仍未验证', evidenceRefs: ['ev6'], capability: 'recovery' },
      ],
    })
    const types = new Set(findings.map((f) => f.type))
    expect(types.size).toBe(7)
    for (const f of findings) {
      expect(f.what.length).toBeGreaterThan(0)
      expect(f.why.length).toBeGreaterThan(0)
      expect(f.confirmedBasis.length).toBeGreaterThan(0)
      expect(f.unknowns.length).toBeGreaterThan(0)
      expect(f.recommendedNextAction.length).toBeGreaterThan(0)
    }
  })
})

describe('ActionDag (v0.3.0)', () => {
  it('linear chain → stable topo order', () => {
    const r = validateActionDag({
      actions: [action('c', ['b']), action('b', ['a']), action('a')],
    })
    expect(r.ok).toBe(true)
    expect(r.topologicalOrder).toEqual(['a', 'b', 'c'])
  })

  it('parallel actions → stable topo order', () => {
    const r = validateActionDag({ actions: [action('b'), action('a'), action('c', ['a', 'b'])] })
    expect(r.ok).toBe(true)
    expect(r.topologicalOrder).toEqual(['a', 'b', 'c'])
  })

  it('cycle → reject', () => {
    const r = validateActionDag({ actions: [action('a', ['b']), action('b', ['a'])] })
    expect(r.ok).toBe(false)
    expect(r.violation).toBe('cycle')
  })

  it('missing prerequisite → reject', () => {
    const r = validateActionDag({ actions: [action('a', ['ghost'])] })
    expect(r.ok).toBe(false)
    expect(r.violation).toBe('missing_prerequisite')
  })
})

describe('MakeBeforeBreak (v0.3.0)', () => {
  it('new path not verified → retire blocked', () => {
    const r = evaluateMakeBeforeBreak({
      newPathActions: [action('rpn-change-1', [], false)],
      verificationActions: [action('rpn-verify-1', [], false)],
      retireActionId: 'rpn-change-2',
      retireAlreadyDone: false,
    })
    expect(r.status).toBe('blocked')
  })

  it('new path verified → retire allowed', () => {
    const r = evaluateMakeBeforeBreak({
      newPathActions: [action('rpn-change-1', [], false)],
      verificationActions: [action('rpn-verify-1', [], true)],
      retireActionId: 'rpn-change-2',
      retireAlreadyDone: false,
    })
    expect(r.status).toBe('allowed')
  })
})

describe('TemporalChange (v0.3.0)', () => {
  it('invalid order rejected', () => {
    expect(
      validateTemporalOrder({
        effectiveAt: '2026-10-02T00:00:00.000Z',
        verificationNotBefore: '2026-10-01T00:00:00.000Z',
        verificationDueAt: null,
        retireOldPathAfter: null,
      }),
    ).toBe(false)
  })

  it('valid window + verified → retireAllowedAt', () => {
    const r = classifyTemporalPhase(
      {
        effectiveAt: '2026-10-01T00:00:00.000Z',
        verificationNotBefore: '2026-10-01T00:00:00.000Z',
        verificationDueAt: '2026-10-15T00:00:00.000Z',
        retireOldPathAfter: '2026-10-20T00:00:00.000Z',
      },
      '2026-10-21T00:00:00.000Z',
      true,
    )
    expect(r.phase).toBe('after')
    expect(r.retireAllowedAt).toBe('2026-10-20T00:00:00.000Z')
  })

  it('new path unverified → retire blocked 即使时间已到', () => {
    const r = classifyTemporalPhase(
      {
        effectiveAt: '2026-10-01T00:00:00.000Z',
        verificationNotBefore: null,
        verificationDueAt: null,
        retireOldPathAfter: '2026-10-20T00:00:00.000Z',
      },
      '2026-10-21T00:00:00.000Z',
      false,
    )
    expect(r.retireAllowedAt).toBeNull()
    expect(r.retireBlockedReason).toContain('新路径尚未全部验证')
  })
})

describe('ProviderPolicy (v0.3.0)', () => {
  it('无 sourceUrl → needs_review', () => {
    expect(
      inferProviderPolicyState({
        provider: 'ChinaMobile',
        policyType: 'recovery',
        sourceUrl: null,
        retrievedAt: T0,
        lastVerifiedAt: T0,
        policyRevision: 1,
      }),
    ).toBe('needs_review')
  })

  it('supports != configured：服务商支持但未配置 → suggestion', () => {
    const policy = {
      provider: 'ChinaMobile',
      policyType: 'recovery',
      sourceUrl: 'https://example.com/policy',
      retrievedAt: T0,
      lastVerifiedAt: T0,
      effectiveFrom: null,
      effectiveTo: null,
      jurisdiction: null,
      accountTypeScope: null,
      parameters: {},
      policyRevision: 1,
      state: 'effective' as const,
    }
    const r = interpretProviderCapability(policy, false)
    expect(r.supports).toBe(true)
    expect(r.configured).toBe(false)
    expect(r.influences).toContain('suggestion')
  })

  it('needs_review policy 不自动影响建议', () => {
    const r = interpretProviderCapability(null, true)
    expect(r.supports).toBe(false)
    expect(r.influences).toEqual([])
  })
})
