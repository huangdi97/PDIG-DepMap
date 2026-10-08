package com.pdig.uivnext.ui.r9

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.model.hexColorOrNull
import com.pdig.uivnext.model.serviceKindLabelZh
import com.pdig.uivnext.model.regionLabelZh
import com.pdig.uivnext.theme.statusLabelZh
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.AssetCard

/** R9 card object-first detail. Four tabs are real interactive states, not decoration. */
@Composable
internal fun R9CardDetailScreen(app: VAppState) {
    if (app.emptyDemo) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text("没有已记录卡片。未知不等于安全。", color = R9.Muted, fontSize = 12.sp)
        }
        return
    }
    val card = app.selectedCardId?.let { UiVNextDemoFixture.cardById(it) }
    if (card == null) {
        Text("未选择有效的卡片对象。请从卡片列表进入详情。", 
            Modifier.fillMaxWidth().padding(20.dp), color = R9.Muted, fontSize = 12.sp)
        return
    }
    val services = UiVNextDemoFixture.servicesForCard(card.id)
    val profile = app.savedPresentationProfile("card", card.id)
    var tab by rememberSaveable(card.id) { mutableIntStateOf(0) }
    var showSimpleArt by rememberSaveable(card.id) { mutableStateOf(false) }
    var importFailed by remember(card.id) { mutableStateOf(false) }
    val ctx = LocalContext.current
    val picturePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val name = importCardArt(ctx, uri)
            if (name == null) {
                importFailed = true
            } else {
                val current = app.savedPresentationProfile("card", card.id)
                    ?: PresentationProfile.defaultFor("card", card.id, card.preset)
                app.savePresentationProfile(current.copy(
                    backgroundKind = "local-image", backgroundValue = name,
                ))
                importFailed = false
            }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 13.dp, vertical = 12.dp)
        .testTag("pdig.r9.screen.card-detail"),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(card.nickname, color = R9.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            R9Badge("换卡面", R9.Blue, Modifier.defaultMinSize(minHeight = 48.dp)
                .clickable { picturePicker.launch("image/*") }
                .testTag("pdig.r11.card.change-image"))
        }
        Box(Modifier.testTag(VTestIds.CARD_DETAIL_IDENTITY)) {
            R10CardFace(card = card,
                privacyMask = app.privacyMask || (profile?.maskSensitive == true),
                profile = profile, modifier = Modifier.fillMaxWidth())
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            R9Badge("● ${statusLabelZh(card.status)}", r9CardStatusTint(card.status))
            Text("选预设图片 ›",
                Modifier.clickable { showSimpleArt = !showSimpleArt }
                    .padding(horizontal = 5.dp, vertical = 10.dp)
                    .testTag("pdig.r11.card.presets.toggle"),
                color = R9.Blue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        if (showSimpleArt) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                R10_ART_CHOICES.forEach { (art, label) ->
                    val selected = selectedCardArt(profile) == art
                    Surface(
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                            .clickable {
                                val current = app.savedPresentationProfile("card", card.id)
                                    ?: PresentationProfile.defaultFor("card", card.id, card.preset)
                                app.savePresentationProfile(r10ArtProfile(current, art))
                                showSimpleArt = false
                            }.testTag("pdig.r11.card.preset.$art"),
                        color = if (selected) R9.Mist else Color.White,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if(selected) R9.Blue else R9.Line),
                    ) {
                        Text(label, Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
                            fontSize = 11.sp, color = R9.Ink)
                    }
                }
            }
        }
        if (importFailed) Text("图片读取失败或超过 12MB，请换一张。",
            color = R9.Rose, fontSize = 11.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("概览", "关联服务", "账单", "安全与风险").forEachIndexed { index, label ->
                val active = tab == index
                Surface(
                    modifier = Modifier.weight(1f).height(48.dp).clickable { tab = index }
                        .testTag("pdig.card.detail.tab.${index}"),
                    color = if(active) R9.Mist else Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if(active) R9.Blue.copy(alpha = .25f) else R9.Line),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(label, color = if(active) R9.Blue else R9.Muted,
                            fontSize = 10.sp, fontWeight = if(active) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1)
                    }
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().testTag(VTestIds.CARD_DETAIL_INFO),
            color = Color.White, shape = RoundedCornerShape(19.dp),
            border = BorderStroke(1.dp, R9.Line),
        ) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when(tab) {
                    0 -> {
                        Text("基本信息", color = R9.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        R9DetailLine("发卡机构", card.issuer)
                        R9DetailLine("卡片类型", if(card.type == "credit") "信用卡" else "储蓄卡")
                        R9DetailLine("国家 / 地区", "${regionFlag(card.region)} ${regionLabelZh(card.region)} · ${card.currency}")
                        R9DetailLine("卡号后四位", r9VisibleLast4(card.last4, app.privacyMask || profile?.maskSensitive == true))
                        R9DetailLine("有效期", card.expiry)
                        R9DetailLine("已记录服务", "${services.size} 项")
                    }
                    1 -> {
                        Text("关联服务（${services.size}）",
                            color = R9.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        if(services.isEmpty()) Text("尚无已确认关系；未知不等于安全。",
                            color = R9.Muted, fontSize = 12.sp)
                        services.chunked(3).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                row.forEach { service ->
                                    Surface(modifier = Modifier.weight(1f),
                                        color = R9.Ice, shape = RoundedCornerShape(13.dp),
                                        border = BorderStroke(1.dp, R9.Line)) {
                                        Column(Modifier.padding(9.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally) {
                                            Surface(shape = RoundedCornerShape(8.dp), color = R9.Blue) {
                                                Text(service.name.take(1), Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            }
                                            Text(service.name, color = R9.Ink, fontSize = 10.sp, maxLines = 1)
                                            Text(serviceKindLabelZh(service.kind), color = R9.Muted,
                                                fontSize = 9.sp, maxLines = 1)
                                        }
                                    }
                                }
                                repeat(3-row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                    2 -> {
                        Text("账单", color = R9.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("尚未导入可核验的账单，不推断交易、余额或扣款金额。",
                            color = R9.Muted, fontSize = 12.sp, lineHeight = 19.sp)
                        R9DetailLine("已记录有效期", card.expiry)
                    }
                    else -> {
                        Text("安全与风险", color = R9.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(if(card.status == "expiring_soon")
                            "卡片临近到期，已记录 ${services.size} 项相关服务需要核查。"
                            else "当前状态：${statusLabelZh(card.status)}。未记录的支付关系仍为未知。",
                            color = R9.Muted, fontSize = 12.sp, lineHeight = 19.sp)
                        R9DetailLine("关联服务", "${services.size} 项")
                    }
                }
            }
        }
        // Image selection is a minor card feature, not a second full-screen product.
        // Editing stays in the current detail workspace with an inline action.
    }
}

@Composable
private fun R9DetailLine(key: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(key, color = R9.Muted, fontSize = 11.sp)
        Text(value, color = R9.Ink, fontSize = 11.sp, maxLines = 1)
    }
}
