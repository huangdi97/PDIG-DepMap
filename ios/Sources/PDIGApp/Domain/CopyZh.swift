// PDIG iOS N4 —— 用户文案（中文）。与 spec/ui/copy-zh.json 的键一一对应。
//
// 硬规则（copy-zh.json.hardRules）：
//  - 普通 UI 不得显示工程枚举（GraphRevision / DependencyProposal /
//    SHARED_FAILURE_DOMAIN / DAG / topological / graphRevision / criticality 等）。
//  - Readiness 永远不表达为 safe / fully safe / 100% / all clear。
//
// 注意：copy 只做键值翻译；枚举 wire 只在 ViewModel 内部使用，渲染层一律走这里。

import Foundation

/// 用户可见中文文案（单一来源，供所有 UI 使用）。
public enum CopyZh {

    // MARK: - home

    public static let homeNeedsAction = "需要你处理"
    public static let homePossibleChange = "可能发生了变化"
    public static let homeUpcoming = "即将到来"
    public static let homeCommonScenarios = "常用场景"
    public static let homeMyInfrastructure = "我的基础设施"
    public static let homeInfrastructureWeakness = "基础设施薄弱点"

    // MARK: - findings

    public static let findingsTitle = "基础设施薄弱点"
    public static let findingSinglePointOfFailure = "只有一个来源，没有备用路径"
    public static let findingSharedFailureDomain = "这两个路径依赖同一个故障点"
    public static let findingRecoveryCycle = "发现一个恢复循环"
    public static let findingUnconfirmedFallback = "备用路径尚未确认"
    public static let findingStaleRecoveryInformation = "恢复信息需要重新确认"
    public static let findingUnknownCriticalPath = "关键路径的影响还不清楚"
    public static let findingPendingVerification = "有一项变更仍未验证"
    public static let findingWhat = "发现了什么"
    public static let findingWhy = "为什么"
    public static let findingConfirmedBasis = "基于什么确认"
    public static let findingUnknowns = "还不知道什么"
    public static let findingAffectedCapability = "可能影响什么"
    public static let findingNextAction = "下一步建议"

    // MARK: - impact

    public static let impactMustChange = "必须处理"
    public static let impactBackupPath = "有备用路径"
    public static let impactNeedsReview = "建议检查"
    public static let impactUnaffected = "不受影响"
    public static let impactTargetOperation = "最后一步：执行原始操作"

    // MARK: - verification

    public static let verificationPending = "待验证"
    public static let verificationEvidenceSuggested = "发现可能是变更后的证据"
    public static let verificationVerified = "已验证"
    public static let verificationFailed = "验证未通过"
    public static let verificationNotRequired = "无需验证"
    /// 产品铁律提示（任务 #10）：添加 ≠ 验证。
    public static let verificationNewPathIsNotVerified = "新手机号已添加 ≠ 新恢复路径已经验证"

    // MARK: - readiness（copy-zh.readiness + 派生 CTA 文案）

    public static let readinessBlocked = "还有必须处理的事项"
    public static let readinessReviewRequired = "还有信息需要确认"
    public static let readinessReadyWithKnownScope = "基于当前信息，可以继续。"
    public static let ctaBlocked = "处理必须事项"
    public static let ctaReviewRequired = "继续确认"
    public static let ctaNeedsRevalidation = "重新检查"
    public static let ctaReadyWithKnownScope = "继续下一步"
    public static let ctaVerifying = "查看验证"
    public static let ctaCompleted = "查看结果"

    // MARK: - plan workflow

    public static let planDraft = "草稿"
    public static let planAnalyzed = "已分析"
    public static let planReviewRequired = "需要确认"
    public static let planReady = "可以开始"
    public static let planInProgress = "进行中"
    public static let planVerifying = "待验证"
    public static let planCompleted = "已完成"
    public static let planCancelled = "已取消"
    public static let planNeedsRevalidation = "需要重新检查"
    public static let planStaleSubtitle = "基础设施在此计划创建后发生了变化，需要重新分析。"

    // MARK: - timeline

    public static let bucketAttention = "需要你处理"
    public static let bucketOverdue = "已逾期"
    public static let bucketToday = "今天"
    public static let bucket7d = "7 天内"
    public static let bucket30d = "30 天内"
    public static let bucket90d = "90 天内"
    public static let bucketLater = "以后"

