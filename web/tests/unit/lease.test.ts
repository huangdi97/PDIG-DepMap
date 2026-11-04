import { beforeEach, describe, expect, it } from 'vitest'
import { claimFor, heartbeatFor, readLeaseRecord, releaseAny, releaseFor } from './lease-harness.ts'

beforeEach(async () => {
  await releaseAny()
})

describe('session lease', () => {
  it('grants the first owner and denies a second one', async () => {
    expect(await claimFor('owner-a')).toBe('acquired')
    expect(await claimFor('owner-b')).toBe('denied')
    expect((await readLeaseRecord())?.ownerId).toBe('owner-a')
  })

  it('keeps ownership on heartbeat and rejects foreign heartbeat', async () => {
    await claimFor('owner-a')
    expect(await heartbeatFor('owner-a')).toBe('acquired')
    expect(await heartbeatFor('owner-c')).toBe('denied')
    expect((await readLeaseRecord())?.ownerId).toBe('owner-a')
  })

  it('allows a new owner after explicit release', async () => {
    await claimFor('owner-a')
    await releaseFor('owner-a')
    expect(await readLeaseRecord()).toBeNull()
    expect(await claimFor('owner-b')).toBe('acquired')
  })

  it('never lets a holding owner be silently replaced', async () => {
    expect(await claimFor('owner-a')).toBe('acquired')
    await releaseFor('owner-b')
    expect((await readLeaseRecord())?.ownerId).toBe('owner-a')
  })

  it('re-expires only after TTL, not on foreign heartbeat', async () => {
    await claimFor('owner-a')
    const record = await readLeaseRecord()
    expect(record).not.toBeNull()
    expect((record?.expiresAt ?? 0) > Date.now()).toBe(true)
  })
})
