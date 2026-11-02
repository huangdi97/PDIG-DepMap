/**
 * UI-thread Domain Client。
 *
 * 唯一职责：把 typed command/query 送进 Domain Worker，并把结构化错误带回来。
 * 这里不做任何 Truth 计算（continuity / stale / readiness / findings 全在 Worker）。
 */
import type {
  CommandResultMap,
  CommandType,
  QueryResultMap,
  QueryType,
  RequestBody,
  ResponseBody,
  RpcError,
  VaultStatus,
  WebCommand,
  WebQuery,
} from './protocol.ts'

export type DomainError = RpcError

export class DomainClient {
  private worker: Worker | null = null
  private nextId = 1
  private readonly pending = new Map<
    number,
    { resolve: (value: unknown) => void; reject: (error: DomainError) => void }
  >()
  private readonly readyWaiters: Array<
    (status: VaultStatus | null, error: DomainError | null) => void
  > = []
  private bootStatus: VaultStatus | null = null
  private bootError: DomainError | null = null
  private started = false

  async start(): Promise<{ status: VaultStatus | null; error: DomainError | null }> {
    if (this.started) return { status: this.bootStatus, error: this.bootError }
    this.started = true
    const worker = new Worker(new URL('./worker/domain.worker.ts', import.meta.url), {
      type: 'module',
      name: 'pdig-domain-worker',
    })
    this.worker = worker
    worker.addEventListener('message', (event: MessageEvent<ResponseBody>) => {
      const response = event.data
      if (response.id === -1) {
        if (response.ok) {
          const data = response.data as { ready: boolean; status: VaultStatus }
          this.bootStatus = data.status
          this.resolveReady(data.status, null)
        } else {
          this.bootError = response.error
          this.resolveReady(null, response.error)
        }
        return
      }
      const entry = this.pending.get(response.id)
      if (!entry) return
      this.pending.delete(response.id)
      if (response.ok) entry.resolve(response.data)
      else entry.reject(response.error)
    })
    worker.addEventListener('error', (event) => {
      const error: DomainError = {
        code: 'UNKNOWN',
        message: event.message || 'domain worker crashed',
        recoverable: false,
      }
      for (const [id, entry] of this.pending) {
        this.pending.delete(id)
        entry.reject(error)
      }
      this.resolveReady(null, error)
    })
    return new Promise((resolve) => {
      this.readyWaiters.push((status, error) => resolve({ status, error }))
    })
  }

  private resolveReady(status: VaultStatus | null, error: DomainError | null): void {
    while (this.readyWaiters.length > 0) {
      const waiter = this.readyWaiters.shift()
      waiter?.(status, error)
    }
  }

  async command<K extends CommandType>(
    command: Extract<WebCommand, { type: K }>,
  ): Promise<CommandResultMap[K]> {
    const body: RequestBody = { id: this.nextId++, kind: 'command', command }
    return (await this.request(body)) as CommandResultMap[K]
  }

  async query<K extends QueryType>(
    query: Extract<WebQuery, { type: K }>,
  ): Promise<QueryResultMap[K]> {
    const body: RequestBody = { id: this.nextId++, kind: 'query', query }
    return (await this.request(body)) as QueryResultMap[K]
  }

  private request(body: RequestBody): Promise<unknown> {
    const worker = this.worker
    if (!worker) {
      return Promise.reject<unknown>({
        code: 'STATE_LOCKED',
        message: 'domain worker 尚未启动',
        recoverable: true,
      } satisfies DomainError)
    }
    return new Promise((resolve, reject) => {
      this.pending.set(body.id, { resolve, reject })
      worker.postMessage(body)
    })
  }

  /** Lock：终止 worker，丢弃所有 in-flight 请求（内存中的明文图随之销毁）。 */
  terminate(): void {
    const error: DomainError = {
      code: 'STATE_LOCKED',
      message: '已锁定',
      recoverable: true,
    }
    for (const [id, entry] of this.pending) {
      this.pending.delete(id)
      entry.reject(error)
    }
    this.worker?.terminate()
    this.worker = null
    this.started = false
    this.bootStatus = null
    this.bootError = null
  }
}

export function userMessageFor(error: DomainError): string {
  switch (error.code) {
    case 'AUTH_FAILED':
      return '口令错误或文件已损坏'
    case 'TAMPERED':
      return '数据完整性校验失败，文件可能已被修改'
    case 'UNSUPPORTED_FORMAT':
      return '该文件版本尚不支持'
    case 'CORRUPT_VAULT':
      return '本地数据损坏，无法读取'
    case 'KDF_BOUNDS':
      return '备份文件的加密参数超出允许范围，已拒绝对其执行解密'
    case 'STORAGE_FULL':
      return '浏览器存储空间不足，变更尚未安全保存'
    case 'STORAGE_ERROR':
      return '无法保存到浏览器存储，变更尚未安全保存'
    case 'LEASE_LOST':
      return '另一个标签页接管了这份数据'
    case 'STATE_LOCKED':
      return '当前处于锁定状态'
    case 'FILE_TOO_LARGE':
      return '文件超过允许的大小上限'
    case 'IMPORT_FAILED':
      return '导入未能完成，未写入任何变更'
    case 'NOT_FOUND':
      return '对象不存在或已被删除'
    case 'CONFLICT':
      return error.message
    case 'VALIDATION':
      return error.message
    default:
      return '操作未完成'
  }
}
