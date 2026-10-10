package com.pdig.uivnext.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.weight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.production.ProductionConsumerInventory
import com.pdig.uivnext.production.ProductionVNextSession
import com.pdig.uivnext.production.VNextProductionRecordState
import com.pdig.uivnext.production.VNextProductionSnapshot
import com.pdig.uivnext.ui.components.ProductionCardAppearanceStrip
import com.pdig.uivnext.ui.components.ProductionPaymentAssetFace
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius

/**
 * Read-only production-bound UI vNext shell.
 *
 * IMPORTANT:
 * - this shell is NOT wired into MainActivity yet;
 * - it never imports UiVNextDemoFixture;
 * - unsupported production routes render an explicit capability hold instead of
 *   silently falling back to Preview/reference data.
 */
@Composable
internal fun ProductionVNextShell(
    session: ProductionVNextSession,
    forcedViewportWidthDp: Int? = null,
) {
    val app = session.appState
    BackHandler(enabled = app.canGoBack()) { app.back() }

    BoxWithConstraints(Modifier.fillMaxSize().testTag("pdig.production-vnext.shell")) {
        val viewportWidth: Dp = forcedViewportWidthDp?.let { Dp(it.toFloat()) } ?: maxWidth
        val breakpoint = resolveMediaBreakpoint(viewportWidth)
        val wide = breakpoint != MediaBreakpoint.COMPACT

        if (wide) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail(app, breakpoint)
                Column(Modifier.weight(1f)) {
                    ProductionTopBar(app)
                    if (isInfraRootScreen(app.screen)) {
                        InfraChipRow(app)
                    }
                    ProductionContent(session, Modifier.weight(1f))
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                ProductionTopBar(app)
                ProductionContent(session, Modifier.weight(1f))
                if (isProductionRoot(app.screen)) BottomNav(app)
            }
        }
    }
}

@Composable
private fun ProductionTopBar(app: VAppState) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(48.dp)
            .testTag("pdig.production-vnext.topbar"),
        color = PdigV2Colors.Surface,
        border = BorderStroke(0.5.dp, PdigV2Colors.BorderSubtle),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (app.canNavigateUp()) {
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { app.navigateUp() }
                        .testTag("pdig.production-vnext.up"),
                    color = androidx.compose.ui.graphics.Color.Transparent,
                    shape = RoundedCornerShape(VRadius.Sm),
                ) {
                    androidx.compose.foundation.layout.Box(
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowLeft,
                            contentDescription = "返回上一级",
                            tint = PdigV2Colors.TextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            Text(
                app.screen.titleZh,
                color = PdigV2Colors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clickable { app.navigate(VScreen.SEARCH) }
                    .testTag("pdig.production-vnext.search.open"),
                color = androidx.compose.ui.graphics.Color.Transparent,
                shape = RoundedCornerShape(VRadius.Sm),
            ) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "搜索",
                        tint = PdigV2Colors.TextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Text(
                productionBoundaryBadge(app.screen),
                color = PdigV2Colors.PrimaryText,
                fontSize = 10.sp,
                modifier = Modifier.testTag("pdig.production-vnext.truth-badge"),
            )
        }
    }
}

