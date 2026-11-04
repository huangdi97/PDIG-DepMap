import 'fake-indexeddb/auto'

// WebCrypto / TextEncoder 在 Node 22 已存在；这里只补浏览器 API 的最小缺失。
if (typeof globalThis.crypto?.subtle === 'undefined') {
  throw new Error('Node 22+ with WebCrypto is required for web unit tests')
}
