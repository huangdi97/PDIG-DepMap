// Harmony N3 Conformance — 文本级 JSON 工具。
//
// 为什么需要它（这不是"重造轮子"，是 ArkTS 的结构性约束）：
//   ArkTS（Stage Model）禁止 any / 动态索引 / 结构化类型推导，
//   JSON.parse 的返回值**无法**安全映射到 class 实例。
//   以 `doc: Record<string, Object>` 收下再取字段，会在编译期被 ArkTS 拒绝
//   （索引签名 / Object→具体类型 均不允许）。
//   因此「解析 fixture 成对象图」这条路在 ArkTS 上不存在，
//   只有「在文本上定位 + 受控扫描」这一条。
//
// 本模块提供的是**通用文本原语**（对象切分、字段定位、字符串解码），
// 它们不做任何值域猜测：只按严格扫描规则推进，越界即抛错。
// 需要"理解结构"的部分（例如 Dependency 的行解析）放在 ConformanceRunner，
// 与它要装载的具体实体放在一起，避免把类型知识散落在通用工具里。
//
// 依赖约束：只能依赖 @ohos.file.fs（读文件）与 ArkTS 内建能力。
// 不得依赖 ArkUI / Ability / relationalStore / HUKS / NAPI / cryptoFramework。

/*
 * 键定位的窗口上限。
 *
 * 为什么必须有上限：`indexOf('"x"')` 是全文件搜索，一旦字段真正缺失，
 * 它会跨过若干个对象命中**后面**某个对象里的同名键 ——
 * 于是解析"成功"了，但读到的是别人的值。这种静默错位比报错危险得多。
 * 本项目的通用纪律：宁可显式失败，也不要静默取到相邻对象的字段。
 */
export class JsonText {
  private src: string = '';
  private pos: number = 0;
  private end: number = 0;

  /** 待扫描文本。 */
  setSource(text: string): void {
    this.src = text;
    this.pos = 0;
    this.end = text.length;
  }

  getSource(): string {
    return this.src;
  }

  /** 设置扫描窗口（[start, stop)）。越界值会被夹到合法范围，不抛错。 */
  setWindow(start: number, stop: number): void {
    let s = start;
    let e = stop;
    if (s < 0) {
      s = 0;
    }
    if (e > this.src.length) {
      e = this.src.length;
    }
    if (e < s) {
      e = s;
    }
    this.pos = s;
    this.end = e;
  }

  /** 把窗口设回整段文本（对象切分完成后回到"全局"视角）。 */
  resetWindow(): void {
    this.pos = 0;
    this.end = this.src.length;
  }

  getPos(): number {
    return this.pos;
  }

  getEnd(): number {
    return this.end;
  }

  /* ------------------------------------------------------------------ */
  /* 字符分类                                                            */
  /* ------------------------------------------------------------------ */

  private isWs(ch: string): boolean {
    return ch === ' ' || ch === '\t' || ch === '\n' || ch === '\r';
  }

  /**
   * 只有当该位置处于字面量边界（串首 / 空白 / 分隔符）时才认作起点。
   *
   * 这一条挡住的是真实存在的错配：
   *   - 找键 `"to"` 时命中 `"toNode"`；
   *   - 找键 `"from"` 时命中 `"fromNode"`。
   * 没有它，`"fromNode": "card-a"` 会被当成 `"from": "card-a"` 读走。
   */
  private isBoundaryAt(i: number): boolean {
    if (i < 0 || i >= this.src.length) {
      return false;
    }
    const ch = this.src.charAt(i);
    return this.isWs(ch) || ch === '{' || ch === '}' || ch === '[' || ch === ']' || ch === ',' || ch === ':';
  }

  /* ------------------------------------------------------------------ */
  /* 定位                                                                */
  /* ------------------------------------------------------------------ */

  /** 在窗口内定位字面量 `token`，返回下标；找不到返回 -1。 */
  find(token: string): number {
    const i = this.src.indexOf(token, this.pos);
    if (i < 0 || i >= this.end) {
      return -1;
    }
    return i;
  }

