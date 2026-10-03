package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.LabelChip
import com.pdig.uivnext.ui.components.SectionHeader

/**
 * Search / Command（任务书 §23-24）：
 * 触控可发现的入口（TopCommandBar），真实搜索（card / number / region / service / change），
 * 不做假搜索 UI；导航命令（Cards / Numbers / Overview / Change Phone / Records / Settings）常驻。
 */
@Composable
fun SearchScreen(app: VAppState) {
    var query by remember { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("搜索 / 命令", color = PdigV2Colors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        SearchField(query, onQuery = { query = it })

        // 导航命令（query 为空时重点展示；触控可发现）
        SectionHeader("前往")
        CommandRow("基础设施总览", "全球基础设施与地区活动") { app.navigate(VScreen.OVERVIEW) }
        CommandRow("卡片", "全球 ${UiVNextDemoFixture.cards.size} 张卡") { app.navigate(VScreen.CARDS) }
        CommandRow("号码", "全球 ${UiVNextDemoFixture.numbers.size} 个号码") { app.navigate(VScreen.NUMBERS) }
        CommandRow("更换手机号", "规划并迁移号码") { app.navigate(VScreen.CHANGE_PHONE) }
        CommandRow("记录", "查看变更、关注与即将发生的事项") { app.navigate(VScreen.RECORDS) }
        CommandRow("薄弱点", "查看恢复、到期与迁移风险") { app.navigate(VScreen.WEAKNESSES) }
        CommandRow("数据源", "查看当前工作区的数据边界") { app.navigate(VScreen.SOURCES) }
        CommandRow("设置 · 个性化", "外观、隐私与动效偏好") { app.navigate(VScreen.PERSONALIZATION) }

        // 真实搜索（不匹配时明示，不做假结果）
        val trimmed = query.trim()
        if (trimmed.isNotEmpty()) {
            SectionHeader("搜索结果")
            val results = searchResults(trimmed)
            if (results.isEmpty()) {
                Text(
                    "没有匹配「$trimmed」。未记录 ≠ 无风险：换个关键词，或使用上方导航命令。",
                    color = PdigV2Colors.TextSecondary,
                    fontSize = 13.sp,
                )
            } else {
                results.forEach { r -> ResultRow(r, app) }
            }
        }
    }
}

/** 搜索字段（PDIG 风格：低噪声边框 + 搜索图标；不依赖 Material 控件外观）。 */
@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTagLocal("pdig.search.field"),
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = PdigV2Colors.TextMuted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("搜索卡片 / 号码 / 地区 / 服务 / 变更", color = PdigV2Colors.TextMuted, fontSize = 14.sp)
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

/** 导航命令行。 */
@Composable
private fun CommandRow(title: String, hint: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickableLocal(onClick = onClick)
            .testTagLocal("pdig.search.command.${title}"),
        color = PdigV2Colors.Surface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(title, color = PdigV2Colors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(hint, color = PdigV2Colors.TextMuted, fontSize = 11.sp)
            }
            LabelChip("前往")
        }
    }
}

/** 搜索结果模型（title/subtitle 抽象于基类，供列表渲染）。 */
private sealed class SearchResult {
    abstract val title: String
    abstract val subtitle: String

    data class CardHit(val id: String, override val title: String, override val subtitle: String) : SearchResult()
    data class NumberHit(val id: String, override val title: String, override val subtitle: String) : SearchResult()
    data class ServiceHit(val id: String, override val title: String, override val subtitle: String) : SearchResult()
    data class RegionHit(val code: String, override val title: String, override val subtitle: String) : SearchResult()
}
private fun searchResults(q: String): List<SearchResult> {
    val query = q.lowercase()
    fun hit(haystacks: List<String>, lower: Boolean = true): Boolean =
        haystacks.any { (if (lower) it.lowercase() else it).contains(query) }

    val out = mutableListOf<SearchResult>()
    UiVNextDemoFixture.cards.forEach { c ->
        if (hit(listOf(c.nickname, c.issuer, c.region, c.masked, c.network, c.currency))) {
            out.add(SearchResult.CardHit(c.id, c.nickname, "${c.issuer} · ${c.region} · ${c.masked}"))
        }
    }
    UiVNextDemoFixture.numbers.forEach { n ->
        if (hit(listOf(n.nickname, n.carrier, n.region, n.countryCode, n.maskedNumber))) {
            out.add(SearchResult.NumberHit(n.id, n.nickname, "${n.countryCode} · ${n.carrier} · ${n.maskedNumber}"))
        }
    }
    UiVNextDemoFixture.services.forEach { s ->
        if (hit(listOf(s.name, s.region, s.kind))) {
            out.add(SearchResult.ServiceHit(s.id, s.name, "地区 ${s.region} · ${searchServiceKindLabel(s.kind)}"))
        }
    }
    UiVNextDemoFixture.regionSummaries().forEach { r ->
        if (hit(listOf(r.displayName, r.regionCode))) {
            out.add(SearchResult.RegionHit(r.regionCode, r.displayName, "${r.cardCount} 张卡 · ${r.phoneCount} 个号码"))
        }
    }
    return out
}

@Composable
private fun ResultRow(result: SearchResult, app: VAppState) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickableLocal { onClick(result, app) }
            .testTagLocal("pdig.search.result"),
        color = PdigV2Colors.Surface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(result.title, color = PdigV2Colors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(result.subtitle, color = PdigV2Colors.TextMuted, fontSize = 12.sp)
        }
    }
}

private fun onClick(result: SearchResult, app: VAppState) {
    when (result) {
        is SearchResult.CardHit -> app.openCard(result.id)
        is SearchResult.NumberHit -> app.openNumber(result.id)
        is SearchResult.ServiceHit -> app.navigate(VScreen.SERVICES)
        is SearchResult.RegionHit -> {
            app.selectRegion(result.code)
            app.navigate(VScreen.OVERVIEW)
        }
    }
}


private fun searchServiceKindLabel(kind: String): String = when (kind) {
    "funding" -> "资金来源"
    "authenticates" -> "登录验证"
    "twoFA" -> "2FA 验证"
    "subscription" -> "订阅"
    else -> "关联服务"
}
