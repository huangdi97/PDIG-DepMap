#!/usr/bin/env node
/**
 * collect-evidence —— 把本地/CI 的视觉矩阵整理成可归档的证据清单。
 *
 * 只写 artifacts/（CI artifact），不改仓库内受版本管理的文件。
 * 明确标注：截图是**候选证据**，不是视觉回归基线；人眼验收为 NOT_RUN。
 */
import { existsSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const ROOT = resolve(HERE, '..')
const MATRIX = resolve(ROOT, 'artifacts/visual/visual-matrix.json')
const OUT = resolve(ROOT, 'artifacts/visual/manifest.json')

if (!existsSync(MATRIX)) {
  console.error(`collect-evidence: missing ${MATRIX}; run the visual matrix first`)
  process.exit(1)
}

const matrix = JSON.parse(readFileSync(MATRIX, 'utf8'))
const manifest = {
  sourceHead: process.env['GITHUB_SHA'] ?? 'local-working-tree',
  generatedAt: new Date().toISOString(),
  nodeVersion: process.version,
  playwrightVersion: '1.63.0',
  browser: 'chromium (Ubuntu CI: pinned by Playwright 1.63.0)',
  fonts: process.env['CI'] ? 'Noto CJK (CI image)' : 'system CJK (local)',
  dataMode: 'demo (?demo=1 synthetic sample reality)',
  testMode: process.env['CI'] ? 'CI ubuntu-latest' : 'local',
  visualRegression: 'NOT_RUN',
  humanVisualReview: 'NOT_RUN',
  shotCount: matrix.shots.length,
  shots: matrix.shots,
}

writeFileSync(OUT, `${JSON.stringify(manifest, null, 2)}\n`, 'utf8')
console.log(
  `collect-evidence: wrote ${OUT} (${manifest.shotCount} shots, head ${manifest.sourceHead.slice(0, 8)})`,
)