  /** 在窗口内定位裸字段名（键或值），带边界判定。 */
  findBare(token: string): number {
    let from = this.pos;
    while (from < this.end) {
      const i = this.src.indexOf(token, from);
      if (i < 0 || i >= this.end) {
        return -1;
      }
      const before = i === 0 ? '{' : this.src.charAt(i - 1);
      const afterIdx = i + token.length;
      const after = afterIdx >= this.src.length ? ',' : this.src.charAt(afterIdx);
      if (this.isBoundaryAt(i) && this.isBoundaryAt(afterIdx)) {
        return i;
      }
      from = i + 1;
    }
    return -1;
  }

  /**
   * 定位 JSON 键 `"name"` 并跳过其后的冒号与空白，返回**值的第一位**。
   * 找不到返回 -1。窗口上限由字段缺失时的显式失败保证，不做跨对象漂移。
   */
  findField(name: string): number {
    const token = '"' + name + '"';
    let from = this.pos;
    while (from < this.end) {
      const i = this.src.indexOf(token, from);
      if (i < 0 || i >= this.end) {
        return -1;
      }
      let j = i + token.length;
      while (j < this.end && this.isWs(this.src.charAt(j))) {
        j++;
      }
      if (j < this.end && this.src.charAt(j) === ':') {
        j++;
        while (j < this.end && this.isWs(this.src.charAt(j))) {
          j++;
        }
        return j;
      }
      from = i + 1;
    }
    return -1;
  }

  /** 从下标 i 处的 `{` / `[` 起定位配对的闭合括号，正确跳过字符串内的括号。 */
  matchBracket(i: number): number {
    const open = this.src.charAt(i);
    let close = '}';
    if (open === '[') {
      close = ']';
    } else if (open !== '{') {
      return -1;
    }
    let depth = 0;
    let inStr = false;
    let esc = false;
    let k = i;
    while (k < this.src.length) {
      const ch = this.src.charAt(k);
      if (inStr) {
        if (esc) {
          esc = false;
        } else if (ch === '\\') {
          esc = true;
        } else if (ch === '"') {
          inStr = false;
        }
      } else if (ch === '"') {
        inStr = true;
      } else if (ch === open) {
        depth++;
      } else if (ch === close) {
        depth--;
        if (depth === 0) {
          return k;
        }
      }
      k++;
    }
    return -1;
  }

  /**
   * 从下标 i 处的 `[` 起，切出**顶层**元素的 [start, end) 区间。
   *
   * 用途：fixture 里所有数组都是"对象数组"，逐元素切出来后
   * 每个元素都能在一个很小的窗口里被严格解析，
   * 于是"字段缺失"必然在该元素窗口内失败，不会漂到下一个元素。
   */
  splitTopLevelElements(i: number): number[] {
    const out: number[] = [];
    if (this.src.charAt(i) !== '[') {
      return out;
    }
    const closeIdx = this.matchBracket(i);
    if (closeIdx < 0) {
      return out;
    }
    let depth = 0;
    let inStr = false;
    let esc = false;
    let elemStart = -1;
    let k = i + 1;
    while (k < closeIdx) {
      const ch = this.src.charAt(k);
      if (inStr) {
        if (esc) {
          esc = false;
        } else if (ch === '\\') {
          esc = true;
        } else if (ch === '"') {
          inStr = false;
        }
      } else if (ch === '"') {
        inStr = true;
        if (depth === 0 && elemStart < 0) {
          elemStart = k;
        }
      } else if (ch === '{' || ch === '[') {
        if (depth === 0 && elemStart < 0) {
          elemStart = k;
        }
        depth++;
      } else if (ch === '}' || ch === ']') {
        depth--;
        if (depth === 0) {
          out.push(elemStart);
          out.push(k + 1);
          elemStart = -1;
        }
      } else if (ch === ',' && depth === 0) {
        if (elemStart >= 0) {
          out.push(elemStart);
          out.push(k);
          elemStart = -1;
        }
      }
      k++;
    }
    if (elemStart >= 0) {
      out.push(elemStart);
      out.push(closeIdx);
    }
    return out;
  }

