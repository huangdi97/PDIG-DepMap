package com.pdig.uivnext.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.app.BuildConfig
import com.pdig.uivnext.ui.r9.R9NowScreen
import com.pdig.uivnext.ui.r9.R10MeScreen
import com.pdig.uivnext.ui.r9.R9InfrastructureScreen
import com.pdig.uivnext.ui.r9.R9CardsScreen
import com.pdig.uivnext.ui.r9.R9NumbersScreen
import com.pdig.uivnext.ui.r9.R9NumberDetailScreen
import com.pdig.uivnext.ui.r9.R9StudioScreen
import com.pdig.uivnext.ui.r9.R10CardImageStudio
import com.pdig.uivnext.ui.r9.R9SearchScreen
import com.pdig.uivnext.ui.r9.R9PreferencesScreen
import com.pdig.uivnext.ui.r9.R9SourcesScreen
import com.pdig.uivnext.ui.r9.R9SecondaryScreen
import com.pdig.uivnext.ui.r9.R9CardDetailScreen
import com.pdig.uivnext.ui.r9.R9ChangePhoneScreen
import com.pdig.uivnext.ui.r9.R9RecordsScreen
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.VGlobeState
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VTouchTarget
import com.pdig.uivnext.ui.screens.CardCustomizationScreen
import com.pdig.uivnext.ui.screens.CardDetailScreen
import com.pdig.uivnext.ui.screens.CardsScreen
import com.pdig.uivnext.ui.screens.DataSourcesScreen
import com.pdig.uivnext.ui.screens.ChangePhoneScreen
import com.pdig.uivnext.ui.screens.NumberCustomizationScreen
import com.pdig.uivnext.ui.screens.NumberDetailScreen
import com.pdig.uivnext.ui.screens.NumbersScreen
import com.pdig.uivnext.ui.screens.NowScreen
import com.pdig.uivnext.ui.screens.OverviewScreen
import com.pdig.uivnext.ui.screens.PersonalizationScreen
import com.pdig.uivnext.ui.screens.RecordsScreen
import com.pdig.uivnext.ui.screens.SearchScreen
import com.pdig.uivnext.ui.screens.SecondaryInfraScreen
import com.pdig.uivnext.ui.screens.clickableLocal
import com.pdig.uivnext.ui.screens.testTagLocal

