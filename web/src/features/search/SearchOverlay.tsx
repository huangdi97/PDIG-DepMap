import { useEffect, useRef, useState, type ReactNode } from 'react'
import { useRuntime } from '../../app/store.tsx'
import { navigate } from '../../app/routes.ts'
import type { SearchProjection, SearchResult } from '../../runtime/protocol.ts'
import { Badge } from '../../components/primitives/index.tsx'
import { useMasked } from '../../privacy/Sensitive.tsx'

const TYPE_LABEL: Record<SearchResult['type'], string> = {
  node: '对象',
  plan: '变更',
  finding: '发现',
  record: '记录',
  source: '来源',
}

export function SearchOverlay({ onClose }: { onClose: () => void }): ReactNode {
  const { runQuery } = useRuntime()
  const masked = useMasked()
  const [query, setQuery] = useState('')
  const [data, setData] = useState<SearchProjection | null>(null)
  const [active, setActive] = useState(0)
  const inputRef = useRef<HTMLInputElement | null>(null)

  useEffect(() => {
    inputRef.current?.focus()
  }, [])

  useEffect(() => {
    if (query.trim() === '') {
      setData(null)
      return
    }
    let cancelled = false
    const timer = setTimeout(() => {
      void runQuery({ type: 'search', query }).then((result) => {
        if (!cancelled) {
          setData(result)
          setActive(0)
        }
      })
    }, 120)
    return () => {
      cancelled = true
      clearTimeout(timer)
    }
  }, [query, runQuery])

  const results = data?.results ?? []
  const go = (result: SearchResult): void => {
    navigate(result.route)
    onClose()
  }

  return (
    <div className="overlay" role="dialog" aria-modal="true" aria-label="全局搜索">
      <div className="overlay-card">
        <div className="overlay-head">
          <input
            ref={inputRef}
            className="overlay-input"
            placeholder="搜索对象、变更、发现、记录、来源"
            value={query}
            aria-label="搜索"
            onChange={(event) => setQuery(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Escape') onClose()
              if (event.key === 'ArrowDown') {
                event.preventDefault()
                setActive((value) => Math.min(value + 1, Math.max(results.length - 1, 0)))
              }
              if (event.key === 'ArrowUp') {
                event.preventDefault()
                setActive((value) => Math.max(value - 1, 0))
              }
              if (event.key === 'Enter') {
                const result = results[active]
                if (result) go(result)
              }
            }}
          />
          <button type="button" className="overlay-close" onClick={onClose} aria-label="关闭搜索">
            Esc
          </button>
        </div>
        <div className="overlay-body">
          {masked ? (
            <p className="form-note" data-testid="search-masked">
              隐私遮蔽已开启：搜索结果中的敏感信息已遮蔽。
            </p>
          ) : null}
          {query.trim() === '' ? (
            <p className="form-note">输入关键词开始搜索。搜索索引只在内存中，锁定后即销毁。</p>
          ) : results.length === 0 ? (
            <p className="form-note">没有匹配结果。</p>
          ) : (
            <ul className="search-list">
              {results.map((result, index) => (
                <li key={`${result.type}-${result.id}`}>
                  <button
                    type="button"
                    className={`search-row ${index === active ? 'active' : ''}`}
                    onMouseEnter={() => setActive(index)}
                    onClick={() => go(result)}
                  >
                    <span className="search-type">
                      <Badge>{TYPE_LABEL[result.type]}</Badge>
                    </span>
                    <span className="search-title">
                      {masked && result.type === 'node' ? '••••••' : result.title}
                    </span>
                    <span className="search-detail">
                      {masked ? '敏感信息已遮蔽' : result.detail}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  )
}
