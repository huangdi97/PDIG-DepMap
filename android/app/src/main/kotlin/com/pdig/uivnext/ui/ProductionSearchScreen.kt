package com.pdig.uivnext.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.production.ProductionVNextSession
import com.pdig.uivnext.production.VNextProductionObject
import com.pdig.uivnext.production.VNextProductionPlanSummary
import com.pdig.uivnext.production.VNextProductionSourceItem
import com.pdig.uivnext.production.VNextProductionSurfaceKind
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius

private sealed interface ProductionSearchHit {
    val stableKey: String
    val title: String
    val subtitle: String

    data class ObjectHit(
        val item: VNextProductionObject,
        override val title: String,
        override val subtitle: String,
    ) : ProductionSearchHit {
        override val stableKey: String = "object:${item.id}"
    }

    data class PlanHit(
        val plan: VNextProductionPlanSummary,
        override val title: String,
        override val subtitle: String,
    ) : ProductionSearchHit {
        override val stableKey: String = "plan:${plan.id}"
    }

    data class SourceHit(
        val source: VNextProductionSourceItem,
        override val title: String,
        override val subtitle: String,
    ) : ProductionSearchHit {
        override val stableKey: String = "source:${source.id}"
    }

    data class RouteHit(
        val screen: VScreen,
        override val title: String,
        override val subtitle: String,
    ) : ProductionSearchHit {
        override val stableKey: String = "route:${screen.route}"
    }
}

/**
 * Search over production Reality only.
 *
 * No fixture lifecycle values, phone/email inference, or provider assumptions are
 * indexed here. A missing result means "not found in current Reality", not "doesn't exist".
 */
@Composable
internal fun ProductionSearchScreen(
    session: ProductionVNextSession,
    modifier: Modifier = Modifier,
) {
    val snapshot = session.dataSource.productionSnapshot()
    if (snapshot == null) {
        Column(
            modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("生产 Reality 暂不可搜索", color = PdigV2Colors.TextPrimary,
                fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("不会回退到 Synthetic Reference 搜索结果。",
                color = PdigV2Colors.TextSecondary, fontSize = 12.sp)
        }
        return
    }

    var query by remember { mutableStateOf("") }
    val normalized = query.trim()
    val hits = remember(snapshot, normalized) {
        if (normalized.isBlank()) emptyList() else productionSearchHits(snapshot, normalized)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("pdig.production-vnext.search"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("搜索", color = PdigV2Colors.TextPrimary, fontSize = 23.sp,
                    fontWeight = FontWeight.Bold)
                Text(
                    "只搜索当前已确认 Reality、ChangePlan 与 SourceInstance。",
                    color = PdigV2Colors.TextMuted,
                    fontSize = 12.sp,
                )
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(100) },
                label = { Text("名称、发行方、尾号、类型、变更或数据源") },
                modifier = Modifier.fillMaxWidth().testTag("pdig.production-vnext.search.input"),
                singleLine = true,
            )
        }

        when {
            normalized.isBlank() -> item {
                SearchBoundary("输入关键词后开始搜索；不会搜索未确认 Proposal/Candidate 内容。")
            }
            hits.isEmpty() -> item {
                SearchBoundary("当前 Reality 中没有匹配项。未找到不等于外部不存在。")
            }
            else -> items(hits, key = { it.stableKey }) { hit ->
                ProductionSearchResult(hit) {
                    openProductionSearchHit(session, hit)
                }
            }
        }
    }
}

