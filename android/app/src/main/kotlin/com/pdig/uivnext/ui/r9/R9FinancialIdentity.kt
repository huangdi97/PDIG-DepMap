package com.pdig.uivnext.ui.r9

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.UiVNextCard
import com.pdig.uivnext.model.VTestIds

/** Preview-only card art: not issuer logos. Uses only real fixture issuer/network values. */
internal data class R9CardPalette(val a: Color, val b: Color, val glint: Color)
internal fun r9CardPalette(issuer: String): R9CardPalette = when {
    issuer.contains("招商") -> R9CardPalette(Color(0xFFFD3645), Color(0xFFB61237), Color(0xFFFFA4A9))
    issuer.contains("HSBC", ignoreCase = true) || issuer.contains("汇丰") ->
        R9CardPalette(Color(0xFFBA0928), Color(0xFF280515), Color(0xFFF95779))
    issuer.contains("Revolut", ignoreCase = true) ->
        R9CardPalette(Color(0xFF7422D7), Color(0xFF1B156E), Color(0xFFC693FF))
    issuer.contains("Chase", ignoreCase = true) ->
        R9CardPalette(Color(0xFF1679EA), Color(0xFF041849), Color(0xFF49C7FF))
    issuer.contains("中国工商") || issuer.contains("工行") ->
        R9CardPalette(Color(0xFFE63838), Color(0xFF65192C), Color(0xFFFFA25B))
    issuer.contains("Monzo", ignoreCase = true) ->
        R9CardPalette(Color(0xFFFF785E), Color(0xFF912644), Color(0xFFFFD3BA))
    issuer.contains("DBS", ignoreCase = true) ->
        R9CardPalette(Color(0xFFDA2843), Color(0xFF50143E), Color(0xFFFF9D82))
    issuer.contains("Capital One", ignoreCase = true) ->
        R9CardPalette(Color(0xFF2579D8), Color(0xFF0B274F), Color(0xFF7AC7FF))
    else -> R9CardPalette(Color(0xFF2068CE), Color(0xFF081A43), Color(0xFF6ABBF7))
}

/** One R9 issuer/material renderer for preview card list AND full card detail. */
@Composable
internal fun R9BankCardFace(
    card: UiVNextCard,
    privacyMask: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: () -> Unit = {},
) {
    val colors = r9CardPalette(card.issuer)
    Surface(
        modifier = modifier.clip(RoundedCornerShape(if(compact) 11.dp else 17.dp))
            .clickable(onClick = onClick).testTag(VTestIds.CARD_FACE),
        color = colors.b, shape = RoundedCornerShape(if(compact) 11.dp else 17.dp),
        shadowElevation = if (compact) 1.dp else 4.dp,
    ) {
        Box(
            Modifier.aspectRatio(1.586f)
                .background(Brush.linearGradient(listOf(colors.a, colors.b, colors.a.copy(alpha = .85f)))),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(colors.glint.copy(alpha = .12f),
                    radius = size.width * .60f,
                    center = Offset(size.width * 1.01f, size.height * .05f))
                drawCircle(colors.glint.copy(alpha = .29f),
                    radius = size.width * .43f,
                    center = Offset(size.width * .95f, size.height * .08f),
                    style = Stroke(width = if(compact) 2f else 7f))
                drawCircle(Color.White.copy(alpha = .13f),
                    radius = size.width * .62f,
                    center = Offset(size.width * .85f, size.height * .97f),
                    style = Stroke(width = if(compact) 2f else 4f))
                drawLine(
                    colors.glint.copy(alpha = .56f),
                    Offset(size.width * .35f, 0f),
                    Offset(size.width * .99f, size.height),
                    strokeWidth = if(compact) 1.2f else 3f,
                )
            }
            Column(
                Modifier.fillMaxSize().padding(if (compact) 7.dp else 18.dp),
                verticalArrangement = Arrangement.spacedBy(if(compact) 1.dp else 6.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        card.issuer,
                        modifier = Modifier.weight(1f),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compact) 8.sp else 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(")))", color = Color.White.copy(alpha = .91f),
                        fontSize = if(compact) 8.sp else 16.sp)
                }
                Spacer(Modifier.weight(1f))
                Text("▤", color = Color(0xFFE7C787),
                    fontSize = if(compact) 12.sp else 24.sp)
                Text(
                    if(privacyMask) "••••" else "••••  ${card.last4}",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if(compact) 9.sp else 18.sp,
                    maxLines = 1,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("${regionFlag(card.region)} ${card.currency}",
                        color = Color.White.copy(alpha = .91f), fontSize = if(compact) 7.sp else 11.sp)
                    Text(card.network.uppercase(), color = Color.White,
                        fontWeight = FontWeight.Black, fontSize = if(compact) 9.sp else 20.sp,
                        maxLines = 1)
                }
            }
        }
    }
}
