package com.pdig.uivnext.ui.r9

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.demo.demoAttention
import com.pdig.uivnext.demo.demoChanges
import com.pdig.uivnext.demo.demoUpcoming
import com.pdig.uivnext.model.ChangeMigration
import com.pdig.uivnext.model.ChangeStage
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.ui.VAppState

/**
 * R9 continuity choreography. Three projections remain distinct:
 * Current = reality; Transition = tracked steps; After = PLAN PROJECTION only.
 * No visual element can claim an unverified stage completed.
 */
@Composable
internal fun R9ChangePhoneScreen(app: VAppState) {
    val projection = app.changeProjection
    val stages = if(app.emptyDemo) emptyList() else UiVNextDemoFixture.changeStages
    val migrations = if(app.emptyDemo) emptyList() else UiVNextDemoFixture.changeMigrations
    val old = UiVNextDemoFixture.numberById("num-cn-1")
    val fresh = UiVNextDemoFixture.numberById("num-cn-3")
    val activeStep = stages.firstOrNull { it.status == "verifying" }
        ?: stages.firstOrNull { it.status != "completed" }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 13.dp, vertical = 12.dp)
        .testTag("pdig.r9.screen.change"),
        verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("current" to "当前", "transition" to "迁移中", "after" to "完成后（计划）")
                .forEach { (key,label) ->
                    Surface(
                        modifier = Modifier.clickable { app.changeProjection = key }
                            .testTag("pdig.change.projection.${key}"),
                        color = if(projection == key) R9.Mist else Color.White,
                        shape = RoundedCornerShape(11.dp),
                        border = BorderStroke(1.dp, if(projection == key) R9.Blue else R9.Line),
                    ) {
                        Text(label, Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
                            color = if(projection == key) R9.Blue else R9.Muted,
                            fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
        }
        R9StepCircles(stages)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("第 ${activeStep?.stage ?: 0}/6 步 · ${r9StageLabel(activeStep?.key ?: "")}",
                color = R9.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            R9Badge(if(projection == "after") "计划投影" else "待验证", R9.Amber)
        }
        R9SectionTitle("影响分析 · 关键服务")
        R9ServiceOrbit(
            number = old?.maskedNumber ?: "号码未记录",
            migrations = migrations,
            projection = projection,
        )
        when(projection) {
            "after" -> Surface(color = Color(0xFFFFF3E6), shape = RoundedCornerShape(14.dp)) {
                Text("完成后预览：仅为计划投影；未确认的迁移和恢复路径绝不显示为已完成。",
                    Modifier.padding(13.dp), color = R9.Ink, fontSize = 11.sp, lineHeight = 18.sp)
            }
            "current" -> Text("当前状态：保留旧号码与已记录服务的关联。",
                color = R9.Muted, fontSize = 11.sp)
            else -> Text("执行计划：完成必须有人工或实际验证，不根据时间推断成功。",
                color = R9.Muted, fontSize = 11.sp)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically) {
            R9NumberNode("旧手机号", old?.maskedNumber ?: "未记录", Color(0xFF1B417B),
                Modifier.weight(1f))
            Text("→", color = R9.Blue, fontSize = 18.sp)
            R9NumberNode("新手机号 · 目标", fresh?.maskedNumber ?: "未记录",
                Color(0xFF1683CF), Modifier.weight(1f))
        }
        R9SectionTitle("阶段明细", "查看记录 →") { app.navigate(VScreen.RECORDS) }
        stages.forEach { stage -> R9StageRow(stage) }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun R9StepCircles(stages: List<ChangeStage>) {
    Row(Modifier.fillMaxWidth().testTag(VTestIds.CHANGE_STEPPER_MINI),
        horizontalArrangement = Arrangement.SpaceBetween) {
        (1..6).forEach { idx ->
            val status = stages.firstOrNull { it.stage == idx }?.status ?: "not_started"
            val col = when(status) {
                "completed" -> R9.Green
                "verifying" -> R9.Amber
                "blocked" -> R9.Rose
                else -> R9.Line
            }
            Box(Modifier.size(29.dp).background(
                if(status == "completed") col else Color.White, CircleShape),
                contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(color = col, radius = size.minDimension / 2f - 1f,
                        style = Stroke(width = 2.5.dp.toPx()))
                }
                Text(idx.toString(), color = if(status == "completed") Color.White else R9.Ink,
                    fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun R9ServiceOrbit(number: String, migrations: List<ChangeMigration>, projection: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(263.dp).testTag("pdig.r9.change.orbit"),
        color = R9.Ice, shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, R9.Line),
    ) {
        Box(Modifier.fillMaxSize().background(R9.World)) {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height * .49f)
                val radius = size.minDimension * .34f
                drawCircle(R9.Blue.copy(alpha = .24f), radius, center,
                    style = Stroke(width = 2.dp.toPx()))
                drawCircle(R9.Blue.copy(alpha = .12f), radius * .78f, center,
                    style = Stroke(width = 2.dp.toPx()))
                val targets = listOf(
                    Offset(center.x-radius*.99f,center.y-radius*.85f),
                    Offset(center.x+radius*.99f,center.y-radius*.85f),
                    Offset(center.x-radius*.99f,center.y+radius*.85f),
                    Offset(center.x+radius*.99f,center.y+radius*.85f),
                )
                targets.take(migrations.size).forEach { target ->
                    drawLine(R9.Blue.copy(alpha = .29f), center, target, strokeWidth = 1.5.dp.toPx())
                    drawCircle(R9.Blue, 3.dp.toPx(), target)
                }
            }
            Surface(
                modifier = Modifier.align(Alignment.Center).size(width = 155.dp, height = 125.dp),
                color = Color(0xFF142F61), shape = RoundedCornerShape(24.dp),
                border = BorderStroke(2.dp, Color(0xFF76BCFF)), shadowElevation = 6.dp,
            ) {
                Column(Modifier.padding(horizontal = 9.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("▥    +86", color = Color(0xFFABD6FF), fontSize = 12.sp)
                    Text(number, color = Color.White, fontSize = 13.sp,
                        fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(
                        when(projection) {
                            "after" -> "计划投影 · 未执行"
                            "current" -> "当前身份 · 已记录"
                            else -> "迁移中 · 待核对"
                        }, color = Color(0xFFB9D9FF), fontSize = 10.sp)
                }
            }
            val corners = listOf(Alignment.TopStart, Alignment.TopEnd,
                Alignment.BottomStart, Alignment.BottomEnd)
            migrations.take(4).forEachIndexed { i,m ->
                Surface(
                    modifier = Modifier.align(corners[i]).padding(horizontal = 7.dp, vertical = 13.dp),
                    color = Color.White.copy(alpha = .96f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, R9.Line),
                ) {
                    Column(Modifier.width(101.dp).padding(horizontal = 7.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(m.service, color = R9.Ink, fontSize = 10.sp, maxLines = 1,
                            fontWeight = FontWeight.Bold)
                        Text(
                            when(m.status) {
                                "completed" -> "已验证"
                                "waiting", "verifying" -> "待验证"
                                "not_started" -> "未开始"
                                else -> "待确认"
                            }, color = R9.Muted, fontSize = 9.sp)
                    }
                }
            }
            Text("仅展示演示数据中的关联", color = R9.Muted, fontSize = 9.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp))
        }
    }
}

@Composable
private fun R9NumberNode(label: String, number: String, accent: Color, modifier: Modifier) {
    Surface(modifier, color = accent, shape = RoundedCornerShape(15.dp)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(label, color = Color(0xFFD5EBFF), fontSize = 10.sp)
            Text(number, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                maxLines = 1)
        }
    }
}

private fun r9StageLabel(key: String): String = when(key) {
    "impact-analysis" -> "影响分析"
    "establish-new-number" -> "建立新号码"
    "verify-new-number" -> "验证新号码"
    "migrate-key-accounts" -> "迁移关键账户"
    "check-recovery-paths" -> "检查恢复路径"
    "retire-old-number" -> "停用旧号"
    else -> "暂无阶段"
}

@Composable
private fun R9StageRow(stage: ChangeStage) {
    Surface(color = Color.White, shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, R9.Line), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Box(Modifier.size(26.dp).background(if(stage.status == "completed") R9.Blue else R9.Mist,
                CircleShape), contentAlignment = Alignment.Center) {
                Text(stage.stage.toString(), color = if(stage.status == "completed") Color.White else R9.Ink,
                    fontSize = 11.sp)
            }
            Text(r9StageLabel(stage.key), Modifier.weight(1f), color = R9.Ink,
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            R9Badge(when(stage.status) {
                "completed" -> "已完成"
                "verifying" -> "待验证"
                "blocked" -> "被阻断"
                else -> "未开始"
            }, when(stage.status) {
                "completed" -> R9.Green
                "blocked" -> R9.Rose
                "verifying" -> R9.Amber
                else -> R9.Muted
            })
        }
    }
}

/** R9 records: meaningful continuity timeline, not a huge generic statistics pane. */
@Composable
internal fun R9RecordsScreen(app: VAppState) {
    val changes = app.demoChanges()
    val attention = app.demoAttention()
    val upcoming = app.demoUpcoming()
    val stages = if(app.emptyDemo) emptyList() else UiVNextDemoFixture.changeStages
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 13.dp, vertical = 12.dp)
        .testTag("pdig.r9.screen.records"),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("追踪变更、风险与接下来已知的时间节点",
            color = R9.Muted, fontSize = 11.sp)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White, shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, R9.Line),
        ) {
            Row(Modifier.padding(vertical = 14.dp)) {
                R9Counter(changes.size, "进行中的变更", R9.Blue, "↻", Modifier.weight(1f))
                R9Counter(attention.size, "需要处理", R9.Rose, "!", Modifier.weight(1f))
                R9Counter(upcoming.size, "即将到来", R9.Amber, "◷", Modifier.weight(1f))
            }
        }
        R9SectionTitle("正在进行")
        changes.forEach { change ->
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { app.navigate(VScreen.CHANGE_PHONE) },
                color = R9.Mist, shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFABC9F3)),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(change.title, color = R9.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("当前阶段 · 验证新号码", color = R9.Amber, fontSize = 11.sp)
                    }
                    Text("继续查看 →", color = R9.Blue, fontSize = 11.sp)
                }
            }
        }
        if(changes.isEmpty()) Text("暂无已记录的进行中变更。", color = R9.Muted, fontSize = 12.sp)
        R9SectionTitle("迁移进度")
        stages.forEach { stage -> R9StageRow(stage) }
        if(stages.isEmpty()) Text("尚无已记录迁移步骤。", color = R9.Muted, fontSize = 12.sp)
        R9SectionTitle("需要处理（${attention.size}）")
        attention.take(3).forEach { item ->
            Surface(color = Color.White, shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, R9.Line), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    R9Badge("!", R9.Rose)
                    Text(item.title, Modifier.weight(1f), color = R9.Ink, fontSize = 11.sp)
                }
            }
        }
        R9SectionTitle("即将到来")
        upcoming.forEach { event ->
            Surface(color = Color.White, shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, R9.Line), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(event.title, color = R9.Ink, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    R9Badge("${event.days} 天", R9.Amber)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}
