package com.pdig.core.scenario

/**
 * ScenarioTemplateRegistry —— 静态产品配置，**不是** Graph Reality。
 *
 * 政策 gate（spec/domain/domain.json → scenarioTemplates.policy）：
 *  - active 模板必须 capability=payment 且可执行
 *  - planned 模板没有 factory，必须被 instantiate 拒绝
 *  - 注册表中不得出现日常生活提醒类模板（喝水/生日/浇花/会议/健身/考试/纪念日/假期）
 *
 * MVP03 只有三个 active 支付场景。
 */

data class ScenarioTemplateInputSpec(
    val key: String,
    val required: Boolean,
    val description: String,
)

data class ScenarioTemplate(
    val id: String,
    val category: String,
    val title: String,
    val description: String,
    val supportedCapabilities: List<String>,
    val requiredInputs: List<ScenarioTemplateInputSpec>,
    val optionalInputs: List<ScenarioTemplateInputSpec>,
    val recommendedLeadTimeDays: Int?,
    val availability: String,
)

private fun paymentInputs() = Pair(
    listOf(
        ScenarioTemplateInputSpec(
            key = "targetPaymentInstrumentId",
            required = true,
            description = "要变更的支付工具节点 id",
        ),
    ),
    listOf(
        ScenarioTemplateInputSpec(
            key = "replacementPaymentInstrumentId",
            required = false,
            description = "替代支付工具节点 id（可选）",
        ),
        ScenarioTemplateInputSpec(
            key = "effectiveDate",
            required = false,
            description = "计划生效日期（ISO 8601）",
        ),
    ),
)

object ScenarioRegistry {

    private fun paymentTemplate(
        id: String,
        title: String,
        description: String,
        leadTimeDays: Int?,
    ): ScenarioTemplate {
        val (required, optional) = paymentInputs()
        return ScenarioTemplate(
            id = id,
            category = "payment",
            title = title,
            description = description,
            supportedCapabilities = listOf("payment"),
            requiredInputs = required,
            optionalInputs = optional,
            recommendedLeadTimeDays = leadTimeDays,
            availability = "active",
        )
    }

    val active: List<ScenarioTemplate> = listOf(
        paymentTemplate(
            id = "replace_payment_card",
            title = "更换银行卡",
            description = "换卡前检查支付钱包、自动扣款和订阅关系。",
            leadTimeDays = null,
        ),
        paymentTemplate(
            id = "expiring_payment_card",
            title = "银行卡即将到期",
            description = "到期前检查仍依赖这张卡的支付路径。",
            leadTimeDays = 30,
        ),
        paymentTemplate(
            id = "close_payment_instrument",
            title = "注销银行卡",
            description = "注销前确认哪些支付关系需要迁移。",
            leadTimeDays = 14,
        ),
    )

    val planned: List<ScenarioTemplate> = listOf(
        ScenarioTemplate(
            id = "replace_phone_number",
            category = "identity",
            title = "更换手机号",
            description = "设计稿（未实现）：手机号关联的验证与恢复路径检查。",
            supportedCapabilities = listOf("access", "recovery", "identity"),
            requiredInputs = listOf(
                ScenarioTemplateInputSpec(
                    key = "targetPhoneAnchorId",
                    required = true,
                    description = "手机号锚点节点 id",
                ),
            ),
            optionalInputs = emptyList(),
            recommendedLeadTimeDays = 30,
            availability = "planned",
        ),
    )

    /** 政策 gate：日常生活提醒类模板禁止进入注册表。 */
    val excludedDomains: List<String> = listOf(
        "water", "birthday", "plant", "meeting",
        "exercise", "exam", "anniversary", "holiday",
    )

    /** planned 模板不可执行。 */
    fun isExecutable(templateId: String): Boolean =
        active.any { it.id == templateId }

    fun get(templateId: String): ScenarioTemplate? =
        active.firstOrNull { it.id == templateId } ?: planned.firstOrNull { it.id == templateId }
}
