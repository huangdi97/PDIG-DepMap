/**
 * RFC 8785 JSON Canonicalization Scheme (JCS) —— ArkTS 受限域实现。
 *
 * 移植自 core/src/crypto/jcs.ts（Core Node reference），值域与之一致：
 * 仅允许 string / 安全整数 / boolean / null / 数组 / 对象。
 * 超出该值域（浮点、bigint、undefined 等）直接抛错，避免静默产出非规范字节。
 *
 * 为什么不用 ArkTS 的 JSON.stringify：
 * 1. JSON.stringify 不保证键序，也不保证转义集合；
 * 2. DEPMAP_CONTAINER_V1 的 AAD 与容器 JSON 必须逐字节可复现 —— 三端
 *    （Core Node / Android Kotlin / Harmony ArkTS）由此互操作。
 *
 * 对象用 JcsObject（而非普通对象字面量）承载，原因：
 * - ArkTS strict 下不做索引签名 / 动态属性访问，Object.keys 亦不可用；
 * - JcsObject 显式持有键与值，排序与序列化完全可控。
 *
 * 状态：IMPLEMENTED。AAD / 容器字节已在主机侧用同构实现复现冻结黄金向量
 * （见 HARMONY_CONTAINER_V1_POC.md）。ArkTS 运行时执行 = NOT_RUN（无设备/镜像）。
 */

export class JcsError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'JcsError';
  }
}

/** RFC 8785 §3.2.2.2 转义：仅转义必须转义的字符，其余原样输出。 */
export function jcsEscapeString(s: string): string {
  let out: string = '"';
  for (let i = 0; i < s.length; i++) {
    const ch: string = s.charAt(i);
    const code: number = s.charCodeAt(i);
    if (ch === '"') {
      out += '\\"';
    } else if (ch === '\\') {
      out += '\\\\';
    } else if (code === 0x08) {
      out += '\\b';
    } else if (code === 0x09) {
      out += '\\t';
    } else if (code === 0x0a) {
      out += '\\n';
    } else if (code === 0x0c) {
      out += '\\f';
    } else if (code === 0x0d) {
      out += '\\r';
    } else if (code < 0x20) {
      out += '\\u00' + code.toString(16).padStart(2, '0');
    } else {
      out += ch;
    }
  }
  return out + '"';
}

/** JCS 对象：显式键值对容器，序列化时按 UTF-16 code unit 排序键。 */
export class JcsObject {
  private keys: string[] = [];
  private values: Object[] = [];

  set(key: string, value: Object): JcsObject {
    this.keys.push(key);
    this.values.push(value);
    return this;
  }

  size(): number {
    return this.keys.length;
  }

  /** 键是否已存在；用于拒绝重复键（JCS 输出必须规范）。 */
  indexOfKey(key: string): number {
    for (let i = 0; i < this.keys.length; i++) {
      if (this.keys[i] === key) {
        return i;
      }
    }
    return -1;
  }

  keyAt(i: number): string {
    return this.keys[i];
  }

  valueAt(i: number): Object {
    return this.values[i];
  }

  /** 按 UTF-16 code unit 升序重排（RFC 8785 §3.2.3）。 */
  sortKeys(): void {
    const n: number = this.keys.length;
    for (let i = 1; i < n; i++) {
      const k: string = this.keys[i];
      const v: Object = this.values[i];
      let j: number = i - 1;
      while (j >= 0 && this.keys[j] > k) {
        this.keys[j + 1] = this.keys[j];
        this.values[j + 1] = this.values[j];
        j--;
      }
      this.keys[j + 1] = k;
      this.values[j + 1] = v;
    }
  }
}

/**
 * JSON 文本 → JCS 值树（受限值域）。
 *
 * 为什么不直接用 `JSON.parse`：
 *   1. ArkTS strict 下拿不到 any / 动态索引，也无法枚举解析出来的键
 *      （`Object.keys` 不可用），而 JCS 的第一步就是**按 UTF-16 code unit 排序键**；
 *   2. JCS 要求**拒绝而不是静默接受**越界值（浮点、非安全整数），
 *      解析期就要有明确的失败点，而不是等到序列化。
 * 因此这里做受限递推下降解析：非法输入一律抛 `JcsError`，绝不兜底成默认值。
 *
 * 为什么放在 Jcs.ets 而不是新建文件：conformance 的可达性 Gate 按模块名单检查，
 * 新增文件要同步登记；这里是 JCS 的另一半（前半 online 序列化），语义上本属同一模块。
 */