  /** 从下标 i 起切成顶层对象序列：[start, end, start, end, ...]。 */
  splitTopLevelObjects(i: number): number[] {
    return this.splitTopLevelElements(i);
  }

  /* ------------------------------------------------------------------ */
  /* 取值                                                                */
  /* ------------------------------------------------------------------ */

  /** 读 JSON 字符串值（要求紧邻位置是 `"`），按 JSON 转义还原。 */
  readString(i: number): string {
    if (i < 0 || this.src.charAt(i) !== '"') {
      return '';
    }
    const out: string[] = [];
    let k = i + 1;
    while (k < this.src.length) {
      const ch = this.src.charAt(k);
      if (ch === '\\') {
        const n = this.src.charAt(k + 1);
        if (n === 'n') {
          out.push('\n');
          k += 2;
          continue;
        }
        if (n === 't') {
          out.push('\t');
          k += 2;
          continue;
        }
        if (n === 'r') {
          out.push('\r');
          k += 2;
          continue;
        }
        if (n === 'b') {
          out.push('\b');
          k += 2;
          continue;
        }
        if (n === 'f') {
          out.push('\f');
          k += 2;
          continue;
        }
        if (n === '"') {
          out.push('"');
          k += 2;
          continue;
        }
        if (n === '\\') {
          out.push('\\');
          k += 2;
          continue;
        }
        if (n === '/') {
          out.push('/');
          k += 2;
          continue;
        }
        if (n === 'u') {
          const hex = this.src.substring(k + 2, k + 6);
          const code = parseInt(hex, 16);
          // 代理对：JSON 里一个星形/emoji 会写成两个 \uXXXX。
          // 分开 push 会导致比对永远不等 —— 必须自己合并。
          const hi = code & 0xfc00;
          if (hi === 0xd800) {
            const hex2 = this.src.substring(k + 8, k + 12);
            const low = parseInt(hex2, 16);
            if ((low & 0xfc00) === 0xdc00) {
              const combined = 0x10000 + ((code & 0x3ff) << 10) + (low & 0x3ff);
              out.push(String.fromCharCode(combined));
              k += 12;
              continue;
            }
          }
          out.push(String.fromCharCode(code));
          k += 6;
          continue;
        }
        out.push(n);
        k += 2;
        continue;
      }
      if (ch === '"') {
        return out.join('');
      }
      out.push(ch);
      k++;
    }
    return '';
  }

  /** 读 JSON 数值（整数或浮点），从下标 i 起。 */
  readNumber(i: number): number {
    let k = i;
    if (k < this.src.length && this.src.charAt(k) === '-') {
      k++;
    }
    while (k < this.src.length) {
      const ch = this.src.charAt(k);
      if ((ch >= '0' && ch <= '9') || ch === '.' || ch === 'e' || ch === 'E' || ch === '+' || ch === '-') {
        k++;
      } else {
        break;
      }
    }
    return Number.parseFloat(this.src.substring(i, k));
  }

  /** 读 JSON 布尔值，非 true 一律 false（调用方只应在确定位置调用）。 */
  readBool(i: number): boolean {
    if (i < 0) {
      return false;
    }
    if (this.src.substring(i, i + 4) === 'true') {
      return true;
    }
    return false;
  }

  /** 判定下标 i 处是否为字面量 null。 */
  isNull(i: number): boolean {
    if (i < 0) {
      return true;
    }
    return this.src.substring(i, i + 4) === 'null';
  }

  /**
   * 取 `name` 字段的字符串值；字段缺失或不是字符串时抛错。
   *
   * 抛错的语义是"fixture 结构与预期契约不符"，而不是"值恰好是空串" ——
   * 两者必须区分，否则一个结构缺陷会被当成一个合法空值静默放过。
   */
  requireString(name: string): string {
    const i = this.findField(name);
    if (i < 0) {
      throw new Error(`JsonText: required string field not found in window: ${name}`);
    }
    if (this.src.charAt(i) !== '"') {
      throw new Error(`JsonText: field ${name} is not a JSON string`);
    }
    return this.readString(i);
  }

