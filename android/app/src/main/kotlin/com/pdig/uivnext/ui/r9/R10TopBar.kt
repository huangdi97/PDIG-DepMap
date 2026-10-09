package com.pdig.uivnext.ui.r9

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
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
import com.pdig.app.BuildConfig
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.ui.VAppState

/** Five-section consumer shell replaces R8 top-command engineering toolbar in preview. */
@Composable
internal fun R10TopBar(app: VAppState) {
    Surface(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().height(59.dp)
            .testTag(VTestIds.NAV_TOP),
        color = Color.White,
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            if (app.canNavigateUp()) {
                Surface(
                    modifier = Modifier.size(42.dp).clickable { app.navigateUp() }
                        .testTag("pdig.r10.top.back"),
                    color = Color.White,
                    shape = RoundedCornerShape(13.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.ArrowBack, "返回上一级", tint = R9.Ink,
                            modifier = Modifier.size(22.dp))
                    }
                }
            }
            if (app.screen == VScreen.NOW) {
                Surface(color = R9.Blue, shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(34.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("P", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    when(app.screen) {
                        VScreen.NOW -> "PDIG"
                        VScreen.ME -> "我"
                        VScreen.CHANGE, VScreen.CHANGE_PHONE -> "更换手机号"
                        VScreen.OVERVIEW, VScreen.INFRASTRUCTURE -> "基础设施"
                        else -> app.screen.titleZh
                    },
                    color = R9.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                if (app.screen == VScreen.NOW) {
                    Text("预览 · ${BuildConfig.GIT_SHA} · R18", color = R9.Muted, fontSize = 9.sp)
                }
            }
            if (app.screen != VScreen.SEARCH) {
                Surface(
                    modifier = Modifier.size(42.dp).clickable { app.navigate(VScreen.SEARCH) }
                        .testTag("pdig.search.entry"),
                    color = Color.White, shape = RoundedCornerShape(14.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Search, contentDescription = "搜索",
                            tint = R9.Muted, modifier = Modifier.size(23.dp))
                    }
                }
            }
            if (app.screen != VScreen.ME) {
                Surface(
                    modifier = Modifier.size(42.dp).clickable { app.navigate(VScreen.ME) }
                        .testTag("pdig.r10.top.me"),
                    color = R9.Mist, shape = RoundedCornerShape(14.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Person, contentDescription = "我",
                            tint = R9.Blue, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}
