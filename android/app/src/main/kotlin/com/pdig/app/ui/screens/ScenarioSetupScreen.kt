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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavController
import com.pdig.app.data.AppContainer
 import com.pdig.app.data.ScenarioPlanRequest
import com.pdig.app.data.NodeRow
import com.pdig.app.ui.Route
import com.pdig.app.ui.components.EmptyState
import com.pdig.app.ui.components.LoadingState
import com.pdig.app.ui.components.PdigCard
import com.pdig.app.ui.components.PdigScrollingPage
import com.pdig.app.ui.components.PdigTopBar
import com.pdig.app.ui.components.SectionHeader
import com.pdig.app.ui.theme.PdigTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
/**
 * 场景设置页：选择要变更的支付工具，然后由 core 创建真实 ChangePlan。
 * 场景清单来自 ScenarioRegistry（planned 模板不可执行）。
 */
@Composable
fun ScenarioSetupScreen(nav: NavController, templateId: String) {
    val context = LocalContext.current
    val container = remember { AppContainer.get(context) }
    val scope = rememberCoroutineScope()
    val template = com.pdig.core.scenario.ScenarioRegistry.get(templateId)
    var instruments by remember { mutableStateOf<List<NodeRow>?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var effectiveDate by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(templateId) {
        instruments = withContext(Dispatchers.IO) {
            container.nodes().filter { it.kind == "payment_instrument" }
        }
    }

    // 同 DataScreens.ImportScreen：planned 场景分支必须整体返回，
    // 不能在嵌套 Column lambda 里 `return@Scaffold`（Compose start/end 失衡会崩）。
    if (!com.pdig.core.scenario.ScenarioRegistry.isExecutable(templateId)) {
        PlannedScenarioNotice(nav = nav, template = template)
        return
    }

    Scaffold(topBar = { PdigTopBar(template?.title ?: "场景", onBack = { nav.popBackStack() }) }) { pad ->
        PdigScrollingPage(
            modifier = Modifier
                .padding(pad)
                .padding(PdigTokens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceMd),
        ) {
            template?.description?.let {
                Text(it, style = PdigTokens.Body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            when {
                instruments == null -> LoadingState()
                instruments!!.isEmpty() -> EmptyState("还没有可以变更的支付工具。")
                else -> {
                    SectionHeader("选择要变更的支付工具")
                    instruments!!.forEach { n ->
                        PdigCard(onClick = { selectedId = n.id }) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(n.name, style = PdigTokens.BodyStrong, modifier = Modifier.weight(1f))
                                if (selectedId == n.id) {
                                    Text("已选择", style = PdigTokens.Label, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    androidx.compose.material3.OutlinedTextField(
                        value = effectiveDate,
                        onValueChange = { effectiveDate = it },
                        label = { Text("计划生效日期（可留空，YYYY-MM-DD）") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "计划生效日期，可留空，格式为四位年-两位月-两位日" },
                        singleLine = true,
                    )

                    Button(
                        onClick = {
                            val target = selectedId ?: return@Button
                            busy = true
                            failure = null
                            scope.launch {
                                // 计划的 actions / readiness 由 core.plan 计算，UI 不推导
                                val created = runCatching {
                                    withContext(Dispatchers.IO) {
                                        container.createPlanForScenario(
                                            ScenarioPlanRequest(
                                                scenarioId = templateId,
                                                targetNodeId = target,
                                                effectiveDate = effectiveDate.trim().ifBlank { null },
                                            ),
                                        )
                                    }
                                }.getOrNull()
                                busy = false
                                if (created == null) {
                                    failure = "无法创建计划：场景不可执行或目标不存在。"
                                } else {
                                    nav.navigate(Route.PLAN.replace("{planId}", created))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = PdigTokens.MinTouchTarget),
                        enabled = selectedId != null && !busy,
                    ) { Text(if (busy) "正在分析…" else "分析影响并创建变更计划") }

                    failure?.let {
                        Text(it, style = PdigTokens.Body, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

/**
 * planned（尚不可执行）场景占位页。
 *
 * 单独成函数：planned 分支若靠 `return@Scaffold` 从嵌套 Column lambda 跳出，
 * 会造成 Compose start/end 失衡并在重组时崩溃。
 */
@Composable
private fun PlannedScenarioNotice(
    nav: NavController,
    template: com.pdig.core.scenario.ScenarioTemplate?,
) {
    Scaffold(topBar = { PdigTopBar(template?.title ?: "场景", onBack = { nav.popBackStack() }) }) { pad ->
        PdigScrollingPage(
            modifier = Modifier
                .padding(pad)
                .padding(PdigTokens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceMd),
        ) {
            template?.description?.let {
                Text(it, style = PdigTokens.Body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            EmptyState("这个场景还在规划中，暂不能执行。")
        }
    }
}
