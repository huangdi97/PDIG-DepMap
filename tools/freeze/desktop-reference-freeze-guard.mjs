// DESKTOP_REFERENCE_FREEZE_GUARD —— 只读自动 Gate（任务书 §48）。
// 验证 DESKTOP_REFERENCE_FREEZE_MANIFEST.json 中 12 张冻结截图 SHA256 与磁盘文件逐项一致。
//
// 用法: node tools/freeze/desktop-reference-freeze-guard.mjs
// PASS → exit 0；任何 mismatch / 缺失 → exit 1（列出差异）。
// 禁止自动更新 baseline：冻结包漂移必须 STOP + 调查（本轮无 Reference Revision 权限）。
import { createHash } from 'node:crypto'
import { readFileSync, existsSync } from 'node:fs'
import { join } from 'node:path'

const ROOT = process.cwd()
const manifestPath = join(ROOT, 'DESKTOP_REFERENCE_FREEZE_MANIFEST.json')

function sha256File(abs) {
  return createHash('sha256').update(readFileSync(abs)).digest('hex')
}

let manifest
try {
  manifest = JSON.parse(readFileSync(manifestPath, 'utf8'))
} catch (err) {
  console.error(`FAIL: cannot read manifest at ${manifestPath}: ${err.message}`)
  process.exit(1)
}

if (!Array.isArray(manifest.screenshots) || manifest.screenshots.length < 12) {
  console.error(`FAIL: manifest.screenshots missing/empty (${manifest.screenshots?.length})`)
  process.exit(1)
}

let mismatches = 0
let checked = 0
for (const shot of manifest.screenshots) {
  const rel = join('artifacts/runtime-evidence/2026-10-02-ui-vnext-phase1f-hf2-final', shot.file)
  const abs = join(ROOT, rel)
  if (!existsSync(abs)) {
    console.error(`MISSING: ${rel} (${shot.screen})`)
    mismatches++
    continue
  }
  checked++
  const actual = sha256File(abs)
  if (actual !== shot.sha256) {
    console.error(`DRIFT: ${shot.screen} ${rel}\n  manifest=${shot.sha256}\n  actual =${actual}`)
    mismatches++
  }
}

if (mismatches > 0) {
  console.error(`\nDESKTOP_REFERENCE_FREEZE_GUARD = FAIL (${mismatches} mismatch of ${manifest.screenshots.length})`)
  console.error('冻结包漂移 → STOP + investigate；不得自动更新 baseline。')
  process.exit(1)
}
console.log(`DESKTOP_REFERENCE_FREEZE_GUARD = PASS (${checked}/${manifest.screenshots.length} SHA256 verified)`)
