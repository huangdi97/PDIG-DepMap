// PDIG 账单解析器（WeChat / Generic CSV / OFX-QFX）—— Swift 移植。
//
// 源头：android/core/.../sources/Parsers.kt（Android 已 CORE_FROZEN，是本端口的基准）。
// 本文件是**忠实移植**，不是重新设计：任何行为差异都必须先改 spec/ 再改三端。
//
// 硬约束（来自 Canonical Spec）：
//  - 坏行保守拒绝：计入 errors，不抛出整次导入
//  - 金额缺失 ≠ 0 元：空串必须显式拒绝（否则会伪造出一笔不存在的交易）
//  - 日期必须完整日历校验（拒绝 2026-02-30 这类自动纠正结果）
//  - Observation 仅存在于导入会话内存，绝不落库
//
// 解码策略：与 Harmony 侧 `sources/Parsers.ets` 同口径（严格 UTF-8 → 仅在 mapping
// 明确声明 GB18030 系编码时回退 GB18030；解不出来就抛错）。Android 的
// `decodeGb18030` 在异常时做 lossy UTF-8 兜底，本端不做——差异方向是**保守**的：
// 拒绝会变成一条可解释的错误，静默解错会变成一条错误的金额。

import Foundation

// ---------------------------------------------------------------------------
// 错误类型
// ---------------------------------------------------------------------------

public struct MissingMappingError: Error, CustomStringConvertible, Equatable {
    public let message: String
    public init(_ message: String) { self.message = message }
    public var description: String { message }
}

public struct UnknownDirectionError: Error, CustomStringConvertible, Equatable {
    public let wire: String
    public init(_ wire: String) { self.wire = wire }
    public var description: String { "unknown direction: \(wire)" }
}

// ---------------------------------------------------------------------------
// 数据模型
// ---------------------------------------------------------------------------

/// `ObservationDirection` 由 `Generated/CanonicalEnums.swift` 提供（codegen 单一来源），
/// 本文件不再重复定义。wire 解析沿用 Android 的失败语义：未知值抛错，不静默兜底。
extension ObservationDirection {
    public static func fromWire(_ w: String) throws -> ObservationDirection {
        guard let d = ObservationDirection(rawValue: w) else {
            throw UnknownDirectionError(w)
        }
        return d
    }
}

public struct Observation: Equatable, Sendable {
    public let source: String
    public let sourceTxnId: String?
    public let merchantTxnId: String?
    public let occurredAt: String
    public let merchantRaw: String
    public let description: String
    public let amount: Double
    public let currency: String
    public let direction: ObservationDirection
    public let paymentMethodRaw: String
    public let status: String
    public let note: String

    public init(
        source: String,
        sourceTxnId: String?,
        merchantTxnId: String?,
        occurredAt: String,
        merchantRaw: String,
        description: String,
        amount: Double,
        currency: String,
        direction: ObservationDirection,
        paymentMethodRaw: String,
        status: String,
        note: String
    ) {
        self.source = source
        self.sourceTxnId = sourceTxnId
        self.merchantTxnId = merchantTxnId
        self.occurredAt = occurredAt
        self.merchantRaw = merchantRaw
        self.description = description
        self.amount = amount
        self.currency = currency
        self.direction = direction
        self.paymentMethodRaw = paymentMethodRaw
        self.status = status
        self.note = note
    }
}

public struct ParseError: Equatable, Sendable {
    public let line: Int
    public let reason: String
    public init(_ line: Int, _ reason: String) {
        self.line = line
        self.reason = reason
    }
}

public struct ParseResult: Equatable, Sendable {
    public let observations: [Observation]
    public let errors: [ParseError]
    public let sourceLabel: String

    public init(_ observations: [Observation], _ errors: [ParseError], _ sourceLabel: String = "") {
        self.observations = observations
        self.errors = errors
        self.sourceLabel = sourceLabel
    }
}

/// 显式字段映射（禁止 AI 自动映射；mapping 必须由用户/导入向导明确给出）。
public struct MappingColumns: Equatable, Sendable {
    public let transactionId: String?
    public let dateTime: String
    public let amount: String?
    public let debit: String?
    public let credit: String?
    public let description: String?
    public let counterparty: String?
    public let currency: String?
    public let balance: String?
    public let transactionType: String?
    public let paymentMethod: String?

