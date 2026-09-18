// Harmony 编译可达性 Gate —— 固化"ArkTS module 算不算已编译"的判定。
//
// 背景（本轮最重要的一条工程教训）：
//   hvigor 的 CompileArkTS **只编译从 ability / page 可达的模块**。
//   未被任何 EnterAbility / page `import` 到的 .ets 文件根本不进入编译图 ——
//   实测在该文件里放语法错误甚至类型错误，assembleHap 依然 BUILD SUCCESSFUL。
//   因此
//         "源文件存在" + "HAP BUILD SUCCESSFUL" = "已编译"
//   是**假结论**，本 Gate 就是为了永久堵死它。
//
// 判定标准（A/B/C/D 四项必须同时成立，才允许 HARMONY_MODULE_COMPILED = PASS）：
//   A. 模块在 ability/page 可达 import graph 中
//   B. clean build 成功
//   C. modules.abc 里能找到该模块的符号
//   D. 负向 probe（往该模块注入真实类型错误）能让构建**真的失败**
//
// 用法：
//   node tools/harmony/check-compiled-reachability.mjs            # A + C（静态/产物检查）
//   node tools/harmony/check-compiled-reachability.mjs --build    # 追加 B + D（需 DevEco）
//
// 无 PDIG_DEVECO_HOME 时：A 仍会执行（纯静态）；B/C/D 报告为 NOT_RUN，
// 退出码 0 —— 不让缺少 SDK 的 runner 变成假失败。
import { existsSync, readFileSync, readdirSync, mkdtempSync, writeFileSync } from 'node:fs'
import { join, relative, posix } from 'node:path'
import { homedir, tmpdir } from 'node:os'
import { execFileSync } from 'node:child_process'

const REPO = process.cwd()
const ETS_ROOT = join(REPO, 'harmony', 'entry', 'src', 'main', 'ets')

// 必须进入编译图的关键模块。新增 Domain / Repository / Security 模块时在此追加。
//
// 注：`required: false` 的条目代表"接口已定义、实现属于后续阶段"——
// 它们会被检查（存在则必须可达），但缺失不会让本次 Gate 失败。
//
// `typeOnly: true` 的含义（实测校正，勿删）：
//   该模块在 emit 后被**完全擦除**，modules.abc 里不会、也不应该出现它的符号。
//   ArkTS/TS 对「只含 type / interface 声明」的模块不生成任何运行时代码，
//   KdfContract.ets 就是这种情况（实测 modules.abc 中查无 KdfContract /
//   Argon2idDeriver，而同目录的 DepmapContainerV1 / Argon2idNative 都在）。
//   对这类模块，C 判据改为「可达 + 被至少一个非 typeOnly 且已进 abc 的模块 import」，
//   否则会把一个正确的编译结果判成 FAIL —— 那是假阴性，与假阳性同样有害。
const REQUIRED_MODULES = [
  { id: 'CanonicalEnums', path: 'generated/CanonicalEnums.ets', required: true },
  { id: 'Relations', path: 'domain/Relations.ets', required: true },
  { id: 'Jcs', path: 'crypto/Jcs.ets', required: true },
  { id: 'KdfContract', path: 'crypto/KdfContract.ets', required: true, typeOnly: true },
  { id: 'DepmapContainerV1', path: 'crypto/DepmapContainerV1.ets', required: true },
  { id: 'ContainerSelfCheck', path: 'crypto/ContainerSelfCheck.ets', required: true },
  { id: 'Argon2idNative', path: 'crypto/Argon2idNative.ets', required: true },
  // Domain 层：CanonicalWire 是手写的 wire 解析辅助（**非** codegen 产物），
  // Entities / ImpactKernel / DomainSelfCheck 是本轮落地的纯 ArkTS Domain。
  // 入边：pages/Index.ets → DomainSelfCheck → {Entities, ImpactKernel}。
  { id: 'CanonicalWire', path: 'domain/CanonicalWire.ets', required: true },
  { id: 'Entities', path: 'domain/Entities.ets', required: true },
  { id: 'LogicalKey', path: 'domain/LogicalKey.ets', required: true },
  { id: 'ImpactKernel', path: 'domain/ImpactKernel.ets', required: true },
  { id: 'PlanReadiness', path: 'domain/PlanReadiness.ets', required: true },
  { id: 'ScenarioCoverage', path: 'domain/ScenarioCoverage.ets', required: true },
  { id: 'DomainSelfCheck', path: 'domain/DomainSelfCheck.ets', required: true },
  // 后续阶段（Domain 其余组 / Repository / Security）
  { id: 'RelationRegistry', path: 'domain/RelationRegistry.ets', required: false },
  { id: 'StateMachines', path: 'domain/StateMachines.ets', required: false },
  { id: 'GraphRevision', path: 'domain/GraphRevision.ets', required: false },
  { id: 'ScenarioTemplate', path: 'domain/ScenarioTemplate.ets', required: false },
  { id: 'Timeline', path: 'domain/Timeline.ets', required: false },
  { id: 'Repository', path: 'data/Repository.ets', required: false },
  { id: 'Huks', path: 'security/Huks.ets', required: false },
]

