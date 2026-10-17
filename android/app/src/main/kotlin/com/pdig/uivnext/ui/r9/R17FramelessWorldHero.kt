package com.pdig.uivnext.ui.r9

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.globe.R15WorldScene
import com.pdig.uivnext.globe.focusCamera
import com.pdig.uivnext.model.RegionPresentation
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.screens.arcPairs

/** Frameless reference world; factual R16 region tags, four distinct mini assets. */
@Composable
internal fun R17FramelessWorldHero(
    app: VAppState, regions: List<RegionPresentation>,
    cards: Int, numbers: Int, accounts: Int, services: Int,
) {
    Column(
        Modifier.fillMaxWidth().testTag(VTestIds.NOW_GLOBE)
            .testTag("pdig.r9.world.stage").testTag("pdig.r15.hero")
            .testTag("pdig.r17.frameless-hero"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("你的全球数字基础设施", color = R9.Ink,
                    fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(if (regions.isEmpty()) "尚无已记录地区"
                    else "连接 ${regions.size} 个地区 · 轻触地球探索",
                    color = R9.Muted, fontSize = 10.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                R17Control("+", "放大地球", "zoom-in") { app.globe.zoomBy(1.20f) }
                R17Control("−", "缩小地球", "zoom-out") { app.globe.zoomBy(1f/1.20f) }
                R17Control("↺", "复位地球", "reset") {
                    app.globe.backToGlobal()
                    app.globe.camera = focusCamera(16f, 107f)
                }
            }
        }
        // An open GPU sky replaces the old bordered 435dp administration card.
        Box(Modifier.fillMaxWidth().height(374.dp)
            .testTag("pdig.r9.world.hero")
            .testTag("pdig.r15.hero.spatial-stage")
            .testTag("pdig.r17.planet-plane")) {
            R15WorldScene(
                controller=app.globe, regions=regions,
                arcingPairs=if (app.emptyDemo) emptyList() else arcPairs(),
                reduceMotion=app.reduceMotion,
                onRegionChosen={
                    app.selectRegion(it.regionCode)
                    app.navigate(VScreen.OVERVIEW)
                },
            )
            R16ProjectedRegionOverlay(
                controller=app.globe, regions=regions,
                onRegionChosen={
                    app.selectRegion(it.regionCode)
                    app.navigate(VScreen.OVERVIEW)
                },
            )
        }
        Row(Modifier.fillMaxWidth().height(67.dp)
            .testTag("pdig.r13.world.asset-rail")
            .testTag("pdig.r15.hero.asset-rail").testTag("pdig.r17.assets"),
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            R17MiniAsset(cards,"银行卡",R9.Amber,"▣",Modifier.weight(1f)) { app.navigate(VScreen.CARDS) }
            R17MiniAsset(numbers,"手机号",R9.Green,"▤",Modifier.weight(1f)) { app.navigate(VScreen.NUMBERS) }
            R17MiniAsset(accounts,"账户",R9.Blue,"◎",Modifier.weight(1f)) { app.navigate(VScreen.ACCOUNTS) }
            R17MiniAsset(services,"服务",Color(0xFF9877ED),"✧",Modifier.weight(1f)) { app.navigate(VScreen.SERVICES) }
        }
    }
}

@Composable
private fun R17MiniAsset(count:Int,label:String,accent:Color,glyph:String,
    modifier:Modifier=Modifier,onClick:()->Unit) {
    Surface(
        modifier=modifier.fillMaxHeight().clickable(onClick=onClick),
        shape=RoundedCornerShape(14.dp), color=Color.White.copy(alpha=.94f),
        border=BorderStroke(1.dp,R9.Line.copy(alpha=.66f)),shadowElevation=1.dp,
    ) {
        Box(Modifier.padding(horizontal=2.dp,vertical=7.dp),
            contentAlignment=Alignment.Center) { R9Counter(count,label,accent,glyph) }
    }
}

@Composable
private fun R17Control(glyph:String, title:String, action:String, onClick:()->Unit) {
    Surface(
        modifier=Modifier.size(34.dp).clickable(onClick=onClick)
            .testTag("pdig.r14.globe.$action"),
        shape=RoundedCornerShape(11.dp), color=Color.White.copy(alpha=.87f),
        border=BorderStroke(1.dp,R9.Line.copy(alpha=.75f)),
    ) {
        Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
            Text(glyph,color=R9.Blue,fontSize=17.sp,fontWeight=FontWeight.Bold,
                modifier=Modifier.semantics { contentDescription=title })
        }
    }
}
