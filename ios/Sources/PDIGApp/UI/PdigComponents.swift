// PdigComponents —— PDIG iOS 统一组件（Quiet Infrastructure，docs/uiux/PDIG_DESIGN_SYSTEM.md §7/§8/§13/§14）。
// 状态永不只靠颜色：icon + label + color 三通道（P0，spec §46）。

import SwiftUI

// MARK: - 卡片（surface + 1pt border + radius 8；替代灰底散用）

struct PdigCard<Content: View>: View {
    private let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        content
            .padding(PdigTheme.Spacing.sm + 2)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(PdigTheme.Color.surface)
            .overlay(
                RoundedRectangle(cornerRadius: PdigTheme.Radius.md, style: .continuous)
                    .stroke(PdigTheme.Color.border, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: PdigTheme.Radius.md, style: .continuous))
    }
}

// MARK: - 状态徽标（icon + label + color 三通道）

/// 可用状态（与 spec §46 / 冻结 §9 对应，禁止在此扩为"工厂"）。
enum StatusKind {
    case verified      // 双勾 success 强
    case completed     // 单勾 secondary 弱（≠ verified）
    case blocked       // error
    case review        // warning
    case verifying     // info
    case unknown       // question

    /// 用文字字形表示双勾/单勾（SF Symbols 无原生双勾）。
    var icon: String {
        switch self {
        case .verified: return "✓✓"
        case .completed: return "✓"
        case .blocked: return "exclamationmark.triangle.fill"
        case .review: return "exclamationmark.triangle"
        case .verifying: return "clock.fill"
        case .unknown: return "questionmark.circle"
        }
    }

    var label: String {
        switch self {
        case .verified: return CopyZh.verificationVerified
        case .completed: return CopyZh.planCompleted
        case .blocked: return CopyZh.readinessBlocked
        case .review: return CopyZh.readinessReviewRequired
        case .verifying: return CopyZh.verificationPending
        case .unknown: return CopyZh.criticalityUnknown
        }
    }

    var color: SwiftUI.Color {
        switch self {
        case .verified: return PdigTheme.Color.success
        case .completed: return PdigTheme.Color.textSecondary
        case .blocked: return PdigTheme.Color.danger
        case .review: return PdigTheme.Color.warning
        case .verifying: return PdigTheme.Color.info
        case .unknown: return PdigTheme.Color.textTertiary
        }
    }

    /// 强状态用实心色底；弱状态（completed/unknown）用中性底，弱于强态。
    var isStrong: Bool {
        switch self {
        case .verified, .blocked, .review, .verifying: return true
        case .completed, .unknown: return false
        }
    }
}

struct StatusBadge: View {
    let kind: StatusKind

    init(_ kind: StatusKind) { self.kind = kind }

    var body: some View {
        HStack(spacing: 4) {
            if kind == .verified || kind == .completed {
                Text(kind.icon).font(.system(size: 10, weight: .bold))
            } else {
                Image(systemName: kind.icon).font(.system(size: 10, weight: .semibold))
            }
            Text(kind.label).font(PdigTheme.Font.label)
        }
        .foregroundStyle(kind.isStrong ? SwiftUI.Color.white : kind.color)
        .padding(.horizontal, 8)
        .padding(.vertical, 3)
        .background(kind.isStrong ? kind.color : PdigTheme.Color.surfaceLow, in: Capsule())
        .overlay(
            Capsule().stroke(kind.isStrong ? SwiftUI.Color.clear : PdigTheme.Color.border, lineWidth: 1)
        )
        .accessibilityLabel(kind.label)
    }
}

// MARK: - 小节标题（文字 + hairline）

struct SectionHeader: View {
    let title: String

    init(_ title: String) { self.title = title }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(PdigTheme.Font.section)
                .foregroundStyle(PdigTheme.Color.textPrimary)
            Rectangle()
                .fill(PdigTheme.Color.border)
                .frame(height: 1)
        }
    }
}