    public init(
        transactionId: String? = nil,
        dateTime: String,
        amount: String? = nil,
        debit: String? = nil,
        credit: String? = nil,
        description: String? = nil,
        counterparty: String? = nil,
        currency: String? = nil,
        balance: String? = nil,
        transactionType: String? = nil,
        paymentMethod: String? = nil
    ) {
        self.transactionId = transactionId
        self.dateTime = dateTime
        self.amount = amount
        self.debit = debit
        self.credit = credit
        self.description = description
        self.counterparty = counterparty
        self.currency = currency
        self.balance = balance
        self.transactionType = transactionType
        self.paymentMethod = paymentMethod
    }
}

public struct MappingOptions: Equatable, Sendable {
    public let delimiter: String
    public let dateFormats: [String]
    public let decimalSeparator: Character
    public let amountSignMode: String
    public let hasHeaderRow: Bool
    public let encoding: String
    public let positiveDirection: String?

    public init(
        delimiter: String = ",",
        dateFormats: [String] = ["YYYY-MM-DD"],
        decimalSeparator: Character = ".",
        amountSignMode: String = "outward_positive",
        hasHeaderRow: Bool = true,
        encoding: String = "utf-8",
        positiveDirection: String? = nil
    ) {
        self.delimiter = delimiter
        self.dateFormats = dateFormats
        self.decimalSeparator = decimalSeparator
        self.amountSignMode = amountSignMode
        self.hasHeaderRow = hasHeaderRow
        self.encoding = encoding
        self.positiveDirection = positiveDirection
    }
}

public struct MappingProfile: Equatable, Sendable {
    public let columns: MappingColumns
    public let options: MappingOptions
    public init(columns: MappingColumns, options: MappingOptions) {
        self.columns = columns
        self.options = options
    }
}

// ---------------------------------------------------------------------------
// 通用工具
// ---------------------------------------------------------------------------

/// 与 JS `Math.round` 对齐：half 向 +∞ 取整（金额恒为非负数时等价）。
internal func jsRound(_ x: Double) -> Double { floor(x + 0.5) }

/// 与 JS `Number.isFinite` 对齐。
internal func isFiniteJs(_ x: Double) -> Bool { x.isFinite }

/// 两位小数金额归一（与 TS `Math.round(n * 100) / 100` 同语义）。
internal func money2(_ n: Double) -> Double { jsRound(n * 100) / 100 }

internal func isLeapYear(_ y: Int) -> Bool { (y % 4 == 0 && y % 100 != 0) || y % 400 == 0 }

internal func daysInMonth(_ y: Int, _ m: Int) -> Int {
    switch m {
    case 2: return isLeapYear(y) ? 29 : 28
    case 4, 6, 9, 11: return 30
    default: return 31
    }
}

/// 完整日历校验：拒绝 JS Date 自动纠正产生的"合法但不存在"的日期。
internal func isValidCalendarDate(_ y: Int, _ m: Int, _ d: Int) -> Bool {
    y >= 1 && y <= 9999 && m >= 1 && m <= 12 && d >= 1 && d <= daysInMonth(y, m)
}

internal func pad2(_ v: Int) -> String { v < 10 ? "0\(v)" : "\(v)" }

internal func pad4(_ v: Int) -> String {
    let s = "\(v)"
    return s.count >= 4 ? s : String(repeating: "0", count: 4 - s.count) + s
}

internal func isoUtc(_ y: Int, _ mo: Int, _ d: Int, _ h: Int, _ mi: Int, _ s: Int) -> String {
    "\(pad4(y))-\(pad2(mo))-\(pad2(d))T\(pad2(h)):\(pad2(mi)):\(pad2(s))+00:00"
}

/// Java `String.trim()` / `Character.isWhitespace` 语义：ASCII 控制空白 + Unicode
/// 分隔符，但**排除** NBSP 家族（0x00A0 / 0x2007 / 0x202F）。
///
/// 注：ArkTS 侧用的是 JS `String.prototype.trim()`，它会连 U+00A0 / U+FEFF 一起
/// 去掉。三端在这个字符类上并非完全一致；本端对齐 Android（frozen 仲裁端）。
internal func isJavaWhitespace(_ c: Character) -> Bool {
    guard let v = c.unicodeScalars.first?.value, c.unicodeScalars.count == 1 else { return false }
    if v == 0x20 || v == 0x09 || v == 0x0A || v == 0x0B || v == 0x0C || v == 0x0D { return true }
    if v >= 0x1C && v <= 0x1F { return true }
    if v == 0x00A0 || v == 0x2007 || v == 0x202F { return false }
    if v == 0x1680 { return true }
    if v >= 0x2000 && v <= 0x200A { return true }
    if v == 0x2028 || v == 0x2029 || v == 0x205F || v == 0x3000 { return true }
    return false
}