class JcsParseCursor {
  private src: string = '';
  private pos: number = 0;

  constructor(src: string) {
    this.src = src;
    this.pos = 0;
  }

  skipWs(): void {
    while (this.pos < this.src.length) {
      const c: string = this.src.charAt(this.pos);
      if (c === ' ' || c === '\t' || c === '\n' || c === '\r') {
        this.pos++;
      } else {
        break;
      }
    }
  }

  atEnd(): boolean {
    return this.pos >= this.src.length;
  }

  peek(): string {
    return this.src.charAt(this.pos);
  }

  expect(ch: string): void {
    if (this.pos >= this.src.length || this.src.charAt(this.pos) !== ch) {
      throw new JcsError(`JCS parse: expected '${ch}' at ${this.pos}`);
    }
    this.pos++;
  }

  parseValue(): Object {
    this.skipWs();
    if (this.pos >= this.src.length) {
      throw new JcsError('JCS parse: unexpected end of input');
    }
    const ch: string = this.src.charAt(this.pos);
    if (ch === '{') {
      return this.parseObject();
    }
    if (ch === '[') {
      return this.parseArray();
    }
    if (ch === '"') {
      return this.parseString();
    }
    if (this.literalAt('true')) {
      return true;
    }
    if (this.literalAt('false')) {
      return false;
    }
    if (this.literalAt('null')) {
      // ArkTS strict 下 `Object` 不允许为 null，因此 JSON 的 null 用一个显式哨兵承载。
      return new JcsNull();
    }
    return this.parseNumber();
  }

  /**
   * 关键字字面量匹配（不用 `String.startsWith`：ArkTS 对该重载的支持不确定，
   * 逐字符比较在所有 ArkTS 版本上都成立）。
   */
  private literalAt(word: string): boolean {
    for (let i = 0; i < word.length; i++) {
      if (this.src.charAt(this.pos + i) !== word.charAt(i)) {
        return false;
      }
    }
    this.pos += word.length;
    return true;
  }

  private parseObject(): JcsObject {
    this.expect('{');
    const obj: JcsObject = new JcsObject();
    this.skipWs();
    if (this.peek() === '}') {
      this.pos++;
      return obj;
    }
    while (true) {
      this.skipWs();
      const key: string = this.parseString();
      this.skipWs();
      this.expect(':');
      const value: Object = this.parseValue();
      // 重复键必须显式拒绝：JCS 输出里同一个键出现两次会产出非规范字节，
      // 而"后写覆盖先写"这种 JSON.parse 语义会让差异被静默吞掉。
      if (obj.indexOfKey(key) >= 0) {
        throw new JcsError(`JCS parse: duplicate object key ${key}`);
      }
      obj.set(key, value);
      this.skipWs();
      const c: string = this.peek();
      if (c === ',') {
        this.pos++;
        continue;
      }
      if (c === '}') {
        this.pos++;
        return obj;
      }
      throw new JcsError(`JCS parse: expected ',' or '}' at ${this.pos}`);
    }
  }

  private parseArray(): Object[] {
    this.expect('[');
    const out: Object[] = [];
    this.skipWs();
    if (this.peek() === ']') {
      this.pos++;
      return out;
    }
    while (true) {
      out.push(this.parseValue());
      this.skipWs();
      const c: string = this.peek();
      if (c === ',') {
        this.pos++;
        continue;
      }
      if (c === ']') {
        this.pos++;
        return out;
      }
      throw new JcsError(`JCS parse: expected ',' or ']' at ${this.pos}`);
    }
  }

  /** 解析 JSON 字符串并做转义还原（含代理对的逐个 \uXXXX）。 */
  private parseString(): string {
    this.expect('"');
    let out: string = '';
    while (true) {
      if (this.pos >= this.src.length) {
        throw new JcsError('JCS parse: unterminated string');
      }
      const c: string = this.src.charAt(this.pos);
      if (c === '"') {
        this.pos++;
        return out;
      }
      if (c !== '\\') {
        out += c;
        this.pos++;
        continue;
      }
      const n: string = this.src.charAt(this.pos + 1);
      if (n === 'u') {
        out += String.fromCharCode(this.parseHex4(this.pos + 2));
        this.pos += 6;
        continue;
      }
      if (n === '"' || n === '\\' || n === '/') {
        out += n;
      } else if (n === 'b') {
        out += '\b';
      } else if (n === 'f') {
        out += '\f';
      } else if (n === 'n') {
        out += '\n';
      } else if (n === 'r') {
        out += '\r';
      } else if (n === 't') {
        out += '\t';
      } else {
        throw new JcsError(`JCS parse: bad escape \\${n}`);
      }
      this.pos += 2;
    }
  }

