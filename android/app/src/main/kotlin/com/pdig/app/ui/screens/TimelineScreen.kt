package com.pdig.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.pdig.app.data.AppContainer
import com.pdig.app.ui.Route
import com.pdig.app.ui.components.EmptyState
import com.pdig.app.ui.components.LoadingState
import com.pdig.app.ui.components.PdigCard
import com.pdig.app.ui.components.PdigTopBar
import com.pdig.app.ui.components.StatusChip
import com.pdig.app.ui.theme.PdigTokens
import com.pdig.core.timeline.TimelineItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
                items?.isEmpty() == true -> EmptyState("还没有需要跟踪的事项。")
                else -> items?.forEach { item ->
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