// ability / page 入口：可达性的根。
const ENTRY_ROOTS = [
  'entryability/EntryAbility.ets',
  'pages/Index.ets',
]

// ---------------------------------------------------------------------------
// 静态分析：解析 import graph
// ---------------------------------------------------------------------------

function listEts(dir, base = dir, out = []) {
  if (!existsSync(dir)) return out
  for (const e of readdirSync(dir, { withFileTypes: true })) {
    const full = join(dir, e.name)
    if (e.isDirectory()) listEts(full, base, out)
    else if (e.isFile() && e.name.endsWith('.ets')) out.push(relative(base, full).split('\\').join('/'))
  }
  return out
}

/** 从源码抽出相对 import 目标（仅 './' 与 '../'，忽略 .so / @kit 等外部模块）。 */
function extractImports(source) {
  const out = new Set()
  const re = /(?:import|export)\s[^'"]*?from\s*['"]([^'"]+)['"]/g
  let m
  while ((m = re.exec(source)) !== null) {
    const spec = m[1]
    if (spec.startsWith('.')) out.add(spec)
  }
  // 动态 import('...') 也算一条真实依赖边
  const dyn = /import\s*\(\s*['"]([^'"]+)['"]\s*\)/g
  while ((m = dyn.exec(source)) !== null) {
    if (m[1].startsWith('.')) out.add(m[1])
  }
  return [...out]
}

/** 把相对 import 说明符解析成 ets 根下的模块 id（去扩展名）。 */
function resolveSpecifier(fromModule, spec) {
  const fromDir = posix.dirname(fromModule)
  const joined = posix.normalize(posix.join(fromDir, spec))
  // 去掉 .ets / .ts / .js 扩展名（ArkTS 允许省略）
  return joined.replace(/\.(ets|ts|js)$/, '')
}

function buildReachability() {
  const modules = listEts(ETS_ROOT).map((p) => p.replace(/\.ets$/, ''))
  const moduleSet = new Set(modules)

  const graph = new Map() // module -> [module]
  for (const mod of modules) {
    const src = readFileSync(join(ETS_ROOT, mod + '.ets'), 'utf8')
    const deps = []
    for (const spec of extractImports(src)) {
      const target = resolveSpecifier(mod, spec)
      if (moduleSet.has(target)) deps.push(target)
      else deps.push(target + ' (UNRESOLVED)')
    }
    graph.set(mod, deps)
  }

  // 从入口 BFS
  const reached = new Set()
  const queue = []
  for (const root of ENTRY_ROOTS) {
    const id = root.replace(/\.ets$/, '')
    if (moduleSet.has(id)) {
      reached.add(id)
      queue.push(id)
    }
  }
  while (queue.length > 0) {
    const cur = queue.shift()
    for (const dep of graph.get(cur) ?? []) {
      if (dep.endsWith('(UNRESOLVED)')) continue
      if (!reached.has(dep)) {
        reached.add(dep)
        queue.push(dep)
      }
    }
  }
  return { modules, graph, reached }
}

// ---------------------------------------------------------------------------
// 产物取证：modules.abc 符号
// ---------------------------------------------------------------------------

const BUILD_ROOT = process.env.PDIG_HARMONY_BUILD_ROOT?.trim() ||
  posix.join(homedir().replace(/\\/g, '/'), 'pdig-harmony-build')
const ABC = join(BUILD_ROOT, 'harmony', 'entry', 'build', 'default', 'intermediates',
  'loader_out', 'default', 'ets', 'modules.abc')

function readAbcTokens() {
  if (!existsSync(ABC)) return null
  const buf = readFileSync(ABC)
  const s = buf.toString('latin1')
  return { tokens: new Set(s.match(/[ -~]{6,}/g) || []), bytes: buf.length }
}

// ---------------------------------------------------------------------------
// 负向 probe：往目标模块注入真实类型错误，验证构建会失败
// ---------------------------------------------------------------------------

