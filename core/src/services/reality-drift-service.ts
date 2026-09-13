import type { Capability } from '../domain/types.ts'
import { DependencyRepository } from '../repositories/dependency-repository.ts'
import {
  RealityDriftRepository,
  type RealityDrift,
  type RealityDriftKind,
  type UpsertDriftSignalInput,
} from '../repositories/reality-drift-repository.ts'
import { userConfirmedBasis } from '../domain/source.ts'
import type { SqliteDriver } from '../db/driver.ts'

/**
 * RealityDrift 检测与处理（MVP03 §25–§29）。
 *
 * 检测（detect...）只写 drift 表，绝不触碰 Reality（RD-003）。
 * 处理（resolve...）由用户显式选择，映射为 Reality mutation（revision +1）或 dismiss。
 * absence 通道不存在：检测入口只接受正向 evidence 信号（RD-002 结构性保证）。
 */

export interface DriftEvidenceSignal {
  /** 观测到「X → candidateRelation → target」正向证据的来源对象 X。 */
  fromNodeId: string
  /** 本次信号的观测次数（跨流累计前）。 */
  observations: number
  evidenceRef: string
  proposalKey?: string
}

export interface DetectDriftsInput {
  targetNodeId: string
  capability: Capability
  signals: DriftEvidenceSignal[]
  /** 触发 drift 的最小观测数（默认 2，保守）。 */
  minObservations?: number
}

export interface DetectDriftsResult {
  created: RealityDrift[]
  updated: RealityDrift[]
  ignored: Array<{ fromNodeId: string; reason: 'already_confirmed' | 'below_threshold' }>
}

export class RealityDriftService {
  private readonly driver: SqliteDriver
  private readonly drifts: RealityDriftRepository
  private readonly deps: DependencyRepository

  constructor(driver: SqliteDriver) {
    this.driver = driver
    this.drifts = new RealityDriftRepository(driver)
    this.deps = new DependencyRepository(driver)
  }

  /**
   * 正向证据 → drift 信号：
   * - X 已是 target 的 confirmed 来源 → 不是 drift（already_confirmed）
   * - 观测数 < minObservations → 忽略（below_threshold）
   * - target 已有其他 confirmed 来源 → possible_replacement
   * - target 无 confirmed 来源但曾有 retired 边 → relation_reappeared
   * - 其余 → possible_additional_path
   */
  detectFromEvidence(input: DetectDriftsInput): DetectDriftsResult {
    const min = input.minObservations ?? 2
    const result: DetectDriftsResult = { created: [], updated: [], ignored: [] }

    const confirmedFrom = new Set(
      this.deps.listActiveIncomingTo(input.targetNodeId, input.capability).map((d) => d.from),
    )
    const retiredIncoming = this.deps
      .listAll()
      .filter(
        (d) =>
          d.to === input.targetNodeId && d.capability === input.capability && d.state === 'retired',
      )

    for (const signal of input.signals) {
      if (confirmedFrom.has(signal.fromNodeId)) {
        result.ignored.push({ fromNodeId: signal.fromNodeId, reason: 'already_confirmed' })
        continue
      }
      // 阈值只挡 drift 的「新建」；已 open 的 drift 后续正向证据无论大小都累计
      const hasOpenForSignal = this.drifts
        .listByStatus('open')
        .some(
          (d) =>
            d.targetNodeId === input.targetNodeId &&
            d.capability === input.capability &&
            d.candidateFrom === signal.fromNodeId,
        )
      if (!hasOpenForSignal && signal.observations < min) {
        result.ignored.push({ fromNodeId: signal.fromNodeId, reason: 'below_threshold' })
        continue
      }
      let kind: RealityDriftKind
      if (confirmedFrom.size > 0) {
        kind = 'possible_replacement'
      } else if (retiredIncoming.some((d) => d.from === signal.fromNodeId)) {
        kind = 'relation_reappeared'
      } else {
        kind = 'possible_additional_path'
      }
      const related =
        kind === 'possible_replacement'
          ? this.deps
              .listActiveIncomingTo(input.targetNodeId, input.capability)
              .filter((d) => d.from !== signal.fromNodeId)
              .map((d) => d.id)
          : []
      const signalInput: UpsertDriftSignalInput = {
        kind,
        targetNodeId: input.targetNodeId,
        capability: input.capability,
        candidateFrom: signal.fromNodeId,
        relatedDependencyIds: related,
        evidenceRef: signal.evidenceRef,
        ...(signal.proposalKey !== undefined ? { proposalKey: signal.proposalKey } : {}),
        observations: signal.observations,
      }
      const { drift, created, changed } = this.drifts.upsertSignal(signalInput)
      if (created) result.created.push(drift)
      else if (changed) result.updated.push(drift)
    }
    return result
  }

