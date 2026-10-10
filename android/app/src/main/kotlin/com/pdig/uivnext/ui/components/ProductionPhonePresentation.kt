package com.pdig.uivnext.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.NUMBER_MATERIAL_CHOICES
import com.pdig.uivnext.model.NUMBER_THEME_PRESETS
import com.pdig.uivnext.model.PRESENTATION_ACCENT_CHOICES
import com.pdig.uivnext.model.PRESENTATION_LAYOUT_CHOICES
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.model.accentLabelZh
import com.pdig.uivnext.model.hexColorOrNull
import com.pdig.uivnext.model.layoutLabelZh
import com.pdig.uivnext.model.materialLabelZh
import com.pdig.uivnext.model.themeLabelZh
import com.pdig.uivnext.production.VNextProductionObject
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.productionIdentityIdentifierLabel
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius

/**
 * Production phone identity face.
 *
 * Unlike the synthetic reference NumberFace this renderer intentionally has no
 * country flag, carrier, SIM type, primary/secondary role or recovery badge:
 * Production may show those only after each concept receives governed authority.
 * Local theme/material/layout remain Presentation only.
 */
@Composable
internal fun ProductionPhoneIdentityFace(
    item: VNextProductionObject,
    app: VAppState,
    profile: PresentationProfile,
    modifier: Modifier = Modifier,
) {
    val hidden = app.privacyMask || profile.maskSensitive
    val displayName = app.numberDisplayNameForScreen(item.id, item.name)
    val identifier = productionIdentityIdentifierLabel(item, hidden) ?: "号码值未记录"
    val accent = hexColorOrNull(profile.accentColor) ?: productionPhoneAccent(profile.themeId)
    val compact = profile.layout == "compact"
    val focused = profile.layout == "focused"

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(if (compact) 142.dp else if (focused) 196.dp else 172.dp)
            .clip(RoundedCornerShape(VRadius.Xl))
            .testTag("pdig.production-vnext.phone.identity-face"),
        color = Color.Transparent,
        shape = RoundedCornerShape(VRadius.Xl),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(numberFaceBrush(profile.themeId)),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawArc(
                    color = accent.copy(alpha = 0.55f),
                    startAngle = 188f,
                    sweepAngle = 142f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.42f, -size.height * 0.18f),
                    size = Size(size.width * 0.66f, size.height * 1.32f),
                    style = Stroke(width = 2.dp.toPx()),
                )
                repeat(5) { index ->
                    val barHeight = size.height * (0.12f + index * 0.075f)
                    drawRect(
                        color = accent.copy(alpha = 0.30f + index * 0.12f),
                        topLeft = Offset(
                            size.width * (0.76f + index * 0.043f),
                            size.height - barHeight - size.height * 0.13f,
                        ),
                        size = Size(size.width * 0.023f, barHeight),
                    )
                }
                when (profile.material) {
                    "glass" -> {
                        drawRect(
                            Color.White.copy(alpha = 0.10f),
                            topLeft = Offset(size.width * 0.12f, 0f),
                            size = Size(size.width * 0.16f, size.height),
                        )
                    }
                    "metal" -> repeat(10) { i ->
                        val y = size.height * i / 10f
                        drawRect(
                            Color.White.copy(alpha = if (i % 2 == 0) 0.045f else 0.012f),
                            topLeft = Offset(0f, y),
                            size = Size(size.width, size.height / 10f + 1f),
                        )
                    }
                    "matte" -> drawRect(Color.Black.copy(alpha = 0.08f))
                }
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(if (compact) 14.dp else 18.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            displayName,
                            color = PdigV2Colors.AssetTextPrimary,
                            fontSize = if (focused) 17.sp else 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text(
                            "手机号身份 · 已确认 Reality",
                            color = PdigV2Colors.AssetTextSecondary,
                            fontSize = 9.sp,
                        )
                    }
                    Text(
                        "•••",
                        color = accent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        identifier,
                        color = PdigV2Colors.AssetTextPrimary,
                        fontSize = if (focused) 24.sp else if (compact) 17.sp else 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (item.identityIdentifierValue == null)
                            "具体号码仍未知 · 不从名称或地区推断"
                        else
                            "标识值独立确认 · 外观不会改写身份",
                        color = PdigV2Colors.AssetTextSecondary,
                        fontSize = 9.sp,
                    )
                }
            }
        }
    }
}

/**
 * Local Presentation controls for a production PHONE_NUMBER identity.
 *
 * Alias/theme/material/layout/masking are explicitly local presentation state and
 * never write Canonical/Personal Reality. This closes the product parity gap
 * without inventing carrier/SIM/role facts.
 */
