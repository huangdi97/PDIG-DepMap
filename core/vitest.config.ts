import { defineConfig } from 'vitest/config'

export default defineConfig({
  test: {
    include: ['tests/**/*.test.ts'],
    pool: 'forks',
    environment: 'node',
    // node:sqlite is experimental in Node 22; silence its warning noise in test output
    env: { NODE_NO_WARNINGS: '1' },
    coverage: {
      provider: 'v8',
      // src 口径：脚本与纯类型文件不计入（driver.ts / interfaces.ts 仅声明，无可执行语句）
      include: ['src/**/*.ts'],
      exclude: ['src/adapters/interfaces.ts', 'src/db/driver.ts'],
      reporter: ['text', 'html'],
    },
  },
})