internal func kotlinTrim(_ s: String) -> String {
    var chars = Array(s)
    var start = 0
    var end = chars.count
    while start < end, isJavaWhitespace(chars[start]) { start += 1 }
    while end > start, isJavaWhitespace(chars[end - 1]) { end -= 1 }
    return String(chars[start..<end])
}

internal func kotlinIsBlank(_ s: String) -> Bool { kotlinTrim(s).isEmpty }

internal func take(_ s: String, _ n: Int) -> String { String(s.prefix(n)) }

internal func hasUtf8Bom(_ bytes: [UInt8]) -> Bool { Gb18030.hasUtf8Bom(bytes) }

internal func stripBom(_ bytes: [UInt8]) -> [UInt8] {
    hasUtf8Bom(bytes) ? Array(bytes.dropFirst(3)) : bytes
}

/// UTF-8 BOM → 严格 utf-8；否则严格 utf-8，失败则回退 GB18030。
internal func decodeBillText(_ bytes: [UInt8]) throws -> String {
    if hasUtf8Bom(bytes) {
        guard let decoded = Gb18030.decodeStrictUtf8(stripBom(bytes)) else {
            throw BillTextDecodeError.bomButNotUtf8
        }
        return decoded
    }
    if let strict = Gb18030.decodeStrictUtf8(bytes) { return strict }
    guard let gb = Gb18030.decode(bytes) else {
        throw BillTextDecodeError.notUtf8AndNotGb18030("unknown")
    }
    return gb
}

/** 与 Kotlin `String.split(Regex("\\r\\n|\\r|\\n"))` 对齐：CRLF / CR / LF，丢弃尾部空串。 */
internal func splitLines(_ text: String) -> [String] {
    var rows: [String] = []
    var cur = ""
    let chars = Array(text)
    var i = 0
    while i < chars.count {
        let ch = chars[i]
        if ch == "\r" {
            if i + 1 < chars.count && chars[i + 1] == "\n" { i += 1 }
            rows.append(cur); cur = ""
        } else if ch == "\n" {
            rows.append(cur); cur = ""
        } else {
            cur.append(ch)
        }
        i += 1
    }
    rows.append(cur)
    while let last = rows.last, last.isEmpty { rows.removeLast() }
    return rows
}

// ---------------------------------------------------------------------------
// CSV
// ---------------------------------------------------------------------------

/// 解析单行 CSV（引号转义 + 引号内逗号）。
public func parseCsvLine(_ line: String) -> [String] {
    var cells: [String] = []
    var cur = ""
    var inQuotes = false
    let chars = Array(line)
    var i = 0
    while i < chars.count {
        let ch = chars[i]
        if inQuotes {
            if ch == "\"" {
                if i + 1 < chars.count && chars[i + 1] == "\"" {
                    cur.append("\"")
                    i += 1
                } else {
                    inQuotes = false
                }
            } else {
                cur.append(ch)
            }
        } else if ch == "\"" {
            inQuotes = true
        } else if ch == "," {
            cells.append(cur); cur = ""
        } else {
            cur.append(ch)
        }
        i += 1
    }
    cells.append(cur)
    return cells
}

/// 解析完整 CSV（支持引号内换行、CRLF / CR-only / LF）。
public func parseCsv(_ text: String, _ delimiter: String) -> [[String]] {
    var rows: [[String]] = []
    var row: [String] = []
    var cur = ""
    var inQuotes = false
    let chars = Array(text)
    var i = 0
    while i < chars.count {
        let ch = chars[i]
        if inQuotes {
            if ch == "\"" {
                if i + 1 < chars.count && chars[i + 1] == "\"" {
                    cur.append("\"")
                    i += 1
                } else {
                    inQuotes = false
                }
            } else {
                cur.append(ch)
            }
        } else if ch == "\"" {
            inQuotes = true
        } else if String(ch) == delimiter {
            row.append(cur); cur = ""
        } else if ch == "\r" && i + 1 < chars.count && chars[i + 1] == "\n" {
            row.append(cur); rows.append(row); row = []; cur = ""
            i += 1
        } else if ch == "\r" || ch == "\n" {
            row.append(cur); rows.append(row); row = []; cur = ""
        } else {
            cur.append(ch)
        }
        i += 1
    }
    if !cur.isEmpty || !row.isEmpty {
        row.append(cur)
        rows.append(row)
    }
    return rows
}

// ---------------------------------------------------------------------------
// 日期 / 金额（Generic CSV）
// ---------------------------------------------------------------------------