    // MARK: - relations / identity

    public static let relationFundingSource = "资金来源"
    public static let relationMerchantAgreement = "自动扣款 / 订阅"
    public static let relationRecovers = "恢复方式 / 找回路径"
    public static let relationAuthenticates = "验证方式 / 登录依据"
    public static let relationControls = "登录控制 / 管理入口"
    public static let criticalityRequired = "必须使用"
    public static let criticalityUnknown = "还不确定"
    public static let phoneNumber = "手机号"
    public static let email = "邮箱"
    public static let securityKey = "安全密钥"
    public static let recoveryPath = "恢复路径"
    public static let authenticationPath = "验证方式"
    public static let newPath = "新路径"
    public static let oldPath = "旧路径"

    // MARK: - make-before-break / change plan v0.3.0

    public static let prerequisite = "必须先完成"
    public static let completesBefore = "完成后才能继续"
    public static let waitingVerification = "等待验证"
    public static let parallel = "可以并行处理"
    public static let verifyBeforeRemove = "验证后才能移除旧路径"
    public static let newPathEstablished = "新路径已建立"
    public static let newPathVerified = "新路径已验证"
    public static let oldPathRetired = "旧路径已停用"

    // MARK: - failure domain / recovery cycle

    public static let sharedDevice = "这两个恢复方式都依赖同一台手机"
    public static let sharedPhoneNumber = "这两个恢复方式都依赖同一个手机号"
    public static let sharedProvider = "这两个路径都属于同一家服务商"
    public static let failureDomainNeedsReview = "它们是否共享同一个故障点还不确定"
    public static let independentPath = "独立恢复路径"
    public static let notIndependent = "不是独立的备用路径"
    public static let recoveryCycleConfirmed = "这个恢复方式本身依赖你正在尝试恢复的账号，因此不能视为独立备用路径"
    public static let recoveryCyclePotential = "这个恢复方式可能依赖你正在尝试恢复的账号，需要确认"
    public static let recoveryCycleNoCycle = "未发现恢复循环"

    // MARK: - scenario

    public static let scenarioReplaceCard = "更换银行卡"
    public static let scenarioExpiringCard = "银行卡即将到期"
    public static let scenarioCloseCard = "注销银行卡"
    public static let scenarioReplacePhone = "更换手机号"
    public static let scenarioPaymentCategory = "支付"
    public static let scenarioIdentityRecoveryCategory = "身份与恢复"
    public static let scenarioSetup = "场景设置"
    public static let selectOldPhone = "选择旧手机号"
    public static let reviewImpact = "查看影响"
    public static let reviewRecoveryPaths = "查看恢复路径"
    public static let reviewFailureDomains = "查看共享故障点"
    public static let addNewPhone = "添加新手机号"
    public static let verifyNewPhone = "验证新手机号"
    public static let migrateAccounts = "迁移关键账户"
    public static let retireOldPhone = "停用旧手机号（新路径全部验证后）"
    public static let scenarioComplete = "完成"

    // MARK: - import

    public static let sourceWechat = "微信账单"
    public static let sourceCsv = "通用 CSV"
    public static let sourceOfx = "OFX / QFX"
    public static let mappingTitle = "选择对应的列"
    public static let mappingHint = "请选择文件中与下列字段对应的列。"
    public static let parseFailed = "账单文件无法解析。"
    public static let mappingMissing = "选择的列在文件中不存在，请重新选择。"

    // MARK: - proposal / drift / candidates

    public static let proposalBasis = "本次分析依据"
    public static let pendingService = "待确认服务"
    public static let reviewAgain = "需要重新检查"
    public static let driftPossibleChange = "可能发生了变化"
    public static let driftConfirmReplacement = "确认是更换了来源"
    public static let driftConfirmAdditional = "确认是新增了一条支付路径"
    public static let driftDismiss = "先忽略"

    // MARK: - backup / lock / common

