package com.pdig.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.pdig.app.data.AppContainer
import com.pdig.app.data.PlanRow
import com.pdig.app.ui.Route
import com.pdig.app.ui.components.EmptyState
import com.pdig.app.ui.components.LoadingState
import com.pdig.app.ui.components.PdigCard
import com.pdig.app.ui.components.PdigScrollingPage
import com.pdig.app.ui.components.PdigTopBar
import com.pdig.app.ui.components.SectionHeader
import com.pdig.app.ui.components.StatusChip
import com.pdig.app.ui.theme.PdigTokens
import com.pdig.core.timeline.TimelineItem

/**
 * 首页：Personal Infrastructure Briefing（spec §64/§186 + UIUX_FREEZE §4）。
 * 顺序固定：需要你处理 → 基础设施薄弱点 → 可能发生了变化 → 即将到来 → 常用场景 → 我的基础设施。
 * healthy 态不显示 "0 条问题" / 健康分，改为「当前没有需要立即处理的事项 + 最近检查范围 +
 * 仍然未知的范围 + 可以主动准备的场景」。
 */
@Composable
fun HomeScreen(nav: NavController) {
    val context = LocalContext.current
    val container = remember { AppContainer.get(context) }
    var items by remember { mutableStateOf<List<TimelineItem>?>(null) }
    var plans by remember { mutableStateOf<List<PlanRow>>(emptyList()) }
    var nodeCount by remember { mutableStateOf(0) }
    var depCount by remember { mutableStateOf(0) }
    var pendingProposalCount by remember { mutableStateOf<Int?>(null) }
    var candidateCount by remember { mutableStateOf<Int?>(null) }
    var driftCount by remember { mutableStateOf<Int?>(null) }
    var findings by remember { mutableStateOf<FindingsModel?>(null) }

    // ⚠ 数据库读写一律放到 IO 线程（2026-09-16 修复）：
    // 此前这些行在主线程执行，全新安装后（dexopt + 打开 SQLCipher 密文库 + 迁移 + Argon2id）
    // 会把主线程占满并触发系统 ANR 对话框（真机实测，本轮 E2E 连续复现）。
    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) {
            HomeCounts(
                timeline = container.timeline(),
                plans = container.plans(),
                nodeCount = container.nodes().size,
                dependencies = container.dependencies().size,
                proposals = container.pendingProposals().size,
                candidates = container.pendingCandidates().size,
                drifts = container.openDrifts().size,
            )
        }
        items = loaded.timeline
        plans = loaded.plans
        nodeCount = loaded.nodeCount
        depCount = loaded.dependencies
        pendingProposalCount = loaded.proposals
        candidateCount = loaded.candidates
        driftCount = loaded.drifts
        findings = withContext(Dispatchers.IO) { buildFindings(container) }
    }

    Scaffold(topBar = { PdigTopBar("PDIG") }) { pad ->
        PdigScrollingPage(
            modifier = Modifier
                .padding(pad)
                .padding(horizontal = PdigTokens.SpaceLg),
        ) {
            if (items == null) {
                LoadingState()
            } else {
                val attention = (items ?: emptyList()).filter { it.bucket == "attention" }
                val upcoming = (items ?: emptyList()).filter { it.bucket != "attention" }
                val hasPendingReview = (pendingProposalCount ?: 0) > 0
                val hasCandidates = (candidateCount ?: 0) > 0
                val planNeedsHandling = plansNeedingHandling(plans)
                val hasAttention = attention.isNotEmpty() || hasPendingReview || hasCandidates || planNeedsHandling > 0

                // 1. 需要你处理
                SectionHeader("需要你处理")
                if (hasAttention) {
                    if (planNeedsHandling > 0) {
                        PdigCard(onClick = { nav.navigate(Route.TIMELINE) }) {
                            Text("有 $planNeedsHandling 个计划需要处理", style = PdigTokens.BodyStrong)
                        }
                    }
                    if (hasPendingReview) {
                        PdigCard(onClick = { nav.navigate(Route.REVIEW) }) {
                            Text("有待确认的关系：$pendingProposalCount 条", style = PdigTokens.BodyStrong)
                        }
                    }
                    if (hasCandidates) {
                        PdigCard(onClick = { nav.navigate(Route.CANDIDATES) }) {
                            Text("有待确认的服务：$candidateCount 个", style = PdigTokens.BodyStrong)
                        }
                    }
                    attention.take(5).forEach { item ->
                        PdigCard(onClick = { nav.navigate(Route.TIMELINE) }) {
                            Column {
                                Text(item.title, style = PdigTokens.BodyStrong)
                                Text(
                                    item.subtitle,
                                    style = PdigTokens.Caption,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    HealthyBriefing(
                        nodeCount = nodeCount,
                        depCount = depCount,
                        findings = findings,
                        onScenarios = { nav.navigate(Route.SCENARIOS) },
                    )
                }

                // 2. 基础设施薄弱点（findings 摘要行，不显示空卡片）
                SectionHeader("基础设施薄弱点")
                FindingsSummary(findings, onOpen = { nav.navigate(Route.FINDINGS) })

                // 3. 可能发生了变化
                SectionHeader("可能发生了变化")
                PdigCard(onClick = { nav.navigate(Route.DRIFT) }) {
                    Text(
                        if ((driftCount ?: 0) > 0) "有 $driftCount 条变化待确认" else "查看待确认的变化",
                        style = PdigTokens.BodyStrong,
                    )
                }

                // 4. 即将到来
                SectionHeader("即将到来")
                if (upcoming.isEmpty()) {
                    EmptyState("未来 90 天内没有已计划的变更。")
                } else {
                    upcoming.take(5).forEach { item ->
                        PdigCard(onClick = { nav.navigate(Route.TIMELINE) }) {
                            Column {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(item.title, style = PdigTokens.BodyStrong, modifier = Modifier.weight(1f))
                                    StatusChip(item.status)
                                }
                                item.scheduledAt?.let {
                                    Text(
                                        it.take(10),
                                        style = PdigTokens.Caption,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. 常用场景（2 列 action row）
                SectionHeader("常用场景")
                ScenarioEntryGrid(onScenario = { templateId ->
                    nav.navigate(Route.SCENARIO_SETUP.replace("{templateId}", templateId))
                })

                // 6. 我的基础设施
                SectionHeader("我的基础设施")
                PdigCard(onClick = { nav.navigate(Route.INFRASTRUCTURE) }) {
                    Text("共 $nodeCount 个对象", style = PdigTokens.BodyStrong)
                }

                // 导入 / 备份恢复与设置入口（保持可达；不改变信息架构）
                SectionHeader("数据与设置")
                PdigCard(onClick = { nav.navigate(Route.SOURCES) }) {
                    Text("数据来源与导入", style = PdigTokens.BodyStrong)
                }
                PdigCard(onClick = { nav.navigate(Route.SETTINGS) }) {
                    Text("设置", style = PdigTokens.BodyStrong)
                }
            }
        }
    }
}

