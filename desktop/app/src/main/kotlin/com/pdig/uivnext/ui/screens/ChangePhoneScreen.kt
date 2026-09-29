package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.ContinuityRail
import com.pdig.uivnext.ui.components.LabelChip
import com.pdig.uivnext.ui.components.SectionHeader
import com.pdig.uivnext.ui.components.StatusBadge

/**
 * Change Phone（flagship）：Continuity Rail × 6 阶段（make-before-break）。
 * OLD NUMBER → services/accounts → NEW NUMBER；plan projection 带「计划」label。
 */
@Composable
fun ChangePhoneScreen(app: VAppState) {
    val old = UiVNextDemoFixture.numberById(UiVNextDemoFixture.changeStages.first().let { "num-cn-1" })
    val new = UiVNextDemoFixture.numberById("num-cn-3")
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("更换手机号", color = PdigV2Colors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(
            "计划投影：以下步骤为当前执行计划；完成状态只在实际验证后标记。",
            color = PdigV2Colors.TextMuted,
            fontSize = 12.sp,
        )

        ContinuityRail(stages = UiVNextDemoFixture.changeStages)

        SectionHeader("旧号码 → 关键服务 → 新号码")
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (old != null) {
                Surface(
                    Modifier.weight(1f),
                    color = PdigV2Colors.SurfaceRaised,
                    shape = RoundedCornerShape(VRadius.Lg),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        LabelChip("旧号码")
                        Spacer(Modifier.height(8.dp))
                        Text(old.maskedNumber, color = PdigV2Colors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("${old.carrier} · 主号", color = PdigV2Colors.TextMuted, fontSize = 12.sp)
                    }
                }
            }
            Text("→", color = PdigV2Colors.TextMuted, fontSize = 16.sp)
            Surface(
                Modifier.weight(1.2f),
                color = PdigV2Colors.Surface.copy(alpha = 0.7f),
                shape = RoundedCornerShape(VRadius.Lg),
                border = androidx.compose.foundation.BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Column(Modifier.padding(14.dp)) {
                    LabelChip("关键服务与账户")
                    Spacer(Modifier.height(8.dp))
                    UiVNextDemoFixture.changeMigrations.forEach { m ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(m.service, color = PdigV2Colors.TextSecondary, fontSize = 13.sp)
                            StatusBadge(m.status)
                        }
                    }
                }
            }
            Text("→", color = PdigV2Colors.TextMuted, fontSize = 16.sp)
            if (new != null) {
                Surface(
                    Modifier.weight(1f),
                    color = PdigV2Colors.Primary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(VRadius.Lg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PdigV2Colors.PrimaryBright),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        LabelChip("新号码", highlight = true)
                        Spacer(Modifier.height(8.dp))
                        Text(new.maskedNumber, color = PdigV2Colors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("${new.carrier} · 副号", color = PdigV2Colors.TextMuted, fontSize = 12.sp)
                    }
                }
            }
        }

        SectionHeader("阶段明细")
        stageDetail(listOf(
            "1 影响分析" to "已确认需要迁移的服务：微信支付、支付宝、招商银行网银、腾讯视频；2 个账户以旧号码为登录验证。",
            "2 建立新号码" to "新号码 +86 139****2204 已加入（保号副号）。",
            "3 验证新号码" to "等待接收验证码并确认（正在验证）。",
            "4 迁移关键账户" to "待验证通过后逐个迁移绑定。",
            "5 检查恢复路径" to "确保每个账户存在非旧号码的恢复方式。",
            "6 停用旧号码" to "阻止执行：新手机号验证通过后才能停用旧手机号（make-before-break）。",
        ))

        SectionHeader("风险提示")
        Surface(color = PdigV2Colors.Warning.copy(alpha = 0.12f), shape = RoundedCornerShape(VRadius.Md), modifier = Modifier.fillMaxWidth()) {
            Text(
                "旧号码是 2 个账户的唯一恢复路径：迁移完成前不要停用；验证阶段未完成时「停用旧号码」必须保持禁用。",
                Modifier.padding(14.dp),
                color = PdigV2Colors.TextPrimary,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun stageDetail(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (title, body) ->
            Surface(
                color = PdigV2Colors.SurfaceRaised,
                shape = RoundedCornerShape(VRadius.Md),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(title, color = PdigV2Colors.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Text(body, color = PdigV2Colors.TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}