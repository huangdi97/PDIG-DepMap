// VNextContinuityRail —— Continuity Rail（连续性轨道 / make-before-break）。
//
// continuity-view.md 契约（desktop AssetSurfaces.ContinuityRail 移植）：
//  - 6 阶段：completed（实勾）/ current（空心+编号）/ blocked（! + 原因）/ verifying（时钟）/ upcoming（灰）；
//  - stage 6 在 stage 3 验证通过前 disabled + 明文原因（绝不只禁用，uiuxV031.blockedReason）；
//  - Rail 语义化（screen reader 朗读 6 阶段、当前态、blocked 原因）。
//  testId：pdig.change.phone.rail / .stage1…6 / .gate。

import SwiftUI

/// Continuity Rail（flagship 换号流程轨道）。
struct ContinuityRail: View {
    let stages: [VChangeStage]

    private var stageLabels: [Int: String] {
        [
            1: VCopy.impactAnalysis,
            2: VCopy.establishNewNumber,
            3: VCopy.verifyNewNumber,
            4: VCopy.migrateAccounts,
            5: VCopy.checkRecoveryPaths,
            6: VCopy.retireOldNumber,
        ]
    }

    var body: some View {
        VStack(alignment: .leading, spacing: VSpace.md) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: VSpace.lg) {
                    ForEach(Array(stages.enumerated()), id: \.offset) { index, stage in
                        RailNode(stage: stage, label: stageLabels[stage.stage] ?? stage.key)
                        if index < stages.count - 1 {
                            Rectangle()
                                .fill(
                                    stage.status == "completed"
                                        ? PdigV2Colors.positive
                                        : PdigV2Colors.borderSubtle
                                )
                                .frame(width: 24, height: 2)
                                .padding(.top, 22)
                        }
                    }
                }
                .padding(VSpace.lg)
            }
            // make-before-break 闸门明文（stage 6 blocked 原因；绝不只禁用）
            if let blocked = stages.first(where: { $0.status == "blocked" }) {
                HStack(spacing: VSpace.sm) {
                    Image(systemName: "exclamationmark.octagon.fill")
                        .foregroundColor(PdigV2Colors.critical)
                        .frame(width: 16)
                    Text(blocked.blockReason ?? VCopy.blockedReason)
                        .font(VFont.secondary())
                        .foregroundColor(PdigV2Colors.textPrimary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(.horizontal, VSpace.lg)
                .padding(.vertical, VSpace.sm)
                .frame(minHeight: 44)
                .background(PdigV2Colors.critical.opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
                .accessibilityIdentifier(VTestIds.changePhoneGate)
            }
        }
        .padding(VSpace.lg)
        .background(PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous)
                .stroke(PdigV2Colors.borderSubtle, lineWidth: 1)
        )
    }
}

/// 阶段节点：实勾 / 空心+编号 / ! / 时钟 / 灰（三通道：icon + label + color）。
private struct RailNode: View {
    let stage: VChangeStage
    let label: String

    private var color: Color { vStatusColor(stage.status) }

    var body: some View {
        VStack(alignment: .center, spacing: 4) {
            ZStack {
                Circle()
                    .fill(stage.status == "completed" ? color : PdigV2Colors.surfaceRaised)
                    .frame(width: 34, height: 34)
                Circle()
                    .stroke(color, lineWidth: 2)
                    .frame(width: 34, height: 34)
                Group {
                    switch stage.status {
                    case "completed":
                        Image(systemName: "checkmark")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(PdigV2Colors.canvasDeep)
                    case "blocked":
                        Image(systemName: "exclamationmark")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(color)
                    case "verifying":
                        Image(systemName: "clock")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(color)
                    default:
                        Text("\(stage.stage)")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(color)
                    }
                }
            }
            Text(label)
                .font(.system(size: 10, weight: .medium))
                .foregroundColor(stage.status == "not_started" ? PdigV2Colors.textMuted : PdigV2Colors.textPrimary)
                .lineLimit(2)
                .multilineTextAlignment(.center)
                .frame(width: 72)
            Text(vStatusLabel(stage.status))
                .font(.system(size: 9, weight: .medium))
                .foregroundColor(color)
        }
        .frame(width: 72)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("第 \(stage.stage) 阶段：\(label)，\(vStatusLabel(stage.status))"
            + (stage.blockReason.map { "，原因：\($0)" } ?? ""))
        .accessibilityIdentifier(VTestIds.changePhoneStage(stage.stage))
    }
}
