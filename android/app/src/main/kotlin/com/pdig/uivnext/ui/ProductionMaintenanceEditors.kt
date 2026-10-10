package com.pdig.uivnext.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.production.ProductionVNextSession
import com.pdig.uivnext.production.VNextMaintenanceActionGateway
import com.pdig.uivnext.production.VNextProductionObject
import com.pdig.uivnext.production.VNextProductionSurfaceKind
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius

@Composable
internal fun ProductionCardMaintenanceControls(
    session: ProductionVNextSession,
    item: VNextProductionObject,
) {
    val authority = session.authorities?.maintenance ?: return
    ProductionCardMaintenanceControls(
        appState = session.appState,
        authority = authority,
        item = item,
    )
}

@Composable
internal fun ProductionCardMaintenanceControls(
    appState: VAppState,
    authority: VNextMaintenanceActionGateway,
    item: VNextProductionObject,
) {
    if (item.surfaceKind != VNextProductionSurfaceKind.PAYMENT_ASSET) return

    val fee = item.maintenanceFactValue("card_annual_fee_amount")
    val currency = item.maintenanceFactValue("card_annual_fee_currency")
    val billing = item.maintenanceFactValue("card_billing_day")
    val due = item.maintenanceFactValue("card_payment_due_day")
    val autopay = item.maintenanceFactValue("card_autopay_mode")
    val annual = item.maintenanceSchedule("card_annual_fee_checkpoint")

    var editFacts by remember { mutableStateOf(false) }
    var editAnnual by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    MaintenanceControlPanel(
        title = "管理生命周期资料",
        description = "记录你已确认的卡片维护事实；不会根据银行常见规则自动填写。",
        primaryLabel = "编辑用卡周期",
        secondaryLabel = "设置年费节点",
        onPrimary = { editFacts = true },
        onSecondary = { editAnnual = true },
        feedback = feedback,
        testTag = "pdig.production-vnext.card.maintenance.controls",
    )

    if (editFacts) {
        var amountInput by remember(item.id, fee) { mutableStateOf(fee.orEmpty()) }
        var currencyInput by remember(item.id, currency) { mutableStateOf(currency.orEmpty()) }
        var billingInput by remember(item.id, billing) { mutableStateOf(billing.orEmpty()) }
        var dueInput by remember(item.id, due) { mutableStateOf(due.orEmpty()) }
        var autopayInput by remember(item.id, autopay) { mutableStateOf(autopay.orEmpty()) }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { editFacts = false },
            title = { Text("编辑用卡周期") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MaintenanceTruthNote()
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it.take(32) },
                            label = { Text("年费金额") },
                            modifier = Modifier.weight(1f)
                                .testTag("pdig.production-vnext.card.maintenance.amount"),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = currencyInput,
                            onValueChange = { currencyInput = it.take(3).uppercase() },
                            label = { Text("币种") },
                            modifier = Modifier.weight(1f)
                                .testTag("pdig.production-vnext.card.maintenance.currency"),
                            singleLine = true,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = billingInput,
                            onValueChange = { billingInput = it.take(2) },
                            label = { Text("账单日 1–31") },
                            modifier = Modifier.weight(1f)
                                .testTag("pdig.production-vnext.card.maintenance.billing-day"),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = dueInput,
                            onValueChange = { dueInput = it.take(2) },
                            label = { Text("还款日 1–31") },
                            modifier = Modifier.weight(1f)
                                .testTag("pdig.production-vnext.card.maintenance.payment-day"),
                            singleLine = true,
                        )
                    }
                    OutlinedTextField(
                        value = autopayInput,
                        onValueChange = { autopayInput = it.take(120) },
                        label = { Text("自动还款方式") },
                        modifier = Modifier.fillMaxWidth()
                            .testTag("pdig.production-vnext.card.maintenance.autopay"),
                        singleLine = true,
                    )
                    error?.let { Text(it, color = PdigV2Colors.Critical, fontSize = 11.sp) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountInput.trim().takeIf { it.isNotEmpty() }
                        val cur = currencyInput.trim().takeIf { it.isNotEmpty() }
                        val billingDay = parseOptionalDay(billingInput)
                        val paymentDay = parseOptionalDay(dueInput)
                        val auto = autopayInput.trim().takeIf { it.isNotEmpty() }

                        error = when {
                            (amount == null) != (cur == null) -> "年费金额与币种必须一起记录。"
                            billingInput.isNotBlank() && billingDay == null -> "账单日必须是 1–31。"
                            dueInput.isNotBlank() && paymentDay == null -> "还款日必须是 1–31。"
                            amount == null && billingDay == null && paymentDay == null && auto == null ->
                                "请至少填写一项已确认资料。"
                            else -> null
                        }
                        if (error == null) {
                            val result = runCatching {
                                authority.confirmCardLifecycle(
                                    nodeId = item.id,
                                    annualFeeAmount = amount,
                                    annualFeeCurrency = cur,
                                    billingDay = billingDay,
                                    paymentDueDay = paymentDay,
                                    autopayMode = auto,
                                )
                            }
                            if (result.isSuccess) {
                                feedback = "已记录到本机 Reality"
                                editFacts = false
                                appState.requestRealityRefresh()
                            } else {
                                error = "保存失败，请检查输入是否符合受治理规则。"
                            }
                        }
                    },
                    modifier = Modifier.testTag("pdig.production-vnext.card.maintenance.save"),
                ) { Text("保存已确认资料") }
            },
            dismissButton = {
                TextButton(onClick = { editFacts = false }) { Text("取消") }
            },
        )
    }

    if (editAnnual) {
        var monthInput by remember(item.id, annual?.month) {
            mutableStateOf(annual?.month?.toString().orEmpty())
        }
        var dayInput by remember(item.id, annual?.day) {
            mutableStateOf(annual?.day?.toString().orEmpty())
        }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { editAnnual = false },
            title = { Text("年费检查节点") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MaintenanceTruthNote()
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = monthInput,
                            onValueChange = { monthInput = it.take(2) },
                            label = { Text("月份 1–12") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = dayInput,
                            onValueChange = { dayInput = it.take(2) },
                            label = { Text("日期 1–31") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                    }
                    Text(
                        "该节点用于提醒/维护，不代表已经扣费，也不会自动完成。",
                        color = PdigV2Colors.TextMuted,
                        fontSize = 10.sp,
                    )
                    error?.let { Text(it, color = PdigV2Colors.Critical, fontSize = 11.sp) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val month = monthInput.toIntOrNull()
                        val day = dayInput.toIntOrNull()
                        error = when {
                            month == null || month !in 1..12 -> "月份必须是 1–12。"
                            day == null || day !in 1..31 -> "日期必须是 1–31。"
                            else -> null
                        }
                        if (error == null) {
                            val result = runCatching {
                                authority.confirmCardAnnualFeeCheckpoint(item.id, month!!, day!!)
                            }
                            if (result.isSuccess) {
                                feedback = "年费节点已记录到本机 Reality"
                                editAnnual = false
                                appState.requestRealityRefresh()
                            } else {
                                error = "保存失败，请检查日期是否符合受治理规则。"
                            }
                        }
                    },
                ) { Text("保存节点") }
            },
            dismissButton = {
                TextButton(onClick = { editAnnual = false }) { Text("取消") }
            },
        )
    }
}

