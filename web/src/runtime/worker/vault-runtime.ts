/**
 * Vault Runtime —— Worker 内的加密持久化 + 生命周期状态机。
 *
 * 模型（§12–§17）：
 *   Encrypted snapshot (IndexedDB, DEPMAP_CONTAINER_V1) --unlock--> in-memory SQLite --> Core
 *
 * - 只在 unlock / create 时做一次 Argon2id（64 MiB / 3 / 1）。
 * - 每次 mutation：Core 事务 → exportGraph → AES-256-GCM（fresh nonce，复用 session key）
 *   → IndexedDB 原子写 → 才向 UI 回 ack（persist-before-ack）。
 * - 持久化失败不谎报成功：status.persisted=false，UI 必须显示“变更尚未安全保存”。
 * - Demo 模式永不写持久化，永不进入生产 vault。
 */
import sqlite3InitModule from '@sqlite.org/sqlite-wasm'
import wasmUrl from '@sqlite.org/sqlite-wasm/sqlite3.wasm?url'
import { BrowserSqliteDriver, type SqliteWasmModule } from './browser-sqlite-driver.ts'
import { CoreAuthority } from './core-authority.ts'
import {
  DEPMAP_FORMAT_VERSION,
  DepmapWebError,
  type KdfParams,
  createDepmapContainer,
  deriveFileEncryptionKey,
  openDepmapContainer,
  sealWithKey,
} from '../../crypto/depmap-web.ts'
import { DEMO_REALITY } from '../../demo/demo-reality.ts'
import { type VaultRecord, deleteVault, readVault, writeVault } from '../../storage/indexed-db.ts'
import type {
  SearchProjection,
  StorageFacts,
  VaultLifecycle,
  VaultStatus,
  WebCommand,
  WebErrorCode,
  WebQuery,
} from '../protocol.ts'

let modulePromise: Promise<SqliteWasmModule> | null = null

async function loadSqliteModule(): Promise<SqliteWasmModule> {
  if (modulePromise === null) {
    modulePromise = (async () => {
      const init = sqlite3InitModule as unknown as (options: {
        locateFile: (file: string) => string
        print?: (message: string) => void
        printErr?: (message: string) => void
      }) => Promise<unknown>
      const module = await init({
        locateFile: () => wasmUrl,
        print: () => undefined,
        printErr: () => undefined,
      })
      return module as SqliteWasmModule
    })()
  }
  return modulePromise
}

export interface VaultRuntimeOptions {
  now: () => string
}

interface Session {
  key: Uint8Array
  salt: Uint8Array
  kdf: KdfParams
}

export class VaultRuntime {
  private readonly now: () => string
  private module: SqliteWasmModule | null = null
  private driver: BrowserSqliteDriver | null = null
  private authority: CoreAuthority | null = null
  private session: Session | null = null
  private lifecycle: VaultLifecycle = 'locked'
  private errorCode: WebErrorCode | null = null
  private hasVault = false
  private lastSavedAt: string | null = null
  private lastSaveOk = true
  private demoMode = false
  private autoLockMinutes = 15
  private storage: StorageFacts = {
    persisted: false,
    persistRequested: false,
    usageBytes: null,
    quotaBytes: null,
  }

  constructor(options: VaultRuntimeOptions) {
    this.now = options.now
  }

  async boot(): Promise<VaultStatus> {
    this.module = await loadSqliteModule()
    await this.refreshStorageFacts()
    const record = await readVault()
    this.hasVault = record !== null
    this.lifecycle = record === null ? 'no_vault' : 'locked'
    return this.status()
  }

  status(): VaultStatus {
    return {
      lifecycle: this.lifecycle,
      hasVault: this.hasVault,
      graphRevision: this.authority ? this.authority.graphRevision() : 0,
      schemaVersion: this.authority ? this.authority.schemaVersion() : 0,
      autoLockMinutes: this.autoLockMinutes,
      lastSavedAt: this.lastSavedAt,
      persisted: this.lastSaveOk && !this.demoMode,
      storage: this.storage,
      demoMode: this.demoMode,
      errorCode: this.errorCode,
    }
  }

