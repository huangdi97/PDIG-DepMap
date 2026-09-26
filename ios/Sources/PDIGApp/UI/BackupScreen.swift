// 备份 / 恢复（task #12）+ 设置 / 安全 / 关于（task #14）+ 删除所有数据（task #13）。

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
    @State private var backupText: String?

    var body: some View {
        VStack(spacing: 0) {
            AppTopBar(title: CopyZh.backupExportTitle) { session.pop() }
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    // 导出
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
                    .padding(12)
                    .background(Color.gray.opacity(0.1), in: RoundedRectangle(cornerRadius: 10))

                    // 恢复
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
                    .padding(12)
                    .background(Color.gray.opacity(0.1), in: RoundedRectangle(cornerRadius: 10))

                    // 存储说明（诚实标注）
                    VStack(alignment: .leading, spacing: 4) {
                        Text(CopyZh.persistenceLabel).font(.headline)
                        Text(SqliteProvider.capabilityNote()).font(.caption).foregroundStyle(.secondary)
                    }
                    .padding(12)
                    .background(Color.yellow.opacity(0.12), in: RoundedRectangle(cornerRadius: 10))

                    if let message = message {
                        Text(message).font(.footnote).foregroundStyle(.secondary)
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
        } catch let e {
            message = "\(e)"
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
            backupText = nil
        } catch let e {
            message = "\(e)"
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
                    Text(CopyZh.securityTitle).font(.headline)
                    HStack {
                        Text(CopyZh.securityBiometric)
                        Spacer()
                        Text(BiometricAuth.isBiometryAvailable() ? "可用" : "不可用")
                            .font(.caption).foregroundStyle(.secondary)
                    }
                    .padding(10)
                    .background(Color.gray.opacity(0.1), in: RoundedRectangle(cornerRadius: 8))
                    HStack {
                        Text(CopyZh.securityDeviceCredential)
                        Spacer()
                        Text("兜底").font(.caption).foregroundStyle(.secondary)
                    }
                    .padding(10)
                    .background(Color.gray.opacity(0.1), in: RoundedRectangle(cornerRadius: 8))

                    Button {
                        session.lockNow()
                    } label: { Text("立即锁定").frame(maxWidth: .infinity).padding(.vertical, 6) }
                        .buttonStyle(.bordered)

                    // 数据
                    Text("数据").font(.headline)
                    Button {
                        session.push(.backup)
                    } label: { Label(CopyZh.backupExportTitle, systemImage: "externaldrive").frame(maxWidth: .infinity).padding(.vertical, 6) }
                        .buttonStyle(.bordered)

                    // 删除所有数据（显式确认，task #13）
                    Button {
                        confirmDelete = true
                    } label: {
                        Text(CopyZh.deleteAllData).foregroundStyle(.red)
                            .frame(maxWidth: .infinity).padding(.vertical, 6)
                    }
                    .buttonStyle(.bordered)

                    // 关于
                    Text(CopyZh.aboutTitle).font(.headline)
                    VStack(alignment: .leading, spacing: 4) {
                        Text(CopyZh.aboutAppName).font(.body.weight(.semibold))
                        Text(CopyZh.aboutVersion).font(.caption).foregroundStyle(.secondary)
                        Text(CopyZh.aboutLocalFirst).font(.caption).foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(10)
                    .background(Color.gray.opacity(0.1), in: RoundedRectangle(cornerRadius: 8))
                }
                .padding()
            }
        }
        .frame(minWidth: 420, minHeight: 600)
        .confirmationDialog(
            CopyZh.deleteAllDataConfirmTitle,
            isPresented: $confirmDelete,
            titleVisibility: .visible
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
