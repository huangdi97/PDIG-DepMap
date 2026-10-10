package com.pdig.uivnext.ui.r9

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.pdig.uivnext.demo.UI_CONTINUITY_REFERENCE_FINDINGS
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.demo.UiContinuityFindingKind
import com.pdig.uivnext.demo.demoNumbers
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.ui.VAppState

/** Recorded-edge snapshot. Never infers unknown relationships or invents a safety score. */
@Composable
internal fun R12ContinuityInsight(app: VAppState) {
    val relationCount = if (app.emptyDemo) 0 else UiVNextDemoFixture.relations.size
    val recoveryNumbers = app.demoNumbers().filter { it.recoveryOnly }
    val referenceFindings = if (app.emptyDemo) emptyList() else UI_CONTINUITY_REFERENCE_FINDINGS
    val structuralFindings = referenceFindings.count {
        it.kind == UiContinuityFindingKind.SINGLE_POINT_OF_FAILURE ||
            it.kind == UiContinuityFindingKind.SHARED_FAILURE_DOMAIN ||
            it.kind == UiContinuityFindingKind.RECOVERY_CYCLE
    }
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        R9SectionTitle("关联与恢复", "查看薄弱点 →") { app.navigate(VScreen.WEAKNESSES) }
        Surface(
            modifier = Modifier.fillMaxWidth().testTag("pdig.r12.now.continuity"),
            color = Color.White, shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, R9.Line),
        ) {
            Column(Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(color = R9.Mist, shape = RoundedCornerShape(11.dp)) {
                        Text("◎", Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                            color = R9.Blue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("已记录 ${relationCount} 条对象关联",
                            color = R9.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            when {
                                app.emptyDemo -> "暂无已记录连续性上下文"
                                structuralFindings > 0 ->
                                    "参考场景含 $structuralFindings 项结构性 Finding；路径数不等于独立路径数"
                                recoveryNumbers.isNotEmpty() ->
                                    "其中 ${recoveryNumbers.size} 个号码具有恢复用途，迁移前需逐项核对"
                                else -> "暂无已记录的恢复用途对象"
                            },
                            color = R9.Muted, fontSize = 10.sp, lineHeight = 15.sp,
                        )
                    }
                    Text("›", fontSize = 21.sp, color = R9.Blue,
                        modifier = Modifier.clickable { app.navigate(VScreen.WEAKNESSES) })
                }
                if (recoveryNumbers.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        recoveryNumbers.take(2).forEach { number ->
                            Surface(modifier = Modifier.weight(1f)
                                .clickable { app.openNumber(number.id) },
                                color = R9.Ice, shape = RoundedCornerShape(11.dp)) {
                                Text(app.numberDisplayName(number.id, number.maskedNumber) + " · 恢复用途",
                                    Modifier.padding(horizontal = 9.dp, vertical = 10.dp),
                                    color = R9.Muted, fontSize = 10.sp, maxLines = 1)
                            }
                        }
                        if (recoveryNumbers.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                if (referenceFindings.isNotEmpty()) {
                    val first = referenceFindings.first()
                    Surface(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { app.navigate(VScreen.WEAKNESSES) },
                        color = R9.Mist,
                        shape = RoundedCornerShape(11.dp),
                    ) {
                        Column(Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("连续性提示 · ${first.title}", color = R9.Ink,
                                fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                                maxLines = 2)
                            Text("查看完整 Findings →", color = R9.Blue, fontSize = 9.sp)
                        }
                    }
                }
                Text("只展示已记录/参考事实；未知关系不能视为安全，也不能用路径数量替代独立性分析。",
                    color = R9.Muted, fontSize = 9.sp)
            }
        }
    }
}
