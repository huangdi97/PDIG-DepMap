/**
 * IndexedDB 持久化层（只存**加密**快照与非敏感元数据）。
 *
 * 绝不存放：明文 nodes/dependencies、邮箱、手机号、服务名、搜索历史、口令、派生密钥。
 */
export const DB_NAME = 'pdig-web'
export const DB_VERSION = 1
export const STORE_VAULT = 'vault'
export const STORE_LEASE = 'lease'

export interface VaultRecord {
  id: 'active'
  /** 完整 DEPMAP_CONTAINER_V1 容器 JSON（已加密）。 */
  containerJson: string
  updatedAt: string
  graphRevision: number
  schemaVersion: number
  demo: boolean
}

export interface LeaseRecord {
  id: 'lease'
  ownerId: string
  expiresAt: number
  updatedAt: string
}

function openDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION)
    request.onupgradeneeded = () => {
      const db = request.result
      if (!db.objectStoreNames.contains(STORE_VAULT))
        db.createObjectStore(STORE_VAULT, { keyPath: 'id' })
      if (!db.objectStoreNames.contains(STORE_LEASE))
        db.createObjectStore(STORE_LEASE, { keyPath: 'id' })
    }
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error ?? new Error('indexedDB open failed'))
  })
}

function requestToPromise<T>(request: IDBRequest<T>): Promise<T> {
  return new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error ?? new Error('indexedDB request failed'))
  })
}

export async function readVault(): Promise<VaultRecord | null> {
  const db = await openDb()
  try {
    const tx = db.transaction(STORE_VAULT, 'readonly')
    const record = await requestToPromise(tx.objectStore(STORE_VAULT).get('active'))
    return (record as VaultRecord | undefined) ?? null
  } finally {
    db.close()
  }
}

export async function writeVault(record: VaultRecord): Promise<void> {
  const db = await openDb()
  try {
    const tx = db.transaction(STORE_VAULT, 'readwrite')
    await requestToPromise(tx.objectStore(STORE_VAULT).put(record))
    await new Promise<void>((resolve, reject) => {
      tx.oncomplete = () => resolve()
      tx.onerror = () => reject(tx.error ?? new Error('vault write failed'))
      tx.onabort = () => reject(tx.error ?? new Error('vault write aborted'))
    })
  } finally {
    db.close()
  }
}

export async function deleteVault(): Promise<void> {
  const db = await openDb()
  try {
    const tx = db.transaction(STORE_VAULT, 'readwrite')
    await requestToPromise(tx.objectStore(STORE_VAULT).delete('active'))
    await new Promise<void>((resolve, reject) => {
      tx.oncomplete = () => resolve()
      tx.onerror = () => reject(tx.error ?? new Error('vault delete failed'))
    })
  } finally {
    db.close()
  }
}

/** 事务内读-改-写：多标签页 lease 的原子性依赖这一点。 */
export async function updateLease<T>(
  decide: (current: LeaseRecord | null, now: number) => { next: LeaseRecord | null; result: T },
): Promise<T> {
  const db = await openDb()
  try {
    const tx = db.transaction(STORE_LEASE, 'readwrite')
    const store = tx.objectStore(STORE_LEASE)
    const current = (await requestToPromise(store.get('lease'))) as LeaseRecord | undefined
    const decision = decide(current ?? null, Date.now())
    if (decision.next === null) {
      await requestToPromise(store.delete('lease'))
    } else {
      await requestToPromise(store.put(decision.next))
    }
    await new Promise<void>((resolve, reject) => {
      tx.oncomplete = () => resolve()
      tx.onerror = () => reject(tx.error ?? new Error('lease write failed'))
    })
    return decision.result
  } finally {
    db.close()
  }
}

export async function readLease(): Promise<LeaseRecord | null> {
  const db = await openDb()
  try {
    const tx = db.transaction(STORE_LEASE, 'readonly')
    const record = await requestToPromise(tx.objectStore(STORE_LEASE).get('lease'))
    return (record as LeaseRecord | undefined) ?? null
  } finally {
    db.close()
  }
}
