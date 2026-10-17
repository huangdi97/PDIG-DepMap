package com.pdig.uivnext.ui.r9

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.demo.demoCards
import com.pdig.uivnext.demo.demoNumbers
import com.pdig.uivnext.demo.demoRegions
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.screens.SearchResult
import com.pdig.uivnext.ui.screens.openSearchResult
import com.pdig.uivnext.ui.screens.searchResults

/** Preview-only global lookup of recorded synthetic objects, not a discovery engine. */
@Composable
internal fun R9SearchScreen(app: VAppState) {
    var query by remember { mutableStateOf("") }
    val q = query.trim()
    val navigationHits = if (q.isEmpty()) {
        emptyList()
    } else {
        searchResults(q, app).filterIsInstance<SearchResult.NavigationHit>()
    }
    val filteredCards = app.demoCards().filter { card ->
        val lifecycle = UiVNextDemoFixture.cardLifecycleFor(card.id)
        q.isNotEmpty() && (
            listOf(card.nickname, card.issuer, card.last4, card.currency) +
                listOfNotNull(
                    lifecycle?.annualFee, lifecycle?.annualFeeDue, lifecycle?.billingDay,
                    lifecycle?.paymentDueDay, lifecycle?.installmentSummary,
                )
            ).any { value -> value.contains(q, ignoreCase = true) }
    }
    val filteredNumbers = app.demoNumbers().filter { number ->
        val lifecycle = UiVNextDemoFixture.numberLifecycleFor(number.id)
        val role = when(number.role) {
            "primary" -> "主号"
            "keep" -> "保号"
            else -> "副号"
        }
        q.isNotEmpty() && (
            listOf(app.numberDisplayName(number.id, number.maskedNumber), number.maskedNumber,
                number.carrier, number.countryCode, role) +
                listOfNotNull(
                    lifecycle?.billingMode, lifecycle?.planCost, lifecycle?.keepAliveDue,
                    lifecycle?.keepAliveCycle, lifecycle?.lastKeepAlive, lifecycle?.renewalMethod,
                )
            ).any { value -> value.contains(q, ignoreCase = true) }
    }
    val filteredRegions = app.demoRegions().filter {
        q.isNotEmpty() && (it.displayName.contains(q, ignoreCase = true) ||
            it.regionCode.contains(q, ignoreCase = true))
    }
    val filteredServices = if(app.emptyDemo) emptyList() else UiVNextDemoFixture.services.filter {
        q.isNotEmpty() && it.name.contains(q, ignoreCase = true)
    }
    val filteredAccounts = if (app.emptyDemo) emptyList() else UiVNextDemoFixture.accounts.filter {
        q.isNotEmpty() && listOfNotNull(it.name, it.provider, it.maskedIdentifier)
            .any { value -> value.contains(q, ignoreCase = true) }
    }
    val filteredEmails = if (app.emptyDemo) emptyList() else UiVNextDemoFixture.emails.filter {
        q.isNotEmpty() && listOfNotNull(it.name, it.provider, it.maskedAddress)
            .any { value -> value.contains(q, ignoreCase = true) }
    }
    val filteredDevices = if (app.emptyDemo) emptyList() else UiVNextDemoFixture.devices.filter {
        q.isNotEmpty() && listOfNotNull(it.name, it.platform, it.kind)
            .any { value -> value.contains(q, ignoreCase = true) }
    }
    val total = navigationHits.size + filteredCards.size + filteredNumbers.size + filteredRegions.size +
        filteredServices.size + filteredAccounts.size + filteredEmails.size + filteredDevices.size

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 13.dp, vertical = 12.dp)
            .testTag("pdig.r9.screen.search"),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Text("搜索数字基础设施", color = R9.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("查找已记录的卡片、号码、账户、邮箱、设备、地区和服务；未找到不代表不存在。",
            color = R9.Muted, fontSize = 11.sp, lineHeight = 17.sp)

        Surface(
            modifier = Modifier.fillMaxWidth().testTag("pdig.search.field"),
            color = Color.White, shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, R9.Line),
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.Search, contentDescription = null,
                    tint = R9.Blue, modifier = Modifier.size(19.dp))
                Box(Modifier.weight(1f)) {
                    if(query.isEmpty()) Text("搜索名称、机构、尾号或国家地区",
                        fontSize = 12.sp, color = R9.Muted)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        textStyle = TextStyle(color = R9.Ink, fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            }
        }
        if (q.isBlank()) {
            R9SectionTitle("快捷前往")
            listOf(
                VScreen.NOW, VScreen.OVERVIEW, VScreen.CARDS, VScreen.NUMBERS,
                VScreen.CHANGE_PHONE, VScreen.RECORDS,
            ).chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { screen ->
                        Surface(
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).clickable {
                                app.navigateFromSearch(screen)
                            }, color = Color.White, shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, R9.Line),
                        ) {
                            Text(screen.titleZh, Modifier.padding(15.dp),
                                color = R9.Blue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if(pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        } else {
            R9SectionTitle("搜索结果（$total）")
            navigationHits.forEach { hit ->
                R9SearchResult(
                    hit.title,
                    hit.subtitle,
                    "页面",
                ) { openSearchResult(hit, app) }
            }
            filteredCards.forEach { card ->
                R9SearchResult(card.nickname, "${card.issuer} · ${regionFlag(card.region)} · ${r9VisibleLast4(card.last4, app.privacyMask || app.savedPresentationProfile("card", card.id)?.maskSensitive == true)}",
                    "卡片") { app.openCard(card.id) }
            }
            filteredNumbers.forEach { number ->
                R9SearchResult(app.numberDisplayNameForScreen(number.id, number.maskedNumber),
                    "${r9VisibleNumber(number.maskedNumber, app.privacyMask || app.savedPresentationProfile("phoneNumber", number.id)?.maskSensitive == true)} · ${regionFlag(number.region)} · ${number.carrier}",
                    "号码") { app.openNumber(number.id) }
            }
            filteredRegions.forEach { region ->
                R9SearchResult(region.displayName, "${region.cardCount} 张卡 · ${region.phoneCount} 个号码",
                    "地区") {
                    app.selectRegion(region.regionCode)
                    app.navigateFromSearch(VScreen.OVERVIEW)
                }
            }
            filteredAccounts.forEach { account ->
                R9SearchResult(account.name,
                    "${regionFlag(account.region)} ${account.provider} · ${if (app.privacyMask) "标识已遮蔽" else account.maskedIdentifier}",
                    "账户") {
                    app.selectRegion(account.region)
                    app.navigateFromSearch(VScreen.ACCOUNTS)
                }
            }
            filteredEmails.forEach { mail ->
                R9SearchResult(mail.name,
                    "${regionFlag(mail.region)} ${mail.provider} · ${if (app.privacyMask) "邮箱已遮蔽" else mail.maskedAddress}",
                    "邮箱") {
                    app.selectRegion(mail.region)
                    app.navigateFromSearch(VScreen.EMAILS)
                }
            }
            filteredDevices.forEach { device ->
                R9SearchResult(device.name,
                    "${regionFlag(device.region)} ${device.platform} · ${device.kind}",
                    "设备") {
                    app.selectRegion(device.region)
                    app.navigateFromSearch(VScreen.DEVICES)
                }
            }
            filteredServices.forEach { service ->
                R9SearchResult(service.name, regionFlag(service.region) + " · 已记录服务",
                    "服务") {
                    app.selectRegion(service.region)
                    app.navigateFromSearch(VScreen.SERVICES)
                }
            }
            if(total == 0) {
                Surface(color = Color.White, shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, R9.Line)) {
                    Text("没有匹配的已记录对象。未知仍为未知。",
                        Modifier.fillMaxWidth().padding(17.dp), color = R9.Muted, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun R9SearchResult(title: String, detail: String, kind: String, action: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { action() }
            .testTag("pdig.search.result"),
        color = Color.White, shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, R9.Line),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, color = R9.Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(detail, color = R9.Muted, fontSize = 11.sp, maxLines = 1)
            }
            R9Badge(kind, R9.Blue)
        }
    }
}
