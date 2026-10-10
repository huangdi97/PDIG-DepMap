package com.pdig.uivnext.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.production.ProductionVNextSession
import com.pdig.uivnext.production.VNextProductionObject
import com.pdig.uivnext.production.VNextProductionSnapshot
import com.pdig.uivnext.production.VNextProductionSurfaceKind
import com.pdig.uivnext.ui.components.ProductionPhoneIdentityFace
import com.pdig.uivnext.ui.components.ProductionPhonePresentationEditor
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius

@Composable
internal fun ProductionInventoryCategoryScreen(
    session: ProductionVNextSession,
    screen: VScreen,
    modifier: Modifier = Modifier,
) {
    val snapshot = session.dataSource.productionSnapshot()
    if (snapshot == null) {
        ProductionObjectUnavailable("生产基础设施快照不可读取", modifier)
        return
    }

    if (screen == VScreen.WEAKNESSES) {
        ProductionWeaknessesScreen(session, modifier)
        return
    }

    val surfaceKind = productionSurfaceForScreen(screen)
    if (surfaceKind == null) {
        ProductionObjectUnavailable(
            "该分类尚未完成正式数据映射；不会强制转换对象类型。",
            modifier,
        )
        return
    }

    val selectedRegion = session.appState.regionFilter
    val regionMemberIds = selectedRegion?.let { code ->
        session.dataSource.productionInventory()
            ?.regions
            ?.firstOrNull { it.territoryCode == code }
            ?.memberObjectIds
            ?.toSet()
            ?: emptySet()
    }
    val objects = snapshot.objects.filter {
        it.surfaceKind == surfaceKind &&
            (regionMemberIds == null || it.id in regionMemberIds)
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("pdig.production-vnext.inventory-category"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(screen.titleZh, color = PdigV2Colors.TextPrimary, fontSize = 23.sp,
                    fontWeight = FontWeight.Bold)
                Text(
                    if (selectedRegion == null)
                        "只显示当前已确认数据中能够明确分类的对象。"
                    else
                        "地区筛选：${TerritoryPresentationCatalog.displayName(selectedRegion)} · 只使用已确认 RegionFact 成员关系。",
                    color = PdigV2Colors.TextMuted,
                    fontSize = 12.sp,
                )
                if (selectedRegion != null) {
                    Surface(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable { session.appState.clearRegion() }
                            .testTag("pdig.production-vnext.region.clear"),
                        color = PdigV2Colors.PrimarySoft,
                        shape = RoundedCornerShape(VRadius.Md),
                    ) {
                        Text(
                            "查看全球",
                            Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                            color = PdigV2Colors.PrimaryText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        if (objects.isEmpty()) {
            item {
                ProductionObjectBoundary(
                    "当前没有该类型的已确认对象。空列表不代表外部不存在，只表示这里尚未记录。"
                )
            }
        } else {
            items(objects, key = { it.id }) { item ->
                ProductionObjectRow(
                    item = item,
                    dependencyCount = dependencyCount(snapshot, item.id),
                    app = session.appState,
                ) {
                    when (screen) {
                        VScreen.CARDS -> session.appState.openCard(item.id)
                        VScreen.NUMBERS -> session.appState.openNumber(item.id)
                        VScreen.EMAILS ->
                            session.appState.openSecondaryObject(VScreen.EMAIL_DETAIL, item.id)
                        VScreen.ACCOUNTS ->
                            session.appState.openSecondaryObject(VScreen.ACCOUNT_DETAIL, item.id)
                        VScreen.DEVICES ->
                            session.appState.openSecondaryObject(VScreen.DEVICE_DETAIL, item.id)
                        VScreen.SERVICES ->
                            session.appState.openSecondaryObject(VScreen.SERVICE_DETAIL, item.id)
                        else -> Unit
                    }
                }
            }
        }

        item {
            when (screen) {
                VScreen.CARDS -> ProductionObjectBoundary(
                    "年费、账单日、还款日、自动还款与维护节点只展示受治理 maintenance_profile 的已确认 Reality；分期摘要仍不属于 Canonical v1。"
                )
                VScreen.NUMBERS -> ProductionObjectBoundary(
                    "这里只有受治理 identity_anchor_profile 已确认 PHONE_NUMBER 的对象。套餐、续费与保号节点只来自该号码的 maintenance_profile；运营商、SIM/eSIM 与恢复语义仍不会由号码、名称或维护资料推断。"
                )
                VScreen.EMAILS -> ProductionObjectBoundary(
                    "这里只有受治理 identity_anchor_profile 已确认 EMAIL_ADDRESS 的对象。若 nested identifier 也已确认，可显示真实邮箱值；地区只来自已确认 RegionFact，Provider 与恢复语义仍不会由邮箱或名称推断。"
                )
                VScreen.DEVICES -> ProductionObjectBoundary(
                    "设备的认证/恢复能力尚未进入正式数据模型；不会根据设备名称猜测它具备哪些能力。"
                )
                else -> ProductionObjectBoundary(
                    "关系数只是已确认关系数量，不代表独立恢复路径数量，也不产生安全分数。"
                )
            }
        }
    }
}

@Composable
internal fun ProductionGenericObjectDetailScreen(
    session: ProductionVNextSession,
    detailScreen: VScreen,
    objectId: String?,
    modifier: Modifier = Modifier,
) {
    if (objectId.isNullOrBlank()) {
        ProductionObjectUnavailable("没有选择生产对象", modifier)
        return
    }
    val snapshot = session.dataSource.productionSnapshot()
    val item = snapshot?.objects?.firstOrNull { it.id == objectId }
    if (snapshot == null || item == null) {
        ProductionObjectUnavailable("对象不存在、已归档或当前无法读取", modifier)
        return
    }

    val expected = when (detailScreen) {
        VScreen.NUMBER_DETAIL -> VNextProductionSurfaceKind.PHONE_IDENTITY
        VScreen.EMAIL_DETAIL -> VNextProductionSurfaceKind.EMAIL_IDENTITY
        VScreen.ACCOUNT_DETAIL -> VNextProductionSurfaceKind.ACCOUNT
        VScreen.DEVICE_DETAIL -> VNextProductionSurfaceKind.DEVICE
        VScreen.SERVICE_DETAIL -> VNextProductionSurfaceKind.SERVICE
        else -> null
    }
    if (expected == null || item.surfaceKind != expected) {
        ProductionObjectUnavailable("对象类型与详情页不匹配；不会强制转换类型", modifier)
        return
    }

    val app = session.appState
    val phoneProfile = if (detailScreen == VScreen.NUMBER_DETAIL)
        app.savedPresentationProfile("phoneNumber", item.id) else null
    val effectivePrivacyMask = app.privacyMask || (phoneProfile?.maskSensitive == true)
    val impact = session.dataSource.productionImpact(item.id)
    val related = snapshot.confirmedDependencies.filter {
        it.fromId == item.id || it.toId == item.id
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("pdig.production-vnext.object-detail"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (detailScreen == VScreen.NUMBER_DETAIL)
                        app.numberDisplayNameForScreen(item.id, item.name)
                    else
                        productionVisibleObjectName(item, effectivePrivacyMask),
                    color = PdigV2Colors.TextPrimary,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    productionObjectSurfaceLabel(item),
                    color = PdigV2Colors.TextMuted,
                    fontSize = 10.sp,
                )
                if (item.identitySubtype != null) {
                    Text(
                        listOfNotNull(
                            productionIdentityBasisLabel(item.identityVerificationBasisType),
                            item.identityConfirmedAt?.take(10),
                            item.identityEvidenceRefs.takeIf { it.isNotEmpty() }
                                ?.let { "证据引用 ${it.size} 项" },
                        ).joinToString(" · "),
                        color = PdigV2Colors.TextMuted,
                        fontSize = 9.sp,
                    )
                    productionIdentityIdentifierLabel(
                        item,
                        effectivePrivacyMask,
                    )?.let { identifier ->
                        Text(
                            identifier,
                            color = PdigV2Colors.TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            listOfNotNull(
                                item.identityIdentifierVerificationBasisType
                                    ?.let(::productionIdentityBasisLabel),
                                item.identityIdentifierConfirmedAt?.take(10),
                                item.identityIdentifierEvidenceRefs.takeIf { it.isNotEmpty() }
                                    ?.let { "标识证据 ${it.size} 项" },
                            ).joinToString(" · "),
                            color = PdigV2Colors.TextMuted,
                            fontSize = 9.sp,
                        )
                    }
                }
            }
        }

        if (detailScreen == VScreen.NUMBER_DETAIL) {
            item {
                val phonePresentation = app.savedPresentationProfile("phoneNumber", item.id)
                    ?: app.presentationProfile("phoneNumber", item.id, "minimal")
                ProductionPhoneIdentityFace(
                    item = item,
                    app = app,
                    profile = phonePresentation,
                )
            }
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { app.openNumberCustomization(item.id) }
                        .testTag("pdig.production-vnext.phone.presentation-entry"),
                    color = PdigV2Colors.PrimarySoft,
                    shape = RoundedCornerShape(VRadius.Md),
                    border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                ) {
                    Row(
                        Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "名称与号码外观",
                                color = PdigV2Colors.PrimaryText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "本机 Presentation · 不修改号码 Reality",
                                color = PdigV2Colors.TextMuted,
                                fontSize = 9.sp,
                            )
                        }
                        Text("→", color = PdigV2Colors.PrimaryText, fontSize = 14.sp)
                    }
                }
            }
            item {
                Text(
                    "号码生命周期",
                    color = PdigV2Colors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            val currentFacts = item.maintenanceFacts.filter { it.state == "confirmed" }
            val currentSchedules = item.maintenanceSchedules.filter {
                it.state == "active" || it.state == "needs_review"
            }
            if (currentFacts.isEmpty() && currentSchedules.isEmpty()) {
                item {
                    ProductionObjectBoundary(
                        "尚未记录受治理的号码生命周期资料；不会从运营商名称、号码前缀或参考数据推断资费/保号状态。"
                    )
                }
            } else {
                items(currentFacts, key = { "maintenance-fact:" + it.id }) { fact ->
                    ProductionFactCard(
                        title = productionMaintenanceFactLabel(fact.kind),
                        subtitle = productionMaintenanceFactValue(fact.kind, fact.value),
                        meta = "已确认 Maintenance Reality · " +
                            productionIdentityBasisLabel(fact.verificationBasisType),
                    )
                }
                items(currentSchedules, key = { "maintenance-schedule:" + it.id }) { schedule ->
                    ProductionFactCard(
                        title = productionMaintenanceScheduleTitle(schedule.kind),
                        subtitle = productionMaintenanceScheduleSummary(schedule),
                        meta = if (schedule.state == "needs_review")
                            "需要核对 · 时间经过不会自动完成"
                        else
                            "已记录计划 · 时间经过不会自动完成",
                    )
                }
            }
            item {
                ProductionPhoneMaintenanceControls(session, item)
            }
        }

        item {
            ProductionObjectBoundary(
                "${related.size} 条已确认关系 · 未确认关系仍可能存在"
            )
        }

        if (related.isNotEmpty()) {
            item {
                Text("已确认关系", color = PdigV2Colors.TextPrimary, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold)
            }
            items(related, key = { it.id }) { dep ->
                val outward = dep.fromId == item.id
                ProductionRelationCard(
                    title = if (outward) dep.toName else dep.fromName,
                    relation = dep.relation,
                    capability = dep.capability,
                    criticality = dep.criticality,
                    direction = if (outward) "从此对象指向" else "指向此对象",
                )
            }
        }

        item {
            Text("如果它发生变化？", color = PdigV2Colors.TextPrimary, fontSize = 14.sp,
                fontWeight = FontWeight.Bold)
        }

        if (impact == null) {
            item {
                ProductionObjectBoundary(
                    "当前无法读取权威影响分析；不会用关系数量代替连续性判断。"
                )
            }
        } else {
            val statusCounts = impact.targets.groupingBy { it.status }.eachCount()
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        (statusCounts["must_change"] ?: 0) to "必须处理",
                        (statusCounts["needs_review"] ?: 0) to "需要核对",
                        (statusCounts["unknown"] ?: 0) to "未知",
                    ).forEach { (count, label) ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = PdigV2Colors.Surface,
                            shape = RoundedCornerShape(VRadius.Md),
                            border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(count.toString(), color = PdigV2Colors.TextPrimary,
                                    fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Text(label, color = PdigV2Colors.TextMuted, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }

            if (impact.checklist.isNotEmpty()) {
                item {
                    Text("核对清单", color = PdigV2Colors.TextPrimary, fontSize = 14.sp,
                        fontWeight = FontWeight.Bold)
                }
                items(impact.checklist.take(10)) { check ->
                    ProductionObjectBoundary(
                        check.title + if (check.detail.isBlank()) "" else " · " + check.detail
                    )
                }
            }
        }

        if (detailScreen == VScreen.NUMBER_DETAIL) {
            item {
                Text(
                    "变更",
                    color = PdigV2Colors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            item {
                ProductionPhoneChangeEntry(
                    session = session,
                    phoneId = item.id,
                    plans = snapshot.plans,
                )
            }
        }

        item {
            when (detailScreen) {
                VScreen.NUMBER_DETAIL -> ProductionObjectBoundary(
                    if (item.identityIdentifierValue != null)
                        "号码值来自独立确认的 identifier Reality；资费/保号只读取 maintenance_profile；运营商、SIM/eSIM、恢复路径仍必须来自各自的受治理事实。"
                    else
                        "当前只确认“这是手机号身份”，具体号码值仍未记录；即使存在维护计划，也不会从名称、地区或 Provider 猜测号码值。"
                )
                VScreen.EMAIL_DETAIL -> ProductionObjectBoundary(
                    if (item.identityIdentifierValue != null)
                        "邮箱值来自独立确认的 identifier Reality；Provider 与恢复路径仍必须来自各自的受治理事实。"
                    else
                        "当前只确认“这是邮箱身份”，具体邮箱值仍未记录；不会从名称或关系猜测。"
                )
                VScreen.DEVICE_DETAIL -> ProductionObjectBoundary(
                    "当前设备详情不会猜测通行密钥、动态验证码、短信验证、恢复因子或秘密位置；这些能力必须来自未来正式的数据语义。"
                )
                else -> ProductionObjectBoundary(
                    "详情只使用正式数据中的对象、已确认关系和权威影响分析；不会混入演示数据。"
                )
            }
        }
    }
}

@Composable
internal fun ProductionNumberCustomizationScreen(
    session: ProductionVNextSession,
    modifier: Modifier = Modifier,
) {
    val app = session.appState
    val objectId = app.selectedNumberId
    val item = session.dataSource.productionSnapshot()
        ?.objects
        ?.firstOrNull {
            it.id == objectId &&
                it.surfaceKind == VNextProductionSurfaceKind.PHONE_IDENTITY
        }
    if (item == null) {
        ProductionObjectUnavailable(
            "没有可定制的已确认手机号身份；不会用 Preview 号码替代。",
            modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("pdig.production-vnext.phone.customization"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "号码名称与外观",
                    color = PdigV2Colors.TextPrimary,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "只编辑本机显示名称、主题、材质、布局和遮蔽；不改变手机号身份或关系。",
                    color = PdigV2Colors.TextMuted,
                    fontSize = 11.sp,
                )
            }
        }
        item {
            ProductionPhonePresentationEditor(
                item = item,
                app = app,
            )
        }
    }
}

@Composable
private fun ProductionWeaknessesScreen(
    session: ProductionVNextSession,
    modifier: Modifier = Modifier,
) {
    val report = session.dataSource.productionFindings()
    if (report == null) {
        ProductionObjectUnavailable(
            "当前无法读取权威连续性发现；不会回退到 Preview 演示结果。",
            modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("pdig.production-vnext.weaknesses"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "薄弱点",
                    color = PdigV2Colors.TextPrimary,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "只展示当前 Android 正式数据链能够证明的连续性发现；未覆盖类型保持明确未知。",
                    color = PdigV2Colors.TextMuted,
                    fontSize = 12.sp,
                )
            }
        }

        if (report.findings.isEmpty()) {
            item {
                ProductionObjectBoundary(
                    "当前受支持的发现类型里没有产生结果。这里不能解释成“基础设施安全”，因为仍有未覆盖类型和未记录关系。"
                )
            }
        } else {
            items(report.findings, key = { it.id }) { finding ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = PdigV2Colors.Surface,
                    shape = RoundedCornerShape(VRadius.Lg),
                    border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                ) {
                    Column(
                        Modifier.padding(13.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            finding.title,
                            color = PdigV2Colors.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            productionFindingTypeLabel(finding.type.name) +
                                " · " + productionFindingTruthLabel(finding.truth.name),
                            color = PdigV2Colors.TextMuted,
                            fontSize = 9.sp,
                        )
                        Text(
                            finding.why,
                            color = PdigV2Colors.TextSecondary,
                            fontSize = 10.sp,
                        )
                        Text(
                            "依据：${finding.confirmedBasis}",
                            color = PdigV2Colors.TextSecondary,
                            fontSize = 10.sp,
                        )
                        Text(
                            "未知：${finding.unknowns}",
                            color = PdigV2Colors.TextMuted,
                            fontSize = 10.sp,
                        )
                        Text(
                            "下一步：${finding.recommendedNextAction}",
                            color = PdigV2Colors.PrimaryText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (finding.evidenceRefs.isNotEmpty()) {
                            Text(
                                "证据引用 ${finding.evidenceRefs.size} 项",
                                color = PdigV2Colors.TextMuted,
                                fontSize = 9.sp,
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                "当前权威覆盖",
                color = PdigV2Colors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            ProductionObjectBoundary(
                report.supportedTypes.joinToString(" · ") { productionFindingTypeLabel(it) }
            )
        }

        if (report.unsupportedTypes.isNotEmpty()) {
            item {
                Text(
                    "尚未接入权威输入",
                    color = PdigV2Colors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            item {
                ProductionObjectBoundary(
                    report.unsupportedTypes.joinToString(" · ") { productionFindingTypeLabel(it) } +
                        "。这些类型不会由关系数量、名称或 UI 侧启发式补出来。"
                )
            }
        }

        item {
            ProductionObjectBoundary(
                "薄弱点不是健康分。单一路径、恢复循环、待复核候选和待验证变更都保留各自证据与未知边界。"
            )
        }
    }
}

private fun productionFindingTypeLabel(type: String): String = when (type) {
    "SINGLE_POINT_OF_FAILURE" -> "单一路径"
    "SHARED_FAILURE_DOMAIN" -> "共享故障域"
    "RECOVERY_CYCLE" -> "恢复循环"
    "UNCONFIRMED_FALLBACK" -> "未确认备用"
    "STALE_RECOVERY_INFORMATION" -> "恢复信息过期"
    "UNKNOWN_CRITICAL_PATH" -> "关键路径影响未知"
    "PENDING_VERIFICATION" -> "待验证变更"
    else -> "连续性发现"
}

private fun productionFindingTruthLabel(truth: String): String = when (truth) {
    "CONFIRMED" -> "已确认事实"
    "PENDING_REVIEW" -> "待复核"
    "DERIVED" -> "基于已确认数据的派生分析"
    else -> "未知"
}

private fun productionMaintenanceFactLabel(kind: String): String = when (kind) {
    "number_billing_mode" -> "计费方式"
    "number_plan_cost" -> "套餐费用"
    "number_plan_currency" -> "费用币种"
    "number_renewal_method" -> "续费 / 保号方式"
    "card_annual_fee_amount" -> "年费"
    "card_annual_fee_currency" -> "年费币种"
    "card_billing_day" -> "账单日"
    "card_payment_due_day" -> "还款日"
    "card_autopay_mode" -> "自动还款"
    else -> "维护事实"
}

private fun productionMaintenanceFactValue(kind: String, value: String): String = when (kind) {
    "card_billing_day", "card_payment_due_day" -> "每月 $value 日"
    else -> value
}

private fun productionMaintenanceScheduleTitle(kind: String): String = when (kind) {
    "number_keep_alive" -> "保号节点"
    "number_plan_renewal" -> "套餐续费"
    "card_annual_fee_checkpoint" -> "年费检查节点"
    "card_billing_checkpoint" -> "账单节点"
    "card_payment_due_checkpoint" -> "还款节点"
    "fact_freshness_review" -> "资料新鲜度复核"
    "custom_maintenance" -> "自定义维护"
    else -> "维护计划"
}

private fun productionMaintenanceScheduleSummary(
    schedule: com.pdig.uivnext.production.VNextProductionMaintenanceSchedule,
): String = when (schedule.cadenceKind) {
    "one_time" -> schedule.dueAt ?: "日期未记录"
    "monthly_day" -> schedule.dayOfMonth?.let { "每月 $it 日" } ?: "每月节点"
    "yearly_month_day" -> if (schedule.month != null && schedule.day != null)
        "每年 ${schedule.month} 月 ${schedule.day} 日"
    else "年度节点"
    "interval_days" -> if (schedule.intervalDays != null)
        "每 ${schedule.intervalDays} 天" +
            (schedule.anchorDate?.let { " · 锚点 $it" } ?: "")
    else "周期节点"
    "manual_only" -> "手动维护"
    else -> "已记录维护计划"
}

@Composable
private fun ProductionObjectRow(
    item: VNextProductionObject,
    dependencyCount: Int,
    app: VAppState,
    onClick: () -> Unit,
) {
    val presentationMask = if (item.surfaceKind == VNextProductionSurfaceKind.PHONE_IDENTITY) {
        app.savedPresentationProfile("phoneNumber", item.id)?.maskSensitive == true
    } else {
        false
    }
    val privacyMask = app.privacyMask || presentationMask
    val visibleName = if (item.surfaceKind == VNextProductionSurfaceKind.PHONE_IDENTITY) {
        app.numberDisplayNameForScreen(item.id, item.name)
    } else {
        productionVisibleObjectName(item, privacyMask)
    }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Row(
            Modifier.padding(13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    visibleName,
                    color = PdigV2Colors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                productionIdentityIdentifierLabel(item, privacyMask)?.let { identifier ->
                    Text(identifier, color = PdigV2Colors.TextSecondary, fontSize = 10.sp)
                }
                Text(productionObjectSurfaceLabel(item), color = PdigV2Colors.TextMuted, fontSize = 9.sp)
            }
            Text(
                "$dependencyCount 关系",
                color = PdigV2Colors.TextSecondary,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun ProductionRelationCard(
    title: String,
    relation: String,
    capability: String,
    criticality: String,
    direction: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = PdigV2Colors.TextPrimary, fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold)
            Text(
                "$direction · ${productionRelationLabel(relation)} · ${productionCapabilityLabel(capability)}",
                color = PdigV2Colors.TextSecondary,
                fontSize = 10.sp,
            )
            Text(
                productionCriticalityLabel(criticality),
                color = PdigV2Colors.TextMuted,
                fontSize = 9.sp,
            )
        }
    }
}

@Composable
private fun ProductionObjectBoundary(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.PrimarySoft.copy(alpha = 0.58f),
        shape = RoundedCornerShape(VRadius.Lg),
    ) {
        Text(text, Modifier.padding(12.dp), color = PdigV2Colors.TextSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun ProductionObjectUnavailable(text: String, modifier: Modifier) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("当前生产绑定不可用", color = PdigV2Colors.TextPrimary,
            fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.padding(4.dp))
        Text(text, color = PdigV2Colors.TextSecondary, fontSize = 12.sp)
    }
}

private fun productionSurfaceForScreen(screen: VScreen): VNextProductionSurfaceKind? = when (screen) {
    VScreen.CARDS -> VNextProductionSurfaceKind.PAYMENT_ASSET
    VScreen.NUMBERS -> VNextProductionSurfaceKind.PHONE_IDENTITY
    VScreen.EMAILS -> VNextProductionSurfaceKind.EMAIL_IDENTITY
    VScreen.ACCOUNTS -> VNextProductionSurfaceKind.ACCOUNT
    VScreen.DEVICES -> VNextProductionSurfaceKind.DEVICE
    VScreen.SERVICES -> VNextProductionSurfaceKind.SERVICE
    else -> null
}

private fun dependencyCount(snapshot: VNextProductionSnapshot, objectId: String): Int =
    snapshot.confirmedDependencies.count { it.fromId == objectId || it.toId == objectId }
