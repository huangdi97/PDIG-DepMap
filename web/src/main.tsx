import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RuntimeProvider } from './app/store.tsx'
import { App } from './app/App.tsx'
import './styles/globals.css'

const container = document.getElementById('app')
if (!container) throw new Error('#app container missing')

createRoot(container).render(
  <StrictMode>
    <RuntimeProvider>
      <App />
    </RuntimeProvider>
  </StrictMode>,
)

/**
 * PWA：只缓存静态 shell。新版本就绪时不强制刷新（避免打断进行中的变更），
 * 而是发出事件让 UI 显示「新版本可用」。
 */
if (import.meta.env.PROD && 'serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    void navigator.serviceWorker
      .register('./sw.js')
      .then((registration) => {
        if (registration.waiting) window.dispatchEvent(new Event('pdig:update-ready'))
        registration.addEventListener('updatefound', () => {
          const installing = registration.installing
          installing?.addEventListener('statechange', () => {
            if (installing.state === 'installed' && navigator.serviceWorker.controller) {
              window.dispatchEvent(new Event('pdig:update-ready'))
            }
          })
        })
      })
      .catch(() => undefined)
  })
}

export async function applyServiceWorkerUpdate(): Promise<void> {
  const registration = await navigator.serviceWorker.getRegistration()
  registration?.waiting?.postMessage({ type: 'SKIP_WAITING' })
  window.location.reload()
}
