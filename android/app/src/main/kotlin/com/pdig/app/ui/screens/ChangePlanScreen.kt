package com.pdig.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.pdig.app.data.AppContainer
import com.pdig.app.data.PlanDetailView
import com.pdig.app.ui.Route
import com.pdig.app.ui.components.EmptyState
import com.pdig.app.ui.components.LoadingState
import com.pdig.app.ui.components.PdigScrollingPage
import com.pdig.app.ui.components.PdigTopBar
import com.pdig.app.ui.components.SectionHeader
import com.pdig.app.ui.components.StatusChip
import com.pdig.app.ui.theme.PdigTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 变更计划页：动作列表以「步骤轨道」（Continuity Rail）呈现。
 * 每步状态：已验证 > 已完成 > 进行中 > 等待前置 > 即将到来；状态 = 形状 + 文字 + 颜色三通道。
 * Make-Before-Break 闸门：未验证的「停用旧路径」动作显示明文原因，禁止只 disabled。
 */
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
                    d.actions.forEachIndexed { i, a ->
                        StepRailRow(
                            state = stepStateOf(a, d.actions),
                            isLast = i == d.actions.lastIndex,
                            action = a,
                            allActions = d.actions,
                            onComplete = {
                                scope.launch {
                                    withContext(Dispatchers.IO) { container.completeAction(planId, a.id) }
                                    reload()
                                }
                            },
                            onVerify = {
                                scope.launch {
                                    withContext(Dispatchers.IO) { container.verifyAction(planId, a.id) }
                                    reload()
                                }
                            },
                        )
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
