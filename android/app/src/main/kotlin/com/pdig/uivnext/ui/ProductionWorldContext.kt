package com.pdig.uivnext.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.globe.R15WorldScene
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius

/**
 * Production Reality world context.
 *
 * Region is not currently a governed Canonical production fact. Therefore this
 * surface deliberately renders the same GPU Earth family with ZERO region labels,
 * ZERO asset pins and ZERO arcs. It preserves the product's spatial visual identity
 * without projecting synthetic geography into Production Reality.
 */
@Composable
internal fun ProductionWorldContext(
    app: VAppState,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .testTag("pdig.production-vnext.world-context"),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Xl),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Box(Modifier.fillMaxSize()) {
            R15WorldScene(
                controller = app.globe,
                regions = emptyList(),
                arcingPairs = emptyList(),
                reduceMotion = app.reduceMotion,
                onRegionChosen = {},
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                color = PdigV2Colors.Surface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(VRadius.Md),
                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Column(Modifier.padding(horizontal = 11.dp, vertical = 8.dp)) {
                    Text(
                        "全球上下文",
                        color = PdigV2Colors.TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "地区定位尚未进入正式数据模型 · 不显示推测资产位置",
                        color = PdigV2Colors.TextMuted,
                        fontSize = 9.sp,
                    )
                }
            }
        }
    }
}
