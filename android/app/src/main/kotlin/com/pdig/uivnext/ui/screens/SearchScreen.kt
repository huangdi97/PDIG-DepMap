package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.LabelChip
import com.pdig.uivnext.ui.components.SectionHeader

@Composable
fun SearchScreen(app: VAppState, breakpoint: MediaBreakpoint) {
    var query by remember { mutableStateOf("") }
    val trimmed = query.trim()

    Column(
        Modifier
            .fillMaxSize()
            .padding(pagePadding(breakpoint))
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "搜索与快捷操作",
            color = PdigV2Colors.TextPrimary,
            fontSize = pageTitleSize(breakpoint),
            fontWeight = FontWeight.Bold,
        )
        Text(
            "从一个入口查找卡片、号码、地区、服务与页面；搜索结果只来自已记录的基础设施。",
            color = PdigV2Colors.TextSecondary,
            fontSize = 13.sp,
        )
        SearchField(query, onQuery = { query = it })

        if (trimmed.isEmpty()) {
            SectionHeader("快捷前往")
            commandTargets.forEach { target ->
                CommandRow(target.title, target.hint) {
                    app.navigateFromSearch(target.screen)
                }
            }
        } else {
            SectionHeader("搜索结果")
            val results = searchResults(trimmed)
            if (results.isEmpty()) {
                Text(
                    "没有匹配「$trimmed」。未记录 ≠ 无风险：可以换个关键词继续搜索。",
                    color = PdigV2Colors.TextSecondary,
                    fontSize = 13.sp,
                )
            } else {
                results.forEach { result -> ResultRow(result, app) }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTagLocal("pdig.search.field"),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.Primary.copy(alpha = 0.20f)),
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = PdigV2Colors.TextMuted,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        "搜索卡片、号码、地区、服务或页面",
                        color = PdigV2Colors.TextMuted,
                        fontSize = 14.sp,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQuery,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = PdigV2Colors.TextPrimary,
                        fontSize = 14.sp,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}


@Composable
private fun CommandRow(title: String, hint: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickableLocal(onClick = onClick)
            .testTagLocal("pdig.search.command.$title"),
        color = PdigV2Colors.Surface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = PdigV2Colors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(hint, color = PdigV2Colors.TextMuted, fontSize = 11.sp)
            }
            LabelChip("前往")
        }
    }
}

@Composable
private fun ResultRow(result: SearchResult, app: VAppState) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickableLocal { openSearchResult(result, app) }
            .testTagLocal("pdig.search.result"),
        color = PdigV2Colors.Surface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(result.title, color = PdigV2Colors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(result.subtitle, color = PdigV2Colors.TextMuted, fontSize = 12.sp)
            }
            LabelChip(result.kind)
        }
    }
}

