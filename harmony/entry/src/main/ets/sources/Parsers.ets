// PDIG 账单解析器（WeChat / Generic CSV / OFX-QFX）。
//
// 逐行移植自 android/core/src/main/kotlin/com/pdig/core/sources/Parsers.kt
// （Android 是该文件的**冻结移植源**，语义必须与之一致，不得"重新发明"）。
//
// 硬约束（来自 Canonical Spec）：
//  - 坏行保守拒绝：计入 errors，不抛出整次导入
//  - 金额缺失 ≠ 0 元：空串必须显式拒绝（否则会伪造出一笔不存在的交易）
//  - 日期必须完整日历校验（拒绝 2026-02-30 这类自动纠正结果）
//  - Observation 仅存在于导入会话内存，绝不落库
//
// 分层约束：本模块**不得**依赖 ArkUI / Ability / ArkData / HUKS / NAPI / cryptoFramework，
// 也不依赖任何 @kit.* —— 它必须能在主机执行面（本地单元测试）上直接跑。
// 字符解码由 ./Utf8 提供（严格 UTF-8）；GB18030 解码不在本层，见 decodeBillText 注释。

import { ObservationDirection } from '../generated/CanonicalEnums';
import { decodeStrictUtf8, hasUtf8Bom, stripUtf8Bom } from './Utf8';

/** 需要 GB18030 解码时抛出 —— 主机执行面没有该能力，不得静默降级。 */
export class CharsetNotSupportedError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'CharsetNotSupportedError';
  }
}

export class MissingMappingError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'MissingMappingError';
  }
}

export class Observation {
  source: string = '';
  sourceTxnId: string | null = null;
  merchantTxnId: string | null = null;
  occurredAt: string = '';
  merchantRaw: string = '';
  description: string = '';
  amount: number = 0;
  currency: string = '';
  direction: ObservationDirection = ObservationDirection.NEUTRAL;
  paymentMethodRaw: string = '';
  status: string = '';
  note: string = '';
}

export class ParseError {
  line: number = 0;
  reason: string = '';
}

export class ParseResult {
  observations: Observation[] = [];
  errors: ParseError[] = [];
  sourceLabel: string = '';
}

function err(line: number, reason: string): ParseError {
  const e: ParseError = new ParseError();
  e.line = line;
  e.reason = reason;
  return e;
}

function take(s: string, n: number): string {
  return s.length <= n ? s : s.substring(0, n);
}

/** 越界取单元格 → 空串（对应 Kotlin 的 `cells.getOrElse(i) { "" }`）。 */
function cellAt(cells: string[], i: number): string {
  return i >= 0 && i < cells.length ? cells[i] : '';
}

function isDigit(ch: string): boolean {
  return ch >= '0' && ch <= '9';
}

// ---------------------------------------------------------------------------
// 通用工具
// ---------------------------------------------------------------------------

function jsRound(x: number): number {
  return Math.floor(x + 0.5);
}

function money2(n: number): number {
  return jsRound(n * 100) / 100;
}

