package com.pdig.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.pdig.app.ui.Route
import com.pdig.app.data.AppContainer
import com.pdig.app.data.PlanRow
import com.pdig.app.ui.components.EmptyState
import com.pdig.app.ui.components.LoadingState
import com.pdig.app.ui.components.PdigCard
import com.pdig.app.ui.components.PdigScrollingPage
import com.pdig.app.ui.components.PdigTopBar
import com.pdig.app.ui.components.SectionHeader
import com.pdig.app.ui.components.StatusChip
import com.pdig.app.ui.theme.PdigTokens
import com.pdig.core.timeline.TimelineItem

/** 首次启动 / 从旧 .depmap 恢复（spec §223）。 */
@Composable
fun OnboardingScreen(nav: NavController) {
    Scaffold { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(PdigTokens.SpaceXl)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceLg),
        ) {
            Text("欢迎使用 PDIG", style = PdigTokens.Display)
            Text(
                "PDIG 帮你记录：服务背后依赖了哪些卡、账户和入口。\n" +
                    "所有数据只保存在这台设备上。",
                style = PdigTokens.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = { nav.navigate(Route.HOME) }, modifier = Modifier.fillMaxWidth()) {
                Text("开始使用")
            }
            Button(onClick = { nav.navigate(Route.RESTORE) }, modifier = Modifier.fillMaxWidth()) {
                Text("从备份文件恢复")
            }
        }
    }
}

/**
 * 首页：Answer-oriented（spec §64/§186）。
 * 顺序固定：需要你处理 → 可能发生了变化 → 即将到来 → 常用场景 → 我的基础设施。
 */
@Composable
fun HomeScreen(nav: NavController) {
    val context = LocalContext.current
    val container = remember { AppContainer.get(context) }
    var items by remember { mutableStateOf<List<TimelineItem>?>(null) }
    var plans by remember { mutableStateOf<List<PlanRow>>(emptyList()) }
    var nodeCount by remember { mutableStateOf(0) }

    // ⚠ 数据库读写一律放到 IO 线程（2026-09-16 修复）：
    // 此前这三行在主线程执行，全新安装后（dexopt + 打开 SQLCipher 密文库 + 迁移 + Argon2id）
    // 会把主线程占满并触发系统 ANR 对话框（真机实测，本轮 E2E 连续复现）。
    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) {
            Triple(container.timeline(), container.plans(), container.nodes().size)
        }
        items = loaded.first
        plans = loaded.second
        nodeCount = loaded.third
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
                val attention = items!!.filter { it.bucket == "attention" }
                val upcoming = items!!.filter { it.bucket != "attention" }

                SectionHeader("需要你处理")
                if (attention.isEmpty()) {
                    EmptyState("现在没有需要你处理的事项。")
                } else {
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
                }

                SectionHeader("可能发生了变化")
                PdigCard(onClick = { nav.navigate(Route.DRIFT) }) {
                    Text("查看待确认的变化", style = PdigTokens.BodyStrong)
                }

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

                SectionHeader("常用场景")
                PdigCard(onClick = { nav.navigate(Route.SCENARIOS) }) { Text("更换银行卡 / 银行卡即将到期 / 注销银行卡") }

                SectionHeader("我的基础设施")
                PdigCard(onClick = { nav.navigate(Route.INFRASTRUCTURE) }) {
                    Text("共 $nodeCount 个对象", style = PdigTokens.BodyStrong)
                }

                // 导入 / 备份恢复此前从首页不可达，核心行程根本走不通。
                // 这里补上最小入口（不改信息架构，只是让已存在的页面可达）。
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

/** 场景中心：当前只 Active 三个场景（spec §67/§187）。 */
@Composable
fun ScenarioCenterScreen(nav: NavController) {
    // 场景清单唯一来源是 ScenarioRegistry —— 硬编码 id 曾被写错成
    // `card_expiring` / `close_bank_card`，与实际模板不符，点进去会被运行时注册中心拒绝。
    val scenarios = com.pdig.core.scenario.ScenarioRegistry.active

    Scaffold(topBar = { PdigTopBar("场景", onBack = { nav.popBackStack() }) }) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(PdigTokens.SpaceLg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceMd),
        ) {
            scenarios.forEach { t ->
                PdigCard(onClick = { nav.navigate(Route.SCENARIO_SETUP.replace("{templateId}", t.id)) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs)) {
                        Text(t.title, style = PdigTokens.BodyStrong)
                        Text(
                            t.description,
                            style = PdigTokens.Caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineScreen(nav: NavController) {
    val context = LocalContext.current
    val container = remember { AppContainer.get(context) }
    var items by remember { mutableStateOf<List<TimelineItem>?>(null) }
    LaunchedEffect(Unit) { items = withContext(Dispatchers.IO) { container.timeline() } }

    Scaffold(topBar = { PdigTopBar("即将到来", onBack = { nav.popBackStack() }) }) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(PdigTokens.SpaceLg)
                .verticalScroll(rememberScrollState()),
        ) {
            when {
                items == null -> LoadingState()
                items!!.isEmpty() -> EmptyState("还没有需要跟踪的事项。")
                else -> items!!.forEach { item ->
                    PdigCard(onClick = { nav.navigate(Route.PLAN.replace("{planId}", item.sourceId)) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs)) {
                            Text(item.title, style = PdigTokens.BodyStrong)
                            Text(item.subtitle, style = PdigTokens.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(PdigTokens.SpaceSm)) {
                                StatusChip(item.status)
                                Text(
                                    when (item.bucket) {
                                        "attention" -> "需要关注"
                                        "overdue" -> "已过期"
                                        "today" -> "今天"
                                        "7d" -> "7 天内"
                                        "30d" -> "30 天内"
                                        "90d" -> "90 天内"
                                        else -> "以后"
                                    },
                                    style = PdigTokens.Label,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
