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

/** Object status and confidence are separate; unknown MUST NOT use a green success tint. */
internal fun r9CardStatusTint(status: String): Color = when(status) {
    "active" -> R9.Green
    "expiring_soon", "warning", "verifying" -> R9.Amber
    "expired", "blocked", "critical" -> R9.Rose
    else -> R9.Muted
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
        modifier = modifier.width(111.dp).defaultMinSize(minHeight = 48.dp).clickable(onClick = onClick)
            .testTag("pdig.r9.region.${region.regionCode}"),
        shape = RoundedCornerShape(15.dp), color = Color.White.copy(alpha = 0.91f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.85f)), shadowElevation = 2.dp,
    ) {
        Row(Modifier.padding(horizontal = 7.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(regionFlag(region.regionCode), fontSize = 18.sp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(region.displayName, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = R9.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${region.cardCount} 张卡 · ${region.phoneCount} 个号",
                    fontSize = 8.sp, color = R9.Muted, maxLines = 1)
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

/** Display-only privacy protection; never mutates PersonalReality or the stored value. */
internal fun r9VisibleLast4(last4: String, hidden: Boolean): String = if (hidden) "••••" else last4
internal fun r9VisibleNumber(number: String, hidden: Boolean): String = if (hidden) "号码已遮蔽" else number
