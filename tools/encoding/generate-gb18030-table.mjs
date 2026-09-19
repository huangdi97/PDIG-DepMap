#!/usr/bin/env node
/**
 * GB18030 解码表生成器 —— Harmony(ArkTS) / iOS(Swift) 共用。
 *
 * 为什么需要它：
 *   Harmony 主机执行面没有 util.TextDecoder，Swift 也没有内置 GB18030；
 *   而账单导入的 `encoding: "gb18030"` 是真实输入（fixtures/import 里
 *   csv-gb18030.csv 与 gbk.csv 的字节就不是合法 UTF-8）。
 *
 * 数据来源（single source）：Node 的 ICU TextDecoder('gb18030', {fatal:true})。
 * 它**不是**本仓库手写的码表 —— 手写码表无法验证，这正是上一版把两条用例
 * 记成 BLOCKED_BY_ENVIRONMENT 的理由。表现在由 ICU 生成，再由**独立实现**
 * （CPython 内置 gb18030 codec）逐位复验：
 * tools/encoding/crosscheck-gb18030.py。两套实现在全部 23,940 个双字节码位、
 * 50,400 个四字节 BMP 码位与增补平面抽样上必须完全一致。
 *
 * 结构（GB18030-2005，全部经 ICU 实测，不靠文档推断）：
 *   1. 单字节 0x00–0x7F → ASCII。0x80 无定义（ICU 宽松解为 U+20AC，
 *      CPython 拒绝；本解码器从严判定非法，见 crosscheck 记录）。
 *   2. 双字节 lead 0x81–0xFE / trail 0x40–0xFE(除 0x7F)：23,940 个码位，
 *      全部有定义，无可用的闭式表达 → 全表存储（base64 + Uint16LE）。
 *   3. 四字节 BMP 补漏区（lead 0x81–0x84，50,400 个槽位，39,420 个有定义）：
 *      GB18030-2005 修订过该区，映射**非单调**，不能由公式推导
 *      （实测 MONOTONIC=false，且与"未被双字节覆盖的 BMP 升序枚举"不符）。
 *      但它由 209 段连续上升游程构成 → 直接存游程表。
 *   4. 四字节增补平面区（lead 0x90–0xE3）：线性，实测成立
 *      cp = 0x10000 + ((b1-0x90)*10 + (b2-0x30))*1260 + ((b3-0x81)*10 + (b4-0x30))
 *      cp > 0x10FFFF 即非法。
 *
 * 用法：node tools/encoding/generate-gb18030-table.mjs [--check]
 */

