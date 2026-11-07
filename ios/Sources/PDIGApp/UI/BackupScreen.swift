// 备份 / 恢复 + 设置 / 安全 / 关于 + 删除所有数据（Quiet Infrastructure）。
// 铁律：原始 Swift Error 禁止直接上屏；导出/导入失败一律人话 + 重试。

import SwiftUI
import PDIGCore
#if os(macOS)
import AppKit
import UniformTypeIdentifiers
#endif
struct BackupScreen: View {
    @ObservedObject var session: AppSession
    @State private var exportPassword = ""
    @State private var importPassword = ""
    @State private var message: String?
    @State private var errorMessage: String?
    @State private var lastFailedExport = false
    @State private var backupText: String?

    var body: some View {
        VStack(spacing: 0) {
            AppTopBar(title: CopyZh.backupExportTitle) { session.pop() }
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    // 导出
                    PdigCard {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(CopyZh.backupExportTitle).font(.headline)
                            Text(CopyZh.backupPasswordNotice).font(.caption).foregroundStyle(.secondary)
                            SecureField("设置导出密码", text: $exportPassword)
                                .textFieldStyle(.roundedBorder)
                            Button {
                                export()
                            } label: { Text("导出加密备份（.depmap）").frame(maxWidth: .infinity).padding(.vertical, 6) }
                                .buttonStyle(.borderedProminent)
                                .disabled(exportPassword.count < 4)
                        }
                    }

                    // 恢复
                    PdigCard {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(CopyZh.backupImportTitle).font(.headline)
                            Text(CopyZh.backupCrossDevice).font(.caption).foregroundStyle(.secondary)
                            SecureField("备份密码", text: $importPassword)
                                .textFieldStyle(.roundedBorder)
                            if backupText == nil {
                                Button {
                                    readBackupFile()
                                } label: { Text("选择备份文件").frame(maxWidth: .infinity).padding(.vertical, 6) }
                                    .buttonStyle(.bordered)
                            }
                            if let text = backupText {
                                Text("已读取 \(text.count) 字符的备份文件").font(.caption).foregroundStyle(.secondary)
                                Button {
                                    importBackup(text)
                                } label: { Text("恢复（覆盖当前数据）").frame(maxWidth: .infinity).padding(.vertical, 6) }
                                    .buttonStyle(.borderedProminent)
                                    .disabled(importPassword.count < 4)
                            }
                        }
                    }

                    // 存储说明（诚实标注）
                    NoticeBanner(
                        icon: "lock.shield",
                        text: SqliteProvider.capabilityNote(),
                        color: PdigTheme.Color.warning
                    )

                    if let message = message {
                        Text(message).font(.footnote).foregroundStyle(PdigTheme.Color.textSecondary)
                    }
                    if let errorMessage = errorMessage {
                        NoticeBanner(
                            icon: "exclamationmark.triangle.fill",
                            text: errorMessage,
                            color: PdigTheme.Color.danger
                        )
                        Button {
                            retryLastAction()
                        } label: { Text(CopyZh.retry).frame(maxWidth: .infinity).padding(.vertical, 6) }
                            .buttonStyle(.bordered)
                    }
                }
                .padding()
            }
        }
        .frame(minWidth: 420, minHeight: 600)
    }

    private func export() {
        guard let repo = session.repository else { return }
        do {
            let json = try BackupViewModel.exportDepmap(repository: repo, password: exportPassword)
            let url = try FileStore.write(json, name: "depmap-\(Int(Date().timeIntervalSince1970)).depmap")
            message = "已导出到 \(url.lastPathComponent)"
            errorMessage = nil
            lastFailedExport = false
        } catch {
            // 原始 Swift Error 禁止上屏：统一人话 + 重试。
            message = nil
            lastFailedExport = true
            errorMessage = CopyZh.exportFailed
        }
    }
    private func readBackupFile() {
        #if os(macOS)
        let panel = NSOpenPanel()
        panel.allowedContentTypes = [.data]
        if panel.runModal() == .OK, let url = panel.url,
           let text = try? String(contentsOf: url, encoding: .utf8) {
            backupText = text
        }
        #else
        message = "iOS 真机使用文件导入器选择 .depmap"
        #endif
    }

    private func importBackup(_ text: String) {
        guard let repo = session.repository else { return }
        do {
            let counts = try BackupViewModel.importDepmap(repository: repo, containerJson: text, password: importPassword)
            session.refresh()
            message = "恢复完成：\(counts["nodes"] ?? 0) 个节点，\(counts["dependencies"] ?? 0) 条关系"
            errorMessage = nil
            lastFailedExport = false
            backupText = nil
        } catch {
            // 密码错误 / 文件被修改等 → 人话；原始 Error 不上屏。
            message = nil
            lastFailedExport = false
            errorMessage = CopyZh.backupWrongPassword
        }
    }

    private func retryLastAction() {
        errorMessage = nil
        if lastFailedExport {
            export()
        } else if let text = backupText {
            importBackup(text)
        }
    }
}

struct SettingsScreen: View {
    @ObservedObject var session: AppSession
    @State private var confirmDelete = false

    var body: some View {
        VStack(spacing: 0) {
            AppTopBar(title: CopyZh.settingsTitle) { session.pop() }
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    // 安全
                    SectionHeader(CopyZh.securityTitle)
                    PdigCard {
                        HStack {
                            Text(CopyZh.securityBiometric)
                            Spacer()
                            Text(BiometricAuth.isBiometryAvailable() ? "可用" : "不可用")
                                .font(.caption).foregroundStyle(.secondary)
                        }
                    }
                    PdigCard {
                        HStack {
                            Text(CopyZh.securityDeviceCredential)
                            Spacer()
                            Text("兜底").font(.caption).foregroundStyle(.secondary)
                        }
                    }

                    Button {
                        session.lockNow()
                    } label: { Text("立即锁定").frame(maxWidth: .infinity).padding(.vertical, 6) }
                        .buttonStyle(.bordered)

                    // 数据
                    SectionHeader("数据")
                    Button {
                        session.push(.backup)
                    } label: { Label(CopyZh.backupExportTitle, systemImage: "externaldrive").frame(maxWidth: .infinity).padding(.vertical, 6) }
                        .buttonStyle(.bordered)

                    // 删除所有数据（显式确认）
                    Button {
                        confirmDelete = true
                    } label: {
                        Text(CopyZh.deleteAllData).foregroundStyle(PdigTheme.Color.danger)
                            .frame(maxWidth: .infinity).padding(.vertical, 6)
                    }
                    .buttonStyle(.bordered)

                    // 关于
                    SectionHeader(CopyZh.aboutTitle)
                    PdigCard {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(CopyZh.aboutAppName).font(.body.weight(.semibold))
                            Text(CopyZh.aboutVersion).font(.caption).foregroundStyle(.secondary)
                            Text(CopyZh.aboutLocalFirst).font(.caption).foregroundStyle(.secondary)
                        }
                    }
                }
                .padding()
            }
        }
        .frame(minWidth: 420, minHeight: 600)
        .alert(
            CopyZh.deleteAllDataConfirmTitle,
            isPresented: $confirmDelete
        ) {
            Button(CopyZh.deleteAllDataConfirmAction, role: .destructive) {
                try? session.deleteAllData()
                session.lockNow()
            }
            Button(CopyZh.cancel, role: .cancel) {}
        } message: {
            Text(CopyZh.deleteAllDataConfirmBody)
        }
    }
}