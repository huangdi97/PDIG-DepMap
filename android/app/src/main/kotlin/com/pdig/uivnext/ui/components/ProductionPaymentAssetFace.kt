package com.pdig.uivnext.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.production.VNextProductionObject
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.ui.r9.importCardArt
import com.pdig.uivnext.ui.r9.loadCardArt

/**
 * Production payment-asset identity face.
 *
 * Unlike the synthetic/reference Card renderer this surface accepts only fields
 * that production Reality can currently prove: name / issuer / last4.
 *
 * It deliberately does NOT invent:
 * region / currency / network / debit-credit / physical-virtual / expiry.
 */
@Composable
internal fun ProductionPaymentAssetFace(
    asset: VNextProductionObject,
    presentation: PresentationProfile?,
    privacyMask: Boolean,
    modifier: Modifier = Modifier,
) {
    val issuer = if (privacyMask) "发行方已遮蔽" else asset.issuer?.takeIf { it.isNotBlank() } ?: "发行方未记录"
    val identity = CardIdentityProfile.forIssuer(if (privacyMask) "" else asset.issuer.orEmpty())
    val theme = cardThemeInfo(presentation?.themeId ?: "minimal")
    val material = when (presentation?.material) {
        "glass" -> CardMaterial.GLASS
        "metal" -> CardMaterial.METAL
        "brushed" -> CardMaterial.BRUSHED
        "matte" -> CardMaterial.MATTE
        "satin" -> CardMaterial.SATIN
        "translucent" -> CardMaterial.TRANSLUCENT
        else -> null
    }

    val context = LocalContext.current
    val localArt = remember(presentation?.backgroundKind, presentation?.backgroundValue) {
        if (presentation?.backgroundKind == "local-image") {
            loadCardArt(context, presentation.backgroundValue)
        } else {
            null
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pdig.production-vnext.card.face"),
        color = Color.Transparent,
        shape = RoundedCornerShape(VRadius.Xl),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
        shadowElevation = 2.dp,
    ) {
        BoxWithConstraints(
            Modifier
                .aspectRatio(1.586f)
                .background(cardFaceBaseBrush(identity, theme)),
        ) {
            // A 2-column consumer card on a phone is ~160dp wide, not a
            // full-width card detail. Adapt the identity typography in place.
            val compactFace = maxWidth < 220.dp
            if (localArt != null && !privacyMask) {
                Image(
                    bitmap = localArt.asImageBitmap(),
                    contentDescription = "本机卡面图片",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.30f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.46f),
                                ),
                            ),
                        ),
                )
            } else {
                Canvas(Modifier.fillMaxSize()) {
                    drawCardArtwork(identity, theme, material)
                }
            }
            Column(Modifier.fillMaxSize().padding(if (compactFace) 9.dp else 18.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(if (compactFace) 1.dp else 3.dp),
                    ) {
                        Text(
                            if (privacyMask) "支付工具（已遮蔽）" else asset.name,
                            color = PdigV2Colors.AssetTextPrimary,
                            fontSize = if (compactFace) 11.sp else 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text(
                            issuer,
                            color = PdigV2Colors.AssetTextSecondary,
                            fontSize = if (compactFace) 9.sp else 12.sp,
                            maxLines = 1,
                        )
                    }
                    if (!compactFace) Text(
                        "支付工具",
                        color = PdigV2Colors.AssetTextMuted,
                        fontSize = 10.sp,
                    )
                }

                Spacer(Modifier.weight(1f))

                Text(
                    when {
                        privacyMask -> "••••"
                        asset.last4.isNullOrBlank() -> "尾号未记录"
                        else -> "••••  ${asset.last4}"
                    },
                    color = PdigV2Colors.AssetTextPrimary,
                    fontSize = if (compactFace) 12.sp else 19.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!compactFace) Text(
                    "只显示已确认身份字段",
                    color = PdigV2Colors.AssetTextMuted,
                    fontSize = 9.sp,
                )
            }
        }
    }
}