  private parseHex4(at: number): number {
    let v: number = 0;
    for (let i = 0; i < 4; i++) {
      const idx: number = at + i;
      if (idx >= this.src.length) {
        throw new JcsError('JCS parse: truncated \\u escape');
      }
      const code: number = this.src.charCodeAt(idx);
      let d: number = -1;
      if (code >= 48 && code <= 57) {
        d = code - 48;
      } else if (code >= 97 && code <= 102) {
        d = code - 87;
      } else if (code >= 65 && code <= 70) {
        d = code - 55;
      }
      if (d < 0) {
        throw new JcsError('JCS parse: bad hex digit in \\u escape');
      }
      v = v * 16 + d;
    }
    return v;
  }

  /**
   * 数字：受限值域只接受**安全整数**。
   * 浮点 / 越界整数在这里就被拒 —— 这才是 fixture 里 `rejections.float` 期望的行为。
   */
  private parseNumber(): number {
    const start: number = this.pos;
    while (this.pos < this.src.length) {
      const c: string = this.src.charAt(this.pos);
      const isNumChar: boolean =
        (c >= '0' && c <= '9') || c === '-' || c === '+' || c === '.' || c === 'e' || c === 'E';
      if (!isNumChar) {
        break;
      }
      this.pos++;
    }
    const text: string = this.src.substring(start, this.pos);
    const n: number = Number(text);
    if (!Number.isFinite(n) || !Number.isSafeInteger(n)) {
      throw new JcsError(`JCS restricted domain: only safe integers allowed, got ${text}`);
    }
    return n;
  }
}

/** 解析一段 JSON 文本为 JCS 值树；越界即抛 `JcsError`。 */
export function jcsParse(text: string): Object {
  const cur: JcsParseCursor = new JcsParseCursor(text);
  const value: Object = cur.parseValue();
  cur.skipWs();
  if (!cur.atEnd()) {
    throw new JcsError('JCS parse: trailing content after value');
  }
  return value;
}

/**
 * JSON `null` 的显式载体。
 *
 * 为什么不用 `null` 字面量：ArkTS strict 下 `Object` 类型域不包含 null
 * （实测：`Type 'null' is not assignable to type 'Object'`）。
 * 用一个类型承载"这里是 JSON null"比放宽类型域更安全 —— 后者会让
 * "忘了处理 null"从编译错误退化成运行时行为差异。
 */
export class JcsNull {
}

/** RFC 8785 JCS 序列化（受限值域）。 */
export function jcsStringify(value: Object): string {
  if (value instanceof JcsNull) {
    return 'null';
  }
  if (value === null) {
    return 'null';
  }
  if (typeof value === 'boolean') {
    return value ? 'true' : 'false';
  }
  if (typeof value === 'string') {
    return jcsEscapeString(value as string);
  }
  if (typeof value === 'number') {
    const n: number = value as number;
    // 受限值域：只允许安全整数。非整数 / 越界整数一律抛错，
    // 不允许静默退化为浮点输出（那会破坏跨端字节一致）。
    if (!Number.isSafeInteger(n)) {
      throw new JcsError(`JCS restricted domain: only safe integers allowed, got ${n}`);
    }
    return n.toString();
  }
  if (value instanceof JcsObject) {
    const obj: JcsObject = value as JcsObject;
    obj.sortKeys();
    let out: string = '{';
    for (let i = 0; i < obj.size(); i++) {
      if (i > 0) {
        out += ',';
      }
      out += jcsEscapeString(obj.keyAt(i)) + ':' + jcsStringify(obj.valueAt(i));
    }
    return out + '}';
  }
  if (value instanceof Array) {
    const arr: Array<Object> = value as Array<Object>;
    let out2: string = '[';
    for (let i = 0; i < arr.length; i++) {
      if (i > 0) {
        out2 += ',';
      }
      out2 += jcsStringify(arr[i]);
    }
    return out2 + ']';
  }
  throw new JcsError('JCS restricted domain: unsupported value type');
}
