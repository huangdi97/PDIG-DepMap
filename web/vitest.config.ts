import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'
import { defineConfig } from 'vitest/config'

const here = dirname(fileURLToPath(import.meta.url))

export default defineConfig({
  resolve: {
    alias: [
      { find: '@core/', replacement: `${resolve(here, '../core/src')}/` },
      { find: 'node:crypto', replacement: resolve(here, 'src/shims/node-crypto.ts') },
    ],
  },
  test: {
    environment: 'node',
    include: ['tests/unit/**/*.test.ts'],
    setupFiles: [resolve(here, 'tests/unit/setup.ts')],
    testTimeout: 60_000,
  },
})