  // ---------------------------------------------------------------------------
  // 用户处理（UI 四选项；全部显式，无一键自动改图）
  // ---------------------------------------------------------------------------

  listOpen(): RealityDrift[] {
    return this.drifts.listByStatus('open')
  }

  /** 「已经换成新来源」：确认新边 + retire 旧相关边（均为 user_confirmed Reality mutation）。 */
  resolveAsReplacement(
    driftId: string,
    criticality: 'required' | 'unknown' = 'unknown',
  ): {
    drift: RealityDrift
    newDependencyId: string
    retiredDependencyIds: string[]
  } {
    const drift = this.drifts.getExisting(driftId)
    if (drift.status !== 'open') throw new Error(`drift ${driftId} is ${drift.status}`)
    if (!drift.candidateFrom) throw new Error(`drift ${driftId} has no candidate source`)

    const { dependency } = this.deps.confirm({
      from: drift.candidateFrom,
      relation: drift.candidateRelation as never,
      to: drift.targetNodeId,
      capability: drift.capability,
      criticality,
      origin: 'manual',
      verificationBasis: userConfirmedBasis(new Date().toISOString()),
      evidenceRefs: drift.evidenceRefs,
    })
    const retired: string[] = []
    for (const depId of drift.relatedDependencyIds) {
      const dep = this.deps.getById(depId)
      if (dep && dep.state === 'active') {
        this.deps.retire(depId)
        retired.push(depId)
      }
    }
    const confirmed = this.drifts.setStatus(driftId, 'confirmed_change')
    // 同 candidate 的其他 open drift 一并 superseded（防重复打扰）
    for (const other of this.drifts.listByStatus('open')) {
      if (
        other.id !== driftId &&
        other.targetNodeId === drift.targetNodeId &&
        other.capability === drift.capability &&
        other.candidateFrom === drift.candidateFrom
      ) {
        this.drifts.setStatus(other.id, 'superseded')
      }
    }
    return { drift: confirmed, newDependencyId: dependency.id, retiredDependencyIds: retired }
  }

  /** 「两个都在用」：只确认新边，旧边保持 active（RD-006）。 */
  resolveAsAdditionalPath(
    driftId: string,
    criticality: 'required' | 'unknown' = 'unknown',
  ): { drift: RealityDrift; newDependencyId: string } {
    const drift = this.drifts.getExisting(driftId)
    if (drift.status !== 'open') throw new Error(`drift ${driftId} is ${drift.status}`)
    if (!drift.candidateFrom) throw new Error(`drift ${driftId} has no candidate source`)

    const { dependency } = this.deps.confirm({
      from: drift.candidateFrom,
      relation: drift.candidateRelation as never,
      to: drift.targetNodeId,
      capability: drift.capability,
      criticality,
      origin: 'manual',
      verificationBasis: userConfirmedBasis(new Date().toISOString()),
      evidenceRefs: drift.evidenceRefs,
    })
    const confirmed = this.drifts.setStatus(driftId, 'confirmed_change')
    for (const other of this.drifts.listByStatus('open')) {
      if (
        other.id !== driftId &&
        other.targetNodeId === drift.targetNodeId &&
        other.capability === drift.capability &&
        other.candidateFrom === drift.candidateFrom
      ) {
        this.drifts.setStatus(other.id, 'superseded')
      }
    }
    return { drift: confirmed, newDependencyId: dependency.id }
  }

  /** 「没有变化」/「稍后确认」—— 两者都不改 Graph（RD-004）；差异仅是是否还期待新证据。 */
  dismiss(driftId: string): RealityDrift {
    const drift = this.drifts.getExisting(driftId)
    if (drift.status !== 'open') throw new Error(`drift ${driftId} is ${drift.status}`)
    return this.drifts.setStatus(driftId, 'dismissed')
  }
}