private fun productionSearchHits(
    snapshot: com.pdig.uivnext.production.VNextProductionSnapshot,
    query: String,
): List<ProductionSearchHit> {
    fun matches(vararg values: String?): Boolean =
        values.filterNotNull().any { it.contains(query, ignoreCase = true) }

    val objectHits = snapshot.objects.mapNotNull { item ->
        val kindLabel = productionObjectKindLabel(item.kind)
        if (!matches(item.name, item.kind, kindLabel, item.issuer, item.last4)) return@mapNotNull null
        ProductionSearchHit.ObjectHit(
            item = item,
            title = item.name,
            subtitle = listOfNotNull(
                kindLabel,
                item.issuer,
                item.last4?.let { "尾号 $it" },
            ).joinToString(" · "),
        )
    }

    val planHits = snapshot.plans.mapNotNull { plan ->
        val scenario = productionScenarioLabel(plan.scenario)
        val state = productionWorkflowStateLabel(plan.workflowState)
        if (!matches(plan.title, plan.scenario, scenario, plan.workflowState, state)) return@mapNotNull null
        ProductionSearchHit.PlanHit(
            plan = plan,
            title = plan.title,
            subtitle = "$scenario · $state",
        )
    }

    val sourceHits = snapshot.sources.mapNotNull { source ->
        if (!matches(source.label, source.adapterId, source.state)) return@mapNotNull null
        ProductionSearchHit.SourceHit(
            source = source,
            title = source.label,
            subtitle = "数据源 · ${productionSourceStateLabel(source.state)}",
        )
    }

    val routes = listOf(
        VScreen.INFRASTRUCTURE to "基础设施对象管理",
        VScreen.CHANGE to "变更与 ChangePlan",
        VScreen.RECORDS to "完成与验证记录",
        VScreen.REVIEW to "待复核建议、候选与漂移",
        VScreen.SOURCES to "数据源",
        VScreen.IMPORT to "建立基础设施",
        VScreen.ME to "我的数字生活",
    ).mapNotNull { (screen, hint) ->
        if (!matches(screen.titleZh, screen.route, hint)) return@mapNotNull null
        ProductionSearchHit.RouteHit(screen, screen.titleZh, hint)
    }

    return (objectHits + planHits + sourceHits + routes).take(50)
}

@Composable
private fun ProductionSearchResult(
    hit: ProductionSearchHit,
    onClick: () -> Unit,
) {
    val clickable = when (hit) {
        is ProductionSearchHit.ObjectHit -> hit.item.surfaceKind in setOf(
            VNextProductionSurfaceKind.PAYMENT_ASSET,
            VNextProductionSurfaceKind.ACCOUNT,
            VNextProductionSurfaceKind.DEVICE,
            VNextProductionSurfaceKind.SERVICE,
        )
        else -> true
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
            .testTag("pdig.production-vnext.search.result"),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(
            Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(hit.title, color = PdigV2Colors.TextPrimary, fontSize = 13.sp,
                fontWeight = FontWeight.Bold)
            Text(hit.subtitle, color = PdigV2Colors.TextSecondary, fontSize = 10.sp)
            if (!clickable) {
                Text(
                    "当前对象类型缺少安全的专用详情映射；不会强制转换。",
                    color = PdigV2Colors.TextMuted,
                    fontSize = 9.sp,
                )
            }
        }
    }
}

private fun openProductionSearchHit(
    session: ProductionVNextSession,
    hit: ProductionSearchHit,
) {
    val app = session.appState
    when (hit) {
        is ProductionSearchHit.ObjectHit -> when (hit.item.surfaceKind) {
            VNextProductionSurfaceKind.PAYMENT_ASSET -> app.openCard(hit.item.id)
            VNextProductionSurfaceKind.ACCOUNT ->
                app.openSecondaryObject(VScreen.ACCOUNT_DETAIL, hit.item.id)
            VNextProductionSurfaceKind.DEVICE ->
                app.openSecondaryObject(VScreen.DEVICE_DETAIL, hit.item.id)
            VNextProductionSurfaceKind.SERVICE ->
                app.openSecondaryObject(VScreen.SERVICE_DETAIL, hit.item.id)
            else -> Unit
        }
        is ProductionSearchHit.PlanHit ->
            app.openProductionPlan(hit.plan.id, hit.plan.scenario)
        is ProductionSearchHit.SourceHit -> app.navigateFromSearch(VScreen.SOURCES)
        is ProductionSearchHit.RouteHit -> app.navigateFromSearch(hit.screen)
    }
}

@Composable
private fun SearchBoundary(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.PrimarySoft.copy(alpha = 0.62f),
        shape = RoundedCornerShape(VRadius.Lg),
    ) {
        Text(text, Modifier.padding(13.dp), color = PdigV2Colors.TextSecondary,
            fontSize = 10.sp)
    }
}
