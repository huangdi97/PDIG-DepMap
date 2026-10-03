package com.pdig.uivnext.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.VGlobeState
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
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
fun VNextContentHost(app: VAppState, breakpoint: MediaBreakpoint, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PdigV2Colors.Canvas, PdigV2Colors.CanvasDeep))),
    ) {
        when (app.screen) {
            VScreen.NOW -> NowScreen(app, breakpoint)
            VScreen.OVERVIEW, VScreen.INFRASTRUCTURE -> OverviewScreen(app, breakpoint)
            VScreen.CARDS -> CardsScreen(app, breakpoint)
            VScreen.CARD_DETAIL -> CardDetailScreen(app, breakpoint)
            VScreen.CARD_CUSTOMIZATION -> CardCustomizationScreen(app, breakpoint)
            VScreen.NUMBERS -> NumbersScreen(app, breakpoint)
            VScreen.NUMBER_DETAIL -> NumberDetailScreen(app)
            VScreen.NUMBER_CUSTOMIZATION -> NumberCustomizationScreen(app, breakpoint)
            VScreen.CHANGE, VScreen.CHANGE_PHONE -> ChangePhoneScreen(app, breakpoint)
            VScreen.RECORDS -> RecordsScreen(app)
            VScreen.SEARCH -> SearchScreen(app)
            VScreen.SOURCES -> DataSourcesScreen(app)
            VScreen.ACCOUNTS, VScreen.EMAILS, VScreen.DEVICES, VScreen.SERVICES, VScreen.WEAKNESSES ->
                SecondaryInfraScreen(app, app.screen)
            VScreen.PERSONALIZATION, VScreen.SETTINGS -> PersonalizationScreen(app)
            else -> PlaceholderScreen(app.screen)
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
            .padding(32.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
    ) {
        Text(screen.titleZh, color = PdigV2Colors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Surface(color = PdigV2Colors.Surface.copy(alpha = 0.9f), shape = RoundedCornerShape(VRadius.Lg), modifier = Modifier.fillMaxWidth()) {
            Text(
                "这里还没有可展示的内容。后续接入数据后，你可以在这里查看和管理${screen.titleZh}。",
                Modifier.padding(20.dp),
                color = PdigV2Colors.TextSecondary,
                fontSize = 14.sp,
            )
        }
    }
}

/** Region Drawer：选中地区后的资产上下文 + 三个动作（查看全部/查看卡片/查看号码）。 */
@Composable
private fun BoxScope.RegionDrawer(app: VAppState) {
    val region = UiVNextDemoFixture.regionSummaries()
        .firstOrNull { it.regionCode == app.regionFilter } ?: return
    Surface(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(24.dp),
        color = PdigV2Colors.Surface.copy(alpha = 0.95f),
        shape = RoundedCornerShape(VRadius.Xl),
        border = BorderStroke(1.dp, PdigV2Colors.BorderStrong),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(region.displayName, color = PdigV2Colors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "${region.cardCount} 张卡 · ${region.phoneCount} 个号码 · ${region.accountCount} 个账户 · ${region.serviceCount} 项服务",
                color = PdigV2Colors.TextSecondary,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(12.dp))
            Text("查看方式", color = PdigV2Colors.TextMuted, fontSize = 12.sp)
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
            Surface(Modifier.fillMaxWidth(), color = PdigV2Colors.PrimarySoft, shape = RoundedCornerShape(VRadius.Md)) {
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
