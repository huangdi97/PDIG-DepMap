// 主机执行面上的 conformance 门禁 —— 驱动真实 ArkTS runner 并核对结果。
//
// 这一层回答的问题：「ConformanceRunner 在**真实 ArkTS 运行时**下，
// 对这 57 个运行时无关用例算出的 actual，是否逐字节等于冻结 fixture 的 expected？」
//
// 它**不**回答的问题：「Harmony 端到端 91/91 达成了吗？」
//   @ohos 依赖的 28 个用例在主机上没有可用实现（本地单元测试对 @ohos.*
//   只提供不可调用的桩），它们依然是 BLOCKED_BY_RUNTIME。
//   设备执行面仍然没有建立。**主机执行面不替代设备执行面。**
//
// 为什么这不是"Node 复刻镜像"（§10 明确禁止的那种）：
//   被测代码是 ConformanceRunner 本身，由真 ArkTS 编译器产出；
//   测试只是把冻结 fixture 作为**数据**喂进去。
//   对照：tools/harmony/check-relations-semantics.mjs 是另写一份 Node 实现
//   去跑同一批 fixture —— 那种结果不能写进 conformance 矩阵，本门禁可以。
//
// 用法：
//   node tools/harmony/run-conformance-host.mjs            # 跑（需 DevEco）
//   node tools/harmony/run-conformance-host.mjs --no-build # 复用上次结果，只解析
//
// 无 PDIG_DEVECO_HOME 时：报 NOT_RUN 并 exit 0 ——
// 不让缺 SDK 的 runner 变成假失败（与其它 Harmony gate 口径一致）。
import { existsSync, readFileSync, writeFileSync, mkdirSync } from 'node:fs'
import { join, posix } from 'node:path'
import { homedir } from 'node:os'
import { execFileSync } from 'node:child_process'

const REPO = process.cwd()
const DEVECO = process.env.PDIG_DEVECO_HOME?.trim() || ''
const BUILD_ROOT = process.env.PDIG_HARMONY_BUILD_ROOT?.trim() ||
  posix.join(homedir().replace(/\\/g, '/'), 'pdig-harmony-build')
const RESULT = join(
  BUILD_ROOT, 'harmony', 'entry', '.test', 'default', 'intermediates',
  'test', 'coverage_data', 'test_result.txt',
)

// ---------------------------------------------------------------------------
// 期望账目
//
// 这三个数字分工不同，**不得互相替代**：
//   EXPECT_HOST_TOTAL   主机执行面上真实跑过的检查数（含元测试与域自检）
//   EXPECT_PASS         其中通过的检查数
//   CANONICAL_*         91 条 canonical 用例的口径 —— **这才是不能只报 67/67 的原因**
//
// 为什么加 CANONICAL_*：只报 "host PASS 91/91" 会让 91 这个分母消失。
// 一个装饰器人的读者会以为 Harmony 已经 91/91；真相是 87/91，
// 剩下 4 条按**性质**分开计（未移植 / 环境缺失 / 设备运行时），不得合并成一句"blocked"。
//
// 2026-09-18：20 条 parser 用例从「未移植」迁入「已执行」（ArkTS adapter 落地），
// 分母不动、分子从 65 → 85。被这次迁移暴露的一个真实缺陷记在 DECISION_LOG：
// decodeBase64 曾按完整 4 字符组分配输出长度，长度 mod 3 != 0 的文件尾部字节被
// 静默截断（EUR → EU / JPY → JP），且只错最后一行。
//
// 2026-09-19：2 条 GB18030 用例从「环境缺失」迁入「已执行」，分子 85 → 87，
// 环境桶归零。依据不是"补了解码器"这么一句话，而是：码表由 Node ICU 生成、
// 再由 CPython 内置 gb18030 codec 逐位独立复验（tools/encoding/ 下三个文件）。
// 2026-09-25（Harmony N3 连续推进轮）新增 data/ 持久化逻辑层主机测试
//   PersistenceHost.test.ets（14 条）：被测代码为平台侧真实 ArkTS 模块
//   （SchemaV3 / PayloadCodec / PayloadMigration / SourceInstance），期望值来自
//   冻结契约与 spec —— 因此 host checks 总数上升，**不改变** canonical 计数口径
//   （87）与三个阻塞桶（每桶 0/0/4）。计数口径 = 执行口径（每个 it() 一条）。
const PERSISTENCE_HOST_TEST_COUNT = 14 // 与 harmony/entry/src/test/PersistenceHost.test.ets 的 it() 数一一对应
const REPOSITORY_HOST_TEST_COUNT = 17 // 与 harmony/entry/src/test/RepositoryHost.test.ets 的 it() 数一一对应（round#3）
const IMPORT_PIPELINE_HOST_TEST_COUNT = 12 // 与 harmony/entry/src/test/ImportPipelineHost.test.ets 的 it() 数一一对应（round#4）
const EXPECT_HOST_TOTAL = 91 + PERSISTENCE_HOST_TEST_COUNT + REPOSITORY_HOST_TEST_COUNT + IMPORT_PIPELINE_HOST_TEST_COUNT // 87 canonical + 3 元测试 + 1 domain 自检 + 14 persistence + 17 repository + 12 import-pipeline
const EXPECT_PASS = 91 + PERSISTENCE_HOST_TEST_COUNT + REPOSITORY_HOST_TEST_COUNT + IMPORT_PIPELINE_HOST_TEST_COUNT
const EXPECT_FAIL = 0
const EXPECT_ERROR = 0
const CANONICAL_TOTAL = 91
const CANONICAL_EXECUTED = 87
const CANONICAL_IMPL_MISSING = 0
const CANONICAL_ENV_BLOCKED = 0
const CANONICAL_RUNTIME_BLOCKED = 4

