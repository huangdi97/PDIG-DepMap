/// <reference lib="webworker" />
/**
 * Domain Worker —— UI 与 Truth 之间的唯一边界。
 *
 * UI 只发送 typed command / query；Worker 内持有：
 * SQLite WASM（内存库）→ Core Repository/Service → projection DTO。
 *
 * 该 Worker 在 lock 时由主线程 terminate（§16），重启后为干净的 locked worker。
 */
import { VaultRuntime } from './vault-runtime.ts'
import { AuthorityError } from './core-authority.ts'
import { DepmapWebError } from '../../crypto/depmap-web.ts'
import type { RequestBody, ResponseBody, RpcError, WebErrorCode } from '../protocol.ts'

const runtime = new VaultRuntime({ now: () => new Date().toISOString() })
const scope = self as unknown as DedicatedWorkerGlobalScope

function toRpcError(error: unknown): RpcError {
  if (error instanceof AuthorityError) {
    const code: WebErrorCode = error.code === 'IMPORT_FAILED' ? 'IMPORT_FAILED' : error.code
    return { code, message: error.message, recoverable: true }
  }
  if (error instanceof DepmapWebError) {
    const code: WebErrorCode =
      error.code === 'auth_failed'
        ? 'AUTH_FAILED'
        : error.code === 'unsupported'
          ? 'UNSUPPORTED_FORMAT'
          : error.code === 'bounds'
            ? 'KDF_BOUNDS'
            : 'CORRUPT_VAULT'
    return { code, message: error.message, recoverable: true }
  }
  const message = error instanceof Error ? error.message : String(error)
  return { code: 'UNKNOWN', message, recoverable: false }
}

async function handle(request: RequestBody): Promise<unknown> {
  if (request.kind === 'command') {
    const command = request.command
    if (command.type === 'import_file') {
      return runtime.command(command)
    }
    return runtime.command(command)
  }
  return runtime.query(request.query)
}

scope.addEventListener('message', (event: MessageEvent<RequestBody>) => {
  const request = event.data
  void (async () => {
    try {
      const data = await handle(request)
      const response: ResponseBody = { id: request.id, ok: true, data }
      scope.postMessage(response)
    } catch (error) {
      const response: ResponseBody = { id: request.id, ok: false, error: toRpcError(error) }
      scope.postMessage(response)
    }
  })()
})

const ready = (async () => {
  const status = await runtime.boot()
  const message: ResponseBody = { id: -1, ok: true, data: { ready: true, status } }
  scope.postMessage(message)
})().catch((error: unknown) => {
  const response: ResponseBody = { id: -1, ok: false, error: toRpcError(error) }
  scope.postMessage(response)
})

void ready
