// 导入流程（task #5）：选文件 → 解析预览 → CSV 映射 → 节点解析 →
// 复查 → 建议复查 → 候选复查 → 漂移复查。解析走 PDIGCore Parsers。

import SwiftUI
import PDIGCore
#if os(macOS)
import UniformTypeIdentifiers
#endif

struct ImportScreen: View {
    @ObservedObject var session: AppSession
    @State private var state: ImportState = ImportState()
    @State private var selectedFile: URL?
    @State private var dateColumn: String = ""
    @State private var amountColumn: String?
    @State private var parsedBytes: [UInt8] = []
    @State private var kind: String = "csv"

    private var mapping: MappingProfile {
        MappingProfile(
            columns: MappingColumns(dateTime: dateColumn.isEmpty ? "日期" : dateColumn, amount: amountColumn),
            options: MappingOptions()
        )
    }

    var body: some View {
        VStack(spacing: 0) {
            AppTopBar(title: "导入") { session.pop() }
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    stepHeader("1. 选择账单文件")
                    filePickerRow

                    if state.stage.rawValue >= ImportStage.parsePreview.rawValue {
                        stepHeader("2. 解析预览")
                        previewSection
                    }
                    if state.stage.rawValue >= ImportStage.mapping.rawValue {
                        stepHeader("3. CSV 映射")
                        mappingSection
                    }
                    if state.stage.rawValue >= ImportStage.resolution.rawValue {
                        stepHeader("4. 节点解析")
                        resolutionSection
                    }
                    if state.stage.rawValue >= ImportStage.review.rawValue {
                        stepHeader("5. 复查")
                        reviewSection
                    }
                    if state.stage.rawValue >= ImportStage.proposals.rawValue {
                        stepHeader("6. 待确认建议")
                        proposalsSection
                    }
                    if state.stage.rawValue >= ImportStage.candidates.rawValue {
                        stepHeader("7. 待确认服务（候选）")
                        candidatesSection
                    }
                    if state.stage.rawValue >= ImportStage.drift.rawValue {
                        stepHeader("8. 可能发生了变化（漂移复查）")
                        driftSection
                    }
                    if state.mappingError != nil {
                        Text(CopyZh.mappingMissing).font(.footnote).foregroundStyle(.red)
                    }
                }
                .padding()
            }
        }
        .frame(minWidth: 420, minHeight: 600)
    }

    private func stepHeader(_ text: String) -> some View {
        Text(text).font(.headline)
    }

    private var filePickerRow: some View {
        Button {
            selectFile()
        } label: {
            Label(selectedFile?.lastPathComponent ?? "选择 CSV / OFX 文件",
                  systemImage: "doc.badge.plus")
                .frame(maxWidth: .infinity)
                .padding(10)
        }
        .buttonStyle(.bordered)
    }

    /// macOS 上直接读文件；iOS 接入点保留（.fileImporter 需真机验证）。
    private func selectFile() {
        #if os(macOS)
        let panel = NSOpenPanel()
        panel.allowedContentTypes = [.commaSeparatedText, .data]
        if panel.runModal() == .OK, let url = panel.url {
            loadFile(url)
        }
        #else
        session.setImportState(ImportState(stage: .pickFile, fileName: "请在 iOS 真机上使用文件导入器选择文件"))
        #endif
    }

    private func loadFile(_ url: URL) {
        guard let data = try? Data(contentsOf: url) else { return }
        parsedBytes = [UInt8](data)
        selectedFile = url
        let text = (try? Gb18030.decodeStrictUtf8(parsedBytes)) ?? String(decoding: parsedBytes, as: UTF8.self)
        kind = ImportFlow.detectKind(fileName: url.lastPathComponent, text: text)
        do {
            let preview = try ImportFlow.preview(kind: kind, data: parsedBytes, mapping: kind == "csv" ? mapping : nil)
            state = preview
            state.stage = .parsePreview
            session.setImportState(state)
        } catch {
            state = ImportState(stage: .parsePreview, mappingError: CopyZh.parseFailed + " \(error)")
        }
    }

    private var previewSection: some View {
        VStack(alignment: .leading, spacing: 6) {
            if let s = state.summary {
                Text("来源：\(s.sourceLabel) · 共 \(s.total) 笔（支出 \(s.outflows) / 收入 \(s.inflows)）")
                Text("商户数：\(s.merchants) · 区间：\(s.firstDate ?? "-") ~ \(s.lastDate ?? "-")")
                    .font(.caption).foregroundStyle(.secondary)
                if !s.errors.isEmpty {
                    Text("解析错误 \(s.errors.count) 条：\(s.errors.prefix(3).joined(separator: "；"))")
                        .font(.caption).foregroundStyle(.orange)
                }
                if kind == "csv" {
                    Button {
                        advance()
                    } label: { Text("继续：CSV 映射").frame(maxWidth: .infinity).padding(.vertical, 6) }
                        .buttonStyle(.borderedProminent)
                } else {
                    Button {
                        advance(to: .resolution)
                    } label: { Text("继续：节点解析").frame(maxWidth: .infinity).padding(.vertical, 6) }
                        .buttonStyle(.borderedProminent)
                }
            } else {
                Text(CopyZh.emptyTitle).foregroundStyle(.secondary)
            }
        }
    }

    private var mappingSection: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(CopyZh.mappingHint).font(.caption).foregroundStyle(.secondary)
            Picker("日期列", selection: $dateColumn) {
                ForEach(state.headerColumns, id: \.self) { Text($0).tag($0) }
            }
            Picker("金额列", selection: $amountColumn) {
                Text("不选择").tag(String?.none)
                ForEach(state.headerColumns, id: \.self) { Text($0).tag(String?.some($0)) }
            }
            Button {
                do {
                    let preview = try ImportFlow.preview(kind: kind, data: parsedBytes, mapping: mapping)
                    state = preview
                    state.stage = .resolution
                    session.setImportState(state)
                } catch {
                    state = ImportState(stage: .mapping, mappingError: CopyZh.mappingMissing)
                }
            } label: { Text("应用映射并解析").frame(maxWidth: .infinity).padding(.vertical, 6) }
                .buttonStyle(.borderedProminent)
        }
    }

    private var resolutionSection: some View {
        VStack(alignment: .leading, spacing: 6) {
            let resolved = ImportFlow.resolveMerchants(rows: state.rows, snapshot: session.snapshot)
            Text("已解析 \(resolved.resolutions.count - resolved.candidates.count) / \(resolved.resolutions.count) 个商户")
                .font(.caption).foregroundStyle(.secondary)
            ForEach(resolved.resolutions.prefix(8), id: \.id) { r in
                HStack {
                    Text(r.merchantRaw).font(.footnote).lineLimit(1)
                    Spacer()
                    Text(r.resolution == "candidate" ? CopyZh.pendingService : (r.matchedNodeName ?? ""))
                        .font(.caption).foregroundStyle(r.resolution == "candidate" ? .orange : .green)
                }
                .padding(6)
                .background(Color.gray.opacity(0.1), in: RoundedRectangle(cornerRadius: 6))
            }
            Button {
                advance(to: .review)
            } label: { Text("继续：复查").frame(maxWidth: .infinity).padding(.vertical, 6) }
                .buttonStyle(.borderedProminent)
        }
    }

    private var reviewSection: some View {
        VStack(alignment: .leading, spacing: 6) {
            ForEach(state.rows.prefix(6), id: \.line) { r in
                HStack {
                    Text(String(r.occurredAt.prefix(10))).font(.caption)
                    Text(r.merchantRaw).font(.footnote).lineLimit(1)
                    Spacer()
                    Text(String(format: "¥%.2f", r.amount)).font(.caption)
                }
                .padding(6)
                .background(Color.gray.opacity(0.1), in: RoundedRectangle(cornerRadius: 6))
            }
            Button {
                advance(to: .proposals)
            } label: { Text("继续：建议复查").frame(maxWidth: .infinity).padding(.vertical, 6) }
                .buttonStyle(.borderedProminent)
        }
    }

    private var proposalsSection: some View {
        VStack(alignment: .leading, spacing: 6) {
            let proposals = ImportFlow.detectProposals(rows: state.rows)
            if proposals.isEmpty {
                Text("未发现周期扣款建议。").foregroundStyle(.secondary)
            } else {
                ForEach(proposals) { p in
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(p.merchant).font(.footnote)
                            Text("\(p.months) 个月 \(p.observations) 笔 · \(p.paymentMethod)")
                                .font(.caption2).foregroundStyle(.secondary)
                        }
                        Spacer()
                        Text(String(format: "%.0f%%", p.confidence * 100)).font(.caption)
                    }
                    .padding(6)
                    .background(Color.gray.opacity(0.1), in: RoundedRectangle(cornerRadius: 6))
                }
                Text(CopyZh.proposalBasis).font(.caption2).foregroundStyle(.secondary)
            }
            Button {
                advance(to: .candidates)
            } label: { Text("继续：候选复查").frame(maxWidth: .infinity).padding(.vertical, 6) }
                .buttonStyle(.borderedProminent)
        }
    }

    private var candidatesSection: some View {
        VStack(alignment: .leading, spacing: 6) {
            let resolved = ImportFlow.resolveMerchants(rows: state.rows, snapshot: session.snapshot)
            if resolved.candidates.isEmpty {
                Text("没有待确认服务。").foregroundStyle(.secondary)
            } else {
                ForEach(resolved.candidates) { c in
                    Text("• \(c.merchantRaw)（未解析，需人工确认是否对应已有服务）")
                        .font(.footnote)
                }
            }
            Button {
                advance(to: .drift)
            } label: { Text("继续：漂移复查").frame(maxWidth: .infinity).padding(.vertical, 6) }
                .buttonStyle(.borderedProminent)
        }
    }

    private var driftSection: some View {
        VStack(alignment: .leading, spacing: 6) {
            let drifts = ImportFlow.detectDrifts(rows: state.rows, snapshot: session.snapshot)
            if drifts.isEmpty {
                Text("未发现支付路径变化。").foregroundStyle(.secondary)
            } else {
                ForEach(drifts) { d in
                    HStack {
                        Text(d.merchant).font(.footnote)
                        Spacer()
                        Text("\(d.oldMethod) → \(d.newMethod)").font(.caption).foregroundStyle(.orange)
                    }
                    .padding(6)
                    .background(Color.orange.opacity(0.1), in: RoundedRectangle(cornerRadius: 6))
                }
                Text(CopyZh.driftPossibleChange).font(.caption2).foregroundStyle(.secondary)
            }
            Button {
                commitImport()
            } label: { Text("完成导入").frame(maxWidth: .infinity).padding(.vertical, 8) }
                .buttonStyle(.borderedProminent)
        }
    }

    private func advance(to stage: ImportStage = .mapping) {
        state.stage = stage
        session.setImportState(state)
    }

    /// 落库：来源实例 + 周期扣款建议（pending）。候选/漂移仅会话展示，
    /// 用户确认后由后续流程写入（本版本保持只读会话语义）。
    private func commitImport() {
        guard let repo = session.repository else { return }
        do {
            let source = SourceInstanceRow(
                id: "src-\(kind)-\(Int(Date().timeIntervalSince1970))",
                adapterId: kind,
                adapterVersion: 1,
                sourceKind: SourceKind.statementFile.wire,
                label: selectedFile?.lastPathComponent ?? kind,
                lastIngestedAt: "2030-01-15T00:00:00+00:00"
            )
            let proposals = ImportFlow.detectProposals(rows: state.rows).map { p in
                PendingProposal(
                    id: p.id,
                    key: p.key,
                    from: "card-main",
                    relation: .fundingSource,
                    to: "service-\(p.merchant)",
                    confidenceScore: p.confidence,
                    observationCount: p.observations,
                    createdAt: "2030-01-15T00:00:00+00:00",
                    updatedAt: "2030-01-15T00:00:00+00:00"
                )
            }
            let snapshot = GraphSnapshot(
                nodes: session.snapshot.nodes,
                dependencies: session.snapshot.dependencies,
                groups: session.snapshot.groups,
                pendingProposals: session.snapshot.pendingProposals + proposals,
                sources: session.snapshot.sources + [source],
                graphRevision: session.snapshot.graphRevision
            )
            try repo.replaceGraph(snapshot)
            session.refresh()
            session.root()
        } catch let e {
            state = ImportState(stage: .drift, mappingError: "导入失败：\(e)")
        }
    }
}
