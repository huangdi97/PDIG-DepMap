import type { ReactNode } from 'react'
import { useRuntime } from '../store.tsx'

export function ToastHost(): ReactNode {
  const { toasts, dismissToast } = useRuntime()
  if (toasts.length === 0) return null
  return (
    <div className="toast-host" aria-live="polite" aria-atomic="false">
      {toasts.map((toast) => (
        <div
          key={toast.id}
          className={`toast toast-${toast.tone}`}
          role={toast.tone === 'error' ? 'alert' : 'status'}
        >
          <span>{toast.message}</span>
          <button type="button" onClick={() => dismissToast(toast.id)} aria-label="关闭提示">
            ✕
          </button>
        </div>
      ))}
    </div>
  )
}
