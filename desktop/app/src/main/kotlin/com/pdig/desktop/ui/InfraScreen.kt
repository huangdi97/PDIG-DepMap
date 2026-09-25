package com.pdig.desktop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pdig.app.data.NodeRow
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.PdigPage

/** 基础设施：左侧节点列表 + 右侧选中对象详情（Reality 只读；修改请前往对象详情）。 */
@Composable
fun InfraScreen(ui: UiState) {
    val nodes = ui.session.graph.nodes()
    var selectedId by remember { mutableStateOf<String?>(null) }
    PdigPage(
        title = "基础设施",
        subtitle = "节点与依赖（Reality 只读；修改请前往对象详情）",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        if (nodes.isEmpty()) {
            EmptyState("还没有节点。先前往「数据来源 → 导入」添加账单，或在待确认服务中确认节点。")
        } else {
            Row(Modifier.fillMaxSize()) {
                NodeListPane(nodes, selectedId, onSelect = { selectedId = it })
                NodeDetailPane(ui, selectedId)
            }
        }
    }
}

/** 左侧：可滚动的节点列表，选中项高亮。 */
@Composable
private fun NodeListPane(nodes: List<NodeRow>, selectedId: String?, onSelect: (String) -> Unit) {
    Column(
        Modifier
            .width(240.dp)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(end = 12.dp),
    ) {
        nodes.forEach { n ->
            val selected = n.id == selectedId
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(n.id) },
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
            ) {
                Column(Modifier.padding(10.dp)) {
                    Text(
                        n.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    )
                    Text(
                        kindLabel(n.kind),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}
