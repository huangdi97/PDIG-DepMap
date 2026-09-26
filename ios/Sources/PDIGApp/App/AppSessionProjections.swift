// AppSession 投影扩展 —— 仓储读取的容错封装（避免 Optional/Optional 嵌套）。

import Foundation
import PDIGCore

public extension AppSession {

    /// 开放漂移（失败返回空，不崩 UI）。
    func openDrifts() -> [GraphRepository.DriftRow] {
        guard let repo = repository else { return [] }
        return (try? repo.loadOpenDrifts()) ?? []
    }

    /// 待确认候选（失败返回空）。
    func pendingCandidatesCount() -> Int {
        guard let repo = repository else { return 0 }
        return (try? repo.loadPendingCandidates())?.count ?? 0
    }
}