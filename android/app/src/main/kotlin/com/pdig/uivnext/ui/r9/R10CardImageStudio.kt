package com.pdig.uivnext.ui.r9

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.ui.VAppState

/**
 * User-facing editor = one picture, one preview, one Save. Not a design-engineering
 * tool; old material/layout/accent controls are intentionally absent.
 */
@Composable
internal fun R10CardImageStudio(app: VAppState) {
    val card = app.selectedCardId?.let(UiVNextDemoFixture::cardById)
    if (app.emptyDemo || card == null) {
        Text("没有已记录卡片可以选择卡面图片。",
            Modifier.padding(20.dp), color = R9.Muted)
        return
    }
    val ctx = LocalContext.current
    val saved = app.savedPresentationProfile("card", card.id)
        ?: PresentationProfile.defaultFor("card", card.id, card.preset)
    // Evidence harness may seed one of the actual consumer art choices. Legacy
    // engineering theme ids (glass/city/material/layout) are intentionally ignored.
    val evidenceArt = app.evidenceThemeId?.takeIf { id ->
        R10_ART_CHOICES.any { it.first == id }
    }
    var editing by remember(card.id, evidenceArt) {
        mutableStateOf(if (evidenceArt != null) r10ArtProfile(saved, evidenceArt) else saved)
    }
    var importError by remember(card.id) { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val name = importCardArt(ctx, uri)
            if (name == null) {
                importError = true
            } else {
                editing = editing.copy(
                    backgroundKind = "local-image",
                    backgroundValue = name,
                )
                importError = false
            }
        }
    }
    val isChanged = editing != saved
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 15.dp)
            .testTag("pdig.r10.screen.card-image"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("选择卡面图片", color = R9.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(card.nickname + " · 一张图片即可",
                    color = R9.Muted, fontSize = 11.sp, maxLines = 1)
            }
            // Always visible above the preview and artwork grid; saving must never
            // depend on scrolling to an off-screen footer.
            Surface(
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    .clickable {
                        if(isChanged) app.savePresentationProfile(editing)
                        app.back()
                    }.testTag("pdig.r10.card-art.save"),
                color = R9.Blue, shape = RoundedCornerShape(13.dp),
            ) {
                Text(if(isChanged) "保存卡面并返回" else "返回卡片",
                    Modifier.padding(horizontal = 10.dp, vertical = 13.dp),
                    color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().testTag(VTestIds.CUSTOMIZATION_PREVIEW),
            color = R9.Ice, shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, R9.Line),
        ) {
            Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                R10CardFace(
                    card = card, privacyMask = app.privacyMask || editing.maskSensitive,
                    profile = editing, modifier = Modifier.fillMaxWidth(),
                )
                Text("预览 · 图片只改变外观，不影响卡片或账户资料",
                    color = R9.Muted, fontSize = 11.sp)
            }
        }
        R9SectionTitle("卡面图片")
        // Real gallery first: it is the simplest, most direct customization.
        Surface(
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 54.dp)
                .clickable { picker.launch("image/*") }
                .testTag("pdig.r10.card-art.choose-photo"),
            color = R9.Mist, shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, R9.Blue.copy(alpha = .25f)),
        ) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("从手机相册选择图片", color = R9.Blue,
                    fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("＋", color = R9.Blue, fontSize = 19.sp)
            }
        }
        if (importError) {
            Text("图片无法读取或超过 12MB，请换一张照片。",
                color = R9.Rose, fontSize = 11.sp)
        }
        Text("也可以直接选一款简洁卡面", color = R9.Muted, fontSize = 11.sp)
        R10ArtGrid(
            selected = selectedCardArt(editing),
            cardId = card.id,
        ) { art ->
            editing = r10ArtProfile(editing, art)
            importError = false
        }
        Text("卡面图片仅存储在本机。预览版不同源码版本使用独立安装空间；更换安装包不会自动同步图片。",
            color = R9.Muted, fontSize = 10.sp, lineHeight = 16.sp)
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun R10ArtGrid(selected: String, cardId: String, onChoose: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().testTag(VTestIds.CUSTOMIZATION_LIBRARY),
        verticalArrangement = Arrangement.spacedBy(9.dp)) {
        R10_ART_CHOICES.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (art, label) ->
                    val chosen = art == selected
                    Surface(
                        modifier = Modifier.weight(1f).clickable { onChoose(art) }
                            .testTag("pdig.r10.card-art.choice.$art"),
                        color = if(chosen) R9.Mist else Color.White,
                        shape = RoundedCornerShape(15.dp),
                        border = BorderStroke(if(chosen) 2.dp else 1.dp,
                            if(chosen) R9.Blue else R9.Line),
                    ) {
                        Column(Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            // Independent color artwork thumbnails. No misleading bank logos.
                            val tone = when(art) {
                                "ocean" -> Color(0xFF087DBC)
                                "sky" -> Color(0xFF87BEEB)
                                "coral" -> Color(0xFFE68F9A)
                                "night" -> Color(0xFF222656)
                                else -> Color(0xFF385FA1)
                            }
                            Surface(color = tone, shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(51.dp)) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                                    Text("◉", Modifier.padding(6.dp), fontSize = 16.sp,
                                        color = Color.White.copy(alpha = .88f))
                                }
                            }
                            Text(label, color = if(chosen) R9.Blue else R9.Ink,
                                fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
