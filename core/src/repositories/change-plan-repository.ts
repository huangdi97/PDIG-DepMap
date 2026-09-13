import type { ChangePlan, ImpactSnapshot, PlanAction } from '../domain/change-plan.ts'
import type { SqliteDriver } from '../db/driver.ts'
import { newId, nowIso } from '../utils/ids.ts'

/**
 * ChangePlan 持久化（Schema v3 `change_plans` 表）。
 * actions / impactSnapshot 以 JSON 内嵌；不建第二套 ChangePlan 存储。
 */

export interface CreateChangePlanInput {
  id?: string
  templateId?: string | null
  scenario: string
  title: string
  targetNodeId?: string | null
  effectiveDate?: string | null
  params?: Record<string, string>
  actions?: PlanAction[]
  /** 创建时的当前 graphRevision（baseline + lastAnalyzed 同值）。 */
  graphRevision: number
}

function rowToPlan(row: Record<string, unknown>): ChangePlan {
  return {
    id: String(row.id),
    templateId: (row.template_id as string | null) ?? null,
    scenario: String(row.scenario),
    title: String(row.title),
    workflowState: String(row.workflow_state) as ChangePlan['workflowState'],
    baselineGraphRevision: Number(row.baseline_graph_revision),
    lastAnalyzedGraphRevision: Number(row.last_analyzed_graph_revision),
    targetNodeId: (row.target_node_id as string | null) ?? null,
    effectiveDate: (row.effective_date as string | null) ?? null,
    params: safeParseObject(row.params_json),
    impactSnapshot:
      typeof row.impact_snapshot_json === 'string'
        ? (JSON.parse(row.impact_snapshot_json) as ImpactSnapshot)
        : null,
    actions: safeParseActions(row.action_items_json),
    createdAt: String(row.created_at),
    updatedAt: String(row.updated_at),
  }
}

function safeParseObject(value: unknown): Record<string, string> {
  try {
    const parsed: unknown = JSON.parse(typeof value === 'string' ? value : '{}')
    if (parsed !== null && typeof parsed === 'object' && !Array.isArray(parsed)) {
      const out: Record<string, string> = {}
      for (const [k, v] of Object.entries(parsed as Record<string, unknown>)) {
        out[k] = String(v)
      }
      return out
    }
  } catch {
    // fallthrough
  }
  return {}
}

function safeParseActions(value: unknown): PlanAction[] {
  try {
    const parsed: unknown = JSON.parse(typeof value === 'string' ? value : '[]')
    return Array.isArray(parsed) ? (parsed as PlanAction[]) : []
  } catch {
    return []
  }
}

export class ChangePlanRepository {
  private readonly driver: SqliteDriver

  constructor(driver: SqliteDriver) {
    this.driver = driver
  }

  create(input: CreateChangePlanInput): ChangePlan {
    const id = input.id ?? newId()
    const now = nowIso()
    this.driver
      .prepare(
        `INSERT INTO change_plans (id, template_id, scenario, title, workflow_state, baseline_graph_revision, last_analyzed_graph_revision, target_node_id, effective_date, params_json, impact_snapshot_json, action_items_json, created_at, updated_at)
         VALUES (?, ?, ?, ?, 'draft', ?, ?, ?, ?, ?, NULL, ?, ?, ?)`,
      )
      .run(
        id,
        input.templateId ?? null,
        input.scenario,
        input.title,
        input.graphRevision,
        input.graphRevision,
        input.targetNodeId ?? null,
        input.effectiveDate ?? null,
        JSON.stringify(input.params ?? {}),
        JSON.stringify(input.actions ?? []),
        now,
        now,
      )
    return this.getById(id) as ChangePlan
  }

  getById(id: string): ChangePlan | null {
    const row = this.driver.prepare(`SELECT * FROM change_plans WHERE id = ?`).get(id)
    return row ? rowToPlan(row) : null
  }

  getExisting(id: string): ChangePlan {
    const plan = this.getById(id)
    if (!plan) throw new Error(`change plan not found: ${id}`)
    return plan
  }

  listAll(): ChangePlan[] {
    return this.driver
      .prepare(`SELECT * FROM change_plans ORDER BY created_at, id`)
      .all()
      .map(rowToPlan)
  }

  listByState(states: ChangePlan['workflowState'][]): ChangePlan[] {
    if (states.length === 0) return []
    const placeholders = states.map(() => '?').join(',')
    return this.driver
      .prepare(
        `SELECT * FROM change_plans WHERE workflow_state IN (${placeholders}) ORDER BY created_at, id`,
      )
      .all(...states)
      .map(rowToPlan)
  }

  /** 更新工作流状态（业务校验在 service 层；completed/cancelled 为终态）。 */
  updateWorkflowState(id: string, state: ChangePlan['workflowState']): ChangePlan {
    this.driver
      .prepare(`UPDATE change_plans SET workflow_state = ?, updated_at = ? WHERE id = ?`)
      .run(state, nowIso(), id)
    return this.getExisting(id)
  }

  /** rebase / 分析回写：快照 + 分析版本号 + 动作集合（保持 done/verification 由 service 合并）。 */
  updateAnalysis(
    id: string,
    snapshot: ImpactSnapshot,
    lastAnalyzedGraphRevision: number,
    actions: PlanAction[],
  ): ChangePlan {
    this.driver
      .prepare(
        `UPDATE change_plans SET impact_snapshot_json = ?, last_analyzed_graph_revision = ?, action_items_json = ?, updated_at = ? WHERE id = ?`,
      )
      .run(
        JSON.stringify(snapshot),
        lastAnalyzedGraphRevision,
        JSON.stringify(actions),
        nowIso(),
        id,
      )
    return this.getExisting(id)
  }

  updateActions(id: string, actions: PlanAction[]): ChangePlan {
    this.driver
      .prepare(`UPDATE change_plans SET action_items_json = ?, updated_at = ? WHERE id = ?`)
      .run(JSON.stringify(actions), nowIso(), id)
    return this.getExisting(id)
  }

  updateSchedule(id: string, effectiveDate: string | null): ChangePlan {
    this.driver
      .prepare(`UPDATE change_plans SET effective_date = ?, updated_at = ? WHERE id = ?`)
      .run(effectiveDate, nowIso(), id)
    return this.getExisting(id)
  }

  countAll(): number {
    const row = this.driver.prepare(`SELECT COUNT(*) AS c FROM change_plans`).get()
    return Number(row?.c ?? 0)
  }
}
