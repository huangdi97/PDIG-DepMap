package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.pdig.uivnext.layout.Phase1FLayout
import com.pdig.uivnext.model.UiVNextNumber
import com.pdig.uivnext.theme.PdigV2Colors

/**
 * ContinuityScene（PHASE 1D §25–28 + PHASE 1F §28–34）—— 独立空间迁移场景。
 *
 * 布局（§29/§31 权重层级）：
 *   OLD 旧号码（左主位 250–300px）← 卫星式服务节点（130–170px）→ NEW 新号码（右主位）。
 *   Old/New 是主角，服务节点次级，路径最弱（§32：2.5 / 2 / 1.5px，无更强辉光）。
 * 几何来自 Phase1FLayout.continuityScene()（probe/测试共用，防漂移）。
 * 绘制辅助拆分：ContinuitySceneDrawing.kt（路径/号码面）、ContinuitySceneNodes.kt（服务节点/投影徽标）。
 *
 * 状态语义（§33/§34）：
 *   current    ：旧号强、新号 ghost，服务绑定旧号
 *   transition ：双方强，已迁移服务视觉靠向新号，等待保持中央，blocked 留在旧号
 *   after      ：旧号强淡出、新号主导；未迁移/未知保持可见未解决（计划投影，不冒充 Reality）
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
    val m = Phase1FLayout.continuityScene()
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        // §29 尺寸：OLD/NEW 250–300px、服务节点 130–170px、可用高 420–480px
        val oldX = w * m.oldXFraction
        val newX = w * m.newXFraction
        val cy = h * 0.5f
        val numW = w * m.numWFraction
        val numH = h * m.numHFraction
        val cols = m.colFractions.map { w * it }
        val rows = m.rowFractions.map { h * it }
        val nodeW = w * m.nodeWFraction
        val nodeH = h * m.nodeHFraction

        // 路径（最弱层级）：先画，再画对象
        services.forEachIndexed { i, srv ->
            drawServicePaths(
                fromX = oldX + numW * 0.5f,
                fromY = cy,
                toX = newX - numW * 0.5f,
                toY = cy,
                sx = cols[i % 2],
                sy = rows[i / 2],
                boxW = nodeW,
                status = srv.status,
                projection = projection,
                w = w,
            )
        }
        // 旧号 / 新号（主层级）
        drawNumberNode(
            textMeasurer = textMeasurer,
            center = Offset(oldX, cy),
            nodeW = numW,
            nodeH = numH,
            title = "旧号码",
            number = old.maskedNumber,
            meta = "${old.carrier} · ${roleLabelScene(old.role)}",
            emphasis = false,
            alpha = if (projection == "after") 0.25f else 1f,
            border = PdigV2Colors.BorderSubtle,
            accent = PdigV2Colors.TextMuted,
        )
        drawNumberNode(
            textMeasurer = textMeasurer,
            center = Offset(newX, cy),
            nodeW = numW,
            nodeH = numH,
            title = "新号码",
            number = new.maskedNumber,
            meta = "${new.carrier} · ${roleLabelScene(new.role)}",
            emphasis = projection != "current",
            alpha = if (projection == "current") 0.35f else 1f,
            border = if (projection == "after") PdigV2Colors.PrimaryBright else PdigV2Colors.PrimaryBright.copy(alpha = 0.4f),
            accent = PdigV2Colors.PrimaryBright,
        )
        // 服务节点（次级层级）
        services.forEachIndexed { i, srv ->
            drawServiceNode(
                textMeasurer = textMeasurer,
                sx = cols[i % 2],
                sy = rows[i / 2],
                name = srv.service,
                role = srv.role,
                status = srv.status,
                projection = projection,
                boxW = nodeW,
                boxH = nodeH,
            )
        }
        if (projection == "after") {
            drawPlanProjectionBadge(textMeasurer, w)
        }
    }
}

/** 场景可用高度常量（§29：420–480px；probe 用）。 */
internal const val CONTINUITY_SCENE_HEIGHT_PX = 430
