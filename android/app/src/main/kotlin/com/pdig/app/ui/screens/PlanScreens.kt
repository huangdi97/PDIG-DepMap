package com.pdig.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.pdig.app.ui.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.pdig.app.data.AppContainer
import com.pdig.app.data.AppContainer.PlanDetailView
import com.pdig.app.data.CandidateRow
import com.pdig.core.domain.ActionVerificationStatus
import com.pdig.app.data.DriftRow
import com.pdig.app.data.PlanRow
import com.pdig.app.data.ProposalRow
import com.pdig.app.ui.components.EmptyState
import com.pdig.app.ui.components.LoadingState
import com.pdig.app.ui.components.PdigCard
import com.pdig.app.ui.components.PdigScrollingPage
import com.pdig.app.ui.components.PdigTopBar
import com.pdig.app.ui.components.SectionHeader
import com.pdig.app.ui.components.StatusChip
import com.pdig.app.ui.theme.PdigTokens

@Composable
fun ChangePlanScreen(nav: NavController, planId: String) {
    val context = LocalContext.current
    val container = remember { AppContainer.get(context) }
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<PlanDetailView?>(null) }

    fun reload() {
        scope.launch { detail = withContext(Dispatchers.IO) { container.planDetail(planId) } }
    }

    LaunchedEffect(planId) { reload() }

    Scaffold(topBar = { PdigTopBar("变更计划", onBack = { nav.popBackStack() }) }) { pad ->
        PdigScrollingPage(
            modifier = Modifier
                .padding(pad)
                .padding(PdigTokens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceMd),
        ) {
            val d = detail
            if (d == null) {
                LoadingState()
            } else {
                Text(d.title, style = PdigTokens.Title)
                Row(horizontalArrangement = Arrangement.spacedBy(PdigTokens.SpaceSm)) {
                    StatusChip(d.readiness.wire)
                    StatusChip(d.workflowState.wire)
                }
                d.effectiveState?.let {
                    EmptyState("本计划依据的图谱已变化，需要重新分析后再继续。")
                }
                SectionHeader("信息时效")
                Text(
                    when {
                        d.effectiveState != null ->
                            "创建后你的基础设施信息发生了变化，需要重新检查（需要重新检查）。"
                        d.currentGraphRevision > d.lastAnalyzedGraphRevision ->
                            "自分析以来信息有更新，需要重新检查（需要重新检查）。"
                        else -> "计划依据的信息没有发生变化。"
                    },
                    style = PdigTokens.Body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                d.effectiveDate?.let { Text("生效日期：${it.take(10)}", style = PdigTokens.Body) }

                SectionHeader("必须处理的事项（${d.mustChangeKeys.size}）")
                Text(
                    "还未处理：${d.unresolvedMustChangeKeys.size} 条。",
                    style = PdigTokens.Body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SectionHeader("处理步骤")
                if (d.actions.isEmpty()) {
                    EmptyState("计划里没有需要执行的步骤。")
                } else {
                    d.actions.forEach { a ->
                        val v = a.verification
                        PdigCard {
                            Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs)) {
                                Text(a.title, style = PdigTokens.BodyStrong)
                                Text(a.title, style = PdigTokens.BodyStrong)
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(PdigTokens.SpaceSm),
                                ) {
                                    Text(
                                        if (a.done) "已完成" else "未完成",
                                        style = PdigTokens.Label,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        "验证：${verificationLabel(v?.status)}",
                                        style = PdigTokens.Label,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(PdigTokens.SpaceSm)) {
                                    if (!a.done) {
                                        Button(onClick = {
                                            scope.launch {
                                                withContext(Dispatchers.IO) {
                                                    container.completeAction(planId, a.id)
                                                }
                                                reload()
                                            }
                                        }) { Text("标记完成") }
                                    }
                                    if (a.done && v != null && v.status != ActionVerificationStatus.VERIFIED) {
                                        Button(onClick = {
                                            scope.launch {
                                                withContext(Dispatchers.IO) {
                                                    container.verifyAction(planId, a.id)
                                                }
                                                reload()
                                            }
                                        }) { Text("确认验证") }
                                    }
                                }
                            }
                        }
                    }
                }

                d.targetNodeId?.let { target ->
                    Button(
                        onClick = { nav.navigate(Route.IMPACT.replace("{nodeId}", target)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = PdigTokens.MinTouchTarget),
                    ) { Text("查看影响范围") }
                }
            }
        }
    }
}

/** 验证状态人话标签。done ≠ verified —— 这两个状态在 UI 上必须同时可见。 */
internal fun verificationLabel(status: ActionVerificationStatus?): String = when (status) {
    null -> "无需验证"
    ActionVerificationStatus.NOT_REQUIRED -> "无需验证"
    ActionVerificationStatus.PENDING -> "待验证"
    ActionVerificationStatus.EVIDENCE_SUGGESTED -> "有佐证提示"
    ActionVerificationStatus.VERIFIED -> "已验证"
    ActionVerificationStatus.FAILED -> "验证失败"
}