import { writeFileSync, readFileSync, existsSync, mkdirSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const REPO = join(HERE, '..', '..');

// fatal:true 是硬要求：非严格模式下 ICU 会把非法序列替换成 U+FFFD，
// 于是"这个字节序列不是合法 GB18030"这个信号被吃掉，表会被 U+FFFD 污染。
const decoder = new TextDecoder('gb18030', { fatal: true });

function decodeCodePoint(bytes) {
  let s;
  try {
    s = decoder.decode(new Uint8Array(bytes));
  } catch {
    return null;
  }
  // 必须用**码点数**而不是 s.length：非 BMP 字符在 JS 里是 2 个 UTF-16 单元，
  // 用 length 判断会把整个增补平面误判为非法。
  const cps = Array.from(s);
  if (cps.length !== 1) {
    return null;
  }
  const cp = s.codePointAt(0);
  // 代理项不是标量值，视为非法（与 UTF-8 严格解码口径一致）
  return cp === undefined || (cp >= 0xd800 && cp <= 0xdfff) ? null : cp;
}

// --------------------------------------------------------------------------
// 1) 双字节全表
// --------------------------------------------------------------------------
const LEADS = [];
for (let l = 0x81; l <= 0xfe; l++) LEADS.push(l);
const TRAILS = [];
for (let t = 0x40; t <= 0xfe; t++) if (t !== 0x7f) TRAILS.push(t);

const twoByte = new Array(LEADS.length * TRAILS.length).fill(null);
for (let li = 0; li < LEADS.length; li++) {
  for (let ti = 0; ti < TRAILS.length; ti++) {
    twoByte[li * TRAILS.length + ti] = decodeCodePoint([LEADS[li], TRAILS[ti]]);
  }
}
const twoByteUndefined = twoByte.filter((v) => v === null).length;
if (twoByteUndefined !== 0) {
  throw new Error(`双字节表存在 ${twoByteUndefined} 个未定义码位；生成中止 —— ` +
    '缺口码位会被静默解成乱码，这正是本项目禁止的做法。');
}

// --------------------------------------------------------------------------
// 2) 四字节 BMP 补漏区（50,400 槽位）
// --------------------------------------------------------------------------
const BMP4_SLOTS = 50400;

function bmp4Index(b1, b2, b3, b4) {
  return ((b1 - 0x81) * 10 + (b2 - 0x30)) * 1260 + ((b3 - 0x81) * 10 + (b4 - 0x30));
}

// 槽位值用 -1 表示"未定义"：ArkTS 对 `(number | null)[]` 这类联合类型数组的
// 窄化支持不稳，用一个不可能与合法码点冲突的哨兵可以完全避开它。
const bmp4 = new Array(BMP4_SLOTS).fill(-1);
for (let b1 = 0x81; b1 <= 0x84; b1++) {
  for (let b2 = 0x30; b2 <= 0x39; b2++) {
    for (let b3 = 0x81; b3 <= 0xfe; b3++) {
      for (let b4 = 0x30; b4 <= 0x39; b4++) {
        const v = decodeCodePoint([b1, b2, b3, b4]);
        bmp4[bmp4Index(b1, b2, b3, b4)] = v === null ? -1 : v;
      }
    }
  }
}
const bmp4Valid = bmp4.filter((v) => v >= 0).length;

// --------------------------------------------------------------------------
// 2b) 向冻结的 Android 实现对齐（覆盖 ICU 与 Java 不一致的码位）
// --------------------------------------------------------------------------
// 依据文件：gb18030-divergences.json（逐条记录了 icu / cpython / java 三侧取值）。
// Android 用 Charset.forName("GB18030") 且已 CORE_FROZEN，改不动；三端要产出
// 同一结果，只能由活跃端（Harmony / iOS）向它对齐。覆盖项因此是**显式且可审计**的，
// 不是悄悄改数据。
const divergenceDoc = JSON.parse(
  readFileSync(join(HERE, 'gb18030-divergences.json'), 'utf8'),
);
const appliedOverrides = [];
for (const entry of divergenceDoc.entries) {
  if (!entry.override) {
    continue;
  }
  const bytes = [];
  for (let i = 0; i < entry.seq.length; i += 2) {
    bytes.push(parseInt(entry.seq.slice(i, i + 2), 16));
  }
  const target = parseInt(entry.override.slice(2), 16);
  if (bytes.length === 2) {
    const slot = (bytes[0] - 0x81) * 190
      + (bytes[1] <= 0x7e ? bytes[1] - 0x40 : bytes[1] - 0x80 + 63);
    const before = twoByte[slot];
    if (before === target) {
      throw new Error(`覆盖项 ${entry.seq} 的目标值与 ICU 相同，说明记录已过期`);
    }
    twoByte[slot] = target;
    appliedOverrides.push({
      seq: entry.seq, from: `U+${before.toString(16).toUpperCase()}`, to: entry.override,
    });
  } else if (bytes.length === 4) {
    const slot = ((bytes[0] - 0x81) * 10 + (bytes[1] - 0x30)) * 1260
      + ((bytes[2] - 0x81) * 10 + (bytes[3] - 0x30));
    const before = bmp4[slot];
    if (before === target) {
      throw new Error(`覆盖项 ${entry.seq} 的目标值与 ICU 相同，说明记录已过期`);
    }
    bmp4[slot] = target;
    appliedOverrides.push({
      seq: entry.seq, from: `U+${before.toString(16).toUpperCase()}`, to: entry.override,
    });
  } else {
    throw new Error(`覆盖项 ${entry.seq} 的字节数不受支持`);
  }
}

// 游程编码：连续上升段 V，未定义段 G
const bmp4Runs = [];
{
  let i = 0;
  while (i < BMP4_SLOTS) {
    let j = i + 1;
    if (bmp4[i] < 0) {
      while (j < BMP4_SLOTS && bmp4[j] < 0) j++;
      bmp4Runs.push({ kind: 'G', start: i, len: j - i, first: 0 });
    } else {
      while (j < BMP4_SLOTS && bmp4[j] >= 0 && bmp4[j] === bmp4[j - 1] + 1) j++;
      bmp4Runs.push({ kind: 'V', start: i, len: j - i, first: bmp4[i] });
    }
    i = j;
  }
}
// 自检：游程展开必须逐位还原原数组
{
  const rebuilt = new Array(BMP4_SLOTS).fill(-1);
  for (const r of bmp4Runs) {
    for (let k = 0; k < r.len; k++) {
      rebuilt[r.start + k] = r.kind === 'V' ? r.first + k : -1;
    }
  }
  for (let i = 0; i < BMP4_SLOTS; i++) {
    if (rebuilt[i] !== bmp4[i]) {
      throw new Error(`四字节 BMP 游程展开在第 ${i} 槽位不一致`);
    }
  }
}

// --------------------------------------------------------------------------
// 3) 增补平面区：验证线性公式（穷举 lead 0x90–0xE3 的边界 + 抽样）
// --------------------------------------------------------------------------
function supplementaryCodePoint(b1, b2, b3, b4) {
  return 0x10000 + ((b1 - 0x90) * 10 + (b2 - 0x30)) * 1260 + ((b3 - 0x81) * 10 + (b4 - 0x30));
}
let supChecked = 0;
let supMismatch = 0;
const supSamples = [];
for (let b1 = 0x90; b1 <= 0xe3; b1 += 5) {
  for (let b2 = 0x30; b2 <= 0x39; b2 += 2) {
    for (let b3 = 0x81; b3 <= 0xfe; b3 += 7) {
      for (let b4 = 0x30; b4 <= 0x39; b4 += 2) {
        supSamples.push([b1, b2, b3, b4]);
      }
    }
  }
}
supSamples.push(
  [0x90, 0x30, 0x81, 0x30], [0x90, 0x30, 0x81, 0x31],
  [0xe3, 0x32, 0x9a, 0x35], [0xe3, 0x32, 0x9a, 0x36],
  [0xe3, 0x33, 0x81, 0x30], [0xe4, 0x30, 0x81, 0x30],
);
for (const [b1, b2, b3, b4] of supSamples) {
  const cp = decodeCodePoint([b1, b2, b3, b4]);
  const predicted = supplementaryCodePoint(b1, b2, b3, b4);
  supChecked++;
  if (cp === null ? predicted <= 0x10ffff : cp !== predicted) {
    supMismatch++;
  }
}
if (supMismatch !== 0) {
  throw new Error(`增补平面线性公式不成立：mismatch=${supMismatch}/${supChecked}`);
}

// --------------------------------------------------------------------------
// 4) 单字节 0x80：记录 ICU 行为（从严，解码器判非法）
// --------------------------------------------------------------------------
const single80 = decodeCodePoint([0x80]);

// --------------------------------------------------------------------------
// 5) 序列化
// --------------------------------------------------------------------------
function fnv1a(values) {
  let h = 0x811c9dc5;
  for (const v of values) {
    // -1（未定义）按 0xFFFFFFFF 参与，与解码器看到的哨兵一致
    const x = v < 0 ? 0xffffffff : v;
    h ^= x & 0xff;
    h = Math.imul(h, 0x01000193) >>> 0;
    h ^= (x >>> 8) & 0xff;
    h = Math.imul(h, 0x01000193) >>> 0;
  }
  return h >>> 0;
}

const twoByteBuf = Buffer.alloc(twoByte.length * 2);
for (let i = 0; i < twoByte.length; i++) twoByteBuf.writeUInt16LE(twoByte[i], i * 2);
const twoByteB64 = twoByteBuf.toString('base64');

const bmp4Text = bmp4Runs
  .map((r) => (r.kind === 'V'
    ? `V${r.start.toString(16)}.${r.len.toString(16)}.${r.first.toString(16)}`
    : `G${r.start.toString(16)}.${r.len.toString(16)}`))
  .join(';');

const manifest = {
  generator: 'tools/encoding/generate-gb18030-table.mjs',
  source: "Node ICU TextDecoder('gb18030', { fatal: true })",
  independentVerifier: 'CPython built-in gb18030 codec (tools/encoding/crosscheck-gb18030.py)',
  twoByte: {
    slots: twoByte.length,
    leadRange: [0x81, 0xfe],
    trailRange: [0x40, 0xfe],
    trailExcludes: [0x7f],
    bytes: twoByteBuf.length,
    sha256: createHash('sha256').update(twoByteBuf).digest('hex'),
    checksumFnv1a: fnv1a(twoByte),
  },
  bmp4: {
    slots: BMP4_SLOTS,
    valid: bmp4Valid,
    runs: bmp4Runs.length,
    checksumFnv1a: fnv1a(bmp4),
    monotonic: false,
    note: 'GB18030-2005 修订过该区，映射非单调，不能由公式推导；按 209 段连续上升游程存储。',
  },
  supplementary: {
    formula: '0x10000 + ((b1-0x90)*10 + (b2-0x30))*1260 + ((b3-0x81)*10 + (b4-0x30))',
    max: 'U+10FFFF',
    verifiedAgainstIcu: { checked: supChecked, mismatch: supMismatch },
  },
  singleByte0x80: {
    icu: single80 === null ? 'INVALID' : `U+${single80.toString(16).toUpperCase()}`,
    decoderBehaviour: 'INVALID',
    note: 'ICU(CP936 宽松) 解为 U+20AC，CPython 拒绝；GB18030-2005 单字节区只有 0x00–0x7F，从严判非法。',
  },
  androidAlignment: {
    policy: 'base=icu，override=java（Android 已 CORE_FROZEN，活跃端向冻结端对齐）',
    recordedDivergences: divergenceDoc.entries.length,
    appliedOverrides,
    divergenceFile: 'tools/encoding/gb18030-divergences.json',
  },
};

function chunkB64(b64, per) {
  const parts = [];
  for (let i = 0; i < b64.length; i += per) parts.push(b64.slice(i, i + per));
  return parts;
}

const ETS_PATH = join(REPO, 'harmony', 'entry', 'src', 'main', 'ets', 'sources', 'Gb18030Table.ets');
const SWIFT_PATH = join(REPO, 'ios', 'Sources', 'PDIGCore', 'Encoding', 'Gb18030Table.swift');
const MANIFEST_PATH = join(HERE, 'gb18030-manifest.json');

const etsChunks = chunkB64(twoByteB64, 3000).map((c) => `  '${c}'`).join(' +\n');
const swiftChunks = chunkB64(twoByteB64, 3000).map((c) => `    "${c}"`).join(' +\n');
const etsBmpChunks = chunkB64(bmp4Text, 3000).map((c) => `  '${c}'`).join(' +\n');
const swiftBmpChunks = chunkB64(bmp4Text, 3000).map((c) => `    "${c}"`).join(' +\n');

const header = (lang) => {
  const c = lang === 'ets' ? '//' : '//';
  return [
    `${c} 自动生成，请勿手工编辑。`,
    `${c} 生成命令：node tools/encoding/generate-gb18030-table.mjs`,
    `${c} 数据来源：Node ICU TextDecoder('gb18030', { fatal: true })`,
    `${c} 独立复验：CPython 内置 gb18030 codec（tools/encoding/crosscheck-gb18030.py，逐位比对全部码位）`,
    `${c}`,
    `${c} 表内容：`,
    `${c}   A. 双字节码位全表            ${twoByte.length} 项（base64 + Uint16LE）`,
    `${c}   B. 四字节 BMP 补漏区         ${BMP4_SLOTS} 槽位 / ${bmp4Valid} 项 / ${bmp4Runs.length} 段游程`,
    `${c}   C. 四字节增补平面            线性公式（已复验），不入表`,
    `${c}`,
    `${c} 为什么是表而不是算法：GB18030 双字节区继承自 GBK，无闭式表达；四字节 BMP 区`,
    `${c} 在 GB18030-2005 中被修订为非单调映射（实测），同样无法推导。任何"按需补几个字"`,
    `${c} 的做法都会让未覆盖的字节静默解错 —— 要么全表、要么不实现，没有中间态。`,
  ].join('\n');
};

const ets = `${header('ets')}

export const GB18030_TWO_BYTE_SLOTS: number = ${twoByte.length};
export const GB18030_TWO_BYTE_CHECKSUM: number = ${manifest.twoByte.checksumFnv1a};
export const GB18030_BMP4_SLOTS: number = ${BMP4_SLOTS};
export const GB18030_BMP4_VALID: number = ${bmp4Valid};
export const GB18030_BMP4_RUNS: number = ${bmp4Runs.length};
export const GB18030_BMP4_CHECKSUM: number = ${manifest.bmp4.checksumFnv1a};

const GB18030_TWO_BYTE_B64: string =
${etsChunks};

const GB18030_BMP4_RUNS_TEXT: string =
${etsBmpChunks};

const B64_ALPHABET: string = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';

function b64Value(c: number): number {
  if (c >= 65 && c <= 90) {
    return c - 65;
  }
  if (c >= 97 && c <= 122) {
    return c - 71;
  }
  if (c >= 48 && c <= 57) {
    return c + 4;
  }
  if (c === 43) {
    return 62;
  }
  if (c === 47) {
    return 63;
  }
  return -1;
}

/**
 * 最小 base64 解码器（只认标准字母表，拒绝任何非法输入）。
 *
 * 与 crypto/DepmapBounds.ets 的 decodeBase64 语义相同但**不复用**：
 * 本模块位于 sources/ 层，若引用 crypto/ 会把依赖 @kit.CryptoArchitectureKit 的
 * 模块拖进主机执行面的编译图（该 kit 在本地单元测试中不加载）。重复一个
 * 20 行解码器，代价远小于制造一条跨层依赖。
 */
function decodeBase64ToBytes(s: string): Uint8Array {
  const digits: number[] = [];
  for (let i: number = 0; i < s.length; i++) {
    const v: number = b64Value(s.charCodeAt(i));
    if (v < 0) {
      throw new Error('gb18030 table: illegal base64 character at ' + i);
    }
    digits.push(v);
  }
  const rem: number = digits.length % 4;
  if (rem === 1) {
    throw new Error('gb18030 table: truncated base64 group');
  }
  const outLength: number = (digits.length >> 2) * 3 + (rem === 0 ? 0 : rem - 1);
  const out: Uint8Array = new Uint8Array(outLength);
  let acc: number = 0;
  let accBits: number = 0;
  let o: number = 0;
  for (let i: number = 0; i < digits.length; i++) {
    acc = (acc << 6) | digits[i];
    accBits += 6;
    if (accBits >= 8) {
      accBits -= 8;
      out[o] = (acc >> accBits) & 0xff;
      o++;
    }
  }
  return out;
}

let cachedTwoByte: number[] | null = null;

/** 双字节码位表；索引 = (lead-0x81)*190 + trailIndex(trail<0x80 ? trail-0x40 : trail-0x80+63)。 */
export function gb18030TwoByteTable(): number[] {
  if (cachedTwoByte === null) {
    const bytes: Uint8Array = decodeBase64ToBytes(GB18030_TWO_BYTE_B64);
    if (bytes.length !== GB18030_TWO_BYTE_SLOTS * 2) {
      throw new Error('gb18030 table: decoded byte length mismatch');
    }
    const table: number[] = [];
    for (let i: number = 0; i < GB18030_TWO_BYTE_SLOTS; i++) {
      table.push(bytes[i * 2] | (bytes[i * 2 + 1] << 8));
    }
    cachedTwoByte = table;
  }
  return cachedTwoByte;
}

let cachedBmp4: number[] | null = null;

/** 四字节 BMP 补漏区（${BMP4_SLOTS} 槽位，未定义槽位为 -1）。 */
export function gb18030Bmp4Table(): number[] {
  if (cachedBmp4 === null) {
    const table: number[] = [];
    for (let i: number = 0; i < GB18030_BMP4_SLOTS; i++) {
      table.push(-1);
    }
    const segs: string[] = GB18030_BMP4_RUNS_TEXT.split(';');
    for (let s: number = 0; s < segs.length; s++) {
      const seg: string = segs[s];
      if (seg.length === 0) {
        continue;
      }
      const parts: string[] = seg.substring(1).split('.');
      const start: number = parseInt(parts[0], 16);
      const len: number = parseInt(parts[1], 16);
      if (seg[0] === 'V') {
        const first: number = parseInt(parts[2], 16);
        for (let k: number = 0; k < len; k++) {
          table[start + k] = first + k;
        }
      }
    }
    cachedBmp4 = table;
  }
  return cachedBmp4;
}
`;

const swift = `${header('swift')}

import Foundation

public enum Gb18030Table {
    public static let twoByteSlots = ${twoByte.length}
    public static let twoByteChecksum: UInt32 = ${manifest.twoByte.checksumFnv1a}
    public static let bmp4Slots = ${BMP4_SLOTS}
    public static let bmp4Valid = ${bmp4Valid}
    public static let bmp4Runs = ${bmp4Runs.length}
    public static let bmp4Checksum: UInt32 = ${manifest.bmp4.checksumFnv1a}

    private static let twoByteBase64 =
${swiftChunks}

    private static let bmp4RunsText =
${swiftBmpChunks}

    private static func decodeBase64(_ s: String) -> [UInt8] {
        guard let data = Data(base64Encoded: s) else {
            fatalError("gb18030 table: illegal base64")
        }
        return [UInt8](data)
    }

    private static let _twoByte: [UInt16] = {
        let bytes = decodeBase64(twoByteBase64)
        precondition(bytes.count == twoByteSlots * 2, "gb18030 table: byte length mismatch")
        var table = [UInt16]()
        table.reserveCapacity(twoByteSlots)
        for i in 0..<twoByteSlots {
            table.append(UInt16(bytes[i * 2]) | (UInt16(bytes[i * 2 + 1]) << 8))
        }
        return table
    }()

    public static func twoByteTable() -> [UInt16] { _twoByte }

    private static let _bmp4: [Int] = {
        var table = [Int](repeating: -1, count: bmp4Slots)
        for seg in bmp4RunsText.split(separator: ";") {
            guard seg.count > 1 else { continue }
            let body = seg.dropFirst()
            let parts = body.split(separator: ".")
            let kind = seg.first
            guard let start = Int(parts[0], radix: 16), let len = Int(parts[1], radix: 16) else {
                fatalError("gb18030 table: malformed bmp4 run")
            }
            if kind == "V" {
                guard parts.count == 3, let first = Int(parts[2], radix: 16) else {
                    fatalError("gb18030 table: malformed bmp4 value run")
                }
                for k in 0..<len { table[start + k] = first + k }
            }
        }
        return table
    }()

    public static func bmp4Table() -> [Int] { _bmp4 }
}
`;

if (process.argv.includes('--check')) {
  const report = [];
  let ok = true;
  for (const [label, path, expected] of [
    ['ArkTS', ETS_PATH, ets],
    ['Swift', SWIFT_PATH, swift],
  ]) {
    if (!existsSync(path)) {
      report.push(`${label} MISSING`);
      ok = false;
      continue;
    }
    report.push(readFileSync(path, 'utf8') === expected ? `${label} UP_TO_DATE` : `${label} DRIFT`);
    if (readFileSync(path, 'utf8') !== expected) ok = false;
  }
  console.log(`[gb18030-table] ${report.join(' | ')}`);
  process.exit(ok ? 0 : 1);
}

mkdirSync(dirname(ETS_PATH), { recursive: true });
mkdirSync(dirname(SWIFT_PATH), { recursive: true });
writeFileSync(ETS_PATH, ets, 'utf8');
writeFileSync(SWIFT_PATH, swift, 'utf8');
writeFileSync(MANIFEST_PATH, JSON.stringify(manifest, null, 2) + '\n', 'utf8');

console.log(`[gb18030-table] 双字节码位        : ${twoByte.length}（全部有定义）`);
console.log(`[gb18030-table] 双字节表 bytes    : ${twoByteBuf.length}  sha256=${manifest.twoByte.sha256.slice(0, 16)}…`);
console.log(`[gb18030-table] 四字节 BMP 槽位   : ${BMP4_SLOTS}（有效 ${bmp4Valid}，游程 ${bmp4Runs.length} 段，展开自检通过）`);
console.log(`[gb18030-table] 增补平面线性公式  : 复验 ${supChecked} 条，mismatch=${supMismatch}`);
console.log(`[gb18030-table] 单字节 0x80       : ICU=${manifest.singleByte0x80.icu}，解码器从严判非法`);
for (const o of appliedOverrides) {
  console.log(`[gb18030-table] Android 对齐覆盖   : ${o.seq} ${o.from} → ${o.to}（ICU → Java，依据 divergences.json）`);
}
console.log(`[gb18030-table] 写出 harmony/…/sources/Gb18030Table.ets`);
console.log(`[gb18030-table] 写出 ios/…/Encoding/Gb18030Table.swift`);