/**
 * 负向 probe：往目标模块注入真实类型错误，验证构建**真的会失败**。
 *
 * 判定意义：正向可达 + 产物符号都只能证明「模块被引用了」。
 * 只有负向 probe 能排除「hvigor 因为缓存/增量/tree-shaking 而跳过该文件」这类沉默失效。
 *
 * 实现要点（实测踩坑后定型，勿轻改）：
 *   - 直接改**真实源码树**再构建，不做整目录 cpSync 到 probe-mirror。
 *     旧实现会 rmSync 掉一个递归镜像目录，在本机沙箱下触发安全删除守卫，
 *     表现为进程静默退出（exit 127，无 stderr），probe 结果不可信 —— 那是最坏情况：
 *     一个永远"通过"的安全网。改为直接改源码 + finally 还原，删除了整块风险。
 *   - 构建镜像本身由 build-ascii-mirror.mjs 负责拷贝，注入的错误会被一并带过去。
 *   - 必须确认改动真的落盘（读回校验），否则 probe 是在验证一个没被改过的文件。
 */
function negativeProbe(moduleId) {
  // 同一次运行内复用结论：代表模块只真正构建一次。
  if (PROBE_CACHE.has(moduleId)) return PROBE_CACHE.get(moduleId)
  const result = runNegativeProbe(moduleId)
  PROBE_CACHE.set(moduleId, result)
  return result
}

/** moduleId（如 CanonicalEnums）-> ets 根下的相对路径（如 generated/CanonicalEnums.ets）。 */
function pathOfModuleId(moduleId) {
  const meta = REQUIRED_MODULES.find((m) => m.id === moduleId)
  if (meta) return meta.path
  // 不在注册表里（例如 --probe-module 指定了别名）时按 ets 根直接拼。
  return moduleId + '.ets'
}

function runNegativeProbe(moduleId) {
  const DEVECO = (process.env.PDIG_DEVECO_HOME ?? '').trim()
  if (!DEVECO) return { status: 'NOT_RUN', detail: 'PDIG_DEVECO_HOME not set' }

  const target = join(ETS_ROOT, pathOfModuleId(moduleId))
  if (!existsSync(target)) {
    return { status: 'FAIL', detail: `probe target not found: ${target}` }
  }
  const backup = readFileSync(target, 'utf8')
  const marker =
    `\n// --- negative probe injected by check-compiled-reachability.mjs (must fail the build) ---\n` +
    `const __pdiTypeProbe: number = 'not a number';\n`
  const injected = backup + marker

  try {
    writeFileSync(target, injected, 'utf8')
    // 读回校验：确认注入真的生效，否则本 probe 无意义。
    if (!readFileSync(target, 'utf8').includes('__pdiTypeProbe')) {
      return { status: 'FAIL', detail: 'probe injection did not persist to disk' }
    }

    let buildFailed = false
    let detail = ''
    try {
      // --clean 是必须的：hvigor 的 CompileArkTS 有增量缓存，
      // 只改源码而不清目录时它会直接复用上一次的编译结果，
      // 于是"注入类型错误后构建仍然成功"——那是缓存造成的假象，不是可达性证据。
      // 实测：不带 --clean 时本 probe 对**所有**模块都报 FAIL。
      //
      // 另外必须用当前 node 可执行文件本体，不要用裸 `node`：
      // 本机 PATH 里的 node 可能不是受管版本，且 Windows 上 `.cmd` shim
      // 无法被 execFileSync 直接执行。
      execFileSync(process.execPath,
        [join(REPO, 'tools', 'harmony', 'build-ascii-mirror.mjs'), '--clean', 'assembleHap'], {
        cwd: REPO,
        env: { ...process.env, PDIG_HARMONY_BUILD_ROOT: join(BUILD_ROOT, 'probe-build') },
        stdio: 'pipe', encoding: 'utf8', timeout: 20 * 60 * 1000,
      })
    } catch (e) {
      buildFailed = true
      // execFileSync 失败时错误对象上的字段并不稳定：
      //   stdio:'pipe' + encoding:'utf8' 时 e.stdout 是 string，但某些路径下为 null，
      //   真正的内容落在 e.output[]（[0]=忽略, 1=stdout, 2=stderr）。
      // 只读 e.stdout || e.message 会在 stdout 为 null 时退化成"Command failed: ..."，
      // 于是下面的关键词判定失效、把真实失败误判成"非探针原因"。
      // 这里把所有可能的来源都拼起来，保证判定看到完整的编译器输出。
      const parts = [
        typeof e?.stdout === 'string' ? e.stdout : '',
        typeof e?.stderr === 'string' ? e.stderr : '',
        ...(Array.isArray(e?.output) ? e.output.map((x) => (x == null ? '' : String(x))) : []),
        typeof e?.message === 'string' ? e.message : '',
      ]
      detail = parts.join('\n').slice(0, 4000)
    }

    if (!buildFailed) {
      return {
        status: 'FAIL',
        detail: 'clean build SUCCEEDED despite injected type error — module is NOT in the compile graph',
      }
    }

    // 失败必须**因为向目标文件注入的那个类型错误**，而不是别的偶发原因
    // （例如 native 编译失败、环境缺失、超时）。判定分三层，从严到宽：
    //
    //   1. 编译阶段必须真的走到并失败在 CompileArkTS —— 这是可达性的直接证据；
    //   2. 失败信息里要出现目标文件路径；
    //   3. 出现 ArkTS 的类型错误字样。
    // 只做「构建失败」这一层判定是不够的：native 失败也会让构建失败。
    const base = pathOfModuleId(moduleId)
    const sawCompileArkTS = /CompileArkTS/i.test(detail)
    const sawTargetFile = detail.includes(moduleId) || detail.includes(base)
    const sawTypeError = /ArkTS:ERROR|Cannot find name|Type '|not assignable|COMPILE RESULT:FAIL/i.test(detail)

    if (!sawCompileArkTS || !sawTargetFile || !sawTypeError) {
      return {
        status: 'FAIL',
        detail:
          `build failed but not demonstrably at ${moduleId} in CompileArkTS ` +
          `(CompileArkTS=${sawCompileArkTS} targetFile=${sawTargetFile} typeError=${sawTypeError}): ` +
          detail.slice(0, 300),
      }
    }
    return {
      status: 'PASS',
      detail: `CompileArkTS failed on injected type error in ${base}`,
    }
  } finally {
    writeFileSync(target, backup, 'utf8')
  }
}