/** 内容宿主：路由 L0 环境背景 + 屏幕分发 + 地区抽屉（REGION_DETAIL）。 */
@Composable
fun VNextContentHost(app: VAppState, breakpoint: MediaBreakpoint, modifier: Modifier = Modifier, onHelp: (() -> Unit)? = null) {
    Box(
        modifier
            .fillMaxSize()
            .testTagLocal("pdig.breakpoint.${breakpoint.name.lowercase()}")
            .background(Brush.verticalGradient(listOf(PdigV2Colors.Canvas, PdigV2Colors.CanvasDeep))),
    ) {
        // R9 is an explicit Preview-only renderer; production and tablet retain
        // their current behavior until separate device evidence supports translation.
        // This is a new screen implementation, not an R8 style modifier.
        val useR9Phone = BuildConfig.FLAVOR == "preview" && breakpoint == MediaBreakpoint.COMPACT
        when (app.screen) {
            VScreen.NOW -> if (useR9Phone) R9NowScreen(app) else NowScreen(app, breakpoint)
            VScreen.ME -> R10MeScreen(app, onHelp)
            VScreen.OVERVIEW, VScreen.INFRASTRUCTURE ->
                if (useR9Phone) R9InfrastructureScreen(app) else OverviewScreen(app, breakpoint)
            VScreen.CARDS -> if (useR9Phone) R9CardsScreen(app) else CardsScreen(app, breakpoint)
            VScreen.CARD_DETAIL -> if (useR9Phone) R9CardDetailScreen(app) else CardDetailScreen(app, breakpoint)
            VScreen.CARD_CUSTOMIZATION -> if (useR9Phone) R10CardImageStudio(app) else CardCustomizationScreen(app, breakpoint)
            VScreen.NUMBERS -> if (useR9Phone) R9NumbersScreen(app) else NumbersScreen(app, breakpoint)
            VScreen.NUMBER_DETAIL -> if (useR9Phone) R9NumberDetailScreen(app) else NumberDetailScreen(app)
            VScreen.NUMBER_CUSTOMIZATION -> if (useR9Phone) R9StudioScreen(app, false) else NumberCustomizationScreen(app, breakpoint)
            VScreen.CHANGE, VScreen.CHANGE_PHONE -> if (useR9Phone) R9ChangePhoneScreen(app) else ChangePhoneScreen(app, breakpoint)
            VScreen.RECORDS -> if (useR9Phone) R9RecordsScreen(app) else RecordsScreen(app, breakpoint)
            VScreen.SEARCH -> if (useR9Phone) R9SearchScreen(app) else SearchScreen(app, breakpoint)
            VScreen.SOURCES -> if (useR9Phone) R9SourcesScreen(app) else DataSourcesScreen(app, breakpoint)
            VScreen.ACCOUNTS, VScreen.EMAILS, VScreen.DEVICES, VScreen.SERVICES, VScreen.WEAKNESSES ->
                if (useR9Phone) R9SecondaryScreen(app, app.screen) else SecondaryInfraScreen(app, app.screen, breakpoint)
            VScreen.PERSONALIZATION, VScreen.SETTINGS -> if (useR9Phone) R9PreferencesScreen(app) else PersonalizationScreen(app, breakpoint)
        }
        if (app.globe.state == VGlobeState.REGION_DETAIL) {
            RegionDrawer(app, breakpoint)
        }
    }
}

/** Region Drawer：选中地区后的资产上下文 + 三个动作（查看全部/查看卡片/查看号码）。 */
@Composable
private fun BoxScope.RegionDrawer(app: VAppState, breakpoint: MediaBreakpoint) {
    val region = UiVNextDemoFixture.regionSummaries()
        .firstOrNull { it.regionCode == app.regionFilter } ?: return
    val compact = breakpoint == MediaBreakpoint.COMPACT
    val drawerModifier = if (compact) {
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .widthIn(max = 440.dp)
    } else {
        Modifier
            .align(Alignment.BottomEnd)
            .padding(24.dp)
            .widthIn(max = 380.dp)
    }
    Surface(
        modifier = drawerModifier.testTagLocal("pdig.region.drawer"),
        color = PdigV2Colors.Surface.copy(alpha = 0.95f),
        shape = RoundedCornerShape(VRadius.Xl),
        border = BorderStroke(1.dp, PdigV2Colors.BorderStrong),
    ) {
        Column(Modifier.padding(if (compact) 16.dp else 20.dp)) {
            Text(region.displayName, color = PdigV2Colors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "${region.cardCount} 张卡 · ${region.phoneCount} 个号码 · ${region.accountCount} 个账户 · ${region.serviceCount} 项服务",
                color = PdigV2Colors.TextSecondary,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(12.dp))
            Text("在该地区查看", color = PdigV2Colors.TextMuted, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
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
            Spacer(Modifier.height(8.dp))
            Surface(Modifier.fillMaxWidth().defaultMinSize(minHeight = VTouchTarget.Min).clickableLocal { app.clearRegion() }, color = PdigV2Colors.PrimarySoft, shape = RoundedCornerShape(VRadius.Md)) {
                Text(
                    "返回全球视图",
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = PdigV2Colors.TextSecondary,
                    fontSize = 12.sp,
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
            .defaultMinSize(minHeight = VTouchTarget.Min)
            .clickableLocal(onClick = onClick),
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Md),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Text(label, color = PdigV2Colors.PrimaryBright, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(hint, color = PdigV2Colors.TextMuted, fontSize = 11.sp)
        }
    }
}
