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
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.ui.VAppState

/** R9 settings reuse WorkspacePreferenceStore; no invented preference backend. */
@Composable
internal fun R9PreferencesScreen(app: VAppState) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 13.dp, vertical = 12.dp)
            .testTag("pdig.r9.screen.preferences"),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Text("显示与个性化", color = R9.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("这些选项只改变你在本机看到的界面；不修改资产、身份或依赖事实。",
            color = R9.Muted, fontSize = 11.sp, lineHeight = 17.sp)
        Surface(color = R9.Ice, shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, R9.Line), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("亮色 · 当前视觉方向", color = R9.Ink,
                            fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("真实地球 · 金融资产身份 · 连续性变更",
                            color = R9.Muted, fontSize = 10.sp)
                    }
                    R9Badge("预览版", R9.Blue)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    R9PrefsTile("界面", "亮色优先", Color.White, Modifier.weight(1f))
                    R9PrefsTile("地球", if(app.reduceMotion) "静态" else "动态",
                        R9.Mist, Modifier.weight(1f))
                    R9PrefsTile("资产", "独立卡面", R9.Ice, Modifier.weight(1f))
                }
            }
        }

        R9SectionTitle("隐私")
        R9PreferenceToggle("隐藏敏感信息",
            if(app.privacyMask) "敏感数字保持遮蔽" else "敏感字段可见",
            app.privacyMask, "privacy") { app.privacyMask = !app.privacyMask }

        R9SectionTitle("动态效果")
        R9PreferenceToggle("减弱动态效果",
            if(app.reduceMotion) "不自动旋转地球" else "缓慢旋转地球",
            app.reduceMotion, "motion") { app.reduceMotion = !app.reduceMotion }

        R9SectionTitle("首页内容")
        R9PreferenceToggle("显示即将到来",
            if(app.showUpcoming) "显示已记录的时间节点" else "不显示即将到来的时间节点",
            app.showUpcoming, "upcoming") { app.showUpcoming = !app.showUpcoming }

        R9SectionTitle("对象外观")
        R9PreferencesAction("卡面图片", "从相册选择或使用简洁卡面", "cards") {
            app.navigate(VScreen.CARDS)
        }
        R9PreferencesAction("手机号定制", "通信身份不套用银行卡面", "numbers") {
            app.navigate(VScreen.NUMBERS)
        }

        R9SectionTitle("数据与来源")
        R9PreferencesAction("数据源", "查看数据来源、记录范围及事实边界", "sources") {
            app.openUtility(VScreen.SOURCES)
        }
        Surface(modifier = Modifier.fillMaxWidth(), color = R9.Mist,
            shape = RoundedCornerShape(15.dp)) {
            Text("未知不等于安全。关闭动画或改变外观不会降低账户恢复、支付或迁移风险。",
                Modifier.padding(14.dp), color = R9.Muted, fontSize = 11.sp, lineHeight = 18.sp)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun R9PrefsTile(title: String, hint: String, bg: Color, modifier: Modifier) {
    Surface(modifier = modifier, color = bg,
        border = BorderStroke(1.dp, R9.Line), shape = RoundedCornerShape(11.dp)) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = R9.Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(hint, color = R9.Muted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun R9PreferenceToggle(title: String, sub: String, on: Boolean, id: String, toggle: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { toggle() }
            .testTag("pdig.r9.prefs.$id"),
        color = Color.White, shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, R9.Line),
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, color = R9.Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(sub, color = R9.Muted, fontSize = 10.sp)
            }
            R9Badge(if(on) "已开启" else "已关闭", if(on) R9.Green else R9.Muted)
        }
    }
}

@Composable
private fun R9PreferencesAction(title: String, sub: String, id: String, action: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { action() }
            .testTag("pdig.r9.prefs.$id"),
        color = Color.White, shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, R9.Line),
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, fontSize = 12.sp, color = R9.Ink, fontWeight = FontWeight.SemiBold)
                Text(sub, fontSize = 10.sp, color = R9.Muted)
            }
            Text("›", color = R9.Blue, fontSize = 20.sp)
        }
    }
}