  /** 取 `name` 字段的数值；字段缺失时抛错。 */
  requireNumber(name: string): number {
    const i = this.findField(name);
    if (i < 0) {
      throw new Error(`JsonText: required number field not found in window: ${name}`);
    }
    return this.readNumber(i);
  }

  /** 取 `name` 字段的布尔值；字段缺失时抛错。 */
  requireBool(name: string): boolean {
    const i = this.findField(name);
    if (i < 0) {
      throw new Error(`JsonText: required bool field not found in window: ${name}`);
    }
    return this.readBool(i);
  }

  /** 取可为 null 的字符串字段：null 或缺失都返回 null。 */
  optionalString(name: string): string | null {
    const i = this.findField(name);
    if (i < 0 || this.isNull(i)) {
      return null;
    }
    return this.readString(i);
  }

  /** 取字符串字段，缺失时用默认值（用于"契约里有默认值"的可选字段）。 */
  optionalStringOr(name: string, fallback: string): string {
    const i = this.findField(name);
    if (i < 0 || this.isNull(i)) {
      return fallback;
    }
    if (this.src.charAt(i) !== '"') {
      throw new Error(`JsonText: field ${name} is not a JSON string`);
    }
    return this.readString(i);
  }

  /** 取布尔字段，缺失时用默认值。 */
  optionalBoolOr(name: string, fallback: boolean): boolean {
    const i = this.findField(name);
    if (i < 0 || this.isNull(i)) {
      return fallback;
    }
    return this.readBool(i);
  }

  /** 取字符串数组字段（元素必须是 JSON 字符串）。 */
  requireStringArray(name: string): string[] {
    const out: string[] = [];
    const i = this.findField(name);
    if (i < 0 || this.src.charAt(i) !== '[') {
      throw new Error(`JsonText: required string array not found: ${name}`);
    }
    const closeIdx = this.matchBracket(i);
    if (closeIdx < 0) {
      throw new Error(`JsonText: unterminated array for field: ${name}`);
    }
    let k = i + 1;
    while (k < closeIdx) {
      const ch = this.src.charAt(k);
      if (ch === '"') {
        out.push(this.readString(k));
        k = this.skipString(k);
        continue;
      }
      k++;
    }
    return out;
  }

  /** 取数值数组字段（整数）。元素本身是数字字面量，无引号。 */
  requireNumberArray(name: string): number[] {
    const out: number[] = [];
    const i = this.findField(name);
    if (i < 0 || this.src.charAt(i) !== '[') {
      throw new Error(`JsonText: required number array not found: ${name}`);
    }
    const closeIdx = this.matchBracket(i);
    if (closeIdx < 0) {
      throw new Error(`JsonText: unterminated array for field: ${name}`);
    }
    let k = i + 1;
    while (k < closeIdx) {
      const ch = this.src.charAt(k);
      if (ch === '-' || (ch >= '0' && ch <= '9')) {
        const n = this.readNumber(k);
        out.push(n);
        const text = `${n}`;
        k += text.length;
        while (k < closeIdx && this.src.charAt(k) === ' ') {
          k++;
        }
        continue;
      }
      k++;
    }
    return out;
  }

  /** 跳过下标 i 处的字符串（i 必须指向开引号），返回闭引号之后的下标。 */
  skipString(i: number): number {
    let k = i + 1;
    while (k < this.src.length) {
      const ch = this.src.charAt(k);
      if (ch === '\\') {
        k += 2;
        continue;
      }
      if (ch === '"') {
        return k + 1;
      }
      k++;
    }
    return this.src.length;
  }

  /**
   * 在一个对象的字符窗口内判断是否含某个键。
   *
   * 注意：命中不代表键属于**本层**。用于对象数组的场景时，
   * 元素窗口本身就是原子的（由 splitTopLevelElements 切出），
   * 所以"含键"与"本层含键"在此语境下等价。
   */
  windowHasKey(start: number, stop: number, name: string): boolean {
    const token = '"' + name + '"';
    const i = this.src.indexOf(token, start);
    return i >= 0 && i < stop;
  }

