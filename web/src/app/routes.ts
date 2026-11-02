/** 极简 hash 路由（静态托管 / PWA / 离线都可用，无需服务端 rewrite）。 */
import { useEffect, useState } from 'react'

export type Route =
  | { name: 'now' }
  | { name: 'infrastructure' }
  | { name: 'node'; id: string }
  | { name: 'findings' }
  | { name: 'change' }
  | { name: 'plan'; id: string }
  | { name: 'records' }
  | { name: 'import' }
  | { name: 'me' }

export const PRIMARY_ROUTES = ['now', 'infrastructure', 'change', 'records', 'me'] as const

export function parseHash(hash: string): Route {
  const path = hash.replace(/^#\/?/, '')
  const parts = path.split('/').filter((segment) => segment !== '')
  const [head, second] = parts
  switch (head) {
    case 'infrastructure':
      return second ? { name: 'node', id: decodeURIComponent(second) } : { name: 'infrastructure' }
    case 'change':
      return second ? { name: 'plan', id: decodeURIComponent(second) } : { name: 'change' }
    case 'records':
      return { name: 'records' }
    case 'import':
      return { name: 'import' }
    case 'me':
      return { name: 'me' }
    case 'findings':
      return { name: 'findings' }
    default:
      return { name: 'now' }
  }
}

export function routeToHash(route: string): string {
  return `#${route.startsWith('/') ? route : `/${route}`}`
}

export function navigate(route: string): void {
  const target = routeToHash(route)
  if (window.location.hash === target) return
  window.location.hash = target
}

export function useRoute(): Route {
  const [route, setRoute] = useState<Route>(() => parseHash(window.location.hash))
  useEffect(() => {
    const onChange = (): void => setRoute(parseHash(window.location.hash))
    window.addEventListener('hashchange', onChange)
    return () => window.removeEventListener('hashchange', onChange)
  }, [])
  return route
}

export function primaryKeyOf(route: Route): (typeof PRIMARY_ROUTES)[number] {
  switch (route.name) {
    case 'infrastructure':
    case 'node':
      return 'infrastructure'
    case 'change':
    case 'plan':
      return 'change'
    case 'records':
      return 'records'
    case 'me':
      return 'me'
    case 'now':
    case 'findings':
    case 'import':
    default:
      return 'now'
  }
}
