// ImportFlow —— 导入管线（task #5）：选文件 → 解析预览 → CSV 映射 →
// 节点解析 → 复查 → 建议复查 → 候选复查 → 漂移复查。
//
// 解析用 PDIGCore Parsers（Wechat / Generic CSV / OFX）。商户解析遵循 AGENTS §15：
// builtin alias exact → normalized exact → conservative fuzzy → 用户确认（候选）。
// 所有推断只产生 Proposal / Candidate / Drift，绝不直接写入已确认图。

import Foundation
import PDIGCore

public enum ImportStage: Int, CaseIterable, Equatable, Sendable {
    case pickFile = 0
    case parsePreview
    case mapping
    case resolution
    case review
    case proposals
    case candidates
    case drift
}

public struct ImportRow: Equatable, Sendable {
    public let line: Int
    public let occurredAt: String
    public let merchantRaw: String
    public let description: String
    public let amount: Double
    public let direction: String
    public let paymentMethodRaw: String
    public let status: String
    public let sourceTxnId: String?
}

public struct ImportSummary: Equatable, Sendable {
    public let sourceLabel: String
    public let total: Int
    public let outflows: Int
    public let inflows: Int
    public let merchants: Int
    public let errors: [String]
    public let firstDate: String?
    public let lastDate: String?
}

public struct ResolutionItem: Equatable, Sendable, Identifiable {
    public let id: String
    public let merchantRaw: String
    public let normalized: String
    public let resolution: String   // matched-node-id | "candidate" | "new"
    public let matchedNodeName: String?
}

public struct ProposalItem: Equatable, Sendable, Identifiable {
    public let id: String
    public let merchant: String
    public let paymentMethod: String
    public let observations: Int
    public let months: Int
    public let confidence: Double
    public let key: String
}

public struct DriftItem: Equatable, Sendable, Identifiable {
    public let id: String
    public let merchant: String
    public let oldMethod: String
    public let newMethod: String
    public let kind: String
}

public struct ImportState: Equatable, Sendable {
    public let stage: ImportStage
    public let fileName: String
    public let rows: [ImportRow]
    public let summary: ImportSummary?
    public let headerColumns: [String]
    public let resolutions: [ResolutionItem]
    public let proposals: [ProposalItem]
    public let candidates: [ResolutionItem]
    public let drifts: [DriftItem]
    public let mappingError: String?

    public init(
        stage: ImportStage = .pickFile,
        fileName: String = "",
        rows: [ImportRow] = [],
        summary: ImportSummary? = nil,
        headerColumns: [String] = [],
        resolutions: [ResolutionItem] = [],
        proposals: [ProposalItem] = [],
        candidates: [ResolutionItem] = [],
        drifts: [DriftItem] = [],
        mappingError: String? = nil
    ) {
        self.stage = stage
        self.fileName = fileName
        self.rows = rows
        self.summary = summary
        self.headerColumns = headerColumns
        self.resolutions = resolutions
        self.proposals = proposals
        self.candidates = candidates
        self.drifts = drifts
        self.mappingError = mappingError
    }
}

public enum ImportFlow {

    /// 识别文件类型（detect 分值最高者胜；不可识别默认 generic csv）。
    public static func detectKind(fileName: String, text: String) -> String {
        let wechat = WechatParser.detect(text)
        let looksOfx = text.contains("<OFX>") || text.contains("OFXHEADER")
        if wechat > 0.5 { return "wechat" }
        if looksOfx { return "ofx" }
        let ext = (fileName as NSString).pathExtension.lowercased()
        if ext == "ofx" || ext == "qfx" { return "ofx" }
        return "csv"
    }

    public static func parse(
        kind: String,
        data: [UInt8],
        mapping: MappingProfile? = nil
    ) throws -> ParseResult {
        switch kind {
        case "wechat": return try WechatParser.parse(data)
        case "ofx": return try OfxParser.parse(data)
        default: return try GenericCsvParser.parse(data, mapping)
        }
    }

    /// 解析预览 + 行模型投影。
    public static func preview(kind: String, data: [UInt8], mapping: MappingProfile?) throws -> ImportState {
        let result = try parse(kind: kind, data: data, mapping: mapping)
        let rows = result.observations.enumerated().map { (i, o) in
            ImportRow(
                line: i + 1,
                occurredAt: o.occurredAt,
                merchantRaw: o.merchantRaw,
                description: o.description,
                amount: o.amount,
                direction: o.direction.wire,
                paymentMethodRaw: o.paymentMethodRaw,
                status: o.status,
                sourceTxnId: o.sourceTxnId
            )
        }
        let outflows = result.observations.filter { $0.direction == .out }.count
        let inflows = result.observations.filter { $0.direction == .`in` }.count
        let merchants = Set(result.observations.map { normalizedMerchant($0.merchantRaw) }).count
        let dates = result.observations.map { $0.occurredAt }.sorted()
        let summary = ImportSummary(
            sourceLabel: result.sourceLabel,
            total: result.observations.count,
            outflows: outflows,
            inflows: inflows,
            merchants: merchants,
            errors: result.errors.map { "第 \($0.line) 行：\($0.reason)" },
            firstDate: dates.first.map { String($0.prefix(10)) },
            lastDate: dates.last.map { String($0.prefix(10)) }
        )
        let header = headerColumns(kind: kind, data: data)
        return ImportState(
            stage: .parsePreview,
            fileName: kind,
            rows: rows,
            summary: summary,
            headerColumns: header
        )
    }