@Composable
internal fun ProductionPhonePresentationEditor(
    item: VNextProductionObject,
    app: VAppState,
    modifier: Modifier = Modifier,
) {
    val saved = app.savedPresentationProfile("phoneNumber", item.id)
        ?: app.presentationProfile("phoneNumber", item.id, "minimal")
    var aliasDraft by remember(item.id, app.numberAlias(item.id)) {
        mutableStateOf(app.numberAlias(item.id))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pdig.production-vnext.phone.presentation"),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        ProductionPhoneIdentityFace(
            item = item,
            app = app,
            profile = saved,
        )

        Text(
            "本机名称与外观",
            color = PdigV2Colors.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )

        OutlinedTextField(
            value = aliasDraft,
            onValueChange = { aliasDraft = it.take(32) },
            label = { Text("自定义名称（可留空）") },
            supportingText = {
                Text("留空时显示已记录的身份名称；这里只改本机显示。")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("pdig.production-vnext.phone.alias"),
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clickable { app.renameNumber(item.id, aliasDraft) }
                .testTag("pdig.production-vnext.phone.alias.save"),
            color = PdigV2Colors.PrimarySoft,
            shape = RoundedCornerShape(VRadius.Md),
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "保存本机名称",
                    color = PdigV2Colors.PrimaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("→", color = PdigV2Colors.PrimaryText, fontSize = 13.sp)
            }
        }

        PresentationChoiceRow(
            title = "主题",
            values = NUMBER_THEME_PRESETS,
            selected = saved.themeId,
            label = { themeLabelZh("number", it) },
            testPrefix = "pdig.production-vnext.phone.theme",
        ) { value ->
            app.savePresentationProfile(
                saved.copy(
                    themeId = value,
                    backgroundKind = "preset",
                    backgroundValue = value,
                ),
            )
        }

        PresentationChoiceRow(
            title = "材质",
            values = NUMBER_MATERIAL_CHOICES,
            selected = saved.material,
            label = ::materialLabelZh,
            testPrefix = "pdig.production-vnext.phone.material",
        ) { value ->
            app.savePresentationProfile(saved.copy(material = value))
        }

        PresentationChoiceRow(
            title = "布局",
            values = PRESENTATION_LAYOUT_CHOICES,
            selected = saved.layout,
            label = ::layoutLabelZh,
            testPrefix = "pdig.production-vnext.phone.layout",
        ) { value ->
            app.savePresentationProfile(saved.copy(layout = value))
        }

        PresentationChoiceRow(
            title = "强调",
            values = PRESENTATION_ACCENT_CHOICES.map { it.value },
            selected = saved.accentColor,
            label = ::accentLabelZh,
            testPrefix = "pdig.production-vnext.phone.accent",
        ) { value ->
            app.savePresentationProfile(saved.copy(accentColor = value))
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clickable {
                    app.savePresentationProfile(
                        saved.copy(maskSensitive = !saved.maskSensitive),
                    )
                }
                .testTag("pdig.production-vnext.phone.mask"),
            color = PdigV2Colors.Surface,
            shape = RoundedCornerShape(VRadius.Md),
            border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "单独遮蔽敏感信息",
                    color = PdigV2Colors.TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (saved.maskSensitive) "已开启" else "未开启",
                    color = if (saved.maskSensitive) PdigV2Colors.PrimaryText else PdigV2Colors.TextMuted,
                    fontSize = 10.sp,
                )
            }
        }

        Text(
            "这些设置只保存在本机 PresentationProfile / 号码显示名称中；不会修改号码标识、关系、地区、维护计划或影响分析。",
            color = PdigV2Colors.TextMuted,
            fontSize = 9.sp,
        )
    }
}

@Composable
private fun PresentationChoiceRow(
    title: String,
    values: List<String>,
    selected: String,
    label: (String) -> String,
    testPrefix: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = PdigV2Colors.TextMuted, fontSize = 10.sp)
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            values.forEach { value ->
                val active = value == selected
                Surface(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 44.dp)
                        .clickable { onSelect(value) }
                        .testTag("$testPrefix.$value"),
                    color = if (active) PdigV2Colors.PrimarySoft else PdigV2Colors.Surface,
                    shape = RoundedCornerShape(VRadius.Md),
                    border = BorderStroke(
                        1.dp,
                        if (active) PdigV2Colors.PrimaryBright else PdigV2Colors.BorderSubtle,
                    ),
                ) {
                    Text(
                        label(value),
                        Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
                        color = if (active) PdigV2Colors.PrimaryText else PdigV2Colors.TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

private fun productionPhoneAccent(theme: String): Color = when (theme) {
    "travel" -> Color(0xFF7FB2FF)
    "recovery" -> Color(0xFFFFC864)
    "banking" -> Color(0xFF6FE3D4)
    "work" -> Color(0xFF9FC7FF)
    "private" -> Color(0xFFC8A7FF)
    "city" -> Color(0xFF7FE0FF)
    "minimal" -> Color(0xFFAEBFDF)
    else -> PdigV2Colors.PrimaryBright
}
