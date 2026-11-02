/**
 * lease 单元测试夹具：直接复用生产逻辑的事务语义（不启动 window / BroadcastChannel）。
 */
import { updateLease, readLease, type LeaseRecord } from '../../src/storage/indexed-db.ts'

const TTL = 6000

export type LeaseOutcome = 'acquired' | 'denied' | 'lost'

export async function claimFor(ownerId: string): Promise<LeaseOutcome> {
  return updateLease<LeaseOutcome>((current, now) => {
    if (current !== null && current.ownerId !== ownerId && current.expiresAt > now) {
      return { next: current, result: 'denied' }
    }
    return {
      next: { id: 'lease', ownerId, expiresAt: now + TTL, updatedAt: new Date(now).toISOString() },
      result: 'acquired',
    }
  })
}

export async function heartbeatFor(ownerId: string): Promise<LeaseOutcome> {
  return updateLease<LeaseOutcome>((current, now) => {
    if (current !== null && current.ownerId !== ownerId && current.expiresAt > now) {
      return { next: current, result: 'denied' }
    }
    return {
      next: { id: 'lease', ownerId, expiresAt: now + TTL, updatedAt: new Date(now).toISOString() },
      result: 'acquired',
    }
  })
}

export async function releaseFor(ownerId: string): Promise<void> {
  await updateLease<null>((current) => {
    if (current !== null && current.ownerId !== ownerId) return { next: current, result: null }
    return { next: null, result: null }
  })
}

export async function readLeaseRecord(): Promise<LeaseRecord | null> {
  return readLease()
}

/** 测试隔离：无条件清空 lease 记录。 */
export async function releaseAny(): Promise<void> {
  await updateLease<null>(() => ({ next: null, result: null }))
}