@Composable
internal fun ProductionPhoneMaintenanceControls(
    session: ProductionVNextSession,
    item: VNextProductionObject,
) {
    val authority = session.authorities?.maintenance ?: return
    ProductionPhoneMaintenanceControls(
        appState = session.appState,
        authority = authority,
        item = item,
    )
}

@Composable
internal fun ProductionPhoneMaintenanceControls(
    appState: VAppState,
    authority: VNextMaintenanceActionGateway,
    item: VNextProductionObject,
) {
    if (item.surfaceKind != VNextProductionSurfaceKind.PHONE_IDENTITY) return

    val planCost = item.maintenanceFactValue("number_plan_cost")
    val planCurrency = item.maintenanceFactValue("number_plan_currency")
    val billingMode = item.maintenanceFactValue("number_billing_mode")
    val renewalMethod = item.maintenanceFactValue("number_renewal_method")
    val keepAlive = item.maintenanceSchedule("number_keep_alive")
    val renewal = item.maintenanceSchedule("number_plan_renewal")

    var editPlan by remember { mutableStateOf(false) }
    var editKeepAlive by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    MaintenanceControlPanel(
        title = "管理号码生命周期",
        description = "套餐、续费与保号只记录你确认的 Reality；不会根据运营商或号码前缀推断。",
        primaryLabel = "编辑套餐资料",
        secondaryLabel = "设置保号 / 续费",
        onPrimary = { editPlan = true },
        onSecondary = { editKeepAlive = true },
        feedback = feedback,
        testTag = "pdig.production-vnext.phone.maintenance.controls",
    )

    if (editPlan) {
        var costInput by remember(item.id, planCost) { mutableStateOf(planCost.orEmpty()) }
        var currencyInput by remember(item.id, planCurrency) { mutableStateOf(planCurrency.orEmpty()) }
        var billingInput by remember(item.id, billingMode) { mutableStateOf(billingMode.orEmpty()) }
        var renewalInput by remember(item.id, renewalMethod) { mutableStateOf(renewalMethod.orEmpty()) }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { editPlan = false },
            title = { Text("编辑套餐资料") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MaintenanceTruthNote()
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = costInput,
                            onValueChange = { costInput = it.take(32) },
                            label = { Text("套餐费用") },
                            modifier = Modifier.weight(1f)
                                .testTag("pdig.production-vnext.phone.maintenance.cost"),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = currencyInput,
                            onValueChange = { currencyInput = it.take(3).uppercase() },
                            label = { Text("币种") },
                            modifier = Modifier.weight(1f)
                                .testTag("pdig.production-vnext.phone.maintenance.currency"),
                            singleLine = true,
                        )
                    }
                    OutlinedTextField(
                        value = billingInput,
                        onValueChange = { billingInput = it.take(120) },
                        label = { Text("计费方式") },
                        modifier = Modifier.fillMaxWidth()
                            .testTag("pdig.production-vnext.phone.maintenance.billing-mode"),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = renewalInput,
                        onValueChange = { renewalInput = it.take(120) },
                        label = { Text("续费 / 保号方式") },
                        modifier = Modifier.fillMaxWidth()
                            .testTag("pdig.production-vnext.phone.maintenance.renewal-method"),
                        singleLine = true,
                    )
                    error?.let { Text(it, color = PdigV2Colors.Critical, fontSize = 11.sp) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cost = costInput.trim().takeIf { it.isNotEmpty() }
                        val cur = currencyInput.trim().takeIf { it.isNotEmpty() }
                        val billing = billingInput.trim().takeIf { it.isNotEmpty() }
                        val method = renewalInput.trim().takeIf { it.isNotEmpty() }
                        error = when {
                            (cost == null) != (cur == null) -> "套餐费用与币种必须一起记录。"
                            cost == null && billing == null && method == null ->
                                "请至少填写一项已确认资料。"
                            else -> null
                        }
                        if (error == null) {
                            val result = runCatching {
                                authority.confirmPhonePlan(
                                    nodeId = item.id,
                                    planCost = cost,
                                    planCurrency = cur,
                                    billingMode = billing,
                                    renewalMethod = method,
                                )
                            }
                            if (result.isSuccess) {
                                feedback = "套餐资料已记录到本机 Reality"
                                editPlan = false
                                appState.requestRealityRefresh()
                            } else {
                                error = "保存失败，请检查输入是否符合受治理规则。"
                            }
                        }
                    },
                    modifier = Modifier.testTag("pdig.production-vnext.phone.plan.save"),
                ) { Text("保存已确认资料") }
            },
            dismissButton = {
                TextButton(onClick = { editPlan = false }) { Text("取消") }
            },
        )
    }

    if (editKeepAlive) {
        var intervalInput by remember(item.id, keepAlive?.intervalDays) {
            mutableStateOf(keepAlive?.intervalDays?.toString().orEmpty())
        }
        var anchorInput by remember(item.id, keepAlive?.anchorDate) {
            mutableStateOf(keepAlive?.anchorDate.orEmpty())
        }
        var renewalDayInput by remember(item.id, renewal?.dayOfMonth) {
            mutableStateOf(renewal?.dayOfMonth?.toString().orEmpty())
        }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { editKeepAlive = false },
            title = { Text("保号与续费计划") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MaintenanceTruthNote()
                    OutlinedTextField(
                        value = intervalInput,
                        onValueChange = { intervalInput = it.take(4) },
                        label = { Text("保号周期（天）") },
                        modifier = Modifier.fillMaxWidth()
                            .testTag("pdig.production-vnext.phone.maintenance.keepalive-interval"),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = anchorInput,
                        onValueChange = { anchorInput = it.take(10) },
                        label = { Text("保号锚点 YYYY-MM-DD") },
                        modifier = Modifier.fillMaxWidth()
                            .testTag("pdig.production-vnext.phone.maintenance.keepalive-anchor"),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = renewalDayInput,
                        onValueChange = { renewalDayInput = it.take(2) },
                        label = { Text("每月续费日（可选，1–31）") },
                        modifier = Modifier.fillMaxWidth()
                            .testTag("pdig.production-vnext.phone.maintenance.renewal-day"),
                        singleLine = true,
                    )
                    Text(
                        "保存计划不会标记已完成。只有你明确点击“记录本次已完成”才写入完成时间。",
                        color = PdigV2Colors.TextMuted,
                        fontSize = 10.sp,
                    )
                    error?.let { Text(it, color = PdigV2Colors.Critical, fontSize = 11.sp) }
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            val interval = intervalInput.toIntOrNull()
                            val anchor = anchorInput.trim()
                            error = when {
                                interval == null || interval !in 1..3660 -> "保号周期必须是 1–3660 天。"
                                !DATE_ONLY.matches(anchor) -> "锚点日期格式必须是 YYYY-MM-DD。"
                                else -> null
                            }
                            if (error == null) {
                                val result = runCatching {
                                    authority.confirmPhoneKeepAliveInterval(
                                        nodeId = item.id,
                                        intervalDays = interval!!,
                                        anchorDate = anchor,
                                    )
                                }
                                if (result.isSuccess) {
                                    feedback = "保号计划已记录到本机 Reality"
                                    appState.requestRealityRefresh()
                                } else {
                                    error = "保存失败，请检查输入是否符合受治理规则。"
                                }
                            }
                        },
                    ) { Text("保存保号计划") }

                    if (renewalDayInput.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                val day = renewalDayInput.toIntOrNull()
                                error = if (day == null || day !in 1..31) "续费日必须是 1–31。" else null
                                if (error == null) {
                                    val result = runCatching {
                                        authority.confirmPhonePlanRenewalMonthly(item.id, day!!)
                                    }
                                    if (result.isSuccess) {
                                        feedback = "套餐续费节点已记录到本机 Reality"
                                        appState.requestRealityRefresh()
                                    } else {
                                        error = "续费节点保存失败。"
                                    }
                                }
                            },
                        ) { Text("保存续费节点") }
                    }

                    if (keepAlive?.intervalDays != null && keepAlive.anchorDate != null) {
                        OutlinedButton(
                            onClick = {
                                val result = runCatching {
                                    authority.markPhoneKeepAliveCompleted(
                                        nodeId = item.id,
                                        intervalDays = keepAlive.intervalDays,
                                        anchorDate = keepAlive.anchorDate,
                                    )
                                }
                                if (result.isSuccess) {
                                    feedback = "本次保号已明确记录完成"
                                    editKeepAlive = false
                                    appState.requestRealityRefresh()
                                } else {
                                    error = "完成记录失败；不会自动标记完成。"
                                }
                            },
                            modifier = Modifier.testTag(
                                "pdig.production-vnext.phone.keepalive.complete",
                            ),
                        ) { Text("记录本次已完成") }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { editKeepAlive = false }) { Text("关闭") }
            },
        )
    }
}