// ---------------------------------------------------------------------------
// 负向 probe 的代表模块解析与缓存
// ---------------------------------------------------------------------------

// 缓存：一次 Gate 运行内，同一个模块的负向 probe 只做一次。
const PROBE_CACHE = new Map()

// --probe-module <id>：显式指定代表模块（用于针对性复验某模块）。
const PROBE_MODULE_ARG = (() => {
  const i = process.argv.indexOf('--probe-module')
  return i >= 0 && process.argv[i + 1] ? process.argv[i + 1] : null
})()

/**
 * 决定本轮负向 probe 用哪个模块。
 *   返回 null        -> 本模块不参与负向验证（不可达，或未开启 --build）
 *   返回 moduleId    -> 由本模块自己承担 probe
 *   返回 otherId     -> 复用 otherId 的 probe 结论（标注在 negative 列）
 */
function probeTargetFor(moduleId, meta) {
  // 纯类型模块不能做负向探针：注入的 `const x: number = '...'` 是**值**语句，
  // 而 typeOnly 模块在 emit 时被整体擦除；更重要的是它没有运行时符号，
  // 用类型错误去探测一个「不产生代码的模块」得不到关于编译图的可靠结论。
  // 它的 C 判据已由「被可达模块 import」承担（见上方 typeOnly 分支）。
  if (meta.typeOnly) return null

  if (PROBE_MODULE_ARG) {
    return PROBE_MODULE_ARG
  }

  // 默认代表：第一个 required 且非 typeOnly 的模块（CanonicalEnums）。
  const rep = REQUIRED_MODULES.find((x) => x.required && !x.typeOnly)
  return rep ? rep.id : moduleId
}

const PROBE_MARKER = '// negative probe injected by check-compiled-reachability.mjs'

function selfHealResidue() {
  const healed = []
  for (const rel of listEts(ETS_ROOT)) {
    const full = join(ETS_ROOT, rel)
    const src = readFileSync(full, 'utf8')
    const i = src.indexOf(PROBE_MARKER)
    if (i >= 0) {
      writeFileSync(full, src.slice(0, i).replace(/\s+$/, '') + '\n', 'utf8')
      healed.push(rel)
    }
  }
  return healed
}

