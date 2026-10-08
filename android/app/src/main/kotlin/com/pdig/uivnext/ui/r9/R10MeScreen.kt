package com.pdig.uivnext.ui.r9

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
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

/**
 * User-first fifth tab. Personal, local preferences and account-like tasks
 * live here, not in four independent top-bar engineering icons.
 */
@Composable
internal fun R10MeScreen(app: VAppState, onHelp: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp, vertical = 17.dp)
            .testTag("pdig.r10.screen.me"),
        verticalArrangement = Arrangement.spacedBy(17.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = BorderStroke(1.dp, R9.Line),
        ) {
            Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(color = R9.Mist, shape = CircleShape, modifier = Modifier.size(55.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Person, contentDescription = null,
                            tint = R9.Blue, modifier = Modifier.size(30.dp))
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("我的数字生活", color = R9.Ink, fontSize = 18.sp,
                        fontWeight = FontWeight.Bold)
                    Text("本机个人工作区 · 数据仅供当前预览",
                        color = R9.Muted, fontSize = 11.sp)
                }
            }
        }
        Surface(color = R9.Mist, shape = RoundedCornerShape(19.dp),
            modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(vertical = 14.dp)) {
                R9Counter(app.demoCards().size, "卡片", R9.Amber, "▣", Modifier.weight(1f))
                R9Counter(app.demoNumbers().size, "号码", R9.Green, "▤", Modifier.weight(1f))
                R9Counter(if (app.emptyDemo) 0 else UiVNextDemoFixture.services.size,
                    "服务", R9.Blue, "✧", Modifier.weight(1f))
            }
        }
        R9SectionTitle("隐私")
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { app.privacyMask = !app.privacyMask }
                .testTag("pdig.r10.me.privacy"),
            color = Color.White, shape = RoundedCornerShape(17.dp),
            border = BorderStroke(1.dp, R9.Line),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                Icon(Icons.Filled.Lock, contentDescription = null,
                    tint = R9.Blue, modifier = Modifier.size(23.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("遮蔽敏感信息", color = R9.Ink,
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("默认不遮蔽；由你随时决定是否隐藏",
                        color = R9.Muted, fontSize = 10.sp)
                }
                R9Badge(if(app.privacyMask) "已开启" else "已关闭",
                    if(app.privacyMask) R9.Blue else R9.Muted)
            }
        }
        R9SectionTitle("我的管理")
        Surface(shape = RoundedCornerShape(19.dp), color = Color.White,
            border = BorderStroke(1.dp, R9.Line)) {
            Column {
                R10MeAction("管理银行卡", "查看、选择卡面图片", "cards") {
                    app.navigate(VScreen.CARDS)
                }
                R10MeAction("管理号码", "查看通信身份与恢复关系", "numbers") {
                    app.navigate(VScreen.NUMBERS)
                }
                R10MeAction("偏好设置", "外观、动态效果和隐私", "settings") {
                    app.navigate(VScreen.SETTINGS)
                }
                R10MeAction("数据来源", "已记录数据与事实边界", "sources") {
                    app.navigate(VScreen.SOURCES)
                }
                if (onHelp != null) {
                    R10MeAction("新手引导", "重新查看操作介绍", "guide") { onHelp() }
                }
            }
        }
        Surface(shape = RoundedCornerShape(16.dp), color = R9.Ice,
            modifier = Modifier.fillMaxWidth()) {
            Text(
                "当前为预览数据。更改图片、底栏、隐私或显示偏好不会改变真实身份、账户关系或 .depmap 文件。",
                Modifier.padding(14.dp),
                color = R9.Muted, fontSize = 11.sp, lineHeight = 18.sp,
            )
        }
        Spacer(Modifier.height(7.dp))
    }
}

@Composable
private fun R10MeAction(title: String, subtitle: String, tag: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 61.dp)
            .clickable(onClick = onClick)
            .testTag("pdig.r10.me.$tag")
            .padding(horizontal = 17.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = R9.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = R9.Muted, fontSize = 10.sp)
        }
        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null,
            tint = R9.Muted, modifier = Modifier.size(20.dp))
    }
}