/** 待确认：Proposal（候选关系）必须由用户确认，不得自动成为 Reality（spec §13）。 */
@Composable
fun PendingReviewScreen(nav: NavController) {
    val context = LocalContext.current
    val container = remember { AppContainer.get(context) }
    var proposals by remember { mutableStateOf<List<ProposalRow>?>(null) }
    var nodeNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            proposals = container.pendingProposals()
            nodeNames = container.nodes(includeArchived = true).associate { it.id to it.name }
        }
    }

    /** 显示人名而非内部 id（依赖详情页同理：不暴露内部术语，spec §65）。 */
    fun name(id: String): String = nodeNames[id] ?: id

    Scaffold(topBar = { PdigTopBar("待确认服务", onBack = { nav.popBackStack() }) }) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(PdigTokens.SpaceLg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceMd),
        ) {
            when {
                proposals == null -> LoadingState()
                proposals!!.isEmpty() -> EmptyState("没有待确认的项目。")
                else -> proposals!!.forEach { p ->
                    var done by remember(p.id) { mutableStateOf(false) }
                    if (done) return@forEach
                    PdigCard {
                        Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceSm)) {
                            Text("${name(p.from)} → ${name(p.to)}", style = PdigTokens.BodyStrong)
                            Text(
                                "观测到 ${p.observationCount} 次，置信度 ${(p.confidence * 100).toInt()}%",
                                style = PdigTokens.Caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "确认前不会当成事实，也不参与影响分析。",
                                style = PdigTokens.Caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(PdigTokens.SpaceSm)) {
                                Button(onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { container.acceptProposal(p.id) }
                                        done = true
                                    }
                                }) { Text("确认") }
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { container.rejectProposal(p.id) }
                                        done = true
                                    }
                                }) { Text("忽略") }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** RealityDrift：positive evidence only；absence-only 永不产生 Drift（spec §23）。 */
@Composable
fun RealityDriftScreen(nav: NavController) {
    val context = LocalContext.current
    val container = remember { AppContainer.get(context) }
    val scope = rememberCoroutineScope()
    var drifts by remember { mutableStateOf<List<DriftRow>?>(null) }
    var resolvedIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    fun reload() {
        scope.launch { drifts = withContext(Dispatchers.IO) { container.openDrifts() } }
    }

    LaunchedEffect(Unit) { reload() }

    Scaffold(topBar = { PdigTopBar("可能发生了变化", onBack = { nav.popBackStack() }) }) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(PdigTokens.SpaceLg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceMd),
        ) {
            when {
                drifts == null -> LoadingState()
                drifts!!.isEmpty() -> EmptyState("没有检测到需要确认的变化。")
                else -> drifts!!.forEach { d ->
                    if (d.id in resolvedIds) return@forEach
                    // H-17：发现 → Review → 用户选择 → Reality mutation。
                    // 已更换 / 两者都在用 是 Reality mutation（resolve_* ∈ bumpsOn）；
                    // 没变化 = dismiss（不改 Reality）；稍后确认 = 保持 open（本屏不操作）。
                    PdigCard {
                        Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs)) {
                            Text("可能发生了变化", style = PdigTokens.BodyStrong)
                            Text(
                                "依据 ${d.observationCount} 条观测记录（${d.detectedAt.take(10)}）。" +
                                    "请确认：这张卡现在怎么在用？",
                                style = PdigTokens.Caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(PdigTokens.SpaceSm)) {
                                Button(onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { container.resolveDriftAsReplacement(d.id) }
                                        resolvedIds = resolvedIds + d.id
                                    }
                                }) { Text("已更换") }
                                Button(onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { container.resolveDriftAsAdditionalPath(d.id) }
                                        resolvedIds = resolvedIds + d.id
                                    }
                                }) { Text("两者都在用") }
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { container.dismissDrift(d.id) }
                                        resolvedIds = resolvedIds + d.id
                                    }
                                }) { Text("没变化") }
                                TextButton(onClick = { resolvedIds = resolvedIds + d.id }) { Text("稍后确认") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdriftCard(d: DriftRow, onClick: () -> Unit) {
    PdigCard(onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs)) {
            Text("可能发生了变化", style = PdigTokens.BodyStrong)
            Text(
                "依据 ${d.observationCount} 条观测记录（${d.detectedAt.take(10)}）",
                style = PdigTokens.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** DiscoveryCandidate：accept 才创建 Node；不进 Impact、不 bump revision（spec §24/§26，H-16）。 */
@Composable
fun CandidateReviewScreen(nav: NavController) {
    val context = LocalContext.current
    val container = remember { AppContainer.get(context) }
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<CandidateRow>?>(null) }
    var acceptedLabels by remember { mutableStateOf<Set<String>>(emptySet()) }

    fun reload() {
        scope.launch { items = withContext(Dispatchers.IO) { container.pendingCandidates() } }
    }

    LaunchedEffect(Unit) { reload() }

    Scaffold(topBar = { PdigTopBar("待确认服务", onBack = { nav.popBackStack() }) }) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(PdigTokens.SpaceLg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceMd),
        ) {
            when {
                items == null -> LoadingState()
                items!!.isEmpty() -> EmptyState("没有新的候选对象。")
                else -> items!!.forEach { c ->
                    if (c.label in acceptedLabels) return@forEach
                    PdigCard {
                        Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs)) {
                            Text(c.label, style = PdigTokens.BodyStrong)
                            Text(
                                "观测到 ${c.observationCount} 次；尚未确认，不会参与影响分析。",
                                style = PdigTokens.Caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(PdigTokens.SpaceSm)) {
                                Button(onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { container.acceptCandidate(c.id) }
                                        acceptedLabels = acceptedLabels + c.label
                                    }
                                }) { Text("确认") }
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { container.dismissCandidate(c.id) }
                                        acceptedLabels = acceptedLabels + c.label
                                    }
                                }) { Text("忽略") }
                            }
                        }
                    }
                }
            }
        }
    }
}
