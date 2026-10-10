package com.pdig.uivnext.ui.r9

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.demo.demoCards
import com.pdig.uivnext.demo.demoNumbers
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.ui.VAppState

/** Source provenance belongs to Preview truth, never a fabricated bank connection. */
@Composable
internal fun R9SourcesScreen(app: VAppState) {
    val cards = app.demoCards().size
    val phones = app.demoNumbers().size
    val accounts = if (app.emptyDemo) 0 else UiVNextDemoFixture.accounts.size
    val emails = if (app.emptyDemo) 0 else UiVNextDemoFixture.emails.size
    val devices = if (app.emptyDemo) 0 else UiVNextDemoFixture.devices.size
    val services = if (app.emptyDemo) 0 else UiVNextDemoFixture.services.size
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 13.dp, vertical = 12.dp)
            .testTag("pdig.r9.screen.sources"),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Text("数据与来源", color = R9.Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("这里展示的是隔离的预览演示数据，不是你真实账户的同步状态。",
            color = R9.Muted, fontSize = 11.sp, lineHeight = 18.sp)
        Surface(
            modifier = Modifier.fillMaxWidth().testTag("pdig.sources.boundary-hero"),
            color = R9.Mist, shape = RoundedCornerShape(19.dp),
            border = BorderStroke(1.dp, R9.Line),
        ) {
            Row(Modifier.padding(horizontal = 9.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "本机隔离" to "演示工作区",
                    "已记录" to "才可使用",
                    "未知" to "不等于安全",
                ).forEach { (heading, caption) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(heading, color = R9.Blue, fontSize = 12.sp,
                            fontWeight = FontWeight.Bold)
                        Text(caption, color = R9.Muted, fontSize = 10.sp)
                    }
                }
            }
        }
        R9SectionTitle("当前预览工作区")
        Surface(color = Color.White, shape = RoundedCornerShape(19.dp),
            border = BorderStroke(1.dp, R9.Line),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("已记录演示对象", color = R9.Ink, fontSize = 13.sp,
                        fontWeight = FontWeight.Bold)
                    R9Badge("演示数据", R9.Blue)
                }
                Row(Modifier.fillMaxWidth()) {
                    R9Counter(cards, "银行卡", R9.Amber, "▣", Modifier.weight(1f))
                    R9Counter(phones, "手机号", R9.Green, "▤", Modifier.weight(1f))
                    R9Counter(accounts, "账户", R9.Blue, "◎", Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth()) {
                    R9Counter(emails, "邮箱", Color(0xFF9670E0), "@", Modifier.weight(1f))
                    R9Counter(devices, "设备", Color(0xFF6486B9), "▥", Modifier.weight(1f))
                    R9Counter(services, "服务", R9.Amber, "✧", Modifier.weight(1f))
                }
            }
        }
        R9SectionTitle("事实边界")
        listOf(
            Triple("已记录", "仅已明确录入的信息才能作为依赖分析依据。", R9.Green),
            Triple("未知", "未录入的银行卡、验证方式和恢复路径均不得推断为安全。", R9.Amber),
            Triple("仅外观", "外观主题、卡面和隐私遮蔽只改变显示，不改变任何核心事实。", R9.Blue),
        ).forEach { (title, detail, tint) ->
            Surface(color = Color.White, shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, R9.Line), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    R9Badge(title, tint)
                    Text(detail, Modifier.weight(1f), fontSize = 11.sp,
                        lineHeight = 17.sp, color = R9.Ink)
                }
            }
        }
        R9SectionTitle("查看基础设施")
        listOf(
            VScreen.OVERVIEW, VScreen.CARDS, VScreen.NUMBERS,
            VScreen.ACCOUNTS, VScreen.EMAILS, VScreen.DEVICES,
            VScreen.SERVICES, VScreen.WEAKNESSES,
        ).forEach { page ->
            Surface(
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).clickable { app.navigate(page) },
                color = Color.White, shape = RoundedCornerShape(13.dp),
                border = BorderStroke(1.dp, R9.Line),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(page.titleZh, color = R9.Ink, fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold)
                    Text("查看 →", color = R9.Blue, fontSize = 11.sp)
                }
            }
        }
    }
}