if (!DEVECO || !existsSync(DEVECO)) {
  console.log('HARMONY_CONFORMANCE_HOST=NOT_RUN (PDIG_DEVECO_HOME 未设置)')
  console.log('  本门禁需要 DevEco 的 hvigor 来执行 ArkTS 本地单元测试。')
  process.exit(0)
}

// 内嵌 fixture 必须与冻结原件一致 —— 否则我们是在拿一份可能已漂移的数据做验证。
try {
  execFileSync(process.execPath, [join(REPO, 'tools/conformance/embed-fixtures.mjs'), '--check'],
    { cwd: REPO, stdio: 'pipe', encoding: 'utf8' })
} catch (e) {
  console.error('HARMONY_CONFORMANCE_HOST=FAIL (内嵌 fixture 已漂移)')
  process.stdout.write(e.stdout || '')
  process.stderr.write(e.stderr || '')
  process.exit(1)
}

if (!process.argv.includes('--no-build')) {
  console.log('[conformance-host] 执行 hvigor 本地单元测试 ...')
  try {
    execFileSync(process.execPath, [join(REPO, 'tools/harmony/build-ascii-mirror.mjs'), 'test'],
      { cwd: REPO, stdio: 'inherit', env: process.env })
  } catch (e) {
    console.error('HARMONY_CONFORMANCE_HOST=FAIL (测试任务未成功结束)')
    process.exit(1)
  }
}

if (!existsSync(RESULT)) {
  console.error('HARMONY_CONFORMANCE_HOST=FAIL (找不到测试结果:', RESULT, ')')
  process.exit(1)
}
const text = readFileSync(RESULT, 'utf8')

// 逐用例结果：test=<name> 后跟 result=Success|Failure|Error。
// 注意用例名有两种形态 ——
//   * conformance 用例：<category>/<caseId>
//   * 元测试：裸名字（environmentPrecondition 等）
// 早期版本只匹配带 `/` 的那种，把 3 条元测试漏掉了（57 而非 60），
// 于是"总数为 60"的断言永远不可能成立。计数口径必须与执行口径一致。
const cases = []
const lines = text.split('\n')
for (let i = 0; i < lines.length; i++) {
  const m = /^test=(.+)$/.exec(lines[i].trim())
  if (!m) continue
  let status = 'UNKNOWN'
  for (let j = i + 1; j < Math.min(i + 8, lines.length); j++) {
    const r = /^result=(\w+)$/.exec(lines[j].trim())
    if (r) { status = r[1]; break }
  }
  cases.push({ id: m[1], status })
}

const summary = /^Tests run:\s*(\d+),\s*Failure:\s*(\d+),\s*Error:\s*(\d+),\s*Pass:\s*(\d+)/m.exec(text)
if (!summary) {
  console.error('HARMONY_CONFORMANCE_HOST=FAIL (结果文件里没有汇总行)')
  process.exit(1)
}
const run = Number(summary[1])
const fail = Number(summary[2])
const err = Number(summary[3])
const pass = Number(summary[4])

console.log('')
console.log('[conformance-host] 执行面 : hvigor 本地单元测试（ArkTS，无设备）')
console.log('[conformance-host] 用例   : ' + cases.length + ' 条（含 3 条元测试）')
console.log('[conformance-host] 汇总   : run=' + run + ' pass=' + pass +
  ' fail=' + fail + ' error=' + err)

const bad = cases.filter((c) => c.status !== 'Success')
if (bad.length > 0) {
  console.log('')
  console.log('[conformance-host] 未通过的用例:')
  for (const b of bad) console.log('  ' + b.status + '  ' + b.id)
}

