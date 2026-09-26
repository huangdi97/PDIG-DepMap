import type { Capability } from './types.ts'

/**
 * InfrastructureFindings — Canonical vNext (v0.3.0)
 *
 * 7 类 Finding 全部是「已确认现实」的派生投影（bumpsGraphRevision=false）。
 * 禁止伪精确总分（Infrastructure Health = 86 之类）。
 * 每个 Finding 必须包含：what / why / confirmed basis / unknowns /
 * affected capability / recommended next action / evidence refs。
 */

export type InfrastructureFindingType =
  | 'SINGLE_POINT_OF_FAILURE'
  | 'SHARED_FAILURE_DOMAIN'
  | 'RECOVERY_CYCLE'
  | 'UNCONFIRMED_FALLBACK'
  | 'STALE_RECOVERY_INFORMATION'
  | 'UNKNOWN_CRITICAL_PATH'
  | 'PENDING_VERIFICATION'

export interface InfrastructureFinding {
  id: string
  type: InfrastructureFindingType
  what: string
  why: string
  confirmedBasis: string
  unknowns: string
  affectedCapability: Capability
  recommendedNextAction: string
  evidenceRefs: string[]
  createdAt: string
}

export interface FindingsInput {
  now: string
  /** (nodeId, capability) 已知关键路径。 */
  criticalPaths: Array<{ nodeId: string; capability: Capability }>
  /** 只有一个来源（无已确认备用）的 key 集合。 */
  singleSourceKeys: Array<{ nodeId: string; capability: Capability; sourceEdgeId: string }>
  /** 共享故障域（confirmed）发现。 */
  sharedFailureDomains: Array<{ what: string; evidenceRefs: string[]; capability: Capability }>
  /** confirmed recovery cycle 发现。 */
  recoveryCycles: Array<{ what: string; evidenceRefs: string[]; capability: Capability }>
  /** 未确认备用（proposal-only / needs_review fallback）。 */
  unconfirmedFallbacks: Array<{ what: string; evidenceRefs: string[]; capability: Capability }>
  /** 过期恢复信息（stale）。 */
  staleRecoveryItems: Array<{ what: string; evidenceRefs: string[]; capability: Capability }>
  /** 关键性未知的路径。 */
  unknownCriticalPaths: Array<{ what: string; evidenceRefs: string[]; capability: Capability }>
  /** 未验证的变更。 */
  pendingVerifications: Array<{ what: string; evidenceRefs: string[]; capability: Capability }>
}

function makeFinding(
  id: string,
  type: InfrastructureFindingType,
  what: string,
  why: string,
  confirmedBasis: string,
  unknowns: string,
  affectedCapability: Capability,
  recommendedNextAction: string,
  evidenceRefs: string[],
  createdAt: string,
): InfrastructureFinding {
  return {
    id,
    type,
    what,
    why,
    confirmedBasis,
    unknowns,
    affectedCapability,
    recommendedNextAction,
    evidenceRefs,
    createdAt,
  }
}

export function generateInfrastructureFindings(input: FindingsInput): InfrastructureFinding[] {
  const findings: InfrastructureFinding[] = []
  let n = 0

  for (const k of input.singleSourceKeys) {
    findings.push(
      makeFinding(
        `fd-${++n}`,
        'SINGLE_POINT_OF_FAILURE',
        `「${k.nodeId}」只有 ${k.capability} 来源，没有已确认的备用路径`,
        '该对象的所有已确认来源集中在一条路径上',
        '已确认的依赖关系',
        '是否真的没有备用路径还不确定（absence ≠ non-existence）',
        k.capability,
        '确认是否存在其他恢复/认证方式，并验证可用',
        [k.sourceEdgeId],
        input.now,
      ),
    )
  }

  for (const s of input.sharedFailureDomains) {
    findings.push(
      makeFinding(
        `fd-${++n}`,
        'SHARED_FAILURE_DOMAIN',
        s.what,
        '两个路径共享同一个故障点',
        '已确认的故障域',
        '两个路径是否真的同时失效还不确定',
        s.capability,
        '为其中一个路径寻找独立来源',
        s.evidenceRefs,
        input.now,
      ),
    )
  }

  for (const c of input.recoveryCycles) {
    findings.push(
      makeFinding(
        `fd-${++n}`,
        'RECOVERY_CYCLE',
        c.what,
        '恢复方式依赖它正在恢复的对象，不能视为独立备用',
        '已确认的恢复关系',
        '是否存在不经过该对象的独立恢复路径',
        c.capability,
        '寻找真正独立的恢复路径',
        c.evidenceRefs,
        input.now,
      ),
    )
  }

  for (const u of input.unconfirmedFallbacks) {
    findings.push(
      makeFinding(
        `fd-${++n}`,
        'UNCONFIRMED_FALLBACK',
        u.what,
        '备用路径存在但尚未确认',
        '记录中的备用来源',
        '该备用是否仍然有效',
        u.capability,
        '确认该备用路径是否可用',
        u.evidenceRefs,
        input.now,
      ),
    )
  }

  for (const s of input.staleRecoveryItems) {
    findings.push(
      makeFinding(
        `fd-${++n}`,
        'STALE_RECOVERY_INFORMATION',
        s.what,
        '恢复/认证信息已超过重新确认周期',
        '记录的最后确认时间',
        '该信息当前是否仍然准确',
        s.capability,
        '重新确认恢复信息',
        s.evidenceRefs,
        input.now,
      ),
    )
  }

  for (const u of input.unknownCriticalPaths) {
    findings.push(
      makeFinding(
        `fd-${++n}`,
        'UNKNOWN_CRITICAL_PATH',
        u.what,
        '关键路径的影响还不清楚',
        '已确认的关键依赖',
        '该路径失效时的实际影响',
        u.capability,
        '确认该路径是否为必须处理',
        u.evidenceRefs,
        input.now,
      ),
    )
  }

  for (const p of input.pendingVerifications) {
    findings.push(
      makeFinding(
        `fd-${++n}`,
        'PENDING_VERIFICATION',
        p.what,
        '有一项变更仍未验证',
        '变更记录',
        '变更是否真的生效',
        p.capability,
        '完成验证后再停用旧路径',
        p.evidenceRefs,
        input.now,
      ),
    )
  }

  return findings.sort((a, b) => (a.id < b.id ? -1 : 1))
}
