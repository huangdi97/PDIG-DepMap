package com.pdig.uivnext.ui.r9

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.RegionPresentation

/** New reference-first preview presentation tokens. No Canonical/data writes. */
internal object R9 {
    val Ink = Color(0xFF112646)
    val Muted = Color(0xFF617896)
    val Blue = Color(0xFF176EF2)
    val Line = Color(0xFFCFE1F8)
    val Mist = Color(0xFFEAF4FF)
    val Paper = Color.White
    val Ice = Color(0xFFF5FAFF)
    val Green = Color(0xFF17B889)
    val Amber = Color(0xFFF39B49)
    val Rose = Color(0xFFDE6376)
    val Sky = Brush.verticalGradient(listOf(Color(0xFFF8FBFF), Color(0xFFD9ECFF), Color(0xFFECF6FF)))
    val World = Brush.radialGradient(listOf(Color(0xFFB1D4FF), Color(0xFFDDEEFF), Color(0xFFF6FBFF)))
}

internal fun regionFlag(code: String): String = when(code) {
    "CN" -> "🇨🇳"
    "HK" -> "🇭🇰"
    "MO" -> "🇲🇴"
    "GB" -> "🇬🇧"
    "US" -> "🇺🇸"
    "SG" -> "🇸🇬"
    else -> "🌐"
}

@Composable
internal fun R9SectionTitle(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = R9.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        if (action != null && onAction != null) {
            Text(action, color = R9.Blue, fontSize = 11.sp,
                modifier = Modifier.clickable { onAction() }.padding(vertical = 9.dp, horizontal = 3.dp))
        }
    }
}

@Composable
internal fun R9RegionPill(region: RegionPresentation, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.width(120.dp).clickable(onClick = onClick)
            .testTag("pdig.r9.region.${region.regionCode}"),
        shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, R9.Line), shadowElevation = 3.dp,
    ) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(regionFlag(region.regionCode), fontSize = 21.sp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(region.displayName, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = R9.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${region.cardCount} 张卡 · ${region.phoneCount} 个号",
                    fontSize = 9.sp, color = R9.Muted, maxLines = 1)
            }
        }
    }
}

@Composable
internal fun R9Counter(value: Int, name: String, accent: Color, glyph: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.size(25.dp).background(accent, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center) {
                Text(glyph, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text(value.toString(), color = R9.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Text(name, color = R9.Muted, fontSize = 10.sp)
    }
}

@Composable
internal fun R9Badge(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(50.dp), border = BorderStroke(1.dp, color.copy(alpha = 0.20f))) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
