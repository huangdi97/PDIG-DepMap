import { defineConfig } from 'vitest/config'

export default defineConfig({
  test: {
    include: ['tests/**/*.test.ts'],
    pool: 'forks',
    environment: 'node',
    // node:sqlite is experimental in Node 22; silence its warning noise in test output
    env: { NODE_NO_WARNINGS: '1' },
  },
})
