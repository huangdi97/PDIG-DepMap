/**
 * Browser Session Lease —— 单写者保证（§19）。
 *
 * 多标签页同时打开同一 Personal Reality 时，不允许 last-writer-wins。
 * 实现：IndexedDB 事务内 read-modify-write（跨标签页原子）+ BroadcastChannel 通知。
 * 非本标签页持有 lease 时：本标签页进入只读阻断（不静默竞争）。
 */
import { type LeaseRecord, readLease, updateLease } from './indexed-db.ts'

const LEASE_TTL_MS = 6000
const HEARTBEAT_MS = 2000
const CHANNEL_NAME = 'pdig-web-lease'

export type LeaseState = 'acquired' | 'denied' | 'lost'

export interface LeaseHandle {
  readonly ownerId: string
  state: LeaseState
  claim: () => Promise<LeaseState>
  release: () => Promise<void>
  onChange: (listener: (state: LeaseState) => void) => () => void
}

function newOwnerId(): string {
  return crypto.randomUUID()
}

export function createLease(): LeaseHandle {
  const ownerId = newOwnerId()
  const listeners = new Set<(state: LeaseState) => void>()
  const channel = typeof BroadcastChannel === 'function' ? new BroadcastChannel(CHANNEL_NAME) : null
  let state: LeaseState = 'denied'
  let timer: ReturnType<typeof setInterval> | null = null

  const setState = (next: LeaseState): void => {
    if (state === next) return
    state = next
    for (const listener of listeners) listener(next)
  }

  const claimOnce = async (): Promise<LeaseState> => {
    const next = await updateLease<LeaseState>((current, now) => {
      if (current !== null && current.ownerId !== ownerId && current.expiresAt > now) {
        return { next: current, result: 'denied' }
      }
      return {
        next: {
          id: 'lease',
          ownerId,
          expiresAt: now + LEASE_TTL_MS,
          updatedAt: new Date(now).toISOString(),
        },
        result: 'acquired',
      }
    })
    if (next === 'acquired') channel?.postMessage({ type: 'claimed', ownerId })
    return next
  }

  /**
   * 争夺 lease。
   *
   * 为什么不是一次尝试：页面 reload / 前进后退时，上一页面实例的 `pagehide` 释放
   * 是异步的，可能还没来得及落库。若直接判定 denied，用户会看到假的
   * “已在另一个标签页打开”。这里在 TTL 窗口内重试；真正活着的另一个标签页会持续
   * 心跳续期，因此在窗口结束后仍会被正确阻断。
   */
  const claim = async (): Promise<LeaseState> => {
    const deadline = Date.now() + LEASE_TTL_MS + 2000
    for (;;) {
      const next = await claimOnce()
      if (next === 'acquired') {
        setState('acquired')
        return 'acquired'
      }
      if (Date.now() > deadline) {
        setState('denied')
        return 'denied'
      }
      await new Promise((resolve) => setTimeout(resolve, 400))
    }
  }

  const heartbeat = async (): Promise<void> => {
    const next = await updateLease<LeaseState>((current, now) => {
      if (current !== null && current.ownerId !== ownerId && current.expiresAt > now) {
        return { next: current, result: 'denied' }
      }
      return {
        next: {
          id: 'lease',
          ownerId,
          expiresAt: now + LEASE_TTL_MS,
          updatedAt: new Date(now).toISOString(),
        },
        result: 'acquired',
      }
    })
    if (next === 'denied' && state === 'acquired') {
      setState('lost')
    } else {
      setState(next)
    }
  }

  const release = async (): Promise<void> => {
    if (timer !== null) {
      clearInterval(timer)
      timer = null
    }
    await updateLease<null>((current) => {
      if (current !== null && current.ownerId !== ownerId) return { next: current, result: null }
      return { next: null, result: null }
    })
    channel?.postMessage({ type: 'released', ownerId })
  }

  if (channel) {
    channel.onmessage = (event: MessageEvent) => {
      const data = event.data as { type?: string; ownerId?: string } | null
      if (!data || data.ownerId === ownerId) return
      if (data.type === 'claimed' && state === 'acquired') {
        void heartbeat()
      }
    }
  }

  const startHeartbeat = (): void => {
    if (timer !== null) return
    timer = setInterval(() => {
      void heartbeat().catch(() => setState('lost'))
    }, HEARTBEAT_MS)
  }

  const handle: LeaseHandle = {
    ownerId,
    get state() {
      return state
    },
    claim: async () => {
      const result = await claim()
      if (result === 'acquired') startHeartbeat()
      return result
    },
    release,
    onChange: (listener) => {
      listeners.add(listener)
      return () => listeners.delete(listener)
    },
  }

  /**
   * 页面卸载：先显式释放；若释放失败（导航中断事务），把 expiresAt 置为 0，
   * 让下一个页面实例可以立即接管。绝不影响“另一个仍然活着的标签页” —
   * 活着的标签页会用心跳续期。
   */
  const expireOwnLease = async (): Promise<void> => {
    await updateLease<null>((current) => {
      if (current === null) return { next: null, result: null }
      if (current.ownerId !== ownerId) return { next: current, result: null }
      return {
        next: { ...current, expiresAt: 0, updatedAt: new Date().toISOString() },
        result: null,
      }
    })
  }

  window.addEventListener('pagehide', () => {
    void release()
    void expireOwnLease()
  })

  return handle
}

/** 只读探测：是否有其他标签页持有 lease。 */
export async function leaseHolder(): Promise<LeaseRecord | null> {
  return readLease()
}
