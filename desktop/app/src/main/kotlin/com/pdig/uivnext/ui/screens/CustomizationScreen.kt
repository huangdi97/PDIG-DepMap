package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.CARD_THEME_PRESETS
import com.pdig.uivnext.model.NUMBER_THEME_PRESETS
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.AssetCard
import com.pdig.uivnext.ui.components.NumberFace
import com.pdig.uivnext.ui.components.SectionHeader

/**
 * Card Customization Studio / Number Customization Studio。
 * Desktop 三栏（Asset Library 22% / Live Preview 46% / Property Inspector 32%）。
 * 编辑对象 = PresentationProfile（本地偏好，绝不写 .depmap；素材 bundled local / procedural）。
 */
@Composable
fun CardCustomizationScreen(app: VAppState) {
    val card = UiVNextDemoFixture.cardById(app.selectedCardId ?: "card-cn-1") ?: return
    var profile by remember { mutableStateOf(PresentationProfile.defaultFor("card", card.id, card.preset)) }
    CustomizationFrame(
        title = "卡面定制 · ${card.nickname}",
        presets = CARD_THEME_PRESETS,
        profile = profile,
        onPreset = { profile = profile.copy(themeId = it, backgroundValue = it) },
        preview = {
            AssetCard(card = card.copy(preset = profile.themeId), privacyMask = app.privacyMask, onClick = {})
        },
        rows = listOf(
            "主题" to profile.themeId,
            "材质" to profile.material,
            "主色" to profile.accentColor,
            "背景" to profile.backgroundValue,
            "布局" to profile.layout,
            "遮蔽" to (if (profile.maskSensitive) "已开启" else "已关闭"),
        ),
        toggles = listOf("显示昵称", "显示网络", "显示地区", "显示币种", "显示状态"),
    )
}

@Composable
fun NumberCustomizationScreen(app: VAppState) {
    val number = UiVNextDemoFixture.numberById(app.selectedNumberId ?: "num-cn-1") ?: return
    var profile by remember { mutableStateOf(PresentationProfile.defaultFor("phoneNumber", number.id, "country")) }
    CustomizationFrame(
        title = "号码面定制 · ${number.nickname}",
        presets = NUMBER_THEME_PRESETS,
        profile = profile,
        onPreset = { profile = profile.copy(themeId = it, backgroundValue = it) },
        preview = {
            NumberFace(number = number, privacyMask = app.privacyMask, onClick = {})
        },
        rows = listOf(
            "主题" to profile.themeId,
            "布局" to profile.layout,
            "背景" to profile.backgroundValue,
            "遮蔽" to (if (profile.maskSensitive) "已开启" else "已关闭"),
            "提示" to "preset visual ≠ 语义角色",
        ),
        toggles = listOf("显示昵称", "显示运营商", "SIM 徽标", "主副号", "用途标签"),
    )
}

@Composable
private fun CustomizationFrame(
    title: String,
    presets: List<String>,
    profile: PresentationProfile,
    onPreset: (String) -> Unit,
    preview: @Composable () -> Unit,
    rows: List<Pair<String, String>>,
    toggles: List<String>,
) {
    var saved by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = PdigV2Colors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Surface(
                color = if (saved) PdigV2Colors.Positive.copy(alpha = 0.2f) else PdigV2Colors.Primary,
                shape = RoundedCornerShape(VRadius.Md),
                modifier = Modifier.clickable { saved = true },
            ) {
                Text(
                    if (saved) "已保存（本地偏好）" else "保存",
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = if (saved) PdigV2Colors.Positive else PdigV2Colors.CanvasDeep,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
        }
        Row(Modifier.fillMaxSize()) {
            // Asset Library（左 22%）
            Column(
                Modifier
                    .weight(0.22f)
                    .fillMaxSize()
                    .testTagLocal(VTestIds.CUSTOMIZATION_LIBRARY),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionHeader("预设")
                presets.forEach { preset ->
                    Surface(
                        Modifier.fillMaxWidth().clickable { onPreset(preset) },
                        color = if (profile.themeId == preset) PdigV2Colors.Primary.copy(alpha = 0.28f) else PdigV2Colors.SurfaceRaised,
                        shape = RoundedCornerShape(VRadius.Md),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (profile.themeId == preset) PdigV2Colors.PrimaryBright else PdigV2Colors.BorderSubtle),
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                Modifier.width(26.dp).height(18.dp),
                                color = PdigV2Colors.PrimaryBright.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(4.dp),
                            ) {}
                            Spacer(Modifier.width(8.dp))
                            Text(preset, color = PdigV2Colors.TextPrimary, fontSize = 13.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            // Live Preview（中 46%）
            Column(
                Modifier
                    .weight(0.46f)
                    .testTagLocal(VTestIds.CUSTOMIZATION_PREVIEW),
                verticalArrangement = Arrangement.Top,
            ) {
                SectionHeader("实时预览")
                Surface(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    color = PdigV2Colors.Surface.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(VRadius.Xl),
                ) {
                    Column(Modifier.padding(20.dp)) { preview() }
                }
                Text(
                    "素材全部来自 bundled local / procedural；不加载远程图片。",
                    color = PdigV2Colors.TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            // Property Inspector（右 32%）
            Column(
                Modifier
                    .weight(0.32f)
                    .fillMaxSize()
                    .testTagLocal(VTestIds.CUSTOMIZATION_INSPECTOR),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SectionHeader("属性与样式")
                rows.forEach { (label, value) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label, color = PdigV2Colors.TextMuted, fontSize = 12.sp)
                        Text(value, color = PdigV2Colors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                toggles.forEach { toggle ->
                    Surface(
                        color = PdigV2Colors.SurfaceRaised,
                        shape = RoundedCornerShape(VRadius.Md),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    ) {
                        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(toggle, color = PdigV2Colors.TextSecondary, fontSize = 13.sp)
                            Surface(color = PdigV2Colors.Primary.copy(alpha = 0.3f), shape = RoundedCornerShape(VRadius.Sm)) {
                                Text("开", Modifier.padding(horizontal = 10.dp, vertical = 3.dp), color = PdigV2Colors.PrimaryBright, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
