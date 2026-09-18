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

/** RFC 8785 JCS 序列化（受限值域）。 */
export function jcsStringify(value: Object): string {
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
