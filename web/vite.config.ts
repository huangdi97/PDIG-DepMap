import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const here = dirname(fileURLToPath(import.meta.url))
const coreSrc = resolve(here, '../core/src')

/**
 * Web build 配置。
 *
 * - base './'：产物部署在 /app/ 子路径下（GitHub Pages），相对路径可移植。
 * - Core 源码直接以 TS 源码形式参与构建（不复制、不 fork），保证 Web 不会
 *   与 Canonical/Core 漂移。浏览器不提供 Node 内置模块，因此 `node:crypto`
 *   被映射到 web/src/shims/node-crypto.ts（sha256/hmac-sha256，纯 TS 实现）。
 * - 显式 alias `@core/*` 让 Worker 与 UI 都只引用同一份 Core。
 */
export default defineConfig({
  base: './',
  plugins: [react()],
  resolve: {
    // Core 源码里 import 'hash-wasm'；CI 只安装 web/node_modules。
    // dedupe 让 Vite 从 web 根解析该裸包，而不是从 importing 文件所在目录（core/src）向上找。
    dedupe: ['hash-wasm'],
    alias: [
      { find: '@core/', replacement: `${coreSrc}/` },
      { find: 'node:crypto', replacement: resolve(here, 'src/shims/node-crypto.ts') },
    ],
  },
  build: {
    outDir: resolve(here, '../website/app'),
    emptyOutDir: true,
    target: 'es2022',
    sourcemap: false,
    assetsInlineLimit: 0,
    rollupOptions: {
      output: {
        // 稳定文件名契约：便于 check:web-generated 与 SW 增量缓存比对。
        entryFileNames: 'assets/[name]-[hash].js',
        chunkFileNames: 'assets/[name]-[hash].js',
        assetFileNames: 'assets/[name]-[hash][extname]',
      },
    },
  },
  worker: {
    format: 'es',
  },
  server: {
    port: 5173,
    strictPort: true,
  },
})
