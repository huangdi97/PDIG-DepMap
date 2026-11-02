/**
 * Runtime Provider —— UI 与 DomainClient / Session Lease / 本地偏好之间的粘合层。
 *
 * 这里只做产品状态编排（loading / locked / lease / toast / 偏好），不做 Truth 计算。
 */
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react'
import { DomainClient, userMessageFor, type DomainError } from '../runtime/client.ts'
import { createLease, type LeaseHandle, type LeaseState } from '../storage/lease.ts'
import type {
  CommandResultMap,
  CommandType,
  QueryResultMap,
  QueryType,
  VaultStatus,
  WebCommand,
  WebQuery,
} from '../runtime/protocol.ts'

const PREFS_KEY = 'pdig.web.prefs.v1'

export interface Prefs {
  privacyMask: boolean
  autoLockMinutes: number
}

const DEFAULT_PREFS: Prefs = { privacyMask: false, autoLockMinutes: 15 }

function readPrefs(): Prefs {
  try {
    const raw = localStorage.getItem(PREFS_KEY)
    if (!raw) return DEFAULT_PREFS
    const parsed = JSON.parse(raw) as Partial<Prefs>
    return {
      privacyMask: parsed.privacyMask === true,
      autoLockMinutes:
        typeof parsed.autoLockMinutes === 'number'
          ? parsed.autoLockMinutes
          : DEFAULT_PREFS.autoLockMinutes,
    }
  } catch {
    return DEFAULT_PREFS
  }
}

export interface Toast {
  id: number
  tone: 'info' | 'error' | 'success'
  message: string
}

interface RuntimeContextValue {
  client: DomainClient
  status: VaultStatus | null
  booting: boolean
  leaseState: LeaseState
  revision: number
  prefs: Prefs
  toasts: Toast[]
  dataError: string | null
  refresh: () => Promise<void>
  runCommand: <K extends CommandType>(
    command: Extract<WebCommand, { type: K }>,
  ) => Promise<CommandResultMap[K] | null>
  runQuery: <K extends QueryType>(
    query: Extract<WebQuery, { type: K }>,
  ) => Promise<QueryResultMap[K] | null>
  setPrefs: (patch: Partial<Prefs>) => void
  lock: () => Promise<void>
  notify: (message: string, tone?: Toast['tone']) => void
  dismissToast: (id: number) => void
}

const RuntimeContext = createContext<RuntimeContextValue | null>(null)

export function useRuntime(): RuntimeContextValue {
  const value = useContext(RuntimeContext)
  if (!value) throw new Error('useRuntime must be used inside RuntimeProvider')
  return value
}

function makeClient(): DomainClient {
  return new DomainClient()
}

