package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.pdig.uivnext.model.UiVNextNumber
import com.pdig.uivnext.theme.PdigV2Colors

/**
 * ContinuityScene（PHASE 1D §25–28）—— 独立空间迁移场景（Compose Canvas，非 Row/Column 表格）。
 * 绘制辅助拆分至 ContinuitySceneDrawing.kt（≤300 行；本文件仅保留编排）。
 *
 * 布局（§27）：OLD NUMBER（左）← 卫星式服务节点 → NEW NUMBER（右）。
 * 状态语义（§28/§34）：
 *   current    ：OLD 实线路径 + NEW ghost 目标
 *   transition ：migrated=绿实线 / waiting=amber 虚线 / blocked=红断点 / not_started=muted
 *   after      ：OLD faded/retired + NEW primary（计划投影，不冒充 Reality）
 */
@Composable
fun ContinuityScene(
    old: UiVNextNumber,
    new: UiVNextNumber,
    services: List<SceneMigration>,
    projection: String,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val oldX = w * 0.07f
        val newX = w * 0.93f
        val cy = h * 0.5f
        val cols = listOf(w * 0.38f, w * 0.60f)
        val rows = listOf(h * 0.26f, h * 0.72f)
        val nodeW = w * 0.16f
        val nodeH = h * 0.34f

        services.forEachIndexed { i, m ->
            drawServicePaths(oldX, cy, newX, cy, cols[i % 2], rows[i / 2], m.status, projection, w)
        }
        drawNumberNode(
            textMeasurer = textMeasurer,
            center = Offset(oldX, cy),
            nodeW = nodeW,
            nodeH = nodeH,
            title = "OLD 旧号码",
            number = old.maskedNumber,
            meta = "${old.carrier} · ${roleLabelScene(old.role)}",
            emphasis = false,
            alpha = if (projection == "after") 0.30f else 1f,
            border = PdigV2Colors.BorderSubtle,
            accent = PdigV2Colors.TextMuted,
        )
        val newAlpha = if (projection == "current") 0.35f else 1f
        drawNumberNode(
            textMeasurer = textMeasurer,
            center = Offset(newX, cy),
            nodeW = nodeW,
            nodeH = nodeH,
            title = "NEW 新号码",
            number = new.maskedNumber,
            meta = "${new.carrier} · ${roleLabelScene(new.role)}",
            emphasis = projection != "current",
            alpha = newAlpha,
            border = if (projection == "after") PdigV2Colors.PrimaryBright else PdigV2Colors.PrimaryBright.copy(alpha = 0.4f),
            accent = PdigV2Colors.PrimaryBright,
        )
        services.forEachIndexed { i, m ->
            drawServiceNode(textMeasurer, cols[i % 2], rows[i / 2], m.service, m.role, m.status, projection, w, h)
        }
        if (projection == "after") {
            drawPlanProjectionBadge(textMeasurer, w)
        }
    }
}