  get isUnlocked(): boolean {
    return this.lifecycle === 'unlocked'
  }

  get isDemo(): boolean {
    return this.demoMode
  }

  setAutoLockMinutes(minutes: number): VaultStatus {
    this.autoLockMinutes = minutes
    return this.status()
  }

  async refreshStorageFacts(): Promise<void> {
    const storage = globalThis.navigator?.storage
    if (!storage) return
    let persisted = false
    try {
      persisted = await storage.persisted()
    } catch {
      persisted = false
    }
    let usageBytes: number | null = null
    let quotaBytes: number | null = null
    try {
      const estimate = await storage.estimate()
      usageBytes = estimate.usage ?? null
      quotaBytes = estimate.quota ?? null
    } catch {
      usageBytes = null
      quotaBytes = null
    }
    if (!persisted) {
      try {
        persisted = await storage.persist()
      } catch {
        persisted = false
      }
    }
    this.storage = {
      persisted,
      persistRequested: true,
      usageBytes,
      quotaBytes,
    }
  }

  // -------------------------------------------------------------------------
  // 生命周期
  // -------------------------------------------------------------------------

  async createVault(password: string): Promise<VaultStatus> {
    if (password.length < 8) {
      throw new DepmapWebError('invalid_structure', '本地密码至少 8 个字符')
    }
    const module = await this.requireModule()
    this.closeAuthority()
    const driver = new BrowserSqliteDriver({ module })
    const salt = crypto.getRandomValues(new Uint8Array(16))
    const kdf: KdfParams = { memoryKiB: 65536, iterations: 3, parallelism: 1 }
    const key = await deriveFileEncryptionKey(password, salt, kdf)
    this.session = { key, salt, kdf }
    this.authority = CoreAuthority.initialize(
      driver,
      { persist: (payload, revision) => this.persist(payload, revision) },
      this.now,
    )
    this.driver = driver
    this.demoMode = false
    this.lastSaveOk = true
    await this.persistNow()
    this.hasVault = true
    this.lifecycle = 'unlocked'
    this.errorCode = null
    return this.status()
  }

  async unlock(password: string): Promise<VaultStatus> {
    const record = await readVault()
    if (!record) {
      this.hasVault = false
      this.lifecycle = 'no_vault'
      return this.status()
    }
    this.lifecycle = 'unlocking'
    try {
      const opened = await openDepmapContainer(record.containerJson, password)
      const module = await this.requireModule()
      this.closeAuthority()
      const driver = new BrowserSqliteDriver({ module })
      const authority = CoreAuthority.fromPayload(
        driver,
        new TextDecoder().decode(opened.plaintext),
        { persist: (payload, revision) => this.persist(payload, revision) },
        this.now,
      )
      this.driver = driver
      this.authority = authority
      this.session = { key: opened.key, salt: opened.salt, kdf: opened.kdf }
      this.demoMode = record.demo
      this.hasVault = true
      this.lastSavedAt = record.updatedAt
      this.lastSaveOk = true
      this.lifecycle = 'unlocked'
      this.errorCode = null
      return this.status()
    } catch (error) {
      this.authority = null
      this.driver = null
      this.session = null
      this.errorCode = mapDepmapError(error)
      if (this.errorCode === 'UNSUPPORTED_FORMAT') {
        this.lifecycle = 'unsupported_vault'
      } else if (this.errorCode === 'AUTH_FAILED') {
        this.lifecycle = 'auth_failed'
      } else if (this.errorCode === 'KDF_BOUNDS') {
        this.lifecycle = 'corrupt_vault'
      } else {
        this.lifecycle = 'corrupt_vault'
      }
      return this.status()
    }
  }

