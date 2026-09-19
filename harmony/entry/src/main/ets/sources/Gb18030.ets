// GB18030 解码（严格）—— 账单导入在严格 UTF-8 失败后的回退层。
//
// 与 Utf8.ets 的严格 UTF-8 同口径：解不出来就返回 undefined，**不替换、不兜底**。
// 决策权留在调用方，不藏在一个解码器里。
//
// 覆盖（GB18030-2005，全部经 Node ICU 实测并由 CPython 内置 codec 独立复验）：
//   1. 单字节 0x00–0x7F        → ASCII
//   2. 双字节 lead 0x81–0xFE / trail 0x40–0xFE(除 0x7F) → 23,940 个码位，全表
//   3. 四字节 BMP 补漏区（lead 0x81–0x84）→ 50,400 槽位 / 39,420 有效，游程表
//   4. 四字节增补平面（lead 0x90–0xE3）  → 线性公式，cp > 0x10FFFF 即非法
//
// 明确不覆盖（从严判非法，不做宽松替换）：
//   - 单字节 0x80：GB18030-2005 的单字节区只有 0x00–0x7F。ICU 按 CP936 宽松解为
//     U+20AC，CPython 拒绝；本实现取严格一侧。
//   - 单字节 0xFF：无定义。
//
// 码表来源见 tools/encoding/generate-gb18030-table.mjs，复验见
// tools/encoding/crosscheck-gb18030.py。**不要手改码表。**

import {
  gb18030TwoByteTable,
  gb18030Bmp4Table,
  GB18030_TWO_BYTE_SLOTS,
  GB18030_BMP4_SLOTS,
} from './Gb18030Table';

const TRAIL_COUNT: number = 190; // trail 0x40–0x7E(63) + 0x80–0xFE(127)

function trailIndex(trail: number): number {
  if (trail >= 0x40 && trail <= 0x7e) {
    return trail - 0x40;
  }
  if (trail >= 0x80 && trail <= 0xfe) {
    return trail - 0x80 + 63;
  }
  return -1;
}

function appendCodePoint(out: string, cp: number): string {
  if (cp <= 0xffff) {
    return out + String.fromCharCode(cp);
  }
  const v: number = cp - 0x10000;
  return out + String.fromCharCode(0xd800 + (v >> 10)) + String.fromCharCode(0xdc00 + (v & 0x3ff));
}

/**
 * 严格 GB18030 解码。任何非法字节序列一律返回 undefined。
 */
export function decodeGb18030(bytes: Uint8Array): string | undefined {
  const twoByte: number[] = gb18030TwoByteTable();
  const bmp4: number[] = gb18030Bmp4Table();
  let out: string = '';
  let i: number = 0;
  while (i < bytes.length) {
    const b1: number = bytes[i];
    if (b1 <= 0x7f) {
      out += String.fromCharCode(b1);
      i++;
      continue;
    }
    if (b1 < 0x81 || b1 > 0xfe) {
      // 0x80 与 0xFF：GB18030-2005 无定义（见文件头说明）
      return undefined;
    }
    if (i + 1 >= bytes.length) {
      return undefined;
    }
    const b2: number = bytes[i + 1];

    // 双字节优先：trail 范围 0x40–0xFE(除 0x7F) 与四字节第二字节 0x30–0x39 不重叠，
    // 因此这个判定是无歧义的，不需要回溯。
    const ti: number = trailIndex(b2);
    if (ti >= 0) {
      const slot: number = (b1 - 0x81) * TRAIL_COUNT + ti;
      if (slot < 0 || slot >= GB18030_TWO_BYTE_SLOTS) {
        return undefined;
      }
      out = appendCodePoint(out, twoByte[slot]);
      i += 2;
      continue;
    }

    // 四字节
    if (b2 < 0x30 || b2 > 0x39) {
      return undefined;
    }
    if (i + 3 >= bytes.length) {
      return undefined;
    }
    const b3: number = bytes[i + 2];
    const b4: number = bytes[i + 3];
    if (b3 < 0x81 || b3 > 0xfe || b4 < 0x30 || b4 > 0x39) {
      return undefined;
    }
    if (b1 >= 0x81 && b1 <= 0x84) {
      const slot: number =
        ((b1 - 0x81) * 10 + (b2 - 0x30)) * 1260 + ((b3 - 0x81) * 10 + (b4 - 0x30));
      if (slot < 0 || slot >= GB18030_BMP4_SLOTS) {
        return undefined;
      }
      const cp: number = bmp4[slot];
      if (cp < 0) {
        return undefined;
      }
      out = appendCodePoint(out, cp);
      i += 4;
      continue;
    }
    if (b1 >= 0x90 && b1 <= 0xe3) {
      const cp: number =
        0x10000 + ((b1 - 0x90) * 10 + (b2 - 0x30)) * 1260 + ((b3 - 0x81) * 10 + (b4 - 0x30));
      if (cp > 0x10ffff) {
        return undefined;
      }
      out = appendCodePoint(out, cp);
      i += 4;
      continue;
    }
    return undefined;
  }
  return out;
}

/**
 * 该 encoding 是否由本模块支持。
 *
 * 注意本模块只导出**解码能力**，不含回退策略：
 * 「先 UTF-8、失败再 GB18030」是 rule 层的决定，留在 Parsers.ets 里，
 * 这样"这个文件到底是哪种编码"这件事始终可见，不会被埋在一个解码器内部。
 */
export function isGb18030Encoding(encoding: string): boolean {
  const e: string = encoding.trim().toLowerCase();
  return e === 'gb18030' || e === 'gbk' || e === 'gb2312' || e === 'cp936' || e === 'x-gbk';
}
