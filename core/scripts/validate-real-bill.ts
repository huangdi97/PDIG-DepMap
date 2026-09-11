/**
 * 真实账单本地验证 CLI（GOAL §23 REAL DATA 流程）
 *
 * 用法：
 *   cd core
 *   node --experimental-strip-types scripts/validate-real-bill.ts --file ../local_private/bill.csv
 *
 * 只读本地文件，不联网。真实账单文件必须放在 local_private/（已 gitignore）。
 * 真实账单内容绝不打印到日志；输出只含商户名/统计/影响清单（ Correctness Gate 人工审计用）。
 */
import { readFileSync, existsSync, mkdirSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { NodeSqliteDriver } from '../src/db/node-driver.ts'
import { migrate } from '../src/schema/migrations.ts'
import { NodeRepository } from '../src/repositories/node-repository.ts'
import { DependencyRepository } from '../src/repositories/dependency-repository.ts'
import { DependencyGroupRepository } from '../src/repositories/group-repository.ts'
import { ImportFlow } from '../src/services/import-pipeline.ts'
import { ConfirmationService } from '../src/services/confirmation-service.ts'
import { simulateDisable } from '../src/impact/kernel.ts'

const args = process.argv.slice(2)
const fileIdx = args.indexOf('--file')
if (fileIdx === -1 || !args[fileIdx + 1]) {
  console.error('usage: --file <path-to-csv>')
  process.exit(1)
}
const billPath = args[fileIdx + 1]!
if (!existsSync(billPath)) {
  console.error(`file not found: ${billPath}`)
  process.exit(1)
}

const raw = new Uint8Array(readFileSync(billPath))
const dataDir = join(dirname(fileURLToPath(import.meta.url)), '..', '.local-cli-data')
mkdirSync(dataDir, { recursive: true })

const driver = new NodeSqliteDriver(join(dataDir, 'validate.db'))
driver.open()
migrate(driver)

const nodes = new NodeRepository(driver)
const deps = new DependencyRepository(driver)
const groups = new DependencyGroupRepository(driver)
const confirm = new ConfirmationService(driver)
const flow = new ImportFlow(driver)

const begin = flow.begin(raw)
console.log(`解析完成：${begin.rawCount} 行，${begin.errors.length} 个坏行`)

// CLI 简化：无候选商户自动创建 service 节点（等效“用户确认创建”，人工审计时复核）
const serviceNameToId = new Map<string, string>()
for (const c of begin.candidates) {
  if (c.resolution === 'auto' && c.resolvedNodeId) continue
  const existing = nodes.findByName(c.merchantRaw)
  if (existing.length === 1) {
    flow.resolveMerchant(c.merchantRaw, existing[0]!.id)
  } else {
    const n = nodes.create({ kind: 'service', name: c.merchantRaw })
    serviceNameToId.set(c.merchantRaw, n.id)
    flow.resolveMerchant(c.merchantRaw, n.id)
  }
}

const outcome = flow.finalize()
console.log(`新观测 ${outcome.newUniqueCount}，重复 ${outcome.duplicateCount}`)
console.log(`识别周期项：${outcome.recurrences.length}`)
for (const r of outcome.recurrences) {
  console.log(`  - ${r.merchantRaw} ${r.period} confidence=${r.confidence} occurrences=${r.occurrences} typicalAmount=${r.typicalAmount}`)
}
console.log(`生成/更新建议：${outcome.proposalKeys.length}`)

// CLI 模式：全部接受（unknown criticality）—— 真实产品中由用户逐条确认
for (const k of outcome.proposalKeys) confirm.acceptProposal(k)
const gpKeys = confirm.detectGroupProposals()
for (const g of gpKeys) confirm.acceptGroupProposal(g)

const graph = {
  dependencies: deps.listActive(),
  groups: groups.listAllActive(),
  nodeNames: Object.fromEntries(nodes.list().map(n => [n.id, n.name]))
}

// 对每张已确认卡做模拟注销
const cards = nodes.list({ kind: 'payment_instrument' })
for (const card of cards) {
  const result = simulateDisable(graph, card.id)
  if (result.targets.length === 0) continue
  console.log(`\n=== 模拟注销：${graph.nodeNames[card.id] ?? card.id} ===`)
  for (const item of result.checklist) {
    console.log(`  [${item.level}] ${item.title}`)
    console.log(`      ${item.detail}`)
  }
}
console.log('\nCorrectness Gate：请逐条人工核实上方 must_change 是否为真实依赖（目标 false positive = 0）。')
driver.close()