private let dateTokens: [(String, String)] = [
    ("YYYY", "(\\d{4})"),
    ("MM", "(\\d{1,2})"),
    ("DD", "(\\d{1,2})"),
    ("HH", "(\\d{1,2})"),
    ("mm", "(\\d{1,2})"),
    ("ss", "(\\d{1,2})"),
]

/**
 * 把 format 字符串编译为正则 + 捕获组顺序。
 *
 * 不得回退的实现要点（legacy 曾踩过）：**先按 token 切分并逐段转义，再拼装捕获组**。
 * 若先把 token 替换成 `(\d{4})`、再对整体做正则元字符转义，会连捕获组自身的括号一起
 * 转义成 `\(\d\{4\}\)`，导致任何含日期 token 的格式永远匹配失败 —— 表现为"所有行
 * 都是 bad date"的静默数据丢失。
 */
private func compileFormat(_ fmt: String) -> (String, [String]) {
    var order: [String] = []
    var source = ""
    let chars = Array(fmt)
    var i = 0
    while i < chars.count {
        var hit: (String, String)? = nil
        for t in dateTokens {
            let tk = Array(t.0)
            if i + tk.count <= chars.count && Array(chars[i..<(i + tk.count)]) == tk {
                hit = t
                break
            }
        }
        if let h = hit {
            order.append(h.0)
            source += h.1
            i += h.0.count
        } else {
            let c = chars[i]
            if ".*+?^${}()|[]\\".contains(c) { source.append("\\") }
            source.append(c)
            i += 1
        }
    }
    return (source, order)
}

private func matchFormat(_ s: String, _ fmt: String) -> String? {
    let (source, order) = compileFormat(fmt)
    if order.isEmpty { return nil }
    guard let re = try? NSRegularExpression(pattern: "^\(source)$") else { return nil }
    let ns = s as NSString
    guard let m = re.firstMatch(in: s, range: NSRange(location: 0, length: ns.length)) else { return nil }
    var values: [Int] = []
    for i in 1...order.count {
        let r = m.range(at: i)
        if r.location == NSNotFound {
            values.append(0)
        } else {
            values.append(Int(ns.substring(with: r)) ?? 0)
        }
    }
    func get(_ t: String) -> Int {
        guard let idx = order.firstIndex(of: t) else { return 0 }
        return idx < values.count ? values[idx] : 0
    }
    let y = get("YYYY")
    let mo = get("MM")
    let d = get("DD")
    let h = get("HH")
    let mi = get("mm")
    let sec = get("ss")
    if mo < 1 || mo > 12 || h > 23 || mi > 59 || sec > 59 { return nil }
    if !isValidCalendarDate(y, mo, d) { return nil }
    return isoUtc(y, mo, d, h, mi, sec)
}

public func parseMappingDate(_ raw: String, _ formats: [String]) -> String? {
    let s = kotlinTrim(raw)
    for fmt in formats {
        if let r = matchFormat(s, fmt) { return r }
    }
    return nil
}

/// 按显式 decimalSeparator 解析金额；空串返回 nil（缺失金额 ≠ 0 元）。
public func parseMappingAmount(_ raw: String, _ decimalSeparator: Character) -> Double? {
    var s = kotlinTrim(raw)
    s = String(s.filter { !isAmountNoiseChar($0) })
    if s.isEmpty { return nil }
    if decimalSeparator == "," {
        s = s.replacingOccurrences(of: ".", with: "").replacingOccurrences(of: ",", with: ".")
    } else {
        s = s.replacingOccurrences(of: ",", with: "")
    }
    guard let n = javaLikeDouble(s) else { return nil }
    if !isFiniteJs(n) { return nil }
    return money2(n)
}

/// Kotlin `Regex("[\\s¥￥\$€£]")` 的字符集：ASCII 空白 + 常见货币符号。
private func isAmountNoiseChar(_ c: Character) -> Bool {
    if isJavaWhitespace(c) { return true }
    return "¥￥$€£".contains(c)
}

/// 与 Kotlin `String.toDoubleOrNull()` 对齐（≈ Java `Double.parseDouble`）：
/// 要求整串可解析，接受可选正负号；不接受千分位、不接受后缀单位。
private func javaLikeDouble(_ s: String) -> Double? {
    if s.isEmpty { return nil }
    // Java 不接受前后空白（调用方已 trim）；Swift 的 Double(_:) 亦要求整串。
    guard let v = Double(s) else { return nil }
    return v
}

// ---------------------------------------------------------------------------
// WeChat 账单
// ---------------------------------------------------------------------------

public enum WechatParser {

    private static let expectedColumns = [
        "交易时间", "交易类型", "交易对方", "商品", "收/支",
        "金额(元)", "支付方式", "当前状态", "交易单号", "商户单号", "备注",
    ]

