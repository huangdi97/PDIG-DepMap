// Harmony Relations 移植的**源码语义镜像**校验（非门禁证据）
//
// ⚠ 定位声明（务必读懂再用）：
//  本脚本 **不是** Harmony 设备端/ArkTS 运行时的执行结果。
//  harmony/entry/src/main/ets/domain/Relations.ets 是 ArkTS，
//  没有模拟器系统镜像（hdc list targets = [Empty]）就无法真跑。
//  这里做的是：把同一份 fixtures 喂给 **语义等价的 Node 实现**，
//  用来在移植过程中尽早发现"写错了"的偏差。
//
//  因此：
//   - 它的结果 **不得** 写入 HARMONY_CONFORMANCE 门禁（门禁必须 NOT_RUN）
//   - 它只能用于开发期自检；真值以设备端执行为准
//
// 用法: node tools/harmony/check-relations-semantics.mjs
import { readFileSync, readdirSync } from 'node:fs'
import { join } from 'node:path'

const ROOT = process.cwd()
const FIX = join(ROOT, 'fixtures/relations')

// ---- 与 CanonicalEnums.ets / Relations.ets 逐字对齐的常量 ----
const PAYMENT_KINDS = ['payment_instrument', 'account', 'service', 'membership']
const DEFS = [
  {
    id: 'funding_source',
    fromKinds: PAYMENT_KINDS,
    toKinds: ['account', 'payment_instrument'],
    capability: 'payment',
    allowsGroup: true,
    allowedGroupModes: ['ANY'],
  },
  {
    id: 'merchant_agreement',
    fromKinds: ['account', 'payment_instrument'],
    toKinds: ['service', 'membership', 'account'],
    capability: 'payment',
    allowsGroup: false,
    allowedGroupModes: [],
  },
]

function getDef(id) {
  return DEFS.find((d) => d.id === id)
}
function has(list, v) {
  return list.indexOf(v) >= 0
}
function allow() {
  return { ok: true }
}
function reject(reason) {
  return { ok: false, reason }
}

function validateRelationUse(fromKind, relation, toKind, capability) {
  const def = getDef(relation)
  if (!def) return reject(`relation '${relation}' is not in the runtime registry`)
  if (capability !== def.capability)
    return reject(`relation '${relation}' requires capability '${def.capability}', got '${capability}'`)
  if (fromKind !== null && fromKind !== '' && !has(def.fromKinds, fromKind))
    return reject(`relation '${relation}' does not allow fromKind '${fromKind}'`)
  if (toKind !== null && toKind !== '' && !has(def.toKinds, toKind))
    return reject(`relation '${relation}' does not allow toKind '${toKind}'`)
  return allow()
}

function validateRelationGroupUse(relation, mode) {
  const def = getDef(relation)
  if (!def) return reject(`relation '${relation}' is not in the runtime registry`)
  if (!def.allowsGroup) return reject(`relation '${relation}' does not allow groups`)
  if (!has(def.allowedGroupModes, mode))
    return reject(`relation '${relation}' does not allow group mode '${mode}'`)
  return allow()
}

function normalize(r) {
  return r.ok ? { ok: true } : { ok: false, reason: r.reason }
}

/** JCS 风格稳定序列化：键序按插入序，这里结构固定为 ok → reason */
function stable(v) {
  return JSON.stringify(v)
}

const files = readdirSync(FIX).filter((f) => f.endsWith('.json')).sort()
let pass = 0
let fail = 0
const failures = []

for (const f of files) {
  const fx = JSON.parse(readFileSync(join(FIX, f), 'utf8'))
  const i = fx.input ?? {}
  let actual
  if (i.relation !== undefined && i.mode !== undefined) {
    actual = validateRelationGroupUse(i.relation, i.mode)
  } else {
    actual = validateRelationUse(i.fromKind ?? null, i.relation, i.toKind ?? null, i.capability)
  }
  const got = stable(normalize(actual))
  const want = stable(fx.expected)
  if (got === want) {
    pass++
  } else {
    fail++
    failures.push({ id: fx.id, want, got })
  }
}

console.log('[relations-semantics-mirror] fixtures:', files.length)
console.log('[relations-semantics-mirror] pass =', pass, 'fail =', fail)
for (const x of failures) {
  console.log('  FAIL', x.id, '\n    expected:', x.want, '\n    actual  :', x.got)
}
process.exit(fail === 0 ? 0 : 1)