@Composable
private fun MaintenanceControlPanel(
    title: String,
    description: String,
    primaryLabel: String,
    secondaryLabel: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    feedback: String?,
    testTag: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag(testTag),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(
            Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, color = PdigV2Colors.TextPrimary, fontSize = 13.sp,
                fontWeight = FontWeight.Bold)
            Text(description, color = PdigV2Colors.TextMuted, fontSize = 10.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPrimary, modifier = Modifier.weight(1f)) {
                    Text(primaryLabel)
                }
                OutlinedButton(onClick = onSecondary, modifier = Modifier.weight(1f)) {
                    Text(secondaryLabel)
                }
            }
            feedback?.let {
                Text(it, color = PdigV2Colors.PrimaryText, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun MaintenanceTruthNote() {
    Text(
        "你正在记录自己的已确认资料。Provider 常见规则、参考值和时间经过都不会自动成为 Reality。",
        color = PdigV2Colors.TextMuted,
        fontSize = 10.sp,
    )
}

private fun VNextProductionObject.maintenanceFactValue(kind: String): String? =
    maintenanceFacts.firstOrNull { it.kind == kind && it.state == "confirmed" }?.value

private fun VNextProductionObject.maintenanceSchedule(kind: String) =
    maintenanceSchedules.firstOrNull {
        it.kind == kind && (it.state == "active" || it.state == "needs_review")
    }

private fun parseOptionalDay(value: String): Int? {
    if (value.isBlank()) return null
    return value.toIntOrNull()?.takeIf { it in 1..31 }
}

private val DATE_ONLY = Regex("""^[0-9]{4}-[0-9]{2}-[0-9]{2}$""")