  async lock(): Promise<VaultStatus> {
    this.lifecycle = 'locking'
    this.closeAuthority()
    this.session = null
    this.demoMode = false
    this.lifecycle = this.hasVault ? 'locked' : 'no_vault'
    this.errorCode = null
    return this.status()
  }

  private closeAuthority(): void {
    this.authority = null
    if (this.driver) {
      this.driver.close()
      this.driver = null
    }
  }

  private requireModule(): Promise<SqliteWasmModule> {
    if (this.module !== null) return Promise.resolve(this.module)
    return loadSqliteModule().then((module) => {
      this.module = module
      return module
    })
  }

  private requireAuthority(): CoreAuthority {
    if (!this.authority || this.lifecycle !== 'unlocked') {
      throw new DepmapWebError('auth_failed', 'vault is locked')
    }
    return this.authority
  }

  // -------------------------------------------------------------------------
  // 持久化
  // -------------------------------------------------------------------------

  private async persist(payloadJson: string, graphRevision: number): Promise<void> {
    if (this.demoMode) {
      // Demo：永不写入持久化，永不进入生产 vault。
      this.lastSaveOk = false
      this.lastSavedAt = null
      return
    }
    const session = this.session
    if (!session) throw new DepmapWebError('auth_failed', 'vault session key is not available')
    const sealed = await sealWithKey(session.key, new TextEncoder().encode(payloadJson), {
      salt: session.salt,
      kdf: session.kdf,
    })
    const record: VaultRecord = {
      id: 'active',
      containerJson: sealed.json,
      updatedAt: this.now(),
      graphRevision,
      schemaVersion: this.authority ? this.authority.schemaVersion() : 0,
      demo: false,
    }
    try {
      await writeVault(record)
      this.lastSavedAt = record.updatedAt
      this.lastSaveOk = true
    } catch (error) {
      this.lastSaveOk = false
      throw error
    }
  }

  async persistNow(): Promise<void> {
    if (!this.authority) return
    await this.authority.persistNow()
  }

  // -------------------------------------------------------------------------
  // Commands / Queries
  // -------------------------------------------------------------------------

  async command(command: WebCommand): Promise<unknown> {
    if (command.type === 'seed_demo') return this.seedDemo()
    if (command.type === 'lock') return this.lock()
    if (command.type === 'unlock') return this.unlock(command.password)
    if (command.type === 'create_vault') return this.createVault(command.password)
    if (command.type === 'reset_local_data') return this.resetLocalData()
    if (command.type === 'export_backup') return this.exportBackup(command.password)
    if (command.type === 'restore_backup') {
      return this.restoreBackup(command.fileText, command.password, command.newVaultPassword)
    }

    const authority = this.requireAuthority()
    try {
      const result = await authority.command(command)
      return result
    } catch (error) {
      if (error instanceof Error && error.message.includes('locked')) {
        this.lifecycle = 'locked'
      }
      throw error
    }
  }

  async query(query: WebQuery): Promise<unknown> {
    if (query.type === 'get_status') return this.status()
    const authority = this.requireAuthority()
    const result = authority.query(query)
    if (query.type === 'search') {
      const projection = result as SearchProjection
      return projection
    }
    return result
  }

  // -------------------------------------------------------------------------
  // Demo
  // -------------------------------------------------------------------------

  private async seedDemo(): Promise<{ ok: true }> {
    const module = await this.requireModule()
    this.closeAuthority()
    const driver = new BrowserSqliteDriver({ module })
    this.driver = driver
    this.demoMode = true
    this.session = null
    this.hasVault = (await readVault()) !== null
    const authority = CoreAuthority.initialize(
      driver,
      { persist: () => Promise.resolve() },
      this.now,
    )
    this.authority = authority
    await DEMO_REALITY.seed((command) => authority.command(command))
    this.lifecycle = 'unlocked'
    this.errorCode = null
    this.lastSaveOk = false
    this.lastSavedAt = null
    return { ok: true }
  }

  // -------------------------------------------------------------------------
  // Backup / Restore
  // -------------------------------------------------------------------------