    public static func detect(_ text: String) -> Double {
        if text.contains("微信支付账单明细") { return 0.99 }
        if text.contains("交易时间") && text.contains("交易对方") && text.contains("收/支") { return 0.9 }
        return 0.0
    }

    private static func parseAmount(_ raw: String) -> Double? {
        var s = kotlinTrim(raw)
        s = String(s.filter { !"¥￥, ".contains($0) && !isJavaWhitespace($0) })
        if s.isEmpty { return nil }
        guard let n = javaLikeDouble(s) else { return nil }
        if !isFiniteJs(n) { return nil }
        return money2(n)
    }

    private static func parseDirection(_ raw: String) -> ObservationDirection? {
        switch kotlinTrim(raw) {
        case "收入": return .in
        case "支出": return .out
        case "/", "中性交易", "": return .neutral
        default: return nil
        }
    }

    /// '2026-01-15 10:23:00' → ISO8601 +08:00（微信账单为中国大陆本地时间）。
    public static func parseWechatTime(_ raw: String) -> String? {
        let s = kotlinTrim(raw)
        guard let re = try? NSRegularExpression(pattern: "^(\\d{4})-(\\d{2})-(\\d{2}) (\\d{2}):(\\d{2}):(\\d{2})$")
        else { return nil }
        let ns = s as NSString
        guard let m = re.firstMatch(in: s, range: NSRange(location: 0, length: ns.length)) else { return nil }
        func g(_ i: Int) -> Int { Int(ns.substring(with: m.range(at: i))) ?? 0 }
        let y = g(1), mo = g(2), d = g(3), h = g(4), mi = g(5), sec = g(6)
        if mo < 1 || mo > 12 { return nil }
        if h > 23 || mi > 59 || sec > 59 { return nil }
        if !isValidCalendarDate(y, mo, d) { return nil }
        return "\(pad4(y))-\(pad2(mo))-\(pad2(d))T\(pad2(h)):\(pad2(mi)):\(pad2(sec))+08:00"
    }

    private static func findHeaderRowIndex(_ lines: [String]) -> Int {
        for (i, line) in lines.enumerated() {
            let cells = parseCsvLine(line)
            if cells.first.map(kotlinTrim) == "交易时间" { return i }
        }
        return -1
    }

    public static func parse(_ raw: [UInt8]) throws -> ParseResult {
        let text = try decodeBillText(raw)
        let lines = splitLines(text)
        var observations: [Observation] = []
        var errors: [ParseError] = []

        let headerIdx = findHeaderRowIndex(lines)
        if headerIdx == -1 {
            return ParseResult(observations, [ParseError(0, "missing column header row (交易时间...)")], "wechat")
        }
        // 列头行本身校验（列漂移容错：只要求首列匹配 + 最小列数）
        let headerCells = parseCsvLine(lines[headerIdx])
        if headerCells.first.map(kotlinTrim) != "交易时间" ||
            headerCells.count < expectedColumns.count - 3
        {
            errors.append(ParseError(headerIdx + 1, "unexpected column header layout"))
        }

        var i = headerIdx + 1
        while i < lines.count {
            let line = lines[i]
            if !kotlinIsBlank(line) {
                let cells = parseCsvLine(line)
                if cells.count < 9 {
                    errors.append(ParseError(i + 1, "expected >=9 columns, got \(cells.count)"))
                } else {
                    let rawTime = cells[0]
                    guard let occurredAt = parseWechatTime(rawTime) else {
                        errors.append(ParseError(i + 1, "bad transaction time: \(take(rawTime, 19))"))
                        i += 1
                        continue
                    }
                    let rawAmount = cells[5]
                    guard let amount = parseAmount(rawAmount) else {
                        errors.append(ParseError(i + 1, "bad amount: \(take(rawAmount, 12))"))
                        i += 1
                        continue
                    }
                    guard let direction = parseDirection(cells[4]) else {
                        errors.append(ParseError(i + 1, "bad direction: \(take(cells[4], 8))"))
                        i += 1
                        continue
                    }
                    let sourceTxnId = kotlinTrim(cells[8])
                    let merchantTxnId = kotlinTrim(cells[9])
                    let status = kotlinTrim(cells[7])

                    observations.append(
                        Observation(
                            source: "wechat",
                            sourceTxnId: sourceTxnId.isEmpty ? nil : sourceTxnId,
                            merchantTxnId: merchantTxnId.isEmpty ? nil : merchantTxnId,
                            occurredAt: occurredAt,
                            merchantRaw: kotlinTrim(cells[2]),
                            description: kotlinTrim(cells[3]),
                            amount: abs(amount),
                            currency: "CNY",
                            direction: direction,
                            paymentMethodRaw: kotlinTrim(cells[6]),
                            status: status,
                            note: kotlinTrim(Array(cells).count > 10 ? cells[10] : "")
                        )
                    )
                }
            }
            i += 1
        }
        return ParseResult(observations, errors, "wechat")
    }
}

