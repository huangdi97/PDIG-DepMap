package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.UiVNextNumber
import com.pdig.uivnext.theme.PdigV2Colors

/**
 * Compact relationship choreography: services orbit the old communication identity.
 * All labels come from the actual change migration fixture, no fake edge/status.
 * This is a spatial summary; actionable migration controls remain below.
 */
@Composable
internal fun CompactContinuityOrbit(number: UiVNextNumber?, projection: String) {
    val items = projectionMigrations(projection).take(4)
    val status = when (projection) {
        "current" -> "当前关联"
        "after" -> "计划投影 · 未验证"
        else -> "迁移中 · 待核对"
    }
    Surface(
        modifier = Modifier.fillMaxWidth().height(236.dp).testTag("pdig.change.continuity-orbit"),
        color = Color(0xFFF5F9FF),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFD4E4F9)),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    listOf(Color(0xFFD3E5FF), Color(0xFFEBF5FF), Color(0xFFFAFCFF)),
                ),
            ),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height * 0.48f)
                val radius = size.minDimension * 0.35f
                drawCircle(
                    color = Color(0xFF76A9F3).copy(alpha = 0.38f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2f),
                )
                drawCircle(
                    color = Color(0xFF4288E8).copy(alpha = 0.17f),
                    radius = radius * 0.76f,
                    center = center,
                    style = Stroke(width = 2f),
                )
                listOf(
                    Offset(center.x - radius * 0.89f, center.y - radius * 0.8f),
                    Offset(center.x + radius * 0.89f, center.y - radius * 0.8f),
                    Offset(center.x - radius * 0.89f, center.y + radius * 0.8f),
                    Offset(center.x + radius * 0.89f, center.y + radius * 0.8f),
                ).take(items.size).forEach { target ->
                    drawLine(
                        color = Color(0xFF7DADF0).copy(alpha = 0.48f),
                        start = center,
                        end = target,
                        strokeWidth = 2f,
                    )
                    drawCircle(color = Color(0xFF3686EB), radius = 5f, center = target)
                }
            }

            Surface(
                modifier = Modifier.align(Alignment.Center).size(width = 154.dp, height = 128.dp),
                color = Color(0xFF132F5C),
                shape = RoundedCornerShape(23.dp),
                border = BorderStroke(2.dp, Color(0xFF78B0FA)),
                shadowElevation = 7.dp,
            ) {
                Column(
                    Modifier.padding(horizontal = 10.dp, vertical = 13.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("▥    ${number?.countryCode ?: "—"}", color = Color(0xFFBBDEFF), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        number?.maskedNumber ?: "号码未记录",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(status, color = Color(0xFFB9D5F6), fontSize = 10.sp, textAlign = TextAlign.Center)
                }
            }
            val positions = listOf(
                Alignment.TopStart to (-3).dp,
                Alignment.TopEnd to (-3).dp,
                Alignment.BottomStart to 3.dp,
                Alignment.BottomEnd to 3.dp,
            )
            items.forEachIndexed { i, migration ->
                val (alignment, y) = positions[i]
                val (symbol, tint) = when {
                    migration.service.contains("微信") -> "微" to Color(0xFF0CB778)
                    migration.service.contains("支付宝") -> "支" to Color(0xFF178EFF)
                    migration.service.contains("招商") -> "招" to Color(0xFFEE424A)
                    migration.service.contains("腾讯") -> "▶" to Color(0xFF24A9E4)
                    else -> migration.service.take(1) to Color(0xFF6885AC)
                }
                Surface(
                    modifier = Modifier.align(alignment).offset(y = y)
                        .padding(horizontal = 7.dp, vertical = 17.dp),
                    color = Color.White.copy(alpha = 0.97f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFD7E5F7)),
                    shadowElevation = 1.dp,
                ) {
                    Row(
                        Modifier.padding(horizontal = 7.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        // These are in-app initials/marks, not downloaded or fabricated provider logos.
                        // Only the known migration items are shown; color never implies a safe status.
                        Surface(
                            modifier = Modifier.size(29.dp),
                            color = tint,
                            shape = RoundedCornerShape(9.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(symbol, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(migration.service, color = PdigV2Colors.TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(
                                when (migration.status) {
                                    "completed" -> "已验证"
                                    "waiting", "verifying" -> "待验证"
                                    "not_started" -> "未开始"
                                    "plan" -> "计划中"
                                    "unresolved" -> "待解决"
                                    else -> "待确认"
                                },
                                color = PdigV2Colors.TextSecondary,
                                fontSize = 9.sp,
                            )
                        }
                    }
                }
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
                color = PdigV2Colors.PrimarySoft,
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    "服务关联均源自当前演示数据",
                    Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = PdigV2Colors.TextSecondary,
                    fontSize = 9.sp,
                )
            }
        }
    }
}