  private async exportBackup(password: string | null): Promise<{
    fileName: string
    containerJson: string
    formatVersion: number
  }> {
    const authority = this.requireAuthority()
    if (this.demoMode) {
      throw new DepmapWebError('unsupported', '示例数据不能导出为备份')
    }
    const payload = authority.exportPayload()
    const stamp = this.now().replace(/[:.]/g, '-')
    if (password === null) {
      const record = await readVault()
      if (!record) throw new DepmapWebError('invalid_structure', '本地 vault 不存在')
      return {
        fileName: `pdig-${stamp}.depmap`,
        containerJson: record.containerJson,
        formatVersion: DEPMAP_FORMAT_VERSION,
      }
    }
    const container = await createDepmapContainer(new TextEncoder().encode(payload), password)
    return { fileName: `pdig-${stamp}.depmap`, containerJson: container.json, formatVersion: 1 }
  }

  private async restoreBackup(
    fileText: string,
    password: string,
    newVaultPassword: string | null,
  ): Promise<{ graphRevision: number; counts: Record<string, number> }> {
    const opened = await openDepmapContainer(fileText, password)
    const module = await this.requireModule()
    // 1) 先在独立 DB 内验证 payload（半恢复禁止）
    const stagingDriver = new BrowserSqliteDriver({ module })
    let staged: CoreAuthority
    try {
      staged = CoreAuthority.fromPayload(
        stagingDriver,
        new TextDecoder().decode(opened.plaintext),
        { persist: () => Promise.resolve() },
        this.now,
      )
    } catch (error) {
      stagingDriver.close()
      throw error
    }
    const integrityDriver = stagingDriver
    const payloadJson = staged.exportPayload()
    const counts = exportCounts(payloadJson)
    integrityDriver.close()

    // 2) 通过验证后再替换当前内存图
    const driver = new BrowserSqliteDriver({ module })
    const authority = CoreAuthority.fromPayload(
      driver,
      payloadJson,
      { persist: (payload, revision) => this.persist(payload, revision) },
      this.now,
    )
    this.closeAuthority()
    this.driver = driver
    this.authority = authority
    this.demoMode = false
    if (newVaultPassword !== null) {
      const salt = crypto.getRandomValues(new Uint8Array(16))
      const kdf: KdfParams = { memoryKiB: 65536, iterations: 3, parallelism: 1 }
      this.session = { key: await deriveFileEncryptionKey(newVaultPassword, salt, kdf), salt, kdf }
    } else if (this.session === null) {
      throw new DepmapWebError('invalid_structure', '恢复需要本地 vault 口令')
    }
    await this.persistNow()
    this.hasVault = true
    this.lifecycle = 'unlocked'
    this.errorCode = null
    return { graphRevision: authority.graphRevision(), counts }
  }

  private async resetLocalData(): Promise<{ ok: true }> {
    this.closeAuthority()
    this.session = null
    this.demoMode = false
    await deleteVault()
    this.hasVault = false
    this.lifecycle = 'no_vault'
    this.lastSavedAt = null
    this.lastSaveOk = true
    this.errorCode = null
    return { ok: true }
  }
}

function exportCounts(payloadJson: string): Record<string, number> {
  const parsed = JSON.parse(payloadJson) as Record<string, unknown>
  const counts: Record<string, number> = {}
  for (const [key, value] of Object.entries(parsed)) {
    if (Array.isArray(value)) counts[key] = value.length
  }
  return counts
}

function mapDepmapError(error: unknown): WebErrorCode {
  if (error instanceof DepmapWebError) {
    switch (error.code) {
      case 'auth_failed':
        return 'AUTH_FAILED'
      case 'unsupported':
        return 'UNSUPPORTED_FORMAT'
      case 'bounds':
        return 'KDF_BOUNDS'
      case 'invalid_json':
      case 'invalid_structure':
        return 'CORRUPT_VAULT'
      default:
        return 'UNKNOWN'
    }
  }
  return 'UNKNOWN'
}
