// UTF-8 解码（严格）—— 账单导入的第一层。
//
// 为什么必须自己实现而不是用 util.TextDecoder：
//   1. `@kit.ArkTS` 在主机执行面不可用（本地单元测试不加载 OHOS kit）；
//   2. 更重要的是**语义**：是否被解出来的正是 Threat model 的一部分。
//      Android 参考实现是 `Charset.forName("UTF-8").newDecoder()
//      .onMalformedInput(REPORT).onUnmappableCharacter(REPORT)` —— **严格**解码，
//      失败则回退 GB18030。若这里用宽容解码（invalid byte → U+FFFD），
//      "这段字节不是 UTF-8"这个信号就丢了，回退分支永远不会触发，
//      GB18030 账单会被静默解成一堆替换字符。
//
// 因此本模块提供 `decodeStrictUtf8`：非法字节一律返回 undefined（不抛、不替换），
// 由调用方决定回退策略 —— 决策权留在 rule 层，而不是藏在一个解码器里。

/**
 * 严格 UTF-8 解码。
 *
 * 拒绝清单（与 ICU / Java Strict UTF-8 一致）：
 *   - 孤立续字节（0x80–0xBF）
 *   - overlong 编码（0xC0/0xC1 开头，以及多字节形式编码出的过小码点）
 *   - 代理区间（U+D800–U+DFFF）
 *   - 超出 U+10FFFF
 *   - 截断序列
 */
export function decodeStrictUtf8(bytes: Uint8Array): string | undefined {
  let out: string = '';
  let i: number = 0;
  while (i < bytes.length) {
    const b0: number = bytes[i];
    let cp: number = 0;
    let need: number = 0;
    if (b0 <= 0x7f) {
      cp = b0;
      need = 0;
    } else if (b0 >= 0xc2 && b0 <= 0xdf) {
      cp = b0 & 0x1f;
      need = 1;
    } else if (b0 >= 0xe0 && b0 <= 0xef) {
      cp = b0 & 0x0f;
      need = 2;
    } else if (b0 >= 0xf0 && b0 <= 0xf4) {
      cp = b0 & 0x07;
      need = 3;
    } else {
      return undefined;
    }
    if (i + need >= bytes.length) {
      return undefined;
    }
    for (let k: number = 1; k <= need; k++) {
      const bk: number = bytes[i + k];
      if (bk < 0x80 || bk > 0xbf) {
        return undefined;
      }
      cp = (cp << 6) | (bk & 0x3f);
    }
    if (need === 1 && cp < 0x80) {
      return undefined;
    }
    if (need === 2 && (cp < 0x800 || (cp >= 0xd800 && cp <= 0xdfff))) {
      return undefined;
    }
    if (need === 3 && (cp < 0x10000 || cp > 0x10ffff)) {
      return undefined;
    }
    if (cp <= 0xffff) {
      out += String.fromCharCode(cp);
    } else {
      const v: number = cp - 0x10000;
      out += String.fromCharCode(0xd800 + (v >> 10));
      out += String.fromCharCode(0xdc00 + (v & 0x3ff));
    }
    i += need + 1;
  }
  return out;
}

export function hasUtf8Bom(bytes: Uint8Array): boolean {
  return bytes.length >= 3 && bytes[0] === 0xef && bytes[1] === 0xbb && bytes[2] === 0xbf;
}

export function stripUtf8Bom(bytes: Uint8Array): Uint8Array {
  if (!hasUtf8Bom(bytes)) {
    return bytes;
  }
  return bytes.subarray(3, bytes.length);
}
