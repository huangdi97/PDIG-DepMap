import type { DepNode, NodeKind } from '../domain/types.ts'
import type { SqliteDriver } from '../db/driver.ts'
import { newId, nowIso } from '../utils/ids.ts'
import { optionalString } from './meta-repository.ts'

export interface CreateNodeInput {
  id?: string
  kind: NodeKind
  templateId?: string | null
  name: string
  issuer?: string | null
  last4?: string | null
  owner?: string
  fields?: Record<string, unknown>
  vaultRef?: string | null
  walletRef?: string | null
}

export interface NodeQuery {
  kind?: NodeKind
  archived?: boolean
}

function parseJsonObject(v: unknown): Record<string, unknown> {
  const raw = typeof v === 'string' ? v : '{}'
  try {
    const parsed: unknown = JSON.parse(raw)
    return parsed !== null && typeof parsed === 'object' ? (parsed as Record<string, unknown>) : {}
  } catch {
    return {}
  }
}

function rowToNode(row: Record<string, unknown>): DepNode {
  return {
    id: String(row.id),
    kind: String(row.kind) as NodeKind,
    templateId: optionalString(row.template_id as never),
    name: String(row.name),
    issuer: optionalString(row.issuer as never),
    last4: optionalString(row.last4 as never),
    owner: String(row.owner),
    archived: Number(row.archived) === 1,
    fields: parseJsonObject(row.fields_json),
    vaultRef: optionalString(row.vault_ref as never),
    walletRef: optionalString(row.wallet_ref as never),
    createdAt: String(row.created_at),
    updatedAt: String(row.updated_at),
  }
}

export class NodeRepository {
  private readonly driver: SqliteDriver

  constructor(driver: SqliteDriver) {
    this.driver = driver
  }

  create(input: CreateNodeInput): DepNode {
    const id = input.id ?? newId()
    const now = nowIso()
    this.driver
      .prepare(
        `INSERT INTO nodes (id, kind, template_id, name, issuer, last4, owner, archived, fields_json, vault_ref, wallet_ref, created_at, updated_at)
         VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?, ?)`,
      )
      .run(
        id,
        input.kind,
        input.templateId ?? null,
        input.name,
        input.issuer ?? null,
        input.last4 ?? null,
        input.owner ?? 'self',
        JSON.stringify(input.fields ?? {}),
        input.vaultRef ?? null,
        input.walletRef ?? null,
        now,
        now,
      )
    return this.getById(id) as DepNode
  }

  getById(id: string): DepNode | null {
    const row = this.driver.prepare(`SELECT * FROM nodes WHERE id = ?`).get(id)
    return row ? rowToNode(row) : null
  }

  /** 必须存在，否则抛错（防止静默建错边）。 */
  getExisting(id: string): DepNode {
    const node = this.getById(id)
    if (!node) throw new Error(`node not found: ${id}`)
    return node
  }

  findByName(name: string): DepNode[] {
    const rows = this.driver.prepare(`SELECT * FROM nodes WHERE name = ?`).all(name)
    return rows.map(rowToNode)
  }

  list(query: NodeQuery = {}): DepNode[] {
    const where: string[] = []
    const params: (string | number)[] = []
    if (query.kind) {
      where.push('kind = ?')
      params.push(query.kind)
    }
    if (query.archived !== undefined) {
      where.push('archived = ?')
      params.push(query.archived ? 1 : 0)
    }
    const sql = `SELECT * FROM nodes ${where.length ? 'WHERE ' + where.join(' AND ') : ''} ORDER BY name, id`
    const rows = this.driver.prepare(sql).all(...params)
    return rows.map(rowToNode)
  }

  update(id: string, patch: Partial<CreateNodeInput> & { archived?: boolean }): DepNode {
    const existing = this.getExisting(id)
    const kind = patch.kind ?? existing.kind
    const templateId = patch.templateId !== undefined ? patch.templateId : existing.templateId
    const name = patch.name ?? existing.name
    const issuer = patch.issuer !== undefined ? patch.issuer : existing.issuer
    const last4 = patch.last4 !== undefined ? patch.last4 : existing.last4
    const owner = patch.owner ?? existing.owner
    const archived = patch.archived !== undefined ? patch.archived : existing.archived
    const fields = patch.fields !== undefined ? patch.fields : existing.fields
    const vaultRef = patch.vaultRef !== undefined ? patch.vaultRef : existing.vaultRef
    const walletRef = patch.walletRef !== undefined ? patch.walletRef : existing.walletRef
    this.driver
      .prepare(
        `UPDATE nodes SET kind = ?, template_id = ?, name = ?, issuer = ?, last4 = ?, owner = ?, archived = ?, fields_json = ?, vault_ref = ?, wallet_ref = ?, updated_at = ?
         WHERE id = ?`,
      )
      .run(
        kind,
        templateId,
        name,
        issuer,
        last4,
        owner,
        archived ? 1 : 0,
        JSON.stringify(fields),
        vaultRef,
        walletRef,
        nowIso(),
        id,
      )
    return this.getById(id) as DepNode
  }
}
