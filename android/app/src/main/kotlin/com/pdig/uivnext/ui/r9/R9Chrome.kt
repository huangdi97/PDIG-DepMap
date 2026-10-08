package com.pdig.uivnext.ui.r9

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.ui.PRIMARY_ENTRIES
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.isEntrySelected

/** R9 phone primary nav: compact 4-item reference shell, 48dp hit targets. */
@Composable
internal fun R9BottomNav(app: VAppState) {
    Surface(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().testTag(VTestIds.NAV_BOTTOM),
        color = Color.White, shadowElevation = 7.dp,
        border = BorderStroke(1.dp, R9.Line.copy(alpha = .65f)),
    ) {
        Row(Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 7.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically) {
            PRIMARY_ENTRIES.forEach { entry ->
                val active = isEntrySelected(entry.screen, app.screen)
                Column(
                    Modifier.weight(1f).fillMaxHeight().clickable { app.navigate(entry.screen) }
                        .testTag("pdig.nav.${entry.screen.route}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Surface(
                        modifier = Modifier.width(55.dp).height(33.dp),
                        color = if (active) Color(0xFFDCE9FF) else Color.White,
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(entry.icon, contentDescription = entry.screen.titleZh,
                                tint = if (active) R9.Blue else R9.Muted,
                                modifier = Modifier.size(21.dp)
                                    .testTag("pdig.nav.${entry.screen.route}.icon"))
                        }
                    }
                    Text(entry.screen.titleZh,
                        modifier = Modifier.testTag("pdig.nav.${entry.screen.route}.label"),
                        color = if (active) R9.Blue else R9.Muted,
                        fontSize = 10.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}
