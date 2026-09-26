// Onboarding（≤3 屏）+ Lock/Unlock 门（task #2）。

import SwiftUI
import PDIGCore

/// Onboarding：3 屏介绍 → 完成（写入解锁盐 → 进入 Lock 门）。
struct OnboardingView: View {
    @ObservedObject var session: AppSession
    @State private var page = 0

    private let pages: [(icon: String, title: String, body: String)] = [
        ("network", "你的数字基础设施，一张图", "银行卡、手机号、账户、设备之间的关系，都在这张图里。"),
        ("lock.shield", "本地优先，不上传", "数据只保存在本机。没有账号、没有云端、没有统计。"),
        ("arrow.triangle.2.circlepath", "换卡 / 换号前先看清楚", "哪些账户和恢复路径会受影响、应该先处理什么，一目了然。"),
    ]

    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            Image(systemName: pages[page].icon)
                .font(.system(size: 56))
                .foregroundStyle(.tint)
            Text(pages[page].title)
                .font(.title2.bold())
            Text(pages[page].body)
                .font(.body)
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
                .padding(.horizontal, 32)
            Spacer()
            HStack(spacing: 8) {
                ForEach(0..<pages.count, id: \.self) { i in
                    Circle()
                        .fill(i == page ? Color.accentColor : Color.gray.opacity(0.4))
                        .frame(width: 8, height: 8)
                }
            }
            Button {
                if page < pages.count - 1 {
                    page += 1
                } else {
                    session.finishOnboarding()
                }
            } label: {
                Text(page < pages.count - 1 ? CopyZh.next : CopyZh.done)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
            }
            .buttonStyle(.borderedProminent)
            .padding(.horizontal, 32)
            Spacer().frame(height: 16)
        }
        .frame(minWidth: 420, minHeight: 600)
    }
}

/// Lock / Unlock 门：LocalAuthentication 生物识别 + 设备密码兜底。
struct LockView: View {
    @ObservedObject var session: AppSession
    @State private var message: String?

    var body: some View {
        VStack(spacing: 20) {
            Spacer()
            Image(systemName: "lock.fill")
                .font(.system(size: 48))
                .foregroundStyle(.secondary)
            Text(CopyZh.lockTitle).font(.title2.bold())
            Text(CopyZh.lockPrompt).foregroundStyle(.secondary)
            if let message = message {
                Text(message).font(.footnote).foregroundStyle(.red)
            }
            Button {
                Task {
                    let outcome = await BiometricAuth.authenticate()
                    switch outcome {
                    case .success:
                        session.unlockSucceeded()
                    case .cancelled:
                        message = CopyZh.lockCancelled
                    case .failed:
                        message = CopyZh.lockFailed
                    case .unavailable:
                        message = CopyZh.lockUnavailable
                    }
                }
            } label: {
                Text(CopyZh.lockTitle)
                    .frame(maxWidth: 240)
                    .padding(.vertical, 8)
            }
            .buttonStyle(.borderedProminent)
            Spacer()
        }
        .frame(minWidth: 420, minHeight: 600)
    }
}
