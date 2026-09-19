// ScenarioTemplateRegistry —— 静态产品配置，**不是** Graph Reality（Swift 移植）。
//
// 源头：android/core/.../scenario/ScenarioRegistry.kt。
//
// 政策 gate（spec/domain/domain.json → scenarioTemplates.policy）：
//  - active 模板必须 capability=payment 且可执行
//  - planned 模板没有 factory，必须被 instantiate 拒绝
//  - 注册表中不得出现日常生活提醒类模板（喝水/生日/浇花/会议/健身/考试/纪念日/假期）

import Foundation

public struct ScenarioTemplateInputSpec: Equatable, Sendable {
    public let key: String
    public let required: Bool
    public let description: String

    public init(key: String, required: Bool, description: String) {
        self.key = key
        self.required = required
        self.description = description
    }
}

public struct ScenarioTemplate: Equatable, Sendable {
    public let id: String
    public let category: String
    public let title: String
    public let description: String
    public let supportedCapabilities: [String]
    public let requiredInputs: [ScenarioTemplateInputSpec]
    public let optionalInputs: [ScenarioTemplateInputSpec]
    public let recommendedLeadTimeDays: Int?
    public let availability: String

    public init(
        id: String,
        category: String,
        title: String,
        description: String,
        supportedCapabilities: [String],
        requiredInputs: [ScenarioTemplateInputSpec],
        optionalInputs: [ScenarioTemplateInputSpec],
        recommendedLeadTimeDays: Int?,
        availability: String
    ) {
        self.id = id
        self.category = category
        self.title = title
        self.description = description
        self.supportedCapabilities = supportedCapabilities
        self.requiredInputs = requiredInputs
        self.optionalInputs = optionalInputs
        self.recommendedLeadTimeDays = recommendedLeadTimeDays
        self.availability = availability
    }
}

public enum ScenarioRegistry {
    private static func paymentInputs() -> ([ScenarioTemplateInputSpec], [ScenarioTemplateInputSpec]) {
        let required = [
            ScenarioTemplateInputSpec(
                key: "targetPaymentInstrumentId",
                required: true,
                description: "要变更的支付工具节点 id"
            )
        ]
        let optional = [
            ScenarioTemplateInputSpec(
                key: "replacementPaymentInstrumentId",
                required: false,
                description: "替代支付工具节点 id（可选）"
            ),
            ScenarioTemplateInputSpec(
                key: "effectiveDate",
                required: false,
                description: "计划生效日期（ISO 8601）"
            ),
        ]
        return (required, optional)
    }

    private static func paymentTemplate(
        id: String,
        title: String,
        description: String,
        leadTimeDays: Int?
    ) -> ScenarioTemplate {
        let (required, optional) = paymentInputs()
        return ScenarioTemplate(
            id: id,
            category: "payment",
            title: title,
            description: description,
            supportedCapabilities: ["payment"],
            requiredInputs: required,
            optionalInputs: optional,
            recommendedLeadTimeDays: leadTimeDays,
            availability: "active"
        )
    }

    public static let active: [ScenarioTemplate] = [
        paymentTemplate(
            id: "replace_payment_card",
            title: "更换银行卡",
            description: "换卡前检查支付钱包、自动扣款和订阅关系。",
            leadTimeDays: nil
        ),
        paymentTemplate(
            id: "expiring_payment_card",
            title: "银行卡即将到期",
            description: "到期前检查仍依赖这张卡的支付路径。",
            leadTimeDays: 30
        ),
        paymentTemplate(
            id: "close_payment_instrument",
            title: "注销银行卡",
            description: "注销前确认哪些支付关系需要迁移。",
            leadTimeDays: 14
        ),
    ]

    public static let planned: [ScenarioTemplate] = [
        ScenarioTemplate(
            id: "replace_phone_number",
            category: "identity",
            title: "更换手机号",
            description: "设计稿（未实现）：手机号关联的验证与恢复路径检查。",
            supportedCapabilities: ["access", "recovery", "identity"],
            requiredInputs: [
                ScenarioTemplateInputSpec(
                    key: "targetPhoneAnchorId",
                    required: true,
                    description: "手机号锚点节点 id"
                )
            ],
            optionalInputs: [],
            recommendedLeadTimeDays: 30,
            availability: "planned"
        ),
    ]

    /// 政策 gate：日常生活提醒类模板禁止进入注册表。
    public static let excludedDomains: [String] = [
        "water", "birthday", "plant", "meeting",
        "exercise", "exam", "anniversary", "holiday",
    ]

    /// planned 模板不可执行。
    public static func isExecutable(_ templateId: String) -> Bool {
        active.contains { $0.id == templateId }
    }

    public static func get(_ templateId: String) -> ScenarioTemplate? {
        active.first { $0.id == templateId } ?? planned.first { $0.id == templateId }
    }
}
