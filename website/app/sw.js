/**
 * Service Worker：只缓存静态 app shell / 图标 / WASM。
 *
 * 绝不缓存：解密后的图、搜索结果、备份文件内容。
 * 数据本身在 IndexedDB（加密容器），与 SW 缓存完全分离。
 */
const CACHE = 'pdig-shell-v1'
const MANIFEST = './sw-manifest.json'

self.addEventListener('install', (event) => {
  event.waitUntil(
    (async () => {
      const response = await fetch(MANIFEST, { cache: 'no-store' })
      const files = await response.json()
      const cache = await caches.open(CACHE)
      await cache.addAll(files)
    })(),
  )
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    (async () => {
      const keys = await caches.keys()
      await Promise.all(keys.filter((key) => key !== CACHE).map((key) => caches.delete(key)))
      await self.clients.claim()
    })(),
  )
})

self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting()
  }
})

self.addEventListener('fetch', (event) => {
  const request = event.request
  if (request.method !== 'GET') return
  const url = new URL(request.url)
  if (url.origin !== self.location.origin) return

  event.respondWith(
    (async () => {
      const cache = await caches.open(CACHE)
      const cached = await cache.match(request, { ignoreSearch: true })
      if (cached) return cached
      try {
        const response = await fetch(request)
        if (response.ok && response.type === 'basic') {
          cache.put(request, response.clone()).catch(() => undefined)
        }
        return response
      } catch {
        const shell = await cache.match('./index.html')
        if (shell) return shell
        throw new Error('offline and not cached')
      }
    })(),
  )
})
