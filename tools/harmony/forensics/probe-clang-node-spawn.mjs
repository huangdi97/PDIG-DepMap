// 环境探针：把结果写入仓库内文件（避免经过宿主管道导致的读取限制）。
import { execFileSync } from 'node:child_process'
import { writeFileSync, mkdirSync } from 'node:fs'
import { join } from 'node:path'
import { homedir } from 'node:os'

const DEVECO = 'D:\\Code\\Harmony\\DevEco Studio'
const OHOS = join(DEVECO, 'sdk', 'default', 'openharmony')
const CLANG = join(OHOS, 'native', 'llvm', 'bin', 'clang.exe')
const SYSROOT = join(OHOS, 'native', 'sysroot')
const ARGON2 = 'E:\\AI\\号卡管理\\third_party\\argon2'
const WORK = join(homedir(), 'pdig-harmony-build', 'clang-envprobe')
mkdirSync(WORK, { recursive: true })

const args = [
  '--target=aarch64-linux-ohos', `--sysroot=${SYSROOT}`,
  '-fPIC', '-O2', '-fvisibility=hidden', '-DA2_VISCTL=1',
  `-I${join(ARGON2, 'include')}`, `-I${join(ARGON2, 'src')}`,
  '-std=c99', '-c', join(ARGON2, 'src', 'argon2.c'),
  '-o', join(WORK, 'a.o'),
]

const lines = []
function attempt(label, opts) {
  try {
    execFileSync(CLANG, args, { stdio: 'pipe', encoding: 'utf8', timeout: 120000, ...opts })
    lines.push(`OK    ${label}`)
  } catch (e) {
    const s = (e.stderr || '').toString()
    const kind = /crash backtrace/.test(s) ? 'CRASH' : (s.split('\n')[0] || 'ERROR').slice(0, 100)
    lines.push(`${kind} ${label}`)
  }
}

attempt('baseline (inherit process.env)', {})
attempt('minimal env (PATH/SystemRoot/TEMP)', {
  env: { PATH: process.env.PATH, SystemRoot: process.env.SystemRoot, TEMP: process.env.TEMP, TMP: process.env.TMP },
})
attempt('windowsHide + no shell', { windowsHide: true })

lines.push('--- interesting env vars in this node process ---')
const interesting = Object.keys(process.env).filter((k) =>
  /WBT|CODEBUDDY|WORKBUDDY|NODE|INJECT|HOOK|SANDBOX|SHIM|MTool|SAFE|APPDATA|LOCALAPPDATA/i.test(k))
for (const k of interesting) {
  const v = String(process.env[k] ?? '')
  lines.push(`${k}=${v.length > 160 ? v.slice(0, 160) + '...' : v}`)
}
if (interesting.length === 0) lines.push('(none matched)')

const target = join(ARGON2, '..', '..', '.probe-clang-out.txt')
writeFileSync(target, lines.join('\n') + '\n', 'utf8')
console.log('written:', target)
