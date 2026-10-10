package com.pdig.uivnext.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.weight
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
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                app.screen.titleZh,
                color = PdigV2Colors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "已确认 Reality",
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
            "生产 Reality 暂不可读取",
            "没有权威快照时不会回退到参考 Fixture。",
            modifier,
        )
        return
    }

    when (app.screen) {
        VScreen.NOW -> ProductionNow(snapshot, inventory, modifier)
        VScreen.INFRASTRUCTURE, VScreen.OVERVIEW ->
            ProductionInfrastructure(inventory, modifier)
        VScreen.CHANGE -> ProductionChange(snapshot, modifier)
        VScreen.RECORDS -> ProductionRecords(session, modifier)
        VScreen.ME -> ProductionMe(snapshot, inventory, modifier)
        VScreen.SOURCES -> ProductionSources(inventory, modifier)
        else -> ProductionUnavailable(
            title = "该页面尚未完成生产数据绑定",
            body = "当前页面不会使用 Synthetic Reference 代替真实 Reality。返回五个一级入口继续查看已绑定内容。",
            modifier = modifier,
        )
    }
}

@Composable
private fun ProductionNow(
    snapshot: VNextProductionSnapshot,
    inventory: ProductionConsumerInventory,
    modifier: Modifier,
) {
    ProductionPage(modifier, "现在", "基于当前加密 Reality 的只读概览") {
        ProductionMetricRow(
            listOf(
                inventory.pendingReviewCount to "待复核",
                snapshot.plans.count { it.workflowState != "closed" } to "进行中变更",
                inventory.activeSourceCount to "活跃数据源",
            ),
        )

        ProductionSection("近期记录 / 计划")
        if (snapshot.timeline.isEmpty()) {
            ProductionEmpty("当前没有可投影的 Timeline 项；这不表示没有风险或依赖。")
        } else {
            snapshot.timeline.take(8).forEach { item ->
                ProductionFactCard(
                    title = item.title,
                    subtitle = item.subtitle.ifBlank { item.status },
                    meta = listOfNotNull(item.scheduledAt, item.bucket).joinToString(" · "),
                )
            }
        }

        ProductionSection("基础设施")
        ProductionInventorySummary(inventory)
    }
}

@Composable
private fun ProductionInfrastructure(
    inventory: ProductionConsumerInventory,
    modifier: Modifier,
) {
    ProductionPage(modifier, "基础设施", "只展示当前 Canonical 可以证明的对象类型") {
        ProductionInventorySummary(inventory)

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
                )
            }
        }

        ProductionSection("身份对象")
        if (inventory.genericIdentityAnchors.isEmpty()) {
            ProductionEmpty("没有已确认的 identity_anchor。")
        } else {
            inventory.genericIdentityAnchors.forEach { identity ->
                ProductionFactCard(
                    title = identity.name,
                    subtitle = "通用身份对象 · 具体 phone/email subtype 尚未 Canonical 化",
                    meta = "${identity.confirmedDependencyCount} 条已确认关系",
                )
            }
        }
    }
}

@Composable
private fun ProductionChange(
    snapshot: VNextProductionSnapshot,
    modifier: Modifier,
) {
    ProductionPage(modifier, "变更", "真实 ChangePlan 只读投影") {
        if (snapshot.plans.isEmpty()) {
            ProductionEmpty("当前没有已记录 ChangePlan。")
        } else {
            snapshot.plans.forEach { plan ->
                ProductionFactCard(
                    title = plan.title,
                    subtitle = "${plan.scenario} · ${plan.workflowState}",
                    meta = listOfNotNull(
                        "图谱修订 ${plan.lastAnalyzedRevision}",
                        plan.effectiveDate,
                    ).joinToString(" · "),
                )
            }
        }
        ProductionBoundaryNote(
            "执行动作必须走 AppContainer Change authority；此只读入口不会本地伪造完成或验证。"
        )
    }
}

@Composable
private fun ProductionRecords(
    session: ProductionVNextSession,
    modifier: Modifier,
) {
    val records = session.dataSource.productionRecords()
    ProductionPage(modifier, "记录", "来自权威 ChangePlan action / verification 状态") {
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
                    meta = record.phase,
                )
            }
        }
        ProductionBoundaryNote("完成与验证是两个不同状态；done != verified。")
    }
}

@Composable
private fun ProductionMe(
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
            "这里展示的对象、关系、计划与记录来自当前生产 Reality；参考生命周期字段不会混入生产数据。"
        )
        ProductionBoundaryNote(
            "号码 / 邮箱在 subtype Canonical 化前仍按通用 identity_anchor 处理，不会根据名称或号码格式推断。"
        )
    }
}

@Composable
private fun ProductionSources(
    inventory: ProductionConsumerInventory,
    modifier: Modifier,
) {
    ProductionPage(modifier, "数据源", "只读生产 SourceInstance 投影") {
        if (inventory.sources.isEmpty()) {
            ProductionEmpty("当前没有已记录生产数据源。")
        } else {
            inventory.sources.forEach { source ->
                ProductionFactCard(
                    title = source.label,
                    subtitle = source.state,
                    meta = source.lastIngestedAt ?: "最近导入时间未记录",
                )
            }
        }
        ProductionBoundaryNote(
            "数据源存在不等于依赖已确认；Import 产生的 Proposal/Candidate/Drift 仍需 Human Review。"
        )
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
private fun ProductionFactCard(title: String, subtitle: String, meta: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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

private fun isProductionRoot(screen: VScreen): Boolean =
    screen in setOf(
        VScreen.NOW,
        VScreen.INFRASTRUCTURE,
        VScreen.CHANGE,
        VScreen.RECORDS,
        VScreen.ME,
    )