@Composable
private fun ProductionContent(
    session: ProductionVNextSession,
    modifier: Modifier = Modifier,
) {
    val app = session.appState
    val snapshot = session.dataSource.productionSnapshot()
    val inventory = session.dataSource.productionInventory()

    if (snapshot == null || inventory == null) {
        ProductionUnavailable(
            "当前已确认数据暂不可读取",
            "没有权威数据快照时不会用演示数据替代。",
            modifier,
        )
        return
    }

    when (app.screen) {
        VScreen.NOW -> ProductionNow(app, snapshot, inventory, modifier)
        VScreen.INFRASTRUCTURE, VScreen.OVERVIEW ->
            ProductionInfrastructure(app, inventory, modifier)
        VScreen.CARDS, VScreen.NUMBERS, VScreen.ACCOUNTS, VScreen.EMAILS,
        VScreen.DEVICES, VScreen.SERVICES, VScreen.WEAKNESSES ->
            ProductionInventoryCategoryScreen(session, app.screen, modifier)
        VScreen.CARD_DETAIL ->
            ProductionCardDetail(session, inventory, app.selectedCardId, modifier)
        VScreen.ACCOUNT_DETAIL, VScreen.DEVICE_DETAIL, VScreen.SERVICE_DETAIL ->
            ProductionGenericObjectDetailScreen(
                session = session,
                detailScreen = app.screen,
                objectId = app.selectedSecondaryObjectId,
                modifier = modifier,
            )
        VScreen.CHANGE -> ProductionChange(app, snapshot, modifier)
        VScreen.CHANGE_PHONE, VScreen.CHANGE_CARD ->
            ProductionChangePlanScreen(session, app.selectedProductionPlanId, modifier)
        VScreen.RECORDS -> ProductionRecords(session, modifier)
        VScreen.ME -> ProductionMe(app, snapshot, inventory, modifier)
        VScreen.SETTINGS, VScreen.PERSONALIZATION ->
            ProductionPreferencesScreen(app, modifier)
        VScreen.SOURCES -> ProductionSources(app, inventory, modifier)
        VScreen.REVIEW -> ProductionReviewScreen(session, modifier)
        VScreen.IMPORT -> ProductionEstablishScreen(session, modifier)
        VScreen.MANUAL_ADD -> ProductionManualEstablishScreen(session, modifier)
        VScreen.MANUAL_RELATION -> ProductionManualRelationshipHoldScreen(modifier)
        VScreen.SEARCH -> ProductionSearchScreen(session, modifier)
        else -> ProductionUnavailable(
            title = "该页面尚未完成生产数据绑定",
            body = "当前页面不会用演示数据替代正式记录。返回五个一级入口继续查看已接入内容。",
            modifier = modifier,
        )
    }
}

@Composable
private fun ProductionNow(
    app: VAppState,
    snapshot: VNextProductionSnapshot,
    inventory: ProductionConsumerInventory,
    modifier: Modifier,
) {
    ProductionPage(modifier, "现在", "基于当前加密数据的概览") {
        ProductionMetricRow(
            listOf(
                inventory.pendingReviewCount to "待复核",
                snapshot.plans.count { it.workflowState != "closed" } to "进行中变更",
                inventory.activeSourceCount to "活跃数据源",
            ),
        )

        if (inventory.pendingReviewCount > 0) {
            ProductionFactCard(
                title = "待复核",
                subtitle = "${inventory.pendingReviewCount} 项建议 / 候选 / 可能变化等待人工决定",
                meta = "确认前不会进入已确认数据",
                onClick = { app.navigate(VScreen.REVIEW) },
            )
        }

        ProductionSection("近期记录 / 计划")
        if (snapshot.timeline.isEmpty()) {
            ProductionEmpty("当前没有可投影的 Timeline 项；这不表示没有风险或依赖。")
        } else {
            snapshot.timeline.take(8).forEach { item ->
                ProductionFactCard(
                    title = item.title,
                    subtitle = item.subtitle.ifBlank { productionTimelineStatusLabel(item.status) },
                    meta = listOfNotNull(
                        item.scheduledAt?.take(10),
                        productionTimelineBucketLabel(item.bucket),
                        productionTimelineStatusLabel(item.status),
                    ).joinToString(" · "),
                )
            }
        }

        ProductionSection("基础设施")
        ProductionInventorySummary(inventory)
    }
}

