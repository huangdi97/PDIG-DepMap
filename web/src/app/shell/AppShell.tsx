import type { ReactNode } from 'react'
import { useRuntime } from '../store.tsx'
import { navigate } from '../routes.ts'
import { PRIMARY_ROUTES } from '../routes.ts'

const NAV_LABELS: Record<(typeof PRIMARY_ROUTES)[number], string> = {
  now: '现在',
  infrastructure: '基础设施',
  change: '变更',
  records: '记录',
  me: '我',
}

const NAV_ICONS: Record<(typeof PRIMARY_ROUTES)[number], ReactNode> = {
  now: (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path d="M4 13.5 12 6l8 7.5" />
      <path d="M6.5 11.5V20h11v-8.5" />
      <path d="M10 20v-5h4v5" />
    </svg>
  ),
  infrastructure: (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <rect x="4" y="4" width="6" height="6" rx="2" />
      <rect x="14" y="4" width="6" height="6" rx="2" />
      <rect x="4" y="14" width="6" height="6" rx="2" />
      <rect x="14" y="14" width="6" height="6" rx="2" />
    </svg>
  ),
  change: (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path d="M4 7h11" />
      <path d="m12 4 3 3-3 3" />
      <path d="M20 17H9" />
      <path d="m12 14-3 3 3 3" />
    </svg>
  ),
  records: (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path d="M7 3h10v4H7z" />
      <path d="M5 5v16h14V5" />
      <path d="M8 11h8M8 15h8" />
    </svg>
  ),
  me: (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <circle cx="12" cy="8" r="4" />
      <path d="M4.5 21c.7-4 3.2-6 7.5-6s6.8 2 7.5 6" />
    </svg>
  ),
}

export function AppShell({
  primary,
  onOpenSearch,
  children,
}: {
  primary: (typeof PRIMARY_ROUTES)[number]
  onOpenSearch: () => void
  children: ReactNode
}): ReactNode {
  const { status, lock } = useRuntime()
  const demo = status?.demoMode === true

  return (
    <div className={`shell ${demo ? 'demo' : ''}`}>
      <a className="skip-link" href="#main">
        跳到主内容
      </a>
      {demo ? (
        <div className="demo-banner" role="status" data-testid="demo-banner">
          DEMO / SAMPLE REALITY — 示例数据，仅用于演示；不会写入本地 vault。
        </div>
      ) : null}

      <nav className="rail" aria-label="主导航">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true">
            P
          </span>
          <span className="brand-text">
            <strong>PDIG</strong>
            <small>个人数字依赖图</small>
          </span>
        </div>
        <ul className="rail-list">
          {PRIMARY_ROUTES.map((key) => (
            <li key={key}>
              <button
                type="button"
                className={`rail-item ${primary === key ? 'active' : ''}`}
                aria-current={primary === key ? 'page' : undefined}
                onClick={() => navigate(key === 'now' ? '/' : `/${key}`)}
              >
                <span className="rail-icon">{NAV_ICONS[key]}</span>
                <span>{NAV_LABELS[key]}</span>
              </button>
            </li>
          ))}
        </ul>
        <div className="rail-foot">
          <button
            type="button"
            className="rail-action"
            onClick={onOpenSearch}
            aria-keyshortcuts="Control+K"
          >
            <span className="rail-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" focusable="false">
                <circle cx="11" cy="11" r="6" />
                <path d="m16 16 4 4" />
              </svg>
            </span>
            <span>搜索</span>
            <kbd>⌘K</kbd>
          </button>
          <button type="button" className="rail-action" onClick={() => void lock()}>
            <span className="rail-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" focusable="false">
                <rect x="5" y="10" width="14" height="11" rx="3" />
                <path d="M8 10V7a4 4 0 0 1 8 0v3" />
              </svg>
            </span>
            <span>锁定</span>
          </button>
        </div>
      </nav>

      <header className="topbar">
        <div className="topbar-brand">PDIG</div>
        <button type="button" className="topbar-search" onClick={onOpenSearch}>
          搜索对象、变更、记录
        </button>
        <button type="button" className="topbar-lock" onClick={() => void lock()}>
          锁定
        </button>
      </header>

      <div className="shell-body">{children}</div>

      <nav className="bottom-nav" aria-label="主导航（紧凑）">
        {PRIMARY_ROUTES.map((key) => (
          <button
            key={key}
            type="button"
            className={`bottom-item ${primary === key ? 'active' : ''}`}
            aria-current={primary === key ? 'page' : undefined}
            onClick={() => navigate(key === 'now' ? '/' : `/${key}`)}
          >
            <span className="bottom-icon">{NAV_ICONS[key]}</span>
            <span>{NAV_LABELS[key]}</span>
          </button>
        ))}
      </nav>
    </div>
  )
}
