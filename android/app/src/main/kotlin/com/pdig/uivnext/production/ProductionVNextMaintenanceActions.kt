package com.pdig.uivnext.production

import com.pdig.app.data.AppContainer
import com.pdig.app.data.MaintenanceWriteResult
import com.pdig.core.domain.MaintenanceCadenceWrite
import com.pdig.core.domain.MaintenanceFactWrite
import com.pdig.core.domain.MaintenanceScheduleWrite
import com.pdig.core.generated.MaintenanceCadenceKind
import com.pdig.core.generated.MaintenanceFactKind
import com.pdig.core.generated.MaintenanceScheduleKind
import com.pdig.core.generated.MaintenanceValueType

/**
 * Consumer-facing maintenance write authority.
 *
 * UI supplies explicit user-entered values. This layer maps them to stable Canonical
 * ids/kinds; Core validates applicability/value syntax and Repository commits Reality.
 */
internal interface VNextMaintenanceActionGateway {
    fun confirmCardLifecycle(
        nodeId: String,
        annualFeeAmount: String? = null,
        annualFeeCurrency: String? = null,
        billingDay: Int? = null,
        paymentDueDay: Int? = null,
        autopayMode: String? = null,
    ): MaintenanceWriteResult

    fun confirmPhonePlan(
        nodeId: String,
        planCost: String? = null,
        planCurrency: String? = null,
        billingMode: String? = null,
        renewalMethod: String? = null,
    ): MaintenanceWriteResult

    fun confirmPhoneKeepAliveInterval(
        nodeId: String,
        intervalDays: Int,
        anchorDate: String,
        lastCompletedAt: String? = null,
    ): MaintenanceWriteResult
}

internal class AppContainerVNextMaintenanceActionGateway(
    private val app: AppContainer,
) : VNextMaintenanceActionGateway {
    override fun confirmCardLifecycle(
        nodeId: String,
        annualFeeAmount: String?,
        annualFeeCurrency: String?,
        billingDay: Int?,
        paymentDueDay: Int?,
        autopayMode: String?,
    ): MaintenanceWriteResult {
        require((annualFeeAmount == null) == (annualFeeCurrency == null)) {
            "annual fee amount and currency must be confirmed together"
        }
        val facts = buildList {
            if (annualFeeAmount != null && annualFeeCurrency != null) {
                add(
                    MaintenanceFactWrite(
                        id = stableFactId(MaintenanceFactKind.CARD_ANNUAL_FEE_AMOUNT),
                        kind = MaintenanceFactKind.CARD_ANNUAL_FEE_AMOUNT,
                        valueType = MaintenanceValueType.DECIMAL_STRING,
                        value = annualFeeAmount.trim(),
                    ),
                )
                add(
                    MaintenanceFactWrite(
                        id = stableFactId(MaintenanceFactKind.CARD_ANNUAL_FEE_CURRENCY),
                        kind = MaintenanceFactKind.CARD_ANNUAL_FEE_CURRENCY,
                        valueType = MaintenanceValueType.CURRENCY_CODE,
                        value = annualFeeCurrency.trim().uppercase(),
                    ),
                )
            }
            billingDay?.let {
                add(
                    MaintenanceFactWrite(
                        id = stableFactId(MaintenanceFactKind.CARD_BILLING_DAY),
                        kind = MaintenanceFactKind.CARD_BILLING_DAY,
                        valueType = MaintenanceValueType.INTEGER,
                        value = it.toString(),
                    ),
                )
            }
            paymentDueDay?.let {
                add(
                    MaintenanceFactWrite(
                        id = stableFactId(MaintenanceFactKind.CARD_PAYMENT_DUE_DAY),
                        kind = MaintenanceFactKind.CARD_PAYMENT_DUE_DAY,
                        valueType = MaintenanceValueType.INTEGER,
                        value = it.toString(),
                    ),
                )
            }
            autopayMode?.trim()?.takeIf { it.isNotEmpty() }?.let {
                add(
                    MaintenanceFactWrite(
                        id = stableFactId(MaintenanceFactKind.CARD_AUTOPAY_MODE),
                        kind = MaintenanceFactKind.CARD_AUTOPAY_MODE,
                        valueType = MaintenanceValueType.TEXT,
                        value = it,
                    ),
                )
            }
        }
        require(facts.isNotEmpty()) { "no card lifecycle fact supplied" }
        return app.confirmMaintenanceFacts(nodeId, facts)
    }

    override fun confirmPhonePlan(
        nodeId: String,
        planCost: String?,
        planCurrency: String?,
        billingMode: String?,
        renewalMethod: String?,
    ): MaintenanceWriteResult {
        require((planCost == null) == (planCurrency == null)) {
            "plan cost and currency must be confirmed together"
        }
        val facts = buildList {
            if (planCost != null && planCurrency != null) {
                add(
                    MaintenanceFactWrite(
                        id = stableFactId(MaintenanceFactKind.NUMBER_PLAN_COST),
                        kind = MaintenanceFactKind.NUMBER_PLAN_COST,
                        valueType = MaintenanceValueType.DECIMAL_STRING,
                        value = planCost.trim(),
                    ),
                )
                add(
                    MaintenanceFactWrite(
                        id = stableFactId(MaintenanceFactKind.NUMBER_PLAN_CURRENCY),
                        kind = MaintenanceFactKind.NUMBER_PLAN_CURRENCY,
                        valueType = MaintenanceValueType.CURRENCY_CODE,
                        value = planCurrency.trim().uppercase(),
                    ),
                )
            }
            billingMode?.trim()?.takeIf { it.isNotEmpty() }?.let {
                add(
                    MaintenanceFactWrite(
                        id = stableFactId(MaintenanceFactKind.NUMBER_BILLING_MODE),
                        kind = MaintenanceFactKind.NUMBER_BILLING_MODE,
                        valueType = MaintenanceValueType.TEXT,
                        value = it,
                    ),
                )
            }
            renewalMethod?.trim()?.takeIf { it.isNotEmpty() }?.let {
                add(
                    MaintenanceFactWrite(
                        id = stableFactId(MaintenanceFactKind.NUMBER_RENEWAL_METHOD),
                        kind = MaintenanceFactKind.NUMBER_RENEWAL_METHOD,
                        valueType = MaintenanceValueType.TEXT,
                        value = it,
                    ),
                )
            }
        }
        require(facts.isNotEmpty()) { "no phone plan fact supplied" }
        return app.confirmMaintenanceFacts(nodeId, facts)
    }

    override fun confirmPhoneKeepAliveInterval(
        nodeId: String,
        intervalDays: Int,
        anchorDate: String,
        lastCompletedAt: String?,
    ): MaintenanceWriteResult =
        app.confirmMaintenanceSchedule(
            nodeId,
            MaintenanceScheduleWrite(
                id = stableScheduleId(MaintenanceScheduleKind.NUMBER_KEEP_ALIVE),
                kind = MaintenanceScheduleKind.NUMBER_KEEP_ALIVE,
                cadence = MaintenanceCadenceWrite(
                    kind = MaintenanceCadenceKind.INTERVAL_DAYS,
                    intervalDays = intervalDays,
                    anchorDate = anchorDate.trim(),
                ),
                lastCompletedAt = lastCompletedAt?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
}

private fun stableFactId(kind: MaintenanceFactKind): String =
    "maintenance:fact:" + kind.wire

private fun stableScheduleId(kind: MaintenanceScheduleKind): String =
    "maintenance:schedule:" + kind.wire