// ---------------------------------------------------------------------------
// Generic CSV
// ---------------------------------------------------------------------------

public enum GenericCsvParser {

    public static func parse(_ data: [UInt8], _ mapping: MappingProfile?) throws -> ParseResult {
        guard let mapping = mapping else {
            throw MissingMappingError("generic_csv requires an explicit MappingProfile")
        }
        var errors: [ParseError] = []
        let text = try decodeText(data, mapping.options.encoding)
        let rawRows = parseCsv(text, mapping.options.delimiter)
        let rows = mapping.options.hasHeaderRow ? Array(rawRows.dropFirst()) : rawRows
        let header: [String] = rawRows.first ?? []

        func colIndexOf(_ name: String?) throws -> Int {
            guard let name = name else { return -1 }
            guard let idx = header.firstIndex(of: name) else {
                throw MissingMappingError("mapping column not found in header: \(name)")
            }
            return idx
        }

        let cTxn = try colIndexOf(mapping.columns.transactionId)
        let cDate = try colIndexOf(mapping.columns.dateTime)
        let cAmount = try colIndexOf(mapping.columns.amount)
        let cDebit = try colIndexOf(mapping.columns.debit)
        let cCredit = try colIndexOf(mapping.columns.credit)
        let cDesc = try colIndexOf(mapping.columns.description)
        let cCounterparty = try colIndexOf(mapping.columns.counterparty)
        let cCurrency = try colIndexOf(mapping.columns.currency)
        let cBalance = try colIndexOf(mapping.columns.balance)
        let cType = try colIndexOf(mapping.columns.transactionType)
        let cMethod = try colIndexOf(mapping.columns.paymentMethod)

        var observations: [Observation] = []
        let signMode = mapping.options.amountSignMode
        let dec = mapping.options.decimalSeparator

        for (rowIdx, cells) in rows.enumerated() {
            if cells.allSatisfy({ kotlinIsBlank($0) }) { continue }
            let line = rowIdx + (mapping.options.hasHeaderRow ? 2 : 1)

            if cDate == -1 || cDate >= cells.count {
                errors.append(ParseError(line, "missing required date column"))
                continue
            }
            let rawDate = cells[cDate]
            guard let occurredAt = parseMappingDate(rawDate, mapping.options.dateFormats) else {
                errors.append(ParseError(line, "bad date: \(take(rawDate, 24))"))
                continue
            }

            var amount: Double? = nil
            var direction: ObservationDirection = .neutral
            if signMode == "debit_credit" {
                let debit = cDebit >= 0 ? parseMappingAmount(cell(cells, cDebit), dec) : nil
                let credit = cCredit >= 0 ? parseMappingAmount(cell(cells, cCredit), dec) : nil
                if let debit = debit, debit > 0 {
                    amount = debit
                    direction = .out
                } else if let credit = credit, credit > 0 {
                    amount = credit
                    direction = .in
                } else {
                    errors.append(ParseError(line, "missing debit/credit amount"))
                    continue
                }
            } else {
                amount = cAmount >= 0 ? parseMappingAmount(cell(cells, cAmount), dec) : nil
                guard let amountNonNil = amount else {
                    // TS 用 cells[-1] → undefined → "undefined"；此处必须复现同一字符串
                    let shown = (cAmount >= 0 && cAmount < cells.count) ? take(cells[cAmount], 16) : "undefined"
                    errors.append(ParseError(line, "bad amount: \(shown)"))
                    continue
                }
                if signMode == "signed" {
                    // signed：金额自带符号。positiveDirection 默认 in（负数=支出）。
                    // 绝不根据数据分布自动猜测方向 —— 必须由 mapping 显式声明。
                    let positiveDirection = mapping.options.positiveDirection ?? "in"
                    if amountNonNil < 0 {
                        direction = (positiveDirection == "in") ? .out : .in
                    } else if amountNonNil > 0 {
                        direction = try ObservationDirection.fromWire(positiveDirection)
                    } else {
                        direction = .neutral
                    }
                    amount = abs(amountNonNil)
                } else {
                    direction = try ObservationDirection.fromWire(mapping.options.positiveDirection ?? "out")
                }
            }

            let currencyRaw = cCurrency >= 0 ? kotlinTrim(cell(cells, cCurrency)) : ""
            let currency = currencyRaw.isEmpty ? nil : currencyRaw

            observations.append(
                Observation(
                    source: "generic_csv",
                    sourceTxnId: nilOrEmpty(cTxn >= 0 ? kotlinTrim(cell(cells, cTxn)) : nil),
                    merchantTxnId: nil,
                    occurredAt: occurredAt,
                    merchantRaw: cCounterparty >= 0 ? kotlinTrim(cell(cells, cCounterparty)) : "",
                    description: cDesc >= 0 ? kotlinTrim(cell(cells, cDesc)) : "",
                    amount: amount ?? 0,
                    currency: currency ?? "XXX",
                    direction: direction,
                    paymentMethodRaw: cMethod >= 0 ? kotlinTrim(cell(cells, cMethod)) : "",
                    status: cType >= 0 ? kotlinTrim(cell(cells, cType)) : "",
                    note: ""
                )
            )
        }
        return ParseResult(observations, errors, "generic_csv")
    }

