package com.pdig.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.pdig.app.ui.components.PdigCard
import com.pdig.app.ui.components.PdigScrollingPage
import com.pdig.app.ui.components.PdigTopBar
import com.pdig.app.ui.components.SectionHeader
import com.pdig.app.ui.components.StatusChip
import com.pdig.app.ui.theme.PdigTokens
import com.pdig.core.domain.ActionVerificationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    ActionVerificationStatus.EVIDENCE_SUGGESTED -> "发现新的依据，请确认"
    ActionVerificationStatus.VERIFIED -> "已验证"
    ActionVerificationStatus.FAILED -> "验证失败"
}