@Composable
private fun ProductionInfrastructure(
    app: VAppState,
    inventory: ProductionConsumerInventory,
    modifier: Modifier,
) {
    ProductionPage(modifier, "基础设施", "只展示当前正式数据模型能够明确识别的对象类型") {
        ProductionInventorySummary(inventory)

        ProductionSection("管理分类")
        listOf(
            Triple(VScreen.CARDS, inventory.counts.paymentAssets, "已绑定"),
            Triple(VScreen.NUMBERS, inventory.counts.genericIdentityAnchors, "等待 phone subtype"),
            Triple(VScreen.ACCOUNTS, inventory.counts.accounts, "已绑定"),
            Triple(VScreen.EMAILS, inventory.counts.genericIdentityAnchors, "等待 email subtype"),
            Triple(VScreen.DEVICES, inventory.counts.devices, "已绑定"),
            Triple(VScreen.SERVICES, inventory.counts.services, "已绑定"),
            Triple(VScreen.WEAKNESSES, 0, "等待 Finding projection"),
        ).chunked(2).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { (screen, count, state) ->
                    val bound = screen in setOf(
                        VScreen.CARDS,
                        VScreen.ACCOUNTS,
                        VScreen.DEVICES,
                        VScreen.SERVICES,
                    )
                    ProductionCategoryEntry(
                        title = screen.titleZh,
                        count = if (bound) count else null,
                        state = state,
                        enabled = true,
                        onClick = { app.navigate(screen) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        ProductionSection("支付工具")
        if (inventory.paymentAssets.isEmpty()) {
            ProductionEmpty("没有已确认的 payment_instrument。")
        } else {
            inventory.paymentAssets.forEach { card ->
                ProductionFactCard(
                    title = card.name,
                    subtitle = listOfNotNull(
                        card.issuer,
                        card.last4?.let { "•••• $it" },
                    ).joinToString(" · ").ifBlank { "已确认支付工具" },
                    meta = "${card.confirmedDependencyCount} 条已确认关系",
                    onClick = { app.openCard(card.id) },
                )
            }
        }

        ProductionSection("身份对象")
        if (inventory.genericIdentityAnchors.isEmpty()) {
            ProductionEmpty("没有已确认的通用身份对象。")
        } else {
            inventory.genericIdentityAnchors.forEach { identity ->
                ProductionFactCard(
                    title = identity.name,
                    subtitle = "通用身份对象 · 手机/邮箱类型尚未完成底层确认",
                    meta = "${identity.confirmedDependencyCount} 条已确认关系",
                )
            }
        }
    }
}

@Composable
private fun ProductionCardDetail(
    session: ProductionVNextSession,
    inventory: ProductionConsumerInventory,
    cardId: String?,
    modifier: Modifier,
) {
    val card = inventory.paymentAssets.firstOrNull { it.id == cardId }
    if (card == null) {
        ProductionUnavailable(
            "未找到该生产支付工具",
            "对象不存在、已归档或当前正式数据不足；不会用演示卡片替代。",
            modifier,
        )
        return
    }
    val app = session.appState
    val snapshot = session.dataSource.productionSnapshot()
    val cardObject = snapshot?.objects?.firstOrNull { it.id == card.id }
    val presentation = app.savedPresentationProfile("card", card.id)
    val impact = session.dataSource.productionImpact(card.id)
    val related = snapshot?.confirmedDependencies.orEmpty().filter {
        it.fromId == card.id || it.toId == card.id
    }
    ProductionPage(modifier, card.name, "支付工具 · 已确认数据") {
        if (cardObject != null) {
            ProductionPaymentAssetFace(
                asset = cardObject,
                presentation = presentation,
                privacyMask = app.privacyMask,
            )
            ProductionCardAppearanceStrip(
                selectedTheme = presentation?.themeId ?: "minimal",
                onSelect = { theme ->
                    val next = app.presentationProfile("card", card.id, theme).copy(
                        themeId = theme,
                        backgroundKind = "preset",
                        backgroundValue = theme,
                    )
                    app.savePresentationProfile(next)
                },
            )
        }

        ProductionFactCard(
            title = card.issuer ?: "发行方未记录",
            subtitle = card.last4?.let { "尾号 $it" } ?: "尾号未记录",
            meta = "${card.confirmedDependencyCount} 条已确认关系",
        )

        ProductionSection("已确认关系")
        if (related.isEmpty()) {
            ProductionEmpty("当前没有已确认关系；这不表示外部没有关联，只表示这里尚未记录。")
        } else {
            related.forEach { dep ->
                val outward = dep.fromId == card.id
                ProductionFactCard(
                    title = if (outward) dep.toName else dep.fromName,
                    subtitle = listOf(
                        if (outward) "从此卡指向" else "指向此卡",
                        productionRelationLabel(dep.relation),
                        productionCapabilityLabel(dep.capability),
                    ).joinToString(" · "),
                    meta = productionCriticalityLabel(dep.criticality),
                )
            }
        }

        ProductionSection("如果它发生变化？")
        if (impact == null) {
            ProductionEmpty("当前无法获得权威影响分析；不会以关系数量代替连续性判断。")
        } else {
            val grouped = impact.targets.groupingBy { it.status }.eachCount()
            val ordered = listOf(
                "must_change" to productionImpactStatusLabel("must_change"),
                "needs_review" to productionImpactStatusLabel("needs_review"),
                "degraded" to productionImpactStatusLabel("degraded"),
                "backup_path" to productionImpactStatusLabel("backup_path"),
                "unaffected" to productionImpactStatusLabel("unaffected"),
                "unknown" to productionImpactStatusLabel("unknown"),
            )
            val facts = ordered.mapNotNull { (status, label) ->
                grouped[status]?.let { it to label }
            }
            if (facts.isEmpty()) {
                ProductionEmpty("当前影响分析没有可展示的受影响对象；这不等于安全。")
            } else {
                facts.chunked(3).forEach { ProductionMetricRow(it) }
            }

            if (impact.checklist.isNotEmpty()) {
                ProductionSection("核对清单")
                impact.checklist.take(8).forEach { item ->
                    ProductionFactCard(
                        title = item.title,
                        subtitle = item.detail,
                        meta = item.level,
                    )
                }
            }
        }

        ProductionSection("变更")
        ProductionCardChangeEntry(
            session = session,
            cardId = card.id,
            plans = snapshot?.plans.orEmpty(),
        )

        ProductionBoundaryNote(
            "年费、账单日、分期等生命周期字段尚未进入正式数据模型，因此这里不会伪造这些信息。"
        )
    }
}

@Composable
private fun ProductionChange(
    app: VAppState,
    snapshot: VNextProductionSnapshot,
    modifier: Modifier,
) {
    ProductionPage(modifier, "变更", "已记录变更计划") {
        if (snapshot.plans.isEmpty()) {
            ProductionEmpty("当前没有已记录变更计划。")
        } else {
            snapshot.plans.forEach { plan ->
                ProductionFactCard(
                    title = plan.title,
                    subtitle = "${productionScenarioLabel(plan.scenario)} · ${productionWorkflowStateLabel(plan.workflowState)}",
                    meta = listOfNotNull(
                        "图谱修订 ${plan.lastAnalyzedRevision}",
                        plan.effectiveDate,
                    ).joinToString(" · "),
                    onClick = { app.openProductionPlan(plan.id, plan.scenario) },
                )
            }
        }
        ProductionSection("准备新变更")
        ProductionFactCard(
            title = "更换支付卡",
            subtitle = "先选择一张已确认支付工具，查看影响后明确建立更换计划",
            meta = "更换支付卡 · 正式执行能力已接入",
            onClick = { app.navigate(VScreen.CARDS) },
        )
        ProductionFactCard(
            title = "更换手机号",
            subtitle = "手机号类型完成底层确认后再开放正式对象选择",
            meta = "不会把通用身份对象猜成手机号",
        )

        ProductionBoundaryNote(
            "执行动作必须走 AppContainer Change authority；打开页面不会本地伪造完成或验证。"
        )
    }
}

@Composable
private fun ProductionRecords(
    session: ProductionVNextSession,
    modifier: Modifier,
) {
    val records = session.dataSource.productionRecords()
    ProductionPage(modifier, "记录", "来自已记录变更步骤与验证状态") {
        if (records.isEmpty()) {
            ProductionEmpty("当前没有已完成或待验证的生产记录。")
        } else {
            records.forEach { record ->
                val state = when (record.state) {
                    VNextProductionRecordState.RECORDED_COMPLETE -> "已记录完成"
                    VNextProductionRecordState.VERIFIED -> "已验证"
                    VNextProductionRecordState.PENDING_VERIFICATION -> "待验证"
                    VNextProductionRecordState.VERIFICATION_FAILED -> "验证失败"
                }
                ProductionFactCard(
                    title = record.actionTitle,
                    subtitle = "${record.planTitle} · $state",
                    meta = listOf(
                        productionActionPhaseLabel(record.phase),
                        if (record.evidenceRefs.isEmpty()) "无验证证据引用" else "验证证据 ${record.evidenceRefs.size} 项",
                        record.occurredAt?.take(10) ?: "发生时间未记录",
                    ).joinToString(" · "),
                )
            }
        }
        ProductionBoundaryNote("完成与验证是两个不同状态；done != verified。")
    }
}

@Composable
private fun ProductionMe(
    app: VAppState,
    snapshot: VNextProductionSnapshot,
    inventory: ProductionConsumerInventory,
    modifier: Modifier,
) {
    ProductionPage(modifier, "我", "个人数字生活 · 生产数据边界") {
        ProductionMetricRow(
            listOf(
                snapshot.revision to "图谱修订",
                inventory.activeSourceCount to "活跃数据源",
                inventory.pendingReviewCount to "待复核",
            ),
        )
        ProductionSection("数据边界")
        ProductionBoundaryNote(
            "这里展示的对象、关系、计划与记录来自当前已确认数据；演示生命周期字段不会混入正式数据。"
        )
        ProductionBoundaryNote(
            "号码 / 邮箱类型完成底层确认前仍按通用身份对象处理，不会根据名称或号码格式猜测。"
        )
        ProductionSection("个人控制面")
        if (inventory.pendingReviewCount > 0) {
            ProductionFactCard(
                title = "待复核",
                subtitle = "${inventory.pendingReviewCount} 项等待人工决定",
                meta = "建议 / 候选 / 可能变化不会自动进入已确认数据",
                onClick = { app.navigate(VScreen.REVIEW) },
            )
        }
        ProductionFactCard(
            title = "数据源",
            subtitle = "${inventory.activeSourceCount} 个活跃数据源",
            meta = "查看已记录的数据来源",
            onClick = { app.navigate(VScreen.SOURCES) },
        )
        ProductionFactCard(
            title = "建立基础设施",
            subtitle = "手工记录与文件导入 authority",
            meta = "建立对象不自动确认关系",
            onClick = { app.navigate(VScreen.IMPORT) },
        )
        ProductionFactCard(
            title = "隐私与偏好",
            subtitle = if (app.privacyMask) "敏感信息遮蔽：已开启" else "敏感信息遮蔽：已关闭",
            meta = "本机显示偏好，不修改已确认数据",
            onClick = { app.navigate(VScreen.SETTINGS) },
        )
        ProductionFactCard(
            title = "搜索",
            subtitle = "搜索当前已确认对象 / 变更计划 / 数据来源",
            meta = "未找到不等于外部不存在",
            onClick = { app.navigate(VScreen.SEARCH) },
        )
    }
}

@Composable
private fun ProductionSources(
    app: VAppState,
    inventory: ProductionConsumerInventory,
    modifier: Modifier,
) {
    ProductionPage(modifier, "数据源", "已记录数据来源") {
        if (inventory.sources.isEmpty()) {
            ProductionEmpty("当前没有已记录生产数据源。")
        } else {
            inventory.sources.forEach { source ->
                ProductionFactCard(
                    title = source.label,
                    subtitle = productionSourceStateLabel(source.state),
                    meta = source.lastIngestedAt?.take(10) ?: "最近导入时间未记录",
                )
            }
        }
        ProductionBoundaryNote(
            "数据来源存在不等于依赖已确认；导入产生的建议、候选对象和可能变化仍需人工复核。"
        )
        ProductionFactCard(
            title = "建立基础设施",
            subtitle = "手工记录 / 文件导入 authority 边界",
            meta = "对象建立不自动创建依赖关系",
            onClick = { app.navigate(VScreen.IMPORT) },
        )
    }
}

@Composable
private fun ProductionCategoryEntry(
    title: String,
    count: Int?,
    state: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 78.dp)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    color = PdigV2Colors.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (count != null) {
                    Text(
                        count.toString(),
                        color = PdigV2Colors.PrimaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(
                state,
                color = if (count != null) PdigV2Colors.TextSecondary else PdigV2Colors.TextMuted,
                fontSize = 9.sp,
            )
        }
    }
}

@Composable
private fun ProductionInventorySummary(inventory: ProductionConsumerInventory) {
    val facts = listOf(
        inventory.counts.paymentAssets to "支付工具",
        inventory.counts.genericIdentityAnchors to "身份对象",
        inventory.counts.accounts to "账户",
        inventory.counts.services to "服务",
        inventory.counts.devices to "设备",
        inventory.counts.memberships to "会员",
        inventory.counts.customObjects to "其他",
    )
    facts.chunked(3).forEach { row ->
        ProductionMetricRow(row)
    }
}

@Composable
private fun ProductionPage(
    modifier: Modifier,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = modifier.fillMaxSize().testTag("pdig.production-vnext.page"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = PdigV2Colors.TextPrimary, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = PdigV2Colors.TextMuted, fontSize = 12.sp)
            }
        }
        item { content() }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun ProductionMetricRow(facts: List<Pair<Int, String>>) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        facts.forEach { (value, label) ->
            Surface(
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 68.dp),
                color = PdigV2Colors.Surface,
                shape = RoundedCornerShape(VRadius.Lg),
                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(value.toString(), color = PdigV2Colors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(label, color = PdigV2Colors.TextMuted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun ProductionSection(title: String) {
    Text(title, color = PdigV2Colors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun ProductionFactCard(
    title: String,
    subtitle: String,
    meta: String,
    onClick: (() -> Unit)? = null,
) {
    val cardModifier = if (onClick == null) Modifier.fillMaxWidth()
    else Modifier.fillMaxWidth().clickable(onClick = onClick)
    Surface(
        modifier = cardModifier,
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = PdigV2Colors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = PdigV2Colors.TextSecondary, fontSize = 11.sp)
            if (meta.isNotBlank()) Text(meta, color = PdigV2Colors.TextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ProductionBoundaryNote(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.PrimarySoft,
        shape = RoundedCornerShape(VRadius.Md),
    ) {
        Text(
            text,
            Modifier.padding(12.dp),
            color = PdigV2Colors.TextSecondary,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun ProductionEmpty(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Md),
    ) {
        Text(text, Modifier.padding(13.dp), color = PdigV2Colors.TextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun ProductionUnavailable(title: String, body: String, modifier: Modifier) {
    Column(
        modifier.fillMaxSize().padding(24.dp).testTag("pdig.production-vnext.unavailable"),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, color = PdigV2Colors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(body, color = PdigV2Colors.TextSecondary, fontSize = 12.sp)
    }
}

private fun productionBoundaryBadge(screen: VScreen): String = when (screen) {
    VScreen.REVIEW -> "待复核 · 未进入已确认数据"
    VScreen.SETTINGS, VScreen.PERSONALIZATION -> "本机呈现偏好"
    VScreen.IMPORT, VScreen.MANUAL_ADD, VScreen.MANUAL_RELATION -> "建立 · 需明确确认"
    VScreen.SEARCH -> "生产数据搜索"
    VScreen.CHANGE_PHONE, VScreen.CHANGE_CARD -> "变更计划 · 正式状态"
    else -> "已确认数据"
}

private fun isProductionRoot(screen: VScreen): Boolean =
    screen in setOf(
        VScreen.NOW,
        VScreen.INFRASTRUCTURE,
        VScreen.CHANGE,
        VScreen.RECORDS,
        VScreen.ME,
    )