    private static func cell(_ cells: [String], _ idx: Int) -> String {
        idx >= 0 && idx < cells.count ? cells[idx] : ""
    }

    private static func nilOrEmpty(_ s: String?) -> String? {
        guard let s = s else { return nil }
        return s.isEmpty ? nil : s
    }

    private static func decodeText(_ data: [UInt8], _ encoding: String) throws -> String {
        if hasUtf8Bom(data) {
            guard let decoded = Gb18030.decodeStrictUtf8(stripBom(data)) else {
                throw BillTextDecodeError.bomButNotUtf8
            }
            return decoded
        }
        if let strict = Gb18030.decodeStrictUtf8(data) { return strict }
        // 严格 UTF-8 失败 → 只有在 mapping 明确声明了 GB18030 系编码时才回退。
        // 声明驱动、而不是"反正解不出来就试试 GBK"：后者会让一份真正损坏的 UTF-8
        // 文件被 GB18030 解出一堆看起来正常的汉字，错误就此隐形。
        if !Gb18030.isGb18030Encoding(encoding) {
            throw BillTextDecodeError.notUtf8AndUnsupportedEncoding(encoding)
        }
        guard let gb = Gb18030.decode(data) else {
            throw BillTextDecodeError.notUtf8AndNotGb18030(encoding)
        }
        return gb
    }
}

// ---------------------------------------------------------------------------
// OFX / QFX
// ---------------------------------------------------------------------------

public enum OfxParser {

    public struct Txn: Equatable, Sendable {
        public let fitid: String?
        public let dtposted: String?
        public let trnamt: String?
        public let trntype: String?
        public let name: String?
        public let memo: String?
        public let currency: String?
        public init(
            fitid: String?, dtposted: String?, trnamt: String?, trntype: String?,
            name: String?, memo: String?, currency: String?
        ) {
            self.fitid = fitid
            self.dtposted = dtposted
            self.trnamt = trnamt
            self.trntype = trntype
            self.name = name
            self.memo = memo
            self.currency = currency
        }
    }

    /** OFX v1 是 SGML 风格：<TAG>VALUE 成对出现，可能带 </TAG>。 */
    public static func parseTransactions(_ text: String) -> ([Txn], Int) {
        var transactions: [Txn] = []
        var malformed = 0
        let blocks = splitIgnoreCase(text, "<STMTTRN>").dropFirst()
        guard !blocks.isEmpty else { return (transactions, 0) }
        for block in blocks {
            let end = firstIndexIgnoreCase(block, "</STMTTRN>") ?? firstIndexIgnoreCase(block, "<STMTTRN>")
            let body: String
            if let end = end {
                body = String(block.prefix(end))
            } else {
                body = block
            }
            func tag(_ name: String) -> String? {
                guard let re = try? NSRegularExpression(
                    pattern: "<\(name)>\\s*([^<\\r\\n]+)",
                    options: .caseInsensitive
                ) else { return nil }
                let ns = body as NSString
                guard let m = re.firstMatch(in: body, range: NSRange(location: 0, length: ns.length))
                else { return nil }
                let r = m.range(at: 1)
                if r.location == NSNotFound { return nil }
                return kotlinTrim(ns.substring(with: r))
            }
            let fitid = tag("FITID")
            let trnamt = tag("TRNAMT")
            // 无 FITID 且无 TRNAMT → 坏块
            if fitid == nil && trnamt == nil {
                malformed += 1
                continue
            }
            transactions.append(
                Txn(
                    fitid: fitid,
                    dtposted: tag("DTPOSTED"),
                    trnamt: trnamt,
                    trntype: tag("TRNTYPE"),
                    name: tag("NAME"),
                    memo: tag("MEMO"),
                    currency: tag("CURDEF") ?? tag("CURRENCY")
                )
            )
        }
        return (transactions, malformed)
    }

