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
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.LabelChip
import com.pdig.uivnext.ui.components.SectionHeader

/** 搜索与快捷操作：真实对象搜索 + 可执行页面跳转。 */
@Composable
fun SearchScreen(app: VAppState) {
    var query by remember { mutableStateOf("") }
    val trimmed = query.trim()

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "搜索与快捷操作",
            color = PdigV2Colors.TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
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
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
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

private data class CommandTarget(
    val screen: VScreen,
    val title: String,
    val hint: String,
    val aliases: List<String> = emptyList(),
)

private val commandTargets = listOf(
    CommandTarget(VScreen.OVERVIEW, "基础设施总览", "全球基础设施与地区活动", listOf("总览", "基础设施", "地球", "地区")),
    CommandTarget(VScreen.CARDS, "卡片", "支付基础设施与绑定关系", listOf("银行卡", "支付")),
    CommandTarget(VScreen.NUMBERS, "号码", "通信身份与恢复依赖", listOf("手机号", "电话", "sim", "esim")),
    CommandTarget(VScreen.ACCOUNTS, "账户", "账户规模与登录恢复关系", listOf("账号")),
    CommandTarget(VScreen.EMAILS, "邮箱", "邮箱身份与恢复角色", listOf("邮件", "email")),
    CommandTarget(VScreen.DEVICES, "设备", "验证器、可信终端与恢复设备", listOf("手机", "电脑")),
    CommandTarget(VScreen.SERVICES, "服务", "订阅、支付与验证服务", listOf("订阅")),
    CommandTarget(VScreen.WEAKNESSES, "薄弱点", "恢复、到期与迁移风险", listOf("风险", "恢复")),
    CommandTarget(VScreen.CHANGE_PHONE, "更换手机号", "规划并迁移号码", listOf("变更", "迁移", "换号")),
    CommandTarget(VScreen.RECORDS, "记录", "变更、关注与时间节点", listOf("历史", "时间线")),
    CommandTarget(VScreen.SOURCES, "数据源", "当前工作区的数据边界", listOf("来源", "数据")),
    CommandTarget(VScreen.PERSONALIZATION, "设置 · 个性化", "外观、隐私与动效偏好", listOf("设置", "隐私", "个性化")),
)

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

private sealed class SearchResult {
    abstract val title: String
    abstract val subtitle: String
    abstract val kind: String

    data class NavigationHit(
        val screen: VScreen,
        override val title: String,
        override val subtitle: String,
    ) : SearchResult() {
        override val kind = "页面"
    }

    data class CardHit(val id: String, override val title: String, override val subtitle: String) : SearchResult() {
        override val kind = "卡片"
    }

    data class NumberHit(val id: String, override val title: String, override val subtitle: String) : SearchResult() {
        override val kind = "号码"
    }

    data class ServiceHit(val id: String, override val title: String, override val subtitle: String) : SearchResult() {
        override val kind = "服务"
    }

    data class RegionHit(val code: String, override val title: String, override val subtitle: String) : SearchResult() {
        override val kind = "地区"
    }
}

private fun searchResults(q: String): List<SearchResult> {
    val query = q.lowercase()
    fun hit(values: List<String>): Boolean = values.any { it.lowercase().contains(query) }

    val out = mutableListOf<SearchResult>()

    commandTargets.forEach { target ->
        if (hit(listOf(target.title, target.hint) + target.aliases)) {
            out.add(SearchResult.NavigationHit(target.screen, target.title, target.hint))
        }
    }

    UiVNextDemoFixture.cards.forEach { card ->
        val region = regionLabel(card.region)
        if (hit(listOf(card.nickname, card.issuer, card.region, region, card.masked, card.network, card.currency))) {
            out.add(SearchResult.CardHit(card.id, card.nickname, "${card.issuer} · $region · ${card.masked}"))
        }
    }

    UiVNextDemoFixture.numbers.forEach { number ->
        val region = regionLabel(number.region)
        if (hit(listOf(number.nickname, number.carrier, number.region, region, number.countryCode, number.maskedNumber))) {
            out.add(
                SearchResult.NumberHit(
                    number.id,
                    number.nickname,
                    "${number.countryCode} · ${number.carrier} · ${number.maskedNumber}",
                ),
            )
        }
    }

    UiVNextDemoFixture.services.forEach { service ->
        val region = regionLabel(service.region)
        val role = searchServiceKindLabel(service.kind)
        if (hit(listOf(service.name, service.region, region, service.kind, role))) {
            out.add(SearchResult.ServiceHit(service.id, service.name, "$region · $role"))
        }
    }

    UiVNextDemoFixture.regionSummaries().forEach { region ->
        if (hit(listOf(region.displayName, region.regionCode))) {
            out.add(
                SearchResult.RegionHit(
                    region.regionCode,
                    region.displayName,
                    "${region.cardCount} 张卡 · ${region.phoneCount} 个号码",
                ),
            )
        }
    }

    return out.distinctBy { "${it.kind}:${it.title}" }
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

private fun onClick(result: SearchResult, app: VAppState) {
    when (result) {
        is SearchResult.CardHit -> app.openCard(result.id)
        is SearchResult.NumberHit -> app.openNumber(result.id)
        is SearchResult.ServiceHit -> app.navigateFromSearch(VScreen.SERVICES)
        is SearchResult.NavigationHit -> app.navigateFromSearch(result.screen)
        is SearchResult.RegionHit -> {
            app.selectRegion(result.code)
            app.navigateFromSearch(VScreen.OVERVIEW)
        }
    }
}

private fun searchServiceKindLabel(kind: String): String = when (kind) {
    "payment" -> "支付"
    "banking" -> "银行"
    "funding" -> "资金来源"
    "authenticates" -> "登录验证"
    "twoFA" -> "2FA 验证"
    "subscription" -> "订阅"
    else -> "关联服务"
}