function isLeapYear(y: number): boolean {
  return (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0;
}

function daysInMonth(y: number, m: number): number {
  if (m === 2) {
    return isLeapYear(y) ? 29 : 28;
  }
  if (m === 4 || m === 6 || m === 9 || m === 11) {
    return 30;
  }
  return 31;
}

/** 完整日历校验：拒绝"合法但不存在"的日期（如 2026-02-30）。 */
function isValidCalendarDate(y: number, m: number, d: number): boolean {
  return y >= 1 && y <= 9999 && m >= 1 && m <= 12 && d >= 1 && d <= daysInMonth(y, m);
}

function pad2(v: number): string {
  return v < 10 ? '0' + v : '' + v;
}

function pad4(v: number): string {
  if (v >= 1000) {
    return '' + v;
  }
  if (v >= 100) {
    return '0' + v;
  }
  if (v >= 10) {
    return '00' + v;
  }
  return '000' + v;
}

function isoUtc(y: number, mo: number, d: number, h: number, mi: number, s: number): string {
  return pad4(y) + '-' + pad2(mo) + '-' + pad2(d) + 'T' + pad2(h) + ':' + pad2(mi) + ':' +
    pad2(s) + '+00:00';
}

/**
 * UTF-8 BOM → 按 UTF-8；否则严格 UTF-8，失败则需要 GB18030。
 *
 * 与 Android `decodeBillText` 的差异（如实记录，不是等价声明）：
 * Android 在严格 UTF-8 失败后回退 `Charset.forName("GB18030")`。
 * 本端目前**没有** GB18030 解码器（需要 2 万余码点的码表，自造既无依据也无验证手段），
 * 因此这里显式抛错，而不是静默替换成 U+FFFD
 * —— 静默替换会把"这份账单其实是 GBK 的"这个事实彻底抹掉，错的字节会一路流到金额里。
 */
function decodeBillText(bytes: Uint8Array): string {
  if (hasUtf8Bom(bytes)) {
    const decoded: string | undefined = decodeStrictUtf8(stripUtf8Bom(bytes));
    if (decoded === undefined) {
      throw new CharsetNotSupportedError('file has a UTF-8 BOM but is not valid UTF-8');
    }
    return decoded;
  }
  const strict: string | undefined = decodeStrictUtf8(bytes);
  if (strict !== undefined) {
    return strict;
  }
  throw new CharsetNotSupportedError('GB18030 decode is not available on this platform yet');
}

// ---------------------------------------------------------------------------
// CSV
// ---------------------------------------------------------------------------

/** 解析单行 CSV（引号转义 + 引号内逗号）。 */
export function parseCsvLine(line: string): string[] {
  const cells: string[] = [];
  let cur: string = '';
  let inQuotes: boolean = false;
  let i: number = 0;
  while (i < line.length) {
    const ch: string = line.charAt(i);
    if (inQuotes) {
      if (ch === '"') {
        if (i + 1 < line.length && line.charAt(i + 1) === '"') {
          cur += '"';
          i++;
        } else {
          inQuotes = false;
        }
      } else {
        cur += ch;
      }
    } else if (ch === '"') {
      inQuotes = true;
    } else if (ch === ',') {
      cells.push(cur);
      cur = '';
    } else {
      cur += ch;
    }
    i++;
  }
  cells.push(cur);
  return cells;
}

/** 解析完整 CSV（支持引号内换行、CRLF / CR-only / LF）。 */
export function parseCsv(text: string, delimiter: string): string[][] {
  const rows: string[][] = [];
  let row: string[] = [];
  let cur: string = '';
  let inQuotes: boolean = false;
  let i: number = 0;
  while (i < text.length) {
    const ch: string = text.charAt(i);
    if (inQuotes) {
      if (ch === '"') {
        if (i + 1 < text.length && text.charAt(i + 1) === '"') {
          cur += '"';
          i++;
        } else {
          inQuotes = false;
        }
      } else {
        cur += ch;
      }
    } else if (ch === '"') {
      inQuotes = true;
    } else if (ch === delimiter) {
      row.push(cur);
      cur = '';
    } else if (ch === '\r' && i + 1 < text.length && text.charAt(i + 1) === '\n') {
      row.push(cur);
      rows.push(row);
      row = [];
      cur = '';
      i++;
    } else if (ch === '\r' || ch === '\n') {
      row.push(cur);
      rows.push(row);
      row = [];
      cur = '';
    } else {
      cur += ch;
    }
    i++;
  }
  if (cur.length > 0 || row.length > 0) {
    row.push(cur);
    rows.push(row);
  }
  return rows;
}

function splitLines(text: string): string[] {
  const out: string[] = [];
  let cur: string = '';
  let i: number = 0;
  while (i < text.length) {
    const ch: string = text.charAt(i);
    if (ch === '\r') {
      if (i + 1 < text.length && text.charAt(i + 1) === '\n') {
        i++;
      }
      out.push(cur);
      cur = '';
    } else if (ch === '\n') {
      out.push(cur);
      cur = '';
    } else {
      cur += ch;
    }
    i++;
  }
  out.push(cur);
  return out;
}

// ---------------------------------------------------------------------------
// 日期 / 金额（Generic CSV）
// ---------------------------------------------------------------------------

const DATE_TOKENS: string[] = ['YYYY', 'MM', 'DD', 'HH', 'mm', 'ss'];

function tokenAt(fmt: string, i: number): string {
  for (let t: number = 0; t < DATE_TOKENS.length; t++) {
    const tok: string = DATE_TOKENS[t];
    if (fmt.substring(i, i + tok.length) === tok) {
      return tok;
    }
  }
  return '';
}

/** 一次格式匹配的捕获结果（token → 值）。 */
class FormatMatch {
  order: string[] = [];
  values: number[] = [];

  get(t: string): number {
    for (let i: number = 0; i < this.order.length; i++) {
      if (this.order[i] === t) {
        return this.values[i];
      }
    }
    return 0;
  }
}

/**
 * 把 format 串与输入串**逐字符对位**匹配。
 *
 * 为什么不用正则：ArkTS 的 RegExp 支持面不确定；更重要的是 legacy 在这条路上踩过 ——
 * 先把 token 替换成 `(...)` 再对整体做元字符转义，会把捕获组自己的括号一起转义掉，
 * 表现为"任何含日期 token 的格式永远匹配失败"的静默数据丢失。
 */
function matchFormatParts(s: string, fmt: string): FormatMatch | undefined {
  const m: FormatMatch = new FormatMatch();
  let si: number = 0;
  let fi: number = 0;
  while (fi < fmt.length) {
    const tok: string = tokenAt(fmt, fi);
    if (tok.length > 0) {
      const maxLen: number = tok === 'YYYY' ? 4 : 2;
      let digits: string = '';
      while (digits.length < maxLen && si < s.length && isDigit(s.charAt(si))) {
        digits += s.charAt(si);
        si++;
      }
      if (digits.length === 0) {
        return undefined;
      }
      m.order.push(tok);
      m.values.push(Number(digits));
      fi += tok.length;
    } else {
      if (si >= s.length || s.charAt(si) !== fmt.charAt(fi)) {
        return undefined;
      }
      si++;
      fi++;
    }
  }
  if (si !== s.length || m.order.length === 0) {
    return undefined;
  }
  return m;
}

function partsToIso(m: FormatMatch): string | undefined {
  const y: number = m.get('YYYY');
  const mo: number = m.get('MM');
  const d: number = m.get('DD');
  const h: number = m.get('HH');
  const mi: number = m.get('mm');
  const sec: number = m.get('ss');
  if (mo < 1 || mo > 12 || h > 23 || mi > 59 || sec > 59) {
    return undefined;
  }
  if (!isValidCalendarDate(y, mo, d)) {
    return undefined;
  }
  return isoUtc(y, mo, d, h, mi, sec);
}

export function parseMappingDate(raw: string, formats: string[]): string | undefined {
  const s: string = raw.trim();
  for (let i: number = 0; i < formats.length; i++) {
    const m: FormatMatch | undefined = matchFormatParts(s, formats[i]);
    if (m !== undefined) {
      const iso: string | undefined = partsToIso(m);
      if (iso !== undefined) {
        return iso;
      }
    }
  }
  return undefined;
}

function isMoneyNoise(ch: string): boolean {
  return ch === ' ' || ch === '\t' || ch === '\n' || ch === '\r' ||
    ch === '¥' || ch === '￥' || ch === '$' || ch === '€' || ch === '£';
}

/** 按显式 decimalSeparator 解析金额；空串返回 undefined（缺失金额 ≠ 0 元）。 */
export function parseMappingAmount(raw: string, decimalSeparator: string): number | undefined {
  let cleaned: string = '';
  const trimmed: string = raw.trim();
  for (let i: number = 0; i < trimmed.length; i++) {
    const ch: string = trimmed.charAt(i);
    if (!isMoneyNoise(ch)) {
      cleaned += ch;
    }
  }
  let s: string = cleaned;
  if (s.length === 0) {
    return undefined;
  }
  if (decimalSeparator === ',') {
    s = s.split('.').join('').split(',').join('.');
  } else {
    s = s.split(',').join('');
  }
  const n: number = Number(s);
  if (!Number.isFinite(n)) {
    return undefined;
  }
  return money2(n);
}

// ---------------------------------------------------------------------------
// WeChat 账单
// ---------------------------------------------------------------------------

const WECHAT_EXPECTED_COLUMNS: number = 11;

export class WechatParser {
  static parse(bytes: Uint8Array): ParseResult {
    const result: ParseResult = new ParseResult();
    result.sourceLabel = 'wechat';
    const text: string = decodeBillText(bytes);
    const lines: string[] = splitLines(text);

    let headerIdx: number = -1;
    for (let i: number = 0; i < lines.length; i++) {
      const cells: string[] = parseCsvLine(lines[i]);
      if (cells.length > 0 && cells[0].trim() === '交易时间') {
        headerIdx = i;
        break;
      }
    }
    if (headerIdx < 0) {
      result.errors.push(err(0, 'missing column header row (交易时间...)'));
      return result;
    }

    const headerCells: string[] = parseCsvLine(lines[headerIdx]);
    if (headerCells.length === 0 || headerCells[0].trim() !== '交易时间' ||
      headerCells.length < WECHAT_EXPECTED_COLUMNS - 3) {
      result.errors.push(err(headerIdx + 1, 'unexpected column header layout'));
    }

    for (let i: number = headerIdx + 1; i < lines.length; i++) {
      const ln: string = lines[i];
      if (ln.trim().length === 0) {
        continue;
      }
      const cells: string[] = parseCsvLine(ln);
      if (cells.length < 9) {
        result.errors.push(err(i + 1, `expected >=9 columns, got ${cells.length}`));
        continue;
      }
      const rawTime: string = cellAt(cells, 0);
      const occurredAt: string | undefined = WechatParser.parseWechatTime(rawTime);
      if (occurredAt === undefined) {
        result.errors.push(err(i + 1, 'bad transaction time: ' + take(rawTime, 19)));
        continue;
      }
      const rawAmount: string = cellAt(cells, 5);
      const amount: number | undefined = WechatParser.parseAmount(rawAmount);
      if (amount === undefined) {
        result.errors.push(err(i + 1, 'bad amount: ' + take(rawAmount, 12)));
        continue;
      }
      const direction: ObservationDirection | undefined =
        WechatParser.parseDirection(cellAt(cells, 4));
      if (direction === undefined) {
        result.errors.push(err(i + 1, 'bad direction: ' + take(cellAt(cells, 4), 8)));
        continue;
      }
      const sourceTxnId: string = cellAt(cells, 8).trim();
      const merchantTxnId: string = cellAt(cells, 9).trim();

      const o: Observation = new Observation();
      o.source = 'wechat';
      o.sourceTxnId = sourceTxnId.length === 0 ? null : sourceTxnId;
      o.merchantTxnId = merchantTxnId.length === 0 ? null : merchantTxnId;
      o.occurredAt = occurredAt;
      o.merchantRaw = cellAt(cells, 2).trim();
      o.description = cellAt(cells, 3).trim();
      o.amount = Math.abs(amount);
      o.currency = 'CNY';
      o.direction = direction;
      o.paymentMethodRaw = cellAt(cells, 6).trim();
      o.status = cellAt(cells, 7).trim();
      o.note = cellAt(cells, 10).trim();
      result.observations.push(o);
    }
    return result;
  }

  private static parseAmount(raw: string): number | undefined {
    let cleaned: string = '';
    const trimmed: string = raw.trim();
    for (let i: number = 0; i < trimmed.length; i++) {
      const ch: string = trimmed.charAt(i);
      if (!(ch === '¥' || ch === '￥' || ch === ',' || ch === ' ' || ch === '\t')) {
        cleaned += ch;
      }
    }
    if (cleaned.length === 0) {
      return undefined;
    }
    const n: number = Number(cleaned);
    if (!Number.isFinite(n)) {
      return undefined;
    }
    return money2(n);
  }

  private static parseDirection(raw: string): ObservationDirection | undefined {
    const s: string = raw.trim();
    if (s === '收入') {
      return ObservationDirection.IN;
    }
    if (s === '支出') {
      return ObservationDirection.OUT;
    }
    if (s === '/' || s === '中性交易' || s === '') {
      return ObservationDirection.NEUTRAL;
    }
    return undefined;
  }

  /**
   * '2026-01-15 10:23:00' → ISO8601 +08:00（微信账单为中国大陆本地时间）。
   *
   * 宽度**严格**：参考实现是 `^(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})$`，
   * 一位数的月/日/时是属于"坏行"而不是"可容错"—— 放宽会让本该进 errors 的行
   * 变成一笔时间错误的 Observation。
   */
  static parseWechatTime(raw: string): string | undefined {
    const s: string = raw.trim();
    if (s.length !== 19) {
      return undefined;
    }
    const slots: number[] = [0, 5, 8, 11, 14, 17];
    const widths: number[] = [4, 2, 2, 2, 2, 2];
    const seps: string[] = ['-', '-', ' ', ':', ':'];
    for (let k: number = 0; k < slots.length; k++) {
      for (let w: number = 0; w < widths[k]; w++) {
        if (!isDigit(s.charAt(slots[k] + w))) {
          return undefined;
        }
      }
      if (k < seps.length && s.charAt(slots[k] + widths[k]) !== seps[k]) {
        return undefined;
      }
    }
    const y: number = Number(s.substring(0, 4));
    const mo: number = Number(s.substring(5, 7));
    const d: number = Number(s.substring(8, 10));
    const h: number = Number(s.substring(11, 13));
    const mi: number = Number(s.substring(14, 16));
    const sec: number = Number(s.substring(17, 19));
    if (mo < 1 || mo > 12 || h > 23 || mi > 59 || sec > 59) {
      return undefined;
    }
    if (!isValidCalendarDate(y, mo, d)) {
      return undefined;
    }
    return pad4(y) + '-' + pad2(mo) + '-' + pad2(d) + 'T' + pad2(h) + ':' + pad2(mi) + ':' +
      pad2(sec) + '+08:00';
  }
}

// ---------------------------------------------------------------------------
// Generic CSV
// ---------------------------------------------------------------------------

export class MappingColumns {
  transactionId: string | null = null;
  dateTime: string = '';
  amount: string | null = null;
  debit: string | null = null;
  credit: string | null = null;
  description: string | null = null;
  counterparty: string | null = null;
  currency: string | null = null;
  balance: string | null = null;
  transactionType: string | null = null;
  paymentMethod: string | null = null;
}

export class MappingOptions {
  delimiter: string = ',';
  dateFormats: string[] = ['YYYY-MM-DD'];
  decimalSeparator: string = '.';
  amountSignMode: string = 'outward_positive';
  hasHeaderRow: boolean = true;
  encoding: string = 'utf-8';
  positiveDirection: string | null = null;

  constructor() {
    this.dateFormats = ['YYYY-MM-DD'];
  }
}

export class MappingProfile {
  columns: MappingColumns = new MappingColumns();
  options: MappingOptions = new MappingOptions();
}

function wireToDirection(wire: string): ObservationDirection {
  if (wire === 'in') {
    return ObservationDirection.IN;
  }
  if (wire === 'out') {
    return ObservationDirection.OUT;
  }
  if (wire === 'neutral') {
    return ObservationDirection.NEUTRAL;
  }
  // 与 Android `fromWire` 一致：未知 wire 是数据缺陷，不得静默兜底成 neutral。
  throw new Error('unknown direction: ' + wire);
}

export class GenericCsvParser {
  static parse(bytes: Uint8Array, mapping: MappingProfile | null): ParseResult {
    const result: ParseResult = new ParseResult();
    result.sourceLabel = 'generic_csv';
    if (mapping === null) {
      throw new MissingMappingError('generic_csv requires an explicit MappingProfile');
    }
    const text: string = GenericCsvParser.decodeText(bytes, mapping.options.encoding);
    const rawRows: string[][] = parseCsv(text, mapping.options.delimiter);
    const rows: string[][] = mapping.options.hasHeaderRow && rawRows.length > 0 ?
      rawRows.slice(1) : rawRows;
    const header: string[] = rawRows.length > 0 ? rawRows[0] : [];

    const cols: MappingColumns = mapping.columns;
    const cTxn: number = GenericCsvParser.colIndexOf(header, cols.transactionId);
    const cDate: number = GenericCsvParser.colIndexOf(header, cols.dateTime);
    const cAmount: number = GenericCsvParser.colIndexOf(header, cols.amount);
    const cDebit: number = GenericCsvParser.colIndexOf(header, cols.debit);
    const cCredit: number = GenericCsvParser.colIndexOf(header, cols.credit);
    const cDesc: number = GenericCsvParser.colIndexOf(header, cols.description);
    const cCounterparty: number = GenericCsvParser.colIndexOf(header, cols.counterparty);
    const cCurrency: number = GenericCsvParser.colIndexOf(header, cols.currency);
    const cType: number = GenericCsvParser.colIndexOf(header, cols.transactionType);
    const cMethod: number = GenericCsvParser.colIndexOf(header, cols.paymentMethod);

    const signMode: string = mapping.options.amountSignMode;
    const dec: string = mapping.options.decimalSeparator;

    for (let rowIdx: number = 0; rowIdx < rows.length; rowIdx++) {
      const cells: string[] = rows[rowIdx];
      let allBlank: boolean = true;
      for (let k: number = 0; k < cells.length; k++) {
        if (cells[k].trim().length > 0) {
          allBlank = false;
        }
      }
      if (allBlank) {
        continue;
      }
      const lineNo: number = rowIdx + (mapping.options.hasHeaderRow ? 2 : 1);

      if (cDate < 0 || cDate >= cells.length) {
        result.errors.push(err(lineNo, 'missing required date column'));
        continue;
      }
      const rawDate: string = cells[cDate];
      const occurredAt: string | undefined = parseMappingDate(rawDate, mapping.options.dateFormats);
      if (occurredAt === undefined) {
        result.errors.push(err(lineNo, 'bad date: ' + take(rawDate, 24)));
        continue;
      }

      let amount: number | undefined = undefined;
      let direction: ObservationDirection = ObservationDirection.NEUTRAL;
      if (signMode === 'debit_credit') {
        const debit: number | undefined = cDebit >= 0 ?
          parseMappingAmount(cellAt(cells, cDebit), dec) : undefined;
        const credit: number | undefined = cCredit >= 0 ?
          parseMappingAmount(cellAt(cells, cCredit), dec) : undefined;
        if (debit !== undefined && debit > 0) {
          amount = debit;
          direction = ObservationDirection.OUT;
        } else if (credit !== undefined && credit > 0) {
          amount = credit;
          direction = ObservationDirection.IN;
        } else {
          result.errors.push(err(lineNo, 'missing debit/credit amount'));
          continue;
        }
      } else {
        amount = cAmount >= 0 ? parseMappingAmount(cellAt(cells, cAmount), dec) : undefined;
        if (amount === undefined) {
          // TS 侧 cells[-1] → undefined → 字符串 "undefined"；此处必须复现同一串。
          const shown: string = cAmount >= 0 && cAmount < cells.length ?
            take(cells[cAmount], 16) : 'undefined';
          result.errors.push(err(lineNo, 'bad amount: ' + shown));
          continue;
        }
        if (signMode === 'signed') {
          // signed：金额自带符号。positiveDirection 默认 in（负数=支出）。
          // 绝不根据数据分布自动猜测方向 —— 必须由 mapping 显式声明。
          const positiveDirection: string = mapping.options.positiveDirection === null ?
            'in' : mapping.options.positiveDirection;
          if (amount < 0) {
            direction = positiveDirection === 'in' ?
              ObservationDirection.OUT : ObservationDirection.IN;
          } else if (amount > 0) {
            direction = wireToDirection(positiveDirection);
          } else {
            direction = ObservationDirection.NEUTRAL;
          }
          amount = Math.abs(amount);
        } else {
          const pd: string = mapping.options.positiveDirection === null ?
            'out' : mapping.options.positiveDirection;
          direction = wireToDirection(pd);
        }
      }

      const currencyRaw: string = cCurrency >= 0 ? cellAt(cells, cCurrency).trim() : '';
      const txnRaw: string = cTxn >= 0 ? cellAt(cells, cTxn).trim() : '';

      const o: Observation = new Observation();
      o.source = 'generic_csv';
      o.sourceTxnId = txnRaw.length === 0 ? null : txnRaw;
      o.merchantTxnId = null;
      o.occurredAt = occurredAt;
      o.merchantRaw = cCounterparty >= 0 ? cellAt(cells, cCounterparty).trim() : '';
      o.description = cDesc >= 0 ? cellAt(cells, cDesc).trim() : '';
      o.amount = amount;
      o.currency = currencyRaw.length === 0 ? 'XXX' : currencyRaw;
      o.direction = direction;
      o.paymentMethodRaw = cMethod >= 0 ? cellAt(cells, cMethod).trim() : '';
      o.status = cType >= 0 ? cellAt(cells, cType).trim() : '';
      o.note = '';
      result.observations.push(o);
    }
    return result;
  }

  private static colIndexOf(header: string[], name: string | null): number {
    if (name === null) {
      return -1;
    }
    for (let i: number = 0; i < header.length; i++) {
      if (header[i] === name) {
        return i;
      }
    }
    throw new MissingMappingError('mapping column not found in header: ' + name);
  }

  private static decodeText(data: Uint8Array, encoding: string): string {
    if (hasUtf8Bom(data)) {
      const decoded: string | undefined = decodeStrictUtf8(stripUtf8Bom(data));
      if (decoded === undefined) {
        throw new CharsetNotSupportedError('file has a UTF-8 BOM but is not valid UTF-8');
      }
      return decoded;
    }
    const strict: string | undefined = decodeStrictUtf8(data);
    if (strict !== undefined) {
      return strict;
    }
    throw new CharsetNotSupportedError(
      `encoding '${encoding}' requires a charset decoder not available on this platform`,
    );
  }
}

// ---------------------------------------------------------------------------
// OFX / QFX
// ---------------------------------------------------------------------------

export class OfxTxn {
  fitid: string | null = null;
  dtposted: string | null = null;
  trnamt: string | null = null;
  trntype: string | null = null;
  name: string | null = null;
  memo: string | null = null;
  currency: string | null = null;
}

/**
 * `parseTransactions` 的结果。
 *
 * 为什么不写成 `{ transactions: OfxTxn[]; malformed: number }` 内联类型：
 * ArkTS 不允许对象字面量当类型声明（`arkts-no-obj-literals-as-types`），
 * 也不允许未对应显式类型的对象字面量返回值（`arkts-no-untyped-obj-literals`）。
 */
export class OfxScanResult {
  transactions: OfxTxn[] = [];
  malformed: number = 0;
}

function indexOfIgnoreCase(text: string, needle: string, from: number): number {
  return text.toLowerCase().indexOf(needle.toLowerCase(), from);
}

/** 读 `<TAG>VALUE`：跳过标签后的空白，取到 `<` 或行尾为止（与参考实现的正则等价）。 */
function tagValue(body: string, name: string): string | null {
  const openTag: string = '<' + name + '>';
  const at: number = indexOfIgnoreCase(body, openTag, 0);
  if (at < 0) {
    return null;
  }
  let i: number = at + openTag.length;
  while (i < body.length) {
    const ch: string = body.charAt(i);
    if (ch === ' ' || ch === '\t' || ch === '\r' || ch === '\n') {
      i++;
    } else {
      break;
    }
  }
  let out: string = '';
  while (i < body.length) {
    const ch: string = body.charAt(i);
    if (ch === '<' || ch === '\r' || ch === '\n') {
      break;
    }
    out += ch;
    i++;
  }
  return out.trim();
}

export class OfxParser {
  /**
   * OFX v1 是 SGML 风格：`<STMTTRN>` 块，块内 `<TAG>VALUE` 成对出现。
   *
   * 块边界取「下一个 `</STMTTRN>` 与下一个 `<STMTTRN>` 中更靠前的那个」；
   * 两者都没有时取到文本末尾 —— 与参考实现 `Regex("</STMTTRN>|<STMTTRN>")` 一致。
   */
  static parseTransactions(text: string): OfxScanResult {
    const out: OfxScanResult = new OfxScanResult();
    const transactions: OfxTxn[] = out.transactions;
    const openTag: string = '<STMTTRN>';
    const closeTag: string = '</STMTTRN>';

    const opens: number[] = [];
    let scan: number = indexOfIgnoreCase(text, openTag, 0);
    while (scan >= 0) {
      opens.push(scan);
      scan = indexOfIgnoreCase(text, openTag, scan + openTag.length);
    }

    for (let k: number = 0; k < opens.length; k++) {
      const bodyStart: number = opens[k] + openTag.length;
      const nextClose: number = indexOfIgnoreCase(text, closeTag, bodyStart);
      const nextOpen: number = k + 1 < opens.length ? opens[k + 1] : -1;
      let bodyEnd: number = text.length;
      if (nextClose >= 0) {
        bodyEnd = nextClose;
      }
      if (nextOpen >= 0 && nextOpen < bodyEnd) {
        bodyEnd = nextOpen;
      }
      const body: string = text.substring(bodyStart, bodyEnd);
      const fitid: string | null = tagValue(body, 'FITID');
      const trnamt: string | null = tagValue(body, 'TRNAMT');
      if (fitid === null && trnamt === null) {
        out.malformed = out.malformed + 1;
        continue;
      }
      const txn: OfxTxn = new OfxTxn();
      txn.fitid = fitid;
      txn.dtposted = tagValue(body, 'DTPOSTED');
      txn.trnamt = trnamt;
      txn.trntype = tagValue(body, 'TRNTYPE');
      txn.name = tagValue(body, 'NAME');
      txn.memo = tagValue(body, 'MEMO');
      const cur: string | null = tagValue(body, 'CURDEF');
      txn.currency = cur === null ? tagValue(body, 'CURRENCY') : cur;
      transactions.push(txn);
    }
    return out;
  }

  /** OFX DTPOSTED YYYYMMDD[HHMMSS...] → ISO(UTC)。 */
  static parseOfxDate(raw: string): string | undefined {
    const s: string = raw.trim();
    if (s.length < 8) {
      return undefined;
    }
    for (let i: number = 0; i < 8; i++) {
      if (!isDigit(s.charAt(i))) {
        return undefined;
      }
    }
    const y: number = Number(s.substring(0, 4));
    const mo: number = Number(s.substring(4, 6));
    const d: number = Number(s.substring(6, 8));
    let h: number = 0;
    let mi: number = 0;
    let sec: number = 0;
    if (s.length >= 14) {
      h = Number(s.substring(8, 10));
      mi = Number(s.substring(10, 12));
      sec = Number(s.substring(12, 14));
    }
    if (mo < 1 || mo > 12 || h > 23 || mi > 59 || sec > 59) {
      return undefined;
    }
    if (!isValidCalendarDate(y, mo, d)) {
      return undefined;
    }
    return isoUtc(y, mo, d, h, mi, sec);
  }

  /** 必须显式拒绝空串：Number('') === 0 会把"缺失金额"伪造成一笔 0 元交易。 */
  static parseOfxAmount(raw: string): number | undefined {
    const s: string = raw.trim();
    if (s.length === 0) {
      return undefined;
    }
    const n: number = Number(s);
    if (!Number.isFinite(n)) {
      return undefined;
    }
    return money2(n);
  }

  static parse(bytes: Uint8Array): ParseResult {
    const result: ParseResult = new ParseResult();
    result.sourceLabel = 'ofx_qfx';
    const text: string = decodeBillText(bytes);
    const parsed = OfxParser.parseTransactions(text);
    for (let i: number = 0; i < parsed.malformed; i++) {
      result.errors.push(err(i + 1, 'malformed STMTTRN block (no FITID/TRNAMT)'));
    }
    for (let t: number = 0; t < parsed.transactions.length; t++) {
      const txn: OfxTxn = parsed.transactions[t];
      const occurredAt: string | undefined = txn.dtposted === null ?
        undefined : OfxParser.parseOfxDate(txn.dtposted);
      if (occurredAt === undefined) {
        result.errors.push(err(result.observations.length + 1,
          'bad DTPOSTED: ' + (txn.dtposted === null ? 'missing' : take(txn.dtposted, 20))));
        continue;
      }
      const amount: number | undefined = txn.trnamt === null ?
        undefined : OfxParser.parseOfxAmount(txn.trnamt);
      if (amount === undefined) {
        result.errors.push(err(result.observations.length + 1,
          'bad TRNAMT: ' + (txn.trnamt === null ? 'missing' : take(txn.trnamt, 20))));
        continue;
      }
      const o: Observation = new Observation();
      o.source = 'ofx_qfx';
      o.sourceTxnId = txn.fitid;
      o.merchantTxnId = null;
      o.occurredAt = occurredAt;
      o.merchantRaw = txn.name === null ? (txn.memo === null ? '' : txn.memo) : txn.name;
      o.description = txn.memo === null ? '' : txn.memo;
      o.amount = Math.abs(amount);
      o.currency = txn.currency === null ? 'XXX' : txn.currency;
      o.direction = amount < 0 ? ObservationDirection.OUT : ObservationDirection.IN;
      o.paymentMethodRaw = '';
      o.status = txn.trntype === null ? '' : txn.trntype;
      o.note = '';
      result.observations.push(o);
    }
    return result;
  }
}
