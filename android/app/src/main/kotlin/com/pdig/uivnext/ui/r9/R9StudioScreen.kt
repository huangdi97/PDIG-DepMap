package com.pdig.uivnext.ui.r9

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.CARD_MATERIAL_CHOICES
import com.pdig.uivnext.model.CARD_THEME_PRESETS
import com.pdig.uivnext.model.NUMBER_MATERIAL_CHOICES
import com.pdig.uivnext.model.NUMBER_THEME_PRESETS
import com.pdig.uivnext.model.PRESENTATION_ACCENT_CHOICES
import com.pdig.uivnext.model.PRESENTATION_LAYOUT_CHOICES
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.model.hexColorOrNull
import com.pdig.uivnext.model.layoutLabelZh
import com.pdig.uivnext.model.materialLabelZh
import com.pdig.uivnext.model.themeLabelZh
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.AssetCard
import com.pdig.uivnext.ui.components.NumberFace

/** R9 consumer Studio: preview FIRST, then true theme/material/layout and privacy controls. */
@Composable
internal fun R9StudioScreen(app: VAppState, isCard: Boolean) {
    val target = if (isCard) "card" else "phoneNumber"
    if (app.emptyDemo) {
        Text("没有已记录对象可以定制；未知不等于安全。",
            Modifier.fillMaxWidth().padding(20.dp), color = R9.Muted, fontSize = 12.sp)
        return
    }
    val card = if (isCard) UiVNextDemoFixture.cardById(app.selectedCardId ?: "card-cn-1") else null
    val number = if (!isCard) UiVNextDemoFixture.numberById(app.selectedNumberId ?: "num-cn-1") else null
    val id = if (isCard) card?.id else number?.id
    if (id == null) {
        Text("找不到当前对象。", Modifier.padding(18.dp), color = R9.Muted)
        return
    }
    val original = PresentationProfile.defaultFor(target, id, card?.preset ?: number?.preset ?: "minimal")
    val saved = app.savedPresentationProfile(target, id) ?: original
    var profile by remember(id, saved.themeId) { mutableStateOf(saved) }
    val dirty = profile != saved
    val presets = if (isCard) CARD_THEME_PRESETS else NUMBER_THEME_PRESETS
    val materials = if (isCard) CARD_MATERIAL_CHOICES else NUMBER_MATERIAL_CHOICES
    val display = if (isCard) card!!.nickname else number!!.nickname

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 13.dp, vertical = 12.dp)
            .testTag("pdig.r9.screen.studio"),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(if(isCard) "卡面定制" else "号码面定制", fontSize = 18.sp,
                    color = R9.Ink, fontWeight = FontWeight.Bold)
                Text(display, fontSize = 11.sp, color = R9.Muted)
            }
            Surface(
                modifier = Modifier.clickable(enabled = dirty) {
                    if (dirty) app.savePresentationProfile(profile)
                }.testTag("pdig.r9.studio.save"),
                color = if(dirty) R9.Blue else R9.Green.copy(alpha = .14f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(if(dirty) "保存外观" else "已保存",
                    Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = if(dirty) Color.White else R9.Green)
            }
        }

        // Preview dominates the screen. This renders the SAME presentation profile
        // that will later appear in object detail; no fake screenshot substitutions.
        Surface(
            modifier = Modifier.fillMaxWidth().testTag(VTestIds.CUSTOMIZATION_PREVIEW),
            color = R9.Ice, shape = RoundedCornerShape(21.dp),
            border = BorderStroke(1.dp, R9.Line),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                R9SectionTitle("实时预览")
                if (card != null) {
                    val hasChanges = profile != original
                    if (!hasChanges && app.savedPresentationProfile("card", card.id) == null) {
                        R9BankCardFace(
                            card = card, privacyMask = app.privacyMask || profile.maskSensitive,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        AssetCard(
                            card = card.copy(preset = profile.themeId),
                            privacyMask = app.privacyMask || profile.maskSensitive,
                            onClick = {}, modifier = Modifier.fillMaxWidth(),
                            presentationMaterial = profile.material,
                            presentationAccent = hexColorOrNull(profile.accentColor),
                            presentationLayout = profile.layout,
                        )
                    }
                } else if (number != null) {
                    NumberFace(
                        number = number.copy(preset = profile.themeId),
                        privacyMask = app.privacyMask || profile.maskSensitive,
                        onClick = {}, modifier = Modifier.fillMaxWidth(),
                        presentationMaterial = profile.material,
                        presentationAccent = hexColorOrNull(profile.accentColor),
                        presentationLayout = profile.layout,
                    )
                }
                Text("外观和遮蔽只影响本机展示，不会改写真实资产、关联关系或存储的身份信息。",
                    color = R9.Muted, fontSize = 10.sp, lineHeight = 17.sp)
            }
        }

        R9StudioChoices(
            title = "选择主题",
            choices = presets.map { it to themeLabelZh(if(isCard) "card" else "number", it) },
            selected = profile.themeId,
            testPrefix = "pdig.r9.studio.theme",
        ) { choice ->
            profile = profile.copy(themeId = choice, backgroundValue = choice)
            if (app.evidenceThemeId != null) app.evidenceThemeId = choice
        }
        R9StudioChoices("材质",
            materials.map { it to materialLabelZh(it) }, profile.material,
            "pdig.r9.studio.material") { profile = profile.copy(material = it) }
        R9StudioChoices("布局",
            PRESENTATION_LAYOUT_CHOICES.map { it to layoutLabelZh(it) }, profile.layout,
            "pdig.r9.studio.layout") { profile = profile.copy(layout = it) }
        R9StudioChoices("强调色",
            PRESENTATION_ACCENT_CHOICES.map { it.value to it.label }, profile.accentColor,
            "pdig.r9.studio.accent") { profile = profile.copy(accentColor = it) }

        Surface(
            modifier = Modifier.fillMaxWidth().clickable {
                profile = profile.copy(maskSensitive = !profile.maskSensitive)
            }.testTag("pdig.r9.studio.mask"),
            color = Color.White, shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, R9.Line),
        ) {
            Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("单独遮蔽敏感信息", fontSize = 12.sp, color = R9.Ink,
                    fontWeight = FontWeight.SemiBold)
                R9Badge(if(profile.maskSensitive) "已开启" else "未开启",
                    if(profile.maskSensitive) R9.Green else R9.Muted)
            }
        }
        if (app.privacyMask) {
            Text("全局隐私遮蔽正在生效：关闭此对象的单独遮蔽也不会覆盖全局保护。",
                color = R9.Muted, fontSize = 10.sp, lineHeight = 16.sp)
        }
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { profile = original },
            color = R9.Mist, shape = RoundedCornerShape(13.dp),
        ) {
            Text("恢复原始外观", Modifier.padding(13.dp),
                color = R9.Blue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(9.dp))
    }
}

@Composable
private fun R9StudioChoices(
    title: String,
    choices: List<Pair<String, String>>,
    selected: String,
    testPrefix: String,
    onSelect: (String) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().testTag(VTestIds.CUSTOMIZATION_LIBRARY),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        R9SectionTitle(title)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            choices.forEach { (key, label) ->
                Surface(
                    modifier = Modifier.clickable { onSelect(key) }
                        .testTag("$testPrefix.$key"),
                    color = if(selected == key) R9.Blue else Color.White,
                    border = BorderStroke(1.dp, if(selected == key) R9.Blue else R9.Line),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(label, Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
                        color = if(selected == key) Color.White else R9.Muted,
                        fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