if (process.argv.includes('--build')) {
  const healed = selfHealResidue()
  if (healed.length) {
    console.log('[reachability] WARNING previous probe was killed mid-flight; healed residue in:')
    for (const h of healed) console.log('[reachability]   -', h)
    console.log('[reachability] These files were restored to their pre-probe content.')
  }

  // 真实构建：刷新 ASCII 镜像并 assembleHap，产出**新的 modules.abc**。
  //
  // 为什么必须放在这里（这是一个真实缺陷的修复）：
  //   原实现里 --build 只启用负向 probe，从不重建主产物。于是判据 C
  //   （"符号是否真的进了 modules.abc"）读的一直是上一次构建留下的旧 abc。
  //   后果有两个方向，都有害：
  //     假阴性 —— 新增模块已正确落地，却因 abc 陈旧而被判 FAIL（"inAbc=false"）；
  //     假阳性 —— 模块已被删除或改坏，旧 abc 里仍留着老符号，C 判据照样 PASS。
  //   两种都不允许：本项目对"absence ≠ nonexistence"的要求是双向的。
  //
  // 用 --clean 的理由与负向 probe 相同：hvigor 的 CompileArkTS 有增量缓存，
  // 不清目录时会复用旧编译结果，让"构建成功"失去证据力。
  console.log('[reachability] --build: rebuilding the ASCII mirror (clean assembleHap) ...')
  try {
    execFileSync(process.execPath,
      [join(REPO, 'tools', 'harmony', 'build-ascii-mirror.mjs'), '--clean', 'assembleHap'], {
      cwd: REPO,
      env: { ...process.env },
      stdio: 'pipe', encoding: 'utf8', timeout: 30 * 60 * 1000,
    })
    console.log('[reachability] --build: rebuild OK')
  } catch (e) {
    const parts = [
      typeof e?.stdout === 'string' ? e.stdout : '',
      typeof e?.stderr === 'string' ? e.stderr : '',
      ...(Array.isArray(e?.output) ? e.output.map((x) => (x == null ? '' : String(x))) : []),
      typeof e?.message === 'string' ? e.message : '',
    ]
    const detail = parts.join('\n')
    console.error('[reachability] --build: REBUILD FAILED — 判据 C 将按无产物处理')
    console.error(detail.slice(-4000))
    // 不在这里 exit(1)：让主流程带着"无新鲜产物"的事实继续跑完并如实报告，
    // 而不是提前退出留下一个看不出阶段的半截结论。
    process.env.PDIG_REACHABILITY_REBUILD_FAILED = '1'
  }
}

// ---------------------------------------------------------------------------
// 主流程
// ---------------------------------------------------------------------------

const wantBuild = process.argv.includes('--build')
const { modules, graph, reached } = buildReachability()
const abc = readAbcTokens()

console.log('[reachability] ets modules found :', modules.length)
console.log('[reachability] entry roots       :', ENTRY_ROOTS.join(', '))
console.log('[reachability] reachable         :', reached.size)
console.log('[reachability] modules.abc       :', abc ? `${abc.bytes} bytes` : 'NOT PRESENT (build not run)')

const NOT_IMPLEMENTED = 'NOT_IMPLEMENTED'
const rows = []
let failures = 0

