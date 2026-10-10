package com.pdig.uivnext.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.production.VNextProductionObject
import com.pdig.uivnext.production.VNextProductionSnapshot
import com.pdig.uivnext.production.VNextProductionSurfaceKind
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius

/**
 * A compact, object-centered visual relationship lens grounded exclusively in
 * confirmed Production dependencies. Edges indicate recorded relationships, NOT
 * independent recovery paths, unique recovery facts, or an inferred risk score.
 */
internal data class ProductionOrbitPeer(
    val objectId: String,
    val relation: String,
    val relationCount: Int,
    val outward: Boolean,
)

internal fun productionOrbitPeers(
    snapshot: VNextProductionSnapshot,
    focalId: String,
): List<ProductionOrbitPeer> {
    val knownIds = snapshot.objects.map { it.id }.toSet()
    return snapshot.confirmedDependencies.mapNotNull { edge ->
        val outward = edge.fromId == focalId
        val peerId = when {
            outward -> edge.toId
            edge.toId == focalId -> edge.fromId
            else -> return@mapNotNull null
        }
        if (peerId == focalId || peerId !in knownIds) return@mapNotNull null
        ProductionOrbitPeer(peerId, edge.relation, 1, outward)
    }.groupBy { it.objectId }
        .toSortedMap()
        .values
        .map { edges -> edges.first().copy(relationCount = edges.size) }
}

private fun canOpenProductionOrbitPeer(item: VNextProductionObject): Boolean =
    item.surfaceKind in setOf(
        VNextProductionSurfaceKind.PAYMENT_ASSET,
        VNextProductionSurfaceKind.PHONE_IDENTITY,
        VNextProductionSurfaceKind.EMAIL_IDENTITY,
        VNextProductionSurfaceKind.ACCOUNT,
        VNextProductionSurfaceKind.DEVICE,
        VNextProductionSurfaceKind.SERVICE,
    )

internal fun openProductionOrbitPeer(app: VAppState, item: VNextProductionObject) {
    when (item.surfaceKind) {
        VNextProductionSurfaceKind.PAYMENT_ASSET -> app.openCard(item.id)
        VNextProductionSurfaceKind.PHONE_IDENTITY -> app.openNumber(item.id)
        VNextProductionSurfaceKind.EMAIL_IDENTITY ->
            app.openSecondaryObject(com.pdig.uivnext.model.VScreen.EMAIL_DETAIL, item.id)
        VNextProductionSurfaceKind.ACCOUNT ->
            app.openSecondaryObject(com.pdig.uivnext.model.VScreen.ACCOUNT_DETAIL, item.id)
        VNextProductionSurfaceKind.DEVICE ->
            app.openSecondaryObject(com.pdig.uivnext.model.VScreen.DEVICE_DETAIL, item.id)
        VNextProductionSurfaceKind.SERVICE ->
            app.openSecondaryObject(com.pdig.uivnext.model.VScreen.SERVICE_DETAIL, item.id)
        else -> Unit
    }
}

@Composable
internal fun ProductionRelationshipConstellation(
    snapshot: VNextProductionSnapshot,
    focal: VNextProductionObject,
    app: VAppState,
    modifier: Modifier = Modifier,
) {
    val peers = productionOrbitPeers(snapshot, focal.id)
    if (peers.isEmpty()) return
    val byId = snapshot.objects.associateBy { it.id }
    val visible = peers.take(6)

    Column(
        modifier = modifier.fillMaxWidth().testTag("pdig.production-vnext.dependency-constellation"),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text("关系视图", fontSize = 14.sp, fontWeight = FontWeight.Bold,
            color = PdigV2Colors.TextPrimary)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PdigV2Colors.Surface,
            shape = RoundedCornerShape(VRadius.Xl),
            border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth().height(285.dp).padding(9.dp)) {
                val bubbleWidth = (maxWidth * 0.31f).coerceAtMost(190.dp)
                val centerWidth = (maxWidth * 0.34f).coerceAtMost(190.dp)
                val bubbleHeight = 65.dp
                val centerTop = 105.dp
                Canvas(Modifier.fillMaxWidth().height(265.dp)) {
                    val center = Offset(size.width / 2f, (centerTop + 30.dp).toPx())
                    visible.forEachIndexed { index, _ ->
                        val leftSide = index % 2 == 0
                        val row = index / 2
                        val destination = Offset(
                            if (leftSide) bubbleWidth.toPx() / 2f
                            else size.width - bubbleWidth.toPx() / 2f,
                            (8.dp + row * 89.dp + bubbleHeight / 2).toPx(),
                        )
                        drawLine(
                            Color(0xFF529AE9).copy(alpha = 0.38f),
                            start = center, end = destination, strokeWidth = 2.dp.toPx(),
                        )
                        drawCircle(Color(0xFF237CDA), 3.dp.toPx(), center = destination)
                    }
                }
                visible.forEachIndexed { index, peer ->
                    val item = byId.getValue(peer.objectId)
                    val leftSide = index % 2 == 0
                    val row = index / 2
                    val label = productionVisibleRelationPeerName(
                        snapshot, item.id, item.name, app,
                    )
                    Surface(
                        modifier = Modifier
                            .offset(
                                x = if (leftSide) 0.dp else maxWidth - bubbleWidth,
                                y = 8.dp + row * 89.dp,
                            )
                            .width(bubbleWidth)
                            .height(bubbleHeight)
                            .then(
                                if (canOpenProductionOrbitPeer(item)) {
                                    Modifier.clickable { openProductionOrbitPeer(app, item) }
                                } else Modifier
                            )
                            .testTag("pdig.production-vnext.orbit.peer"),
                        color = PdigV2Colors.SurfaceRaised,
                        shape = RoundedCornerShape(VRadius.Md),
                        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                    ) {
                        Column(
                            Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(label, color = PdigV2Colors.TextPrimary,
                                fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(productionRelationLabel(peer.relation) +
                                    if (peer.relationCount > 1) " · ${peer.relationCount} 条" else "",
                                color = PdigV2Colors.TextMuted, fontSize = 9.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .offset(x = (maxWidth - centerWidth) / 2, y = centerTop)
                        .width(centerWidth)
                        .height(62.dp),
                    color = PdigV2Colors.PrimarySoft,
                    shape = RoundedCornerShape(VRadius.Lg),
                    border = BorderStroke(1.dp, Color(0xFF68A6E9)),
                ) {
                    Column(
                        Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            productionVisibleRelationPeerName(snapshot, focal.id, focal.name, app),
                            color = PdigV2Colors.TextPrimary, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text("当前对象", color = PdigV2Colors.TextMuted, fontSize = 9.sp)
                    }
                }
            }
        }
        Text(
            "${snapshot.confirmedDependencies.count { it.fromId == focal.id || it.toId == focal.id }} 条已确认关系" +
                if (peers.size > visible.size) " · 仅展示前 ${visible.size} 个关联对象" else "",
            color = PdigV2Colors.TextSecondary, fontSize = 10.sp,
        )
        Text(
            "连线仅代表已确认的对象关系，不代表独立恢复路径或安全结论；下方列表保留全部关系。",
            color = PdigV2Colors.TextMuted, fontSize = 9.sp,
        )
    }
}