// MARK: - 空态（这里是什么 / 为什么为空 / 下一步）

struct EmptyState: View {
    let icon: String
    let title: String
    let message: String
    var actionTitle: String? = nil
    var action: (() -> Void)? = nil

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 24))
                .foregroundStyle(PdigTheme.Color.textTertiary)
            Text(title).font(.system(size: 14, weight: .semibold))
            Text(message)
                .font(.system(size: 13))
                .foregroundStyle(PdigTheme.Color.textSecondary)
                .multilineTextAlignment(.center)
            if let actionTitle, let action {
                Button(action: action) {
                    Text(actionTitle).font(PdigTheme.Font.button)
                }
                .buttonStyle(.borderedProminent)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 32)
        .padding(.horizontal, 16)
    }
}

// MARK: - Continuity Rail（步骤轨道；冻结 §3/§14，spec §53）

/// 单步状态（语义可视化，禁止做成装饰图）。
enum RailStepState: Equatable {
    case done                // 实心 success 对勾
    case current(index: Int) // 空心 primary + 编号
    case blocked             // 实心 danger + !
    case verifying           // 空心 info + 时钟
    case future              // 灰空心
}

struct StepRail: View {
    let steps: [RailStepState]

    /// 当前进行的步骤索引（含）前的都算 done。
    private var currentStepIndex: Int {
        for (i, s) in steps.enumerated() where s != .done {
            return i
        }
        return steps.count
    }

    var body: some View {
        HStack(spacing: 2) {
            ForEach(steps.indices, id: \.self) { i in
                if i > 0 { connector(before: i) }
                node(steps[i])
            }
        }
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("步骤 \(currentStepIndex + 1) / \(steps.count)")
    }

    @ViewBuilder
    private func node(_ s: RailStepState) -> some View {
        ZStack {
            switch s {
            case .done:
                Circle().fill(PdigTheme.Color.success)
                Image(systemName: "checkmark")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(.white)
            case .current(let index):
                Circle().stroke(PdigTheme.Color.primary, lineWidth: 1.5)
                Text("\(index + 1)")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(PdigTheme.Color.primary)
            case .blocked:
                Circle().fill(PdigTheme.Color.danger)
                Text("!")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(.white)
            case .verifying:
                Circle().stroke(PdigTheme.Color.info, lineWidth: 1.5)
                Image(systemName: "clock.fill")
                    .font(.system(size: 9))
                    .foregroundStyle(PdigTheme.Color.info)
            case .future:
                Circle().stroke(PdigTheme.Color.textTertiary, lineWidth: 1.5)
            }
        }
        .frame(width: 22, height: 22)
    }

    /// 节点间连线：已完成段实线 success，未完成段虚灰线。
    private func connector(before i: Int) -> some View {
        let prevDone = steps[i - 1] == .done
        return Group {
            if prevDone {
                Rectangle().fill(PdigTheme.Color.success).frame(height: 2)
            } else {
                Rectangle()
                    .stroke(PdigTheme.Color.border, style: StrokeStyle(lineWidth: 2, dash: [3, 3]))
                    .frame(height: 2)
            }
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - 说明（gate 明文原因 / 轻提示），danger/soft 底

struct NoticeBanner: View {
    let icon: String
    let text: String
    let color: SwiftUI.Color

    init(icon: String, text: String, color: SwiftUI.Color) {
        self.icon = icon
        self.text = text
        self.color = color
    }

    var body: some View {
        HStack(alignment: .top, spacing: 6) {
            Image(systemName: icon).font(.system(size: 12)).foregroundStyle(color)
            Text(text)
                .font(.footnote)
                .foregroundStyle(PdigTheme.Color.textPrimary)
        }
        .padding(10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(color.opacity(0.1), in: RoundedRectangle(cornerRadius: PdigTheme.Radius.md, style: .continuous))
    }
}