export function RuntimeProvider({ children }: { children: ReactNode }): ReactNode {
  const clientRef = useRef<DomainClient>(makeClient())
  const leaseRef = useRef<LeaseHandle | null>(null)
  const [status, setStatus] = useState<VaultStatus | null>(null)
  const [booting, setBooting] = useState(true)
  const [leaseState, setLeaseState] = useState<LeaseState>('denied')
  const [revision, setRevision] = useState(0)
  const [prefs, setPrefsState] = useState<Prefs>(() => readPrefs())
  const [toasts, setToasts] = useState<Toast[]>([])
  const [dataError, setDataError] = useState<string | null>(null)
  const toastId = useRef(1)

  const notify = useCallback((message: string, tone: Toast['tone'] = 'info') => {
    const id = toastId.current++
    setToasts((current) => [...current, { id, tone, message }].slice(-3))
    setTimeout(() => {
      setToasts((current) => current.filter((toast) => toast.id !== id))
    }, 6000)
  }, [])

  const dismissToast = useCallback((id: number) => {
    setToasts((current) => current.filter((toast) => toast.id !== id))
  }, [])

  const refresh = useCallback(async () => {
    const client = clientRef.current
    try {
      const next = await client.query({ type: 'get_status' })
      setStatus(next)
      setRevision((value) => value + 1)
      setDataError(null)
    } catch (error) {
      const domainError = error as DomainError
      setDataError(userMessageFor(domainError))
    }
  }, [])

  const boot = useCallback(async () => {
    const lease = createLease()
    leaseRef.current = lease
    lease.onChange((next) => setLeaseState(next))
    const claimed = await lease.claim()
    setLeaseState(claimed)
    const started = await clientRef.current.start()
    if (started.error) {
      setDataError(userMessageFor(started.error))
      setStatus(null)
      setBooting(false)
      return
    }
    let current = started.status
    setStatus(current)
    const demoRequested = new URLSearchParams(window.location.search).get('demo') === '1'
    if (
      demoRequested &&
      current &&
      (current.lifecycle === 'no_vault' || current.lifecycle === 'locked')
    ) {
      try {
        await clientRef.current.command({ type: 'seed_demo' })
        current = await clientRef.current.query({ type: 'get_status' })
        setStatus(current)
      } catch (error) {
        notify(userMessageFor(error as DomainError), 'error')
      }
    }
    setRevision((value) => value + 1)
    setBooting(false)
  }, [notify])

  useEffect(() => {
    void boot()
  }, [boot])

  const runCommand = useCallback(
    async <K extends CommandType>(
      command: Extract<WebCommand, { type: K }>,
    ): Promise<CommandResultMap[K] | null> => {
      try {
        const result = await clientRef.current.command(command)
        await refresh()
        return result as CommandResultMap[K]
      } catch (error) {
        notify(userMessageFor(error as DomainError), 'error')
        return null
      }
    },
    [notify, refresh],
  )

  const runQuery = useCallback(
    async <K extends QueryType>(
      query: Extract<WebQuery, { type: K }>,
    ): Promise<QueryResultMap[K] | null> => {
      try {
        const result = await clientRef.current.query(query)
        return result
      } catch (error) {
        setDataError(userMessageFor(error as DomainError))
        return null
      }
    },
    [],
  )

  const setPrefs = useCallback((patch: Partial<Prefs>) => {
    setPrefsState((current) => {
      const next = { ...current, ...patch }
      try {
        localStorage.setItem(PREFS_KEY, JSON.stringify(next))
      } catch {
        // 偏好写失败不影响产品语义（隐私遮蔽仍然只影响当前会话）。
      }
      return next
    })
  }, [])

  const lock = useCallback(async () => {
    const client = clientRef.current
    client.terminate()
    const next = makeClient()
    clientRef.current = next
    const started = await next.start()
    setStatus(started.status)
    setRevision((value) => value + 1)
  }, [])

  // Auto lock：仅在 unlocked 且真实 vault（非 demo）时启动。
  useEffect(() => {
    if (!status || status.lifecycle !== 'unlocked' || status.demoMode) return
    const minutes = prefs.autoLockMinutes
    if (minutes <= 0) return
    let timer: ReturnType<typeof setTimeout> | null = setTimeout(() => {
      void lock()
    }, minutes * 60_000)
    const reset = (): void => {
      if (timer !== null) clearTimeout(timer)
      timer = setTimeout(() => {
        void lock()
      }, minutes * 60_000)
    }
    const events = ['pointerdown', 'keydown', 'visibilitychange', 'focus'] as const
    for (const event of events) window.addEventListener(event, reset)
    return () => {
      if (timer !== null) clearTimeout(timer)
      for (const event of events) window.removeEventListener(event, reset)
    }
  }, [status, prefs.autoLockMinutes, lock])

  // Tab 隐藏时清空敏感 DOM（Web 无法提供 FLAG_SECURE 等价能力）。
  const [hidden, setHidden] = useState(false)
  useEffect(() => {
    const onChange = (): void => setHidden(document.visibilityState === 'hidden')
    document.addEventListener('visibilitychange', onChange)
    return () => document.removeEventListener('visibilitychange', onChange)
  }, [])

  let effectiveStatus = status
  if (status && leaseState !== 'acquired' && status.lifecycle !== 'no_vault') {
    effectiveStatus = { ...status, lifecycle: 'lease_denied' }
  }

  const value = useMemo<RuntimeContextValue>(
    () => ({
      client: clientRef.current,
      status: effectiveStatus,
      booting,
      leaseState,
      revision,
      prefs,
      toasts,
      dataError,
      refresh,
      runCommand,
      runQuery,
      setPrefs,
      lock,
      notify,
      dismissToast,
    }),
    [
      effectiveStatus,
      booting,
      leaseState,
      revision,
      prefs,
      toasts,
      dataError,
      refresh,
      runCommand,
      runQuery,
      setPrefs,
      lock,
      notify,
      dismissToast,
    ],
  )

  return (
    <RuntimeContext.Provider value={value}>
      {hidden ? <div className="privacy-veil" role="presentation" /> : null}
      {children}
    </RuntimeContext.Provider>
  )
}

export function usePrivacyMask(): boolean {
  return useRuntime().prefs.privacyMask
}