    public static let backupExportTitle = "导出加密备份"
    public static let backupImportTitle = "从备份恢复"
    public static let backupPasswordNotice = "密码一旦忘记将无法找回。"
    public static let backupCrossDevice = "备份文件可以在其他设备上导入。"
    public static let backupWrongPassword = "密码错误，或备份文件已被修改。"
    public static let lockTitle = "解锁"
    public static let lockPrompt = "验证身份以继续"
    public static let lockCancelled = "已取消验证。"
    public static let lockFailed = "身份验证失败。"
    public static let lockUnavailable = "当前设备无法完成身份验证。"
    public static let confirm = "确认"
    public static let cancel = "取消"
    public static let save = "保存"
    public static let delete = "删除"
    public static let next = "下一步"
    public static let back = "返回"
    public static let done = "完成"
    public static let retry = "重试"
    public static let emptyTitle = "还没有内容"
    public static let loading = "加载中…"
    public static let errorTitle = "出错了"

    // MARK: - node detail（六问卡）

    public static let nodeWhat = "是什么"
    public static let nodeConfirmed = "确认了什么"
    public static let nodeOpenQuestions = "待处理问题"
    public static let nodeEvidence = "依据与最近确认"

    // MARK: - infrastructure

    public static let infraByItem = "按对象"
    public static let infraByCapability = "按能力"
    public static let capabilityPayment = "支付"
    public static let capabilityRecovery = "身份与恢复"
    public static let capabilityAccess = "访问与认证"
    public static let capabilityDevice = "设备"
    public static let capabilityService = "关键服务"

    // MARK: - settings / security / about

    public static let settingsTitle = "设置"
    public static let securityTitle = "安全"
    public static let aboutTitle = "关于"
    public static let deleteAllData = "删除所有数据"
    public static let deleteAllDataConfirmTitle = "确认删除所有数据？"
    public static let deleteAllDataConfirmBody = "本机的基础设施图、关系与导入记录将被永久删除，无法恢复。"
    public static let deleteAllDataConfirmAction = "删除所有数据"
    public static let aboutAppName = "个人数字依赖图"
    public static let aboutVersion = "v0.3.0"
    public static let aboutLocalFirst = "本地优先：数据只保存在本机，不上传、无账号、无统计。"
    public static let persistenceLabel = "数据存储"
    public static let persistenceSqlcipherNotRun = "SQLCipher 尚未接入（NOT_RUN）：当前使用 PDIGCore 序列化 + 系统 SQLite 文件存储。"
    public static let securityBiometric = "面容 / 触控 ID 解锁"
    public static let securityDeviceCredential = "设备密码解锁"

    // MARK: - helper 映射

    /// 按 Capability wire 转中文（UI 渲染专用，不暴露 wire）。
    public static func capability(_ wire: String) -> String {
        switch wire {
        case Capability.payment.wire: return capabilityPayment
        case Capability.recovery.wire: return capabilityRecovery
        case Capability.authentication.wire: return capabilityAccess
        case Capability.access.wire: return capabilityAccess
        case Capability.identity.wire: return capabilityRecovery
        case Capability.communication.wire: return capabilityService
        default: return wire
        }
    }

    /// 按 Relation wire 转中文。
    public static func relation(_ wire: String) -> String {
        switch wire {
        case Relation.fundingSource.wire: return relationFundingSource
        case Relation.merchantAgreement.wire: return relationMerchantAgreement
        case Relation.recovers.wire: return relationRecovers
        case Relation.authenticates.wire: return relationAuthenticates
        case Relation.controls.wire: return relationControls
        default: return wire
        }
    }

    /// 按 NodeKind wire 转中文（用于基础设施分组与节点详情）。
    public static func nodeKind(_ wire: String) -> String {
        switch wire {
        case NodeKind.paymentInstrument.wire: return "银行卡"
        case NodeKind.identityAnchor.wire: return phoneNumber
        case NodeKind.account.wire: return "账户"
        case NodeKind.service.wire: return "服务"
        case NodeKind.membership.wire: return "会员"
        case NodeKind.device.wire: return "设备"
        default: return "自定义对象"
        }
    }

    /// timeline bucket 转中文。
    public static func timelineBucket(_ wire: String) -> String {
        switch wire {
        case TimelineBucket.attention.rawValue: return bucketAttention
        case TimelineBucket.overdue.rawValue: return bucketOverdue
        case TimelineBucket.today.rawValue: return bucketToday
        case TimelineBucket.v7d.rawValue: return bucket7d
        case TimelineBucket.v30d.rawValue: return bucket30d
        case TimelineBucket.v90d.rawValue: return bucket90d
        case TimelineBucket.later.rawValue: return bucketLater
        default: return bucketLater
        }
    }
}
