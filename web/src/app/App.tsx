import { useEffect, useState, type ReactNode } from 'react'
import { useRuntime } from './store.tsx'
import { primaryKeyOf, useRoute } from './routes.ts'
import { AppShell } from './shell/AppShell.tsx'
import { ContextPanel } from './shell/ContextPanel.tsx'
import { ToastHost } from './shell/ToastHost.tsx'
import { SearchOverlay } from '../features/search/SearchOverlay.tsx'
import { LockScreen } from '../features/lock/LockScreen.tsx'
import { Onboarding } from '../features/onboarding/Onboarding.tsx'
import { NowView } from '../features/now/NowView.tsx'
import { InfrastructureView } from '../features/infrastructure/InfrastructureView.tsx'
import { NodeDetail } from '../features/infrastructure/NodeDetail.tsx'
import { FindingsView } from '../features/findings/FindingsView.tsx'
import { ChangeView } from '../features/change/ChangeView.tsx'
import { PlanView } from '../features/change/PlanView.tsx'
import { RecordsView } from '../features/records/RecordsView.tsx'
import { ImportView } from '../features/import/ImportView.tsx'
import { MeView } from '../features/settings/MeView.tsx'

export function App(): ReactNode {
  const { status, booting, dataError } = useRuntime()

  if (booting) {
    return (
      <div className="boot">
        <div className="boot-mark" aria-hidden="true">
          P
        </div>
        <p>正在准备本地工作区…</p>
      </div>
    )
  }

  if (!status) {
    return (
      <div className="boot boot-error" role="alert">
        <h1>无法启动本地工作区</h1>
        <p>{dataError ?? '浏览器环境不支持所需的加密或存储能力。'}</p>
        <p className="boot-hint">
          请使用最新版 Chromium / Firefox / Safari，并确认未处于隐私浏览限制模式。
        </p>
        <button
          type="button"
          className="btn btn-secondary"
          onClick={() => window.location.reload()}
        >
          重新加载
        </button>
      </div>
    )
  }

  if (status.lifecycle === 'lease_denied') {
    return (
      <div className="boot boot-error" role="alert" data-testid="lease-blocked">
        <h1>PDIG 已在另一个标签页中打开</h1>
        <p>
          为避免两个标签页互相覆盖，这里保持只读阻断。请切换到已打开的那个标签页，或关闭它后重新加载。
        </p>
        <button
          type="button"
          className="btn btn-secondary"
          onClick={() => window.location.reload()}
        >
          重新检查
        </button>
      </div>
    )
  }

  if (status.lifecycle === 'no_vault') {
    return <Onboarding />
  }

  if (status.lifecycle !== 'unlocked') {
    return <LockScreen />
  }

  return <Workspace />
}

function Workspace(): ReactNode {
  const { status, refresh } = useRuntime()
  const route = useRoute()
  const [searchOpen, setSearchOpen] = useState(false)
  const primary = primaryKeyOf(route)

  useEffect(() => {
    const onKey = (event: KeyboardEvent): void => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault()
        setSearchOpen(true)
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [])

  // 每次进入主界面拉一次数据（确保跨标签页/跨路由的读数不会陈旧）。
  useEffect(() => {
    void refresh()
  }, [refresh])

  return (
    <AppShell primary={primary} onOpenSearch={() => setSearchOpen(true)}>
      <div className="workspace-grid">
        <main className="workspace-main" id="main">
          {route.name === 'now' ? <NowView /> : null}
          {route.name === 'findings' ? <FindingsView /> : null}
          {route.name === 'infrastructure' ? <InfrastructureView /> : null}
          {route.name === 'node' ? <NodeDetail nodeId={route.id} /> : null}
          {route.name === 'change' ? <ChangeView /> : null}
          {route.name === 'plan' ? <PlanView planId={route.id} /> : null}
          {route.name === 'records' ? <RecordsView /> : null}
          {route.name === 'import' ? <ImportView /> : null}
          {route.name === 'me' ? <MeView /> : null}
        </main>
        <ContextPanel route={route} />
      </div>
      {status && !status.persisted && status.lifecycle === 'unlocked' ? (
        <div className="save-banner" role="status" data-testid="unsaved-banner">
          {status.demoMode
            ? 'DEMO / SAMPLE REALITY：示例数据不会写入本地 vault。'
            : '变更尚未安全保存：浏览器存储写入未成功，请导出加密备份。'}
        </div>
      ) : null}
      {searchOpen ? <SearchOverlay onClose={() => setSearchOpen(false)} /> : null}
      <ToastHost />
    </AppShell>
  )
}