    private static func firstIndexIgnoreCase(_ haystack: String, _ needle: String) -> Int? {
        let h = Array(haystack).map { String($0).lowercased() }
        let n = Array(needle).map { String($0).lowercased() }
        if n.isEmpty || h.count < n.count { return nil }
        for i in 0...(h.count - n.count) {
            var ok = true
            for j in 0..<n.count {
                // 逐字符小写比较按 Character 粒度：多字符大小写折叠不做特殊处理（标记均为 ASCII）
                if Array(h[i + j])[0] != Array(n[j])[0], h[i + j] != n[j] {
                    ok = false
                    break
                }
            }
            if ok { return i }
        }
        return nil
    }

    private static func splitIgnoreCase(_ text: String, _ sep: String) -> [String] {
        var result: [String] = []
        var rest = text
        while let idx = firstIndexIgnoreCase(rest, sep) {
            let chars = Array(rest)
            result.append(String(chars.prefix(idx)))
            rest = String(chars.dropFirst(idx + sep.count))
        }
        result.append(rest)
        return result
    }

    /** OFX DTPOSTED YYYYMMDD[HHMMSS.XXX[gmt offset]] → ISO(UTC)。 */
    public static func parseOfxDate(_ raw: String) -> String? {
        let s = kotlinTrim(raw)
        guard let re = try? NSRegularExpression(pattern: "^(\\d{4})(\\d{2})(\\d{2})(?:(\\d{2})(\\d{2})(\\d{2}))?")
        else { return nil }
        let ns = s as NSString
        guard let m = re.firstMatch(in: s, range: NSRange(location: 0, length: ns.length)) else { return nil }
        func g(_ i: Int) -> Int {
            let r = m.range(at: i)
            if r.location == NSNotFound { return 0 }
            return Int(ns.substring(with: r)) ?? 0
        }
        let y = g(1), mo = g(2), d = g(3), h = g(4), mi = g(5), sec = g(6)
        if mo < 1 || mo > 12 || h > 23 || mi > 59 || sec > 59 { return nil }
        if !isValidCalendarDate(y, mo, d) { return nil }
        return isoUtc(y, mo, d, h, mi, sec)
    }

    /** 必须显式拒绝空串：Number('') === 0 会把"缺失金额"伪造成一笔 0 元交易。 */
    public static func parseOfxAmount(_ raw: String) -> Double? {
        let s = kotlinTrim(raw)
        if s.isEmpty { return nil }
        guard let n = javaLikeDouble(s) else { return nil }
        if !isFiniteJs(n) { return nil }
        return money2(n)
    }

    public static func parse(_ data: [UInt8]) throws -> ParseResult {
        let text = try decodeBillText(data)
        let (transactions, malformed) = parseTransactions(text)
        var errors: [ParseError] = []
        for i in 0..<malformed {
            errors.append(ParseError(i + 1, "malformed STMTTRN block (no FITID/TRNAMT)"))
        }
        var observations: [Observation] = []
        for txn in transactions {
            guard let occurredAt = txn.dtposted.flatMap({ parseOfxDate($0) }) else {
                errors.append(
                    ParseError(
                        observations.count + 1,
                        "bad DTPOSTED: \(txn.dtposted.map { take($0, 20) } ?? "missing")"
                    )
                )
                continue
            }
            guard let amount = txn.trnamt.flatMap({ parseOfxAmount($0) }) else {
                errors.append(
                    ParseError(
                        observations.count + 1,
                        "bad TRNAMT: \(txn.trnamt.map { take($0, 20) } ?? "missing")"
                    )
                )
                continue
            }
            observations.append(
                Observation(
                    source: "ofx_qfx",
                    sourceTxnId: txn.fitid,
                    merchantTxnId: nil,
                    occurredAt: occurredAt,
                    merchantRaw: txn.name ?? txn.memo ?? "",
                    description: txn.memo ?? "",
                    amount: abs(amount),
                    currency: txn.currency ?? "XXX",
                    direction: amount < 0 ? .out : .in,
                    paymentMethodRaw: "",
                    status: txn.trntype ?? "",
                    note: ""
                )
            )
        }
        return ParseResult(observations, errors, "ofx_qfx")
    }
}
