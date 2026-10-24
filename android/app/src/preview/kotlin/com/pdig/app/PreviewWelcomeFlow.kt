package com.pdig.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.theme.PdigV2Colors

/** v2.3-R1 §11.10: three screens only; synthetic preview, no personal data. */
@Composable
internal fun PreviewWelcomeFlow(onFinish: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = step > 0) { step-- }
    val title = listOf("看清你的数字生活", "改变之前，先看影响", "你的数据，由你掌握")
    val body = listOf(
        "号码、银行卡、账户和服务相互连接，一张地图就能了解自己的数字基础设施。",
        "换卡、换号之前，先查看受影响的服务，再按顺序准备、迁移、验证。",
        "PDIG 默认本地优先；未确认的关系不能自动变成事实，未知也不等于安全。",
    )
    val labels = listOf(
        listOf("银行卡", "号码", "账户", "服务"),
        listOf("当前", "影响分析", "迁移中", "验证"),
        listOf("本地处理", "人工确认", "演示数据", "保护隐私"),
    )
    Surface(
        modifier = Modifier.fillMaxSize().testTag("pdig.onboarding.root"),
        color = PdigV2Colors.Canvas,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFEAF2FF), Color(0xFFF8FAFF))))
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(32.dp), color = PdigV2Colors.PrimaryBright, shape = RoundedCornerShape(9.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("P", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("PDIG", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PdigV2Colors.TextPrimary)
                }
                Text("跳过", modifier = Modifier.clickable(onClick = onFinish).padding(12.dp).testTag("pdig.onboarding.skip"), fontSize = 14.sp, color = PdigV2Colors.TextSecondary)
            }
            Spacer(Modifier.height(26.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { i ->
                    Box(Modifier.weight(1f).height(5.dp).background(if (i <= step) PdigV2Colors.PrimaryBright else Color(0xFFD7E3F6), CircleShape))
                }
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).testTag("pdig.onboarding.step.${step + 1}"),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(18.dp))
                Text("${step + 1} / 3", color = PdigV2Colors.PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                Text(title[step], color = PdigV2Colors.TextPrimary, fontSize = 27.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text(body[step], color = PdigV2Colors.TextSecondary, fontSize = 14.sp, lineHeight = 23.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(26.dp))
                Surface(
                    Modifier.fillMaxWidth().height(230.dp),
                    color = Color.White,
                    shape = RoundedCornerShape(26.dp),
                    border = BorderStroke(1.dp, Color(0xFFD8E6FB)),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFFB5D1FF), Color(0xFFECF4FF), Color.White))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(Modifier.size(168.dp), shape = CircleShape, color = Color(0xFFC9DFFF), border = BorderStroke(1.dp, Color(0xFF92B9F7))) {
                            Box(contentAlignment = Alignment.Center) {
                                Surface(Modifier.size(128.dp), shape = CircleShape, color = Color(0xFF2867D8)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(listOf("🌐", "⇄", "✓")[step], fontSize = 48.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        val placements = listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd)
                        labels[step].forEachIndexed { index, label ->
                            Surface(Modifier.align(placements[index]).padding(12.dp), color = Color.White, shape = RoundedCornerShape(12.dp)) {
                                Text(label, Modifier.padding(horizontal = 9.dp, vertical = 7.dp), fontSize = 10.sp, color = PdigV2Colors.TextPrimary)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (step == 2) {
                    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFE1EDFF)) {
                        Text(
                            "当前是 UI 演示版：卡片、号码、风险和迁移步骤都是模拟示例，不读取或修改你的真实数据。",
                            Modifier.padding(14.dp), fontSize = 12.sp, lineHeight = 19.sp, color = PdigV2Colors.TextPrimary,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            Button(
                onClick = { if (step == 2) onFinish() else step++ },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("pdig.onboarding.next"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PdigV2Colors.PrimaryBright),
            ) { Text(if (step == 2) "进入演示空间" else "继续 →", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(8.dp))
            if (step > 0) {
                OutlinedButton(onClick = { step-- }, modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pdig.onboarding.back"), shape = RoundedCornerShape(16.dp)) {
                    Text("上一步", color = PdigV2Colors.TextSecondary)
                }
            } else {
                Text("无需登录 · 仅演示交互 · 不访问真实数据", modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = PdigV2Colors.TextMuted)
            }
        }
    }
}
