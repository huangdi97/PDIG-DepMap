package com.pdig.uivnext.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.VGlobeState
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VType
import com.pdig.uivnext.ui.screens.CardCustomizationScreen
import com.pdig.uivnext.ui.screens.CardDetailScreen
import com.pdig.uivnext.ui.screens.CardsScreen
import com.pdig.uivnext.ui.screens.ChangePhoneScreen
import com.pdig.uivnext.ui.screens.NumberCustomizationScreen
import com.pdig.uivnext.ui.screens.NumberDetailScreen
import com.pdig.uivnext.ui.screens.NumbersScreen
import com.pdig.uivnext.ui.screens.NowScreen
import com.pdig.uivnext.ui.screens.OverviewScreen
import com.pdig.uivnext.ui.screens.PersonalizationScreen
import com.pdig.uivnext.ui.screens.clickableLocal
import com.pdig.uivnext.ui.screens.testTagLocal

/**
 * 内容宿主：L0 Environment 背景 + 屏幕分发 + 地区抽屉（REGION_DETAIL）。
 * 结构顺序（G2）：environment → globe → overlay → controls。
 */
@Composable
fun VNextContentHost(app: VAppState, breakpoint: MediaBreakpoint) {
    Box(Modifier.fillMaxSize()) {
        EnvironmentBackdrop(Modifier.fillMaxSize())
        when (app.screen) {
            VScreen.NOW -> NowScreen(app, breakpoint)
            VScreen.OVERVIEW, VScreen.INFRASTRUCTURE -> OverviewScreen(app, breakpoint)
            VScreen.CARDS -> CardsScreen(app, breakpoint)
            VScreen.CARD_DETAIL -> CardDetailScreen(app)
            VScreen.CARD_CUSTOMIZATION -> CardCustomizationScreen(app)
            VScreen.NUMBERS -> NumbersScreen(app, breakpoint)
            VScreen.NUMBER_DETAIL -> NumberDetailScreen(app)
            VScreen.NUMBER_CUSTOMIZATION -> NumberCustomizationScreen(app)
            VScreen.CHANGE, VScreen.CHANGE_PHONE -> ChangePhoneScreen(app)
            VScreen.PERSONALIZATION, VScreen.SETTINGS -> PersonalizationScreen(app)
            else -> PlaceholderScreen(app.screen)
        }
        if (app.paletteOpen) {
            Surface(
                Modifier
                    .fillMaxSize()
                    .clickableLocal { app.paletteOpen = false },
                color = PdigV2Colors.CanvasDeep.copy(alpha = 0.40f),
            ) {}
            Box(Modifier.fillMaxSize().padding(top = 64.dp), contentAlignment = Alignment.TopCenter) {
                CommandPalette(app)
            }
        }
        if (app.globe.state == VGlobeState.REGION_DETAIL) {
            RegionDrawer(app)
        }
    }
}

@Composable
private fun PlaceholderScreen(screen: VScreen) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(VSpacing.Xxxl),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(VSpacing.Lg),
    ) {
        Text(screen.titleZh, color = PdigV2Colors.TextPrimary, style = VType.PageTitle)
        Surface(color = PdigV2Colors.Surface.copy(alpha = 0.96f), shape = RoundedCornerShape(VRadius.Lg), modifier = Modifier.fillMaxWidth()) {
            Text(
                "该页面将在后续迭代接入：当前 IA 已包含 ${screen.titleZh}（route=${screen.route}），v0.x 焦点为「卡片」与「号码」。",
                Modifier.padding(VSpacing.Xl),
                color = PdigV2Colors.TextSecondary,
                style = VType.Secondary,
            )
        }
    }
}

/**
 * Region Drawer（G10）：选中地区后的资产上下文（glass overlay，L2/L6 允许）。
 * 展示 Cards / Numbers / Accounts / Services 摘要 + 三个动作：查看全部 / 查看卡片 / 查看号码。
 */
@Composable
private fun BoxScope.RegionDrawer(app: VAppState) {
    val region = UiVNextDemoFixture.regionSummaries()
        .firstOrNull { it.regionCode == app.regionFilter } ?: return
    Surface(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(VSpacing.Xxl)
            .testTagLocal(VTestIds.REGION_DRAWER),
        color = PdigV2Colors.SurfaceGlass,
        shape = RoundedCornerShape(VRadius.Xl2),
        border = androidx.compose.foundation.BorderStroke(1.dp, PdigV2Colors.BorderStrong),
    ) {
        Column(Modifier.padding(VSpacing.Xxl)) {
            Text(region.displayName, style = VType.SectionTitle, color = PdigV2Colors.TextPrimary)
            Text(
                "${region.cardCount} 张卡 · ${region.phoneCount} 个号码 · ${region.accountCount} 个账户 · ${region.serviceCount} 项服务",
                style = VType.Secondary,
                color = PdigV2Colors.TextSecondary,
            )
            Spacer(Modifier.height(VSpacing.Lg))
            Text("查看方式", style = VType.Label, color = PdigV2Colors.TextMuted)
            Spacer(Modifier.height(VSpacing.Sm))
            DrawerAction("查看全部", "该地区所有基础设施") {
                app.regionFilter = region.regionCode
                app.navigate(VScreen.OVERVIEW)
                app.globe.state = VGlobeState.REGION_SELECTED
            }
            DrawerAction("查看卡片", "该地区卡片列表") {
                app.regionFilter = region.regionCode
                app.navigate(VScreen.CARDS)
                app.globe.state = VGlobeState.REGION_SELECTED
            }
            DrawerAction("查看号码", "该地区号码列表") {
                app.regionFilter = region.regionCode
                app.navigate(VScreen.NUMBERS)
                app.globe.state = VGlobeState.REGION_SELECTED
            }
            Spacer(Modifier.height(VSpacing.Sm))
            Surface(Modifier.fillMaxWidth(), color = PdigV2Colors.PrimarySoft, shape = RoundedCornerShape(VRadius.Md)) {
                Text(
                    "Escape 返回全球视图",
                    Modifier.padding(horizontal = VSpacing.Md, vertical = VSpacing.Sm),
                    color = PdigV2Colors.TextSecondary,
                    style = VType.Secondary,
                )
            }
        }
    }
}

@Composable
private fun DrawerAction(label: String, hint: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickableLocal(onClick = onClick),
        color = PdigV2Colors.SurfaceRaised.copy(alpha = 0.9f),
        shape = RoundedCornerShape(VRadius.Md),
    ) {
        Column(Modifier.padding(horizontal = VSpacing.Md, vertical = VSpacing.Sm)) {
            Text(label, color = PdigV2Colors.PrimaryBright, style = VType.Label)
            Text(hint, color = PdigV2Colors.TextMuted, style = VType.Meta)
        }
    }
}