    /// 商户解析（AGENTS §15 顺序）。
    public static func resolveMerchants(
        rows: [ImportRow],
        snapshot: GraphSnapshot
    ) -> (resolutions: [ResolutionItem], candidates: [ResolutionItem]) {
        var resolutions: [ResolutionItem] = []
        var candidates: [ResolutionItem] = []
        var seen = Set<String>()
        for row in rows {
            let raw = row.merchantRaw
            let norm = normalizedMerchant(raw)
            if seen.contains(norm) { continue }
            seen.insert(norm)
            // 1. builtin alias exact
            var matched: DepNode? = snapshot.nodes.first { $0.name == raw }
            // 2. normalized exact
            if matched == nil {
                matched = snapshot.nodes.first { normalizedMerchant($0.name) == norm }
            }
            // 3. conservative fuzzy（前缀 ≥4 字精确匹配，绝不猜）
            if matched == nil {
                matched = snapshot.nodes.first { candidate in
                    let cn = normalizedMerchant(candidate.name)
                    return cn.count >= 4 && (norm.hasPrefix(cn) || cn.hasPrefix(norm))
                }
            }
            if let node = matched {
                resolutions.append(ResolutionItem(
                    id: norm,
                    merchantRaw: raw,
                    normalized: norm,
                    resolution: node.id,
                    matchedNodeName: node.name
                ))
            } else {
                let candidate = ResolutionItem(
                    id: norm,
                    merchantRaw: raw,
                    normalized: norm,
                    resolution: "candidate",
                    matchedNodeName: nil
                )
                candidates.append(candidate)
                resolutions.append(candidate)
            }
        }
        return (resolutions.sorted { $0.id < $1.id }, candidates.sorted { $0.id < $1.id })
    }

    /// 周期扣款建议：同一商户 + 同一支付方式在 ≥2 个不同月份出现 → 建议。
    /// 只产出 Proposal（pending），不写图。
    public static func detectProposals(rows: [ImportRow]) -> [ProposalItem] {
        var buckets: [String: (merchant: String, method: String, count: Int, months: Set<String>)] = [:]
        for row in rows {
            let key = normalizedMerchant(row.merchantRaw) + "|" + normalizedMerchant(row.paymentMethodRaw)
            var b = buckets[key] ?? (merchant: row.merchantRaw, method: row.paymentMethodRaw, count: 0, months: [])
            b.count += 1
            b.months.insert(String(row.occurredAt.prefix(7)))
            buckets[key] = b
        }
        var items: [ProposalItem] = []
        for (key, b) in buckets where b.months.count >= 2 {
            let confidence = min(0.9, Double(b.months.count) / 12.0)
            items.append(ProposalItem(
                id: key,
                merchant: b.merchant,
                paymentMethod: b.method,
                observations: b.count,
                months: b.months.count,
                confidence: confidence,
                key: "proposal|\(key)"
            ))
        }
        return items.sorted { $0.id < $1.id }
    }

    /// 漂移：同一商户历史上用 A 卡支付、本次用 B 卡 → possible_replacement。
    /// 需要历史图（dependencies）。无历史则无漂移（absence 不产生断言）。
    public static func detectDrifts(
        rows: [ImportRow],
        snapshot: GraphSnapshot
    ) -> [DriftItem] {
        var methodByMerchant: [String: String] = [:]
        for row in rows {
            let norm = normalizedMerchant(row.merchantRaw)
            let method = normalizedMerchant(row.paymentMethodRaw)
            if method.isEmpty { continue }
            if let prev = methodByMerchant[norm], prev != method {
                return [DriftItem(
                    id: "drift-\(norm)",
                    merchant: row.merchantRaw,
                    oldMethod: prev,
                    newMethod: method,
                    kind: DriftKind.possibleReplacement.wire
                )]
            }
            methodByMerchant[norm] = method
        }
        return []
    }

    // MARK: - helpers

    /// 保守归一化：小写 + 去空白 + 去标点。解析失败时保原文（不猜）。
    public static func normalizedMerchant(_ raw: String) -> String {
        let lowered = raw.lowercased()
        let filtered = lowered.filter { $0.isLetter || $0.isNumber }
        return filtered
    }

    private static func headerColumns(kind: String, data: [UInt8]) -> [String] {
        let text = (try? Gb18030.decodeStrictUtf8(data)) ?? String(decoding: data, as: UTF8.self)
        guard let firstLine = text.components(separatedBy: "\n").first else { return [] }
        return parseCsvLine(firstLine).map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
    }
}