// 机器可读报告（N5 跨平台差分要读它）。
// 只写**真实执行过**的用例：没跑的用例在这里出现就是伪造证据。
// Harmony 的 actual 不落盘（ArkTS 主机测试不保证可写文件），
// 因此本报告只带 verdict，字节级 actual 比对留给 Android × iOS。
function writeReport(canonCases) {
  const results = {}
  for (const c of canonCases) {
    const slash = c.id.indexOf('/')
    const category = slash > 0 ? c.id.slice(0, slash) : ''
    const caseId = slash > 0 ? c.id.slice(slash + 1) : c.id
    results[caseId] = {
      status: (c.status === 'Success') ? 'PASS' : 'FAIL',
      category,
    }
  }
  const report = {
    platform: 'harmony',
    specVersion: '1.0.0',
    summary: {
      pass: canonCases.filter((c) => c.status === 'Success').length,
      fail: canonCases.filter((c) => c.status !== 'Success').length,
      notImplemented: CANONICAL_IMPL_MISSING,
      envBlocked: CANONICAL_ENV_BLOCKED,
      runtimeBlocked: CANONICAL_RUNTIME_BLOCKED,
      total: CANONICAL_TOTAL,
    },
    results,
  }
  const dir = join(REPO, 'conformance', 'reports')
  mkdirSync(dir, { recursive: true })
  writeFileSync(join(dir, 'harmony.json'), JSON.stringify(report, null, 2) + '\n', 'utf8')
}

const ok = run === EXPECT_HOST_TOTAL && pass === EXPECT_PASS &&
  fail === EXPECT_FAIL && err === EXPECT_ERROR && bad.length === 0

// canonical 口径从报告文件里独立核对（不由 host 检查数反推）。
/**
 * canonical 口径：独立再看一遍，不能由"检查数"代替。
 *
 *为什么呢：只报 "host PASS 89/89" 会让 91 这个分母消失，
 * 最有资格的判据就是测试里的 `accountingSplitMatchesSection11`（它直接读 report 对象）。
 * 这里只做两件能独立做的事：
 *   1. 数出本轮**真的执行过**的 canonical 用例数（test_result.txt 里
 *      形如 `<category>/<caseId>` 的测试名；元测试是裸名，不含 `/`，天然被排除）；
 *   2. 把它与本文件声明的常量核对，并把三个桶按同一常量打印出来。
 * 于是"91 这个分母不被 89/89 吞掉"这件事有两个独立的见证：测试 + 本门禁。
 */
function countExecutedCanonical(text) {
  const lines = text.split('\n');
  let executed = 0;
  let failed = 0;
  for (let i = 0; i < lines.length; i++) {
    const m = /^test=(.+)$/.exec(lines[i].trim());
    if (!m) continue;
    if (!m[1].includes('/')) continue; // 元测试 / 域自检
    let status = '';
    for (let j = i + 1; j < Math.min(i + 8, lines.length); j++) {
      const r = /^result=(\w+)$/.exec(lines[j].trim());
      if (r) { status = r[1]; break; }
    }
    executed++;
    if (status === 'Failure' || status === 'Error') failed++;
  }
  return { executed: executed, failed: failed };
}

const canon = countExecutedCanonical(text);
const canonOk = canon.executed === CANONICAL_EXECUTED && canon.failed === 0 &&
  CANONICAL_EXECUTED + CANONICAL_IMPL_MISSING + CANONICAL_ENV_BLOCKED +
  CANONICAL_RUNTIME_BLOCKED === CANONICAL_TOTAL;

console.log('');
console.log('[ canonical ] HARMONY_TOTAL_CANONICAL    = ' + CANONICAL_TOTAL);
console.log('[ canonical ] HARMONY_HOST_EXECUTED      = ' + canon.executed +
  '   (fail=' + canon.failed + ')');
console.log('[ canonical ] HARMONY_HOST_IMPL_MISSING  = ' + CANONICAL_IMPL_MISSING +
  '   (纯逻辑未移植，不需要设备)');
console.log('[ canonical ] HARMONY_ENV_BLOCKED        = ' + CANONICAL_ENV_BLOCKED +
  '   (GB18030 字符集转换)');
console.log('[ canonical ] HARMONY_DEVICE_BLOCKED     = ' + CANONICAL_RUNTIME_BLOCKED +
  '   (Argon2id 原生 / ArkData)');
console.log('[ canonical ] 三桶计数由测试内 accountingSplitMatchesSection11 独立核对');
console.log('');
console.log('HARMONY_CONFORMANCE_HOST=' + (ok && canonOk ? 'PASS' : 'FAIL'));
console.log('HARMONY_HOST_PASS=' + CANONICAL_EXECUTED + '/' + CANONICAL_TOTAL);
const canonCases = cases.filter((c) => c.id.includes('/'));

if (ok && canonOk) {
  writeReport(canonCases);
  console.log('  → ' + CANONICAL_EXECUTED + ' 条 canonical 用例在真实 ArkTS 运行时下逐字节复现 expected');
  console.log('  → 余下 ' + (CANONICAL_TOTAL - CANONICAL_EXECUTED) + ' 条按性质分三桶，'
    + '明细见 HARMONY_REMAINING_6_AUDIT.md');
}
process.exit(ok && canonOk ? 0 : 1);
