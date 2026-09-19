// 生成物里 Swift 保留字 case 现在带反引号，引用处也必须带反引号。
// 只处理 ios/Sources 下**我们自己写**的源码（Generated/ 由 codegen 负责）。
import { readdirSync, readFileSync, writeFileSync, statSync } from 'node:fs'
import { join } from 'node:path'

const ROOT = 'ios/Sources'
const KEYWORDS = ['in', 'any', 'open']

function walk(dir) {
  const out = []
  for (const name of readdirSync(dir)) {
    const p = join(dir, name)
    const st = statSync(p)
    if (st.isDirectory()) {
      if (p.endsWith('Generated')) continue
      out.push(...walk(p))
    } else if (name.endsWith('.swift')) {
      out.push(p)
    }
  }
  return out
}

let changed = 0
for (const file of walk(ROOT)) {
  const src = readFileSync(file, 'utf8')
  let out = src
  for (const kw of KEYWORDS) {
    // `.in` / `.any` / `.open` 作为成员引用；后面必须不是标识符字符
    out = out.replace(new RegExp('\\.' + kw + '(?![A-Za-z0-9_])', 'g'), '.`' + kw + '`')
  }
  if (out !== src) {
    writeFileSync(file, out, 'utf8')
    changed++
    console.log('patched', file)
  }
}
console.log('patchedFiles=' + changed)