internal data class ProductionCardThemeChoice(
    val id: String,
    val label: String,
)

internal val PRODUCTION_CARD_THEME_CHOICES = listOf(
    ProductionCardThemeChoice("minimal", "原生"),
    ProductionCardThemeChoice("glass", "玻璃"),
    ProductionCardThemeChoice("metal", "金属"),
    ProductionCardThemeChoice("abstract", "抽象"),
    ProductionCardThemeChoice("deep-space", "深空"),
)

/**
 * Tiny in-detail appearance control — intentionally not a second product.
 */
@Composable
internal fun ProductionCardAppearanceStrip(
    selectedTheme: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().testTag("pdig.production-vnext.card.appearance"),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            "卡面外观",
            color = PdigV2Colors.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PRODUCTION_CARD_THEME_CHOICES.forEach { choice ->
                val selected = choice.id == selectedTheme
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(choice.id) },
                    color = if (selected) PdigV2Colors.PrimarySoft else PdigV2Colors.Surface,
                    shape = RoundedCornerShape(VRadius.Md),
                    border = BorderStroke(
                        1.dp,
                        if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.BorderSubtle,
                    ),
                ) {
                    Box(
                        Modifier.padding(horizontal = 6.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            choice.label,
                            color = if (selected) PdigV2Colors.PrimaryText else PdigV2Colors.TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
        Text(
            "仅改变本机呈现；不会修改卡片身份、关系或影响分析。",
            color = PdigV2Colors.TextMuted,
            fontSize = 9.sp,
        )
    }
}


/**
 * Small Production appearance utility.
 *
 * Presets and local photos are Presentation only. Selecting a local photo copies
 * the user-picked content into app-private storage through the same hardened
 * import path as Preview; no content:// URI or arbitrary path is persisted.
 */
@Composable
internal fun ProductionCardAppearanceEditor(
    profile: PresentationProfile,
    onSave: (PresentationProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var importError by remember(profile.targetId) { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val localName = importCardArt(context, uri)
            if (localName == null) {
                importError = true
            } else {
                importError = false
                onSave(
                    profile.copy(
                        backgroundKind = "local-image",
                        backgroundValue = localName,
                    ),
                )
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().testTag("pdig.production-vnext.card.appearance-editor"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProductionCardAppearanceStrip(
            selectedTheme = profile.themeId,
            onSelect = { theme ->
                importError = false
                onSave(
                    profile.copy(
                        themeId = theme,
                        backgroundKind = "preset",
                        backgroundValue = theme,
                    ),
                )
            },
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clickable { picker.launch("image/*") }
                .testTag("pdig.production-vnext.card.choose-photo"),
            color = PdigV2Colors.PrimarySoft,
            shape = RoundedCornerShape(VRadius.Md),
            border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "从相册更换卡面",
                    color = PdigV2Colors.PrimaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("＋", color = PdigV2Colors.PrimaryText, fontSize = 16.sp)
            }
        }
        if (importError) {
            Text(
                "图片无法读取或超过 12MB，请换一张照片。",
                color = PdigV2Colors.Critical,
                fontSize = 10.sp,
            )
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clickable {
                    onSave(profile.copy(maskSensitive = !profile.maskSensitive))
                }
                .testTag("pdig.production-vnext.card.mask"),
            color = PdigV2Colors.Surface,
            shape = RoundedCornerShape(VRadius.Md),
            border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "单独遮蔽敏感信息",
                    color = PdigV2Colors.TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (profile.maskSensitive) "已开启" else "未开启",
                    color = if (profile.maskSensitive) PdigV2Colors.PrimaryText else PdigV2Colors.TextMuted,
                    fontSize = 10.sp,
                )
            }
        }
        Text(
            "图片、主题与单独遮蔽只保存在本机 Presentation；不会改变卡片身份、依赖或影响分析。",
            color = PdigV2Colors.TextMuted,
            fontSize = 9.sp,
        )
    }
}