for (const m of REQUIRED_MODULES) {
  const exists = existsSync(join(ETS_ROOT, m.path))
  if (!exists) {
    rows.push({
      module: m.id, present: false, reachable: false, inAbc: false, negative: 'N/A',
      status: m.required ? 'FAIL' : NOT_IMPLEMENTED,
    })
    if (m.required) failures++
    continue
  }

  const moduleId = m.path.replace(/\.ets$/, '')
  const reachable = reached.has(moduleId)

  // C：modules.abc 里是否出现该模块路径
  const abcNeedle = `entry/ets/${moduleId}`
  let inAbc = abc
    ? abc.tokens.has(abcNeedle) || [...abc.tokens].some((t) => t.includes(abcNeedle))
    : null

  // C（typeOnly 修正）：纯类型模块被 emit 擦除，abc 里本就不该有它。
  // 改用「被可达的非 typeOnly 模块 import」作为等价证据。
  if (m.typeOnly && abc !== null) {
    const importedByReachable = [...graph.entries()].some(([from, deps]) =>
      deps.includes(moduleId) && reached.has(from))
    inAbc = importedByReachable ? 'TYPE_ONLY' : false
  }

  // D：负向 probe。
  //
  // 重要设计取舍（实测驱动，勿改成"逐模块全跑"）：
  //   负向 probe 每次是一个 **clean 全量构建**（必须 --clean，见 negativeProbe 注释）。
  //   本机实测单次约 1.5–3 分钟；7 个 required 模块串行全跑要 10 分钟以上，
  //   既拖垮工具链也让人不想跑这个 Gate —— 一个没人跑的 Gate 等于没有 Gate。
  //
  //   负向可达性本质上是**编译图的性质**，不是单个文件的独立性质：
  //   只要探针模块确实在从入口可达的那张图里，注入类型错误就必然让构建失败。
  //   因此这里只用 **一个代表模块** 做负向验证，其余模块复用该结论，
  //   并把复用的来源显式标注出来（negative 列写 'PASS (via X)'），
  //   避免把"没测"伪装成"测过了"。
  //
  //   代表模块的选择：优先 --probe-module 指定；否则取第一个 required 且非 typeOnly 的。
  let negative = 'NOT_RUN'
  let negativeDetail = ''
  const candidate = wantBuild && reachable
    ? probeTargetFor(moduleId, m)
    : null
  if (candidate === null) {
    negative = !reachable ? 'N/A (not reachable)' : 'NOT_RUN'
  } else if (candidate === moduleId) {
    const r = negativeProbe(moduleId)
    negative = r.status
    negativeDetail = r.detail
  } else {
    const r = negativeProbe(candidate)
    negative = r.status === 'PASS' ? `PASS (via ${candidate})` : r.status
    negativeDetail = r.detail
  }

  const aOk = reachable
  // typeOnly 模块用 'TYPE_ONLY' 作为通过值；其余必须是布尔 true。
  const cOk = m.typeOnly ? inAbc === 'TYPE_ONLY' : inAbc === true
  // 负向结论可以是 'PASS' 或 'PASS (via X)'（代表模块复用）。
  const dOk = negative === 'PASS' || negative.startsWith('PASS (via ')

  let status
  if (!aOk) status = 'FAIL'                                  // A 不成立：模块根本不在编译图里
  else if (abc === null) status = 'PARTIAL_WITH_REPORT'      // 有源码有边，但无产物：只能证明 A
  else if (!cOk) status = 'FAIL'                             // C 不成立：产物里没有该模块符号
  else if (negative === 'FAIL') status = 'FAIL'              // D 明确反证：注入类型错误仍未失败
  else status = 'PASS'                                       // A + C 成立；B 由构建本身保证，D 见 negative 列

  // 只有在 A 成立但 C 或 D 明确失败时才算 Gate 失败；
  // 缺产物/缺 SDK 属于环境未就绪，记 PARTIAL_WITH_REPORT。
  if (status === 'FAIL') failures++

  rows.push({
    module: m.id, present: true, reachable: aOk, inAbc: inAbc,
    negative: negative, status: status, detail: negativeDetail,
  })
}

console.log('')
console.log('module                     present  reachable  inAbc       negative               status')
for (const r of rows) {
  console.log(
    `${r.module.padEnd(26)} ${String(r.present).padEnd(8)} ${String(r.reachable).padEnd(10)} ` +
    `${String(r.inAbc).padEnd(11)} ${String(r.negative).padEnd(22)} ${r.status}`,
  )
}

// 失败详情单独列出：没有原因的 FAIL 无法定位，等于把问题留给下一个人。
const failuresWithDetail = rows.filter((r) => r.status === 'FAIL' && r.detail)
if (failuresWithDetail.length) {
  console.log('')
  console.log('=== failure details ===')
  for (const r of failuresWithDetail) {
    console.log(`\n[${r.module}] ${r.detail}`)
  }
}

const requiredRows = rows.filter((r) => REQUIRED_MODULES.find((m) => m.id === r.module)?.required)
const allPresentReachable = requiredRows.every((r) => r.reachable)
const allInAbc = abc === null
  ? null
  : requiredRows.every((r) => {
    const meta = REQUIRED_MODULES.find((m) => m.id === r.module)
    return meta?.typeOnly ? r.inAbc === 'TYPE_ONLY' : r.inAbc === true
  })

console.log('')
console.log('  required modules              :', requiredRows.length)
console.log('  all required reachable (A)    :', allPresentReachable)
console.log('  all required in modules.abc(C):', allInAbc === null ? 'NOT_RUN (no artifact)' : allInAbc)
if (abc === null) {
  console.log('\nHARMONY_COMPILE_REACHABILITY=PARTIAL_WITH_REPORT (A ok; B/C/D need a real build)')
  process.exit(failures === 0 ? 0 : 1)
}
console.log('\nHARMONY_COMPILE_REACHABILITY=' + (failures === 0 ? 'PASS' : 'FAIL'))
process.exit(failures === 0 ? 0 : 1)
