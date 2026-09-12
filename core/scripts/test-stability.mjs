#!/usr/bin/env node
/**
 * Engineering Baseline V1 — Flaky 检测：连续运行完整测试套件 3 轮。
 * 跨平台（Windows/macOS/Linux）；任一轮失败即 exit 1。
 * 政策：禁止用 retry 掩盖 flaky（docs/FLAKY_TEST_REPORT.md）。
 */
import { spawnSync } from 'node:child_process'

const RUNS = 3
const npxCmd = process.platform === 'win32' ? 'npx.cmd' : 'npx'
const failures = []

for (let i = 1; i <= RUNS; i++) {
  console.log(`\n=== stability run ${i}/${RUNS} ===`)
  const r = spawnSync(npxCmd, ['vitest', 'run'], {
    stdio: ['ignore', 'pipe', 'inherit'],
    encoding: 'utf8',
    // Windows 下 .cmd 启动器必须显式 shell（Node 安全策略，spawn 不再自动 shell）
    shell: process.platform === 'win32',
    env: { ...process.env, NODE_NO_WARNINGS: '1' },
  })
  const out = String(r.stdout ?? '')
  const summary = [...out.matchAll(/Tests\s+(\d+)\s+passed\((?:\s*)\((\d+)\)/g)]
  const summaryLine = out
    .split(/\r?\n/)
    .filter((l) => /Test Files|Tests\s+\d+/.test(l))
    .slice(-2)
  for (const l of summaryLine) console.log('  ' + l.trim())
  if (r.status !== 0) {
    failures.push(i)
    console.log(`  run ${i}: FAILED (exit ${r.status})`)
  }
}

if (failures.length > 0) {
  console.error(`\nstability gate FAIL: runs ${failures.join(', ')} red`)
  process.exit(1)
}
console.log(`\nstability gate PASS (${RUNS} consecutive green runs)`)