  /**
   * 在 [start, stop) 内寻找**已闭合**的顶层对象起点。
   *
   * fixture 中形如 `"nodeNames": { "card-a": "招行信用卡", ... }` 的映射表：
   * 值声明在 "graph" 之下，但 "nodeNames" 这个词在文件里出现两次
   * （`"graph": { ..., "nodeNames": {`）。这里用"向后找第一个已闭合的
   * `{`"来锚定，命中即是真正的映射表，而不是更外层那个大对象。
   */
  findClosedObjectAfter(token: string): number {
    let from = this.pos;
    while (from < this.end) {
      const i = this.src.indexOf(token, from);
      if (i < 0 || i >= this.end) {
        return -1;
      }
      let k = i;
      while (k < this.end) {
        const ch = this.src.charAt(k);
        if (ch === '{') {
          const c = this.matchBracket(k);
          if (c > 0) {
            return k;
          }
          break;
        }
        if (ch === ':' || ch === ',') {
          // 继续向后找值
        }
        k++;
      }
      from = i + token.length;
    }
    return -1;
  }

  /**
   * 解析 `{ "k": "v", ... }` 形式的字符串映射表（nodeNames / leadingTimeByTemplate）。
   */
  parseStringMap(i: number): Map<string, string> {
    const m = new Map<string, string>();
    if (i < 0 || this.src.charAt(i) !== '{') {
      return m;
    }
    const closeIdx = this.matchBracket(i);
    if (closeIdx < 0) {
      return m;
    }
    let k = i + 1;
    while (k < closeIdx) {
      if (this.src.charAt(k) === '"') {
        const key = this.readString(k);
        k = this.skipString(k);
        while (k < closeIdx && (this.isWs(this.src.charAt(k)) || this.src.charAt(k) === ':')) {
          k++;
        }
        if (k < closeIdx && this.src.charAt(k) === '"') {
          const val = this.readString(k);
          m.set(key, val);
          k = this.skipString(k);
        } else {
          // 非字符串值（例如 null）：本工具的消费者只关心字符串表，
          // 用空串占位并继续，不抛错 —— 与"缺失 ≠ 存在"保持可区分。
          m.set(key, '');
        }
        while (k < closeIdx && this.src.charAt(k) !== ',') {
          k++;
        }
        k++;
        continue;
      }
      k++;
    }
    return m;
  }
}

/* ==========================================================================
 * 模块级工具
 * ========================================================================== */

/**
 * 去掉 JSON 文本里的**非语义空白**（字符串字面量之外的空格 / 制表 / 换行 / 回车）。
 *
 * 为什么必须有这一步（2026-09-18 主机执行面实测所得）：
 * fixture 里的 `expected` 是按 2 空格缩进的**美化** JSON，
 * 而实现侧产出的是**紧凑** JSON。harness 的比对契约是
 *
 *     JSON.stringify(actual) === JSON.stringify(expected)
 *
 * 即「空白无关、**键序相关**」。两侧都必须先化成紧凑形式再比；
 * 拿美化文本直接和紧凑文本比，任何用例都不可能相等 ——
 * 首轮主机执行时 41 个用例全挂在这一条上。
 *
 * 本函数**只去空白**：不重排键、不改转义、不做任何美化。
 * 键序是契约的一部分，动它就会把真实缺陷掩盖掉。
 */
export function compactJson(text: string): string {
  let out = '';
  let inStr = false;
  let esc = false;
  for (let i = 0; i < text.length; i++) {
    const c = text.charAt(i);
    if (inStr) {
      out += c;
      if (esc) {
        esc = false;
      } else if (c === '\\') {
        esc = true;
      } else if (c === '"') {
        inStr = false;
      }
      continue;
    }
    if (c === '"') {
      inStr = true;
      out += c;
      continue;
    }
    if (c === ' ' || c === '\t' || c === '\n' || c === '\r') {
      continue;
    }
    out += c;
  }
  return out;
}
