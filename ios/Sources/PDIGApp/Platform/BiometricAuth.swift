// BiometricAuth —— LocalAuthentication 封装（设备凭据兜底）。
//
// 平台安全层：优先生物识别（面容/触控 ID），失败/不可用时回退设备密码
// （LAPolicyDeviceOwnerAuthentication）。macOS CI 只验证编译；
// 真机交互验证为 NOT_RUN。

import Foundation
import LocalAuthentication

public enum AuthOutcome: Equatable, Sendable {
    case success
    case cancelled
    case failed
    case unavailable
}

public enum BiometricAuth {

    /// 当前设备是否支持生物识别（仅探测，不弹窗）。
    public static func isBiometryAvailable() -> Bool {
        let context = LAContext()
        var error: NSError?
        return context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error)
    }

    /// 验证身份：生物识别，失败时兜底设备密码。
    public static func authenticate(reason: String = CopyZh.lockPrompt) async -> AuthOutcome {
        let context = LAContext()
        var error: NSError?
        let policy = isBiometryAvailable()
            ? LAPolicy.deviceOwnerAuthenticationWithBiometrics
            : LAPolicy.deviceOwnerAuthentication

        guard context.canEvaluatePolicy(policy, error: &error) else {
            return .unavailable
        }
        do {
            let ok = try await context.evaluatePolicy(policy, localizedReason: reason)
            return ok ? .success : .failed
        } catch let laError as LAError {
            switch laError.code {
            case .userCancel, .appCancel, .systemCancel:
                return .cancelled
            default:
                return .failed
            }
        } catch {
            return .failed
        }
    }
}
