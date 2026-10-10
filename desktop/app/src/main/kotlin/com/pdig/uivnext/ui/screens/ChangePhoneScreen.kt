package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.ChangeStage
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VType
import com.pdig.uivnext.theme.statusColor
import com.pdig.uivnext.theme.statusLabelZh
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.SectionHeader
import com.pdig.uivnext.ui.components.collectVNextInteraction
import com.pdig.uivnext.ui.components.rememberVNextInteractionSource


/**
 * Change Phone（PHASE 1D §25–30）：ContinuityScene 为主角（Compose Canvas），
 * 顶部 6 步 rail 降为 thin progress timeline（高度降 30–40%），下方详情默认折叠。
 */
@Composable
fun ChangePhoneScreen(app: VAppState) {
    val old = UiVNextDemoFixture.numberById("num-cn-1") ?: return
    val new = UiVNextDemoFixture.numberById("num-cn-3") ?: return
    val stages = UiVNextDemoFixture.changeStages
    val migrations = UiVNextDemoFixture.changeMigrations.map { m ->
        val role = when (m.service) {
            "微信支付" -> "支付 · 登录验证"
            "支付宝" -> "支付 · 恢复"
            "招商银行网银" -> "银行 · 资金"
            "腾讯视频" -> "订阅"
            else -> "登录验证"
        }
        SceneMigration(m.service, m.status, role)
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(VSpacing.Xxl),
        verticalArrangement = Arrangement.spacedBy(VSpacing.Xl),
    ) {
        PageHeader(
            title = "更换手机号",
            subtitle = "让旧号码承担的，由新号码接管 · 验证新号码可用之前，不要停用旧号码",
            trailing = {
                Surface(color = PdigV2Colors.PrimarySoft, shape = RoundedCornerShape(VRadius.Md)) {
                    Text(
                        "进度 ${stages.count { it.status == "completed" }} / ${stages.size}",
                        Modifier.padding(horizontal = VSpacing.Lg, vertical = VSpacing.Sm),
                        color = PdigV2Colors.PrimaryBright,
                        style = VType.Label,
                    )
                }
            },
        )

        // 顶部 6 步 → 紧凑 phase rail（thin timeline，高度比 1C 低 30–40%）
        CompactPhaseRail(stages, Modifier.testTagLocal(VTestIds.CHANGE_PROGRESS))

        // 投影选择（current / transition / after；after = Plan Projection）
        ProjectionSelector(app)

        // 中央 ContinuityScene（Canvas 场景；主角）
        ContinuityScene(
            old = old,
            new = new,
            services = migrations,
            projection = app.changeProjection,
            modifier = Modifier
                .fillMaxWidth()
                .height(CONTINUITY_SCENE_HEIGHT_PX.dp),
        )

        // 下方详情（默认折叠为 Inspector/Expandable）
        CollapsibleDetails(migrations)
        // 风险提示：compact critical notice（§37 不横贯大 banner）
        // 风险提示：compact notice（§36/§37：不横贯全宽 banner，除非 severity 真正 critical）
        Row(
            Modifier
                .fillMaxWidth(0.72f)
                .background(PdigV2Colors.Warning.copy(alpha = 0.10f), RoundedCornerShape(VRadius.Md))
                .padding(horizontal = VSpacing.Lg, vertical = VSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = PdigV2Colors.Warning,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(VSpacing.Sm))
            Text(
                "旧号码是 2 个账户的唯一恢复路径；「停用旧号码」必须保持禁用，直到新号码验证完成。",
                color = PdigV2Colors.TextPrimary,
                style = VType.Secondary,
            )
        }
    }
}

/** 紧凑 phase rail：单行圆点 + 细线 + 标签（高度 ~44dp，替代原 6 行大 rail）。 */
@Composable
private fun CompactPhaseRail(stages: List<ChangeStage>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
        stages.forEachIndexed { index, stage ->
            val color = statusColor(stage.status)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    Modifier
                        .size(if (stage.status == "verifying") 14.dp else 10.dp)
                        .background(color, CircleShape)
                        .border(1.dp, color.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (stage.status == "verifying") {
                        Box(Modifier.size(6.dp).background(PdigV2Colors.CanvasDeep, CircleShape))
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    stageLabelShort(stage.key),
                    style = VType.Label,
                    color = if (stage.status == "not_started") PdigV2Colors.TextMuted else PdigV2Colors.TextPrimary,
                    maxLines = 1,
                )
            }
            if (index < stages.lastIndex) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(PdigV2Colors.BorderSubtle, RoundedCornerShape(1.dp)),
                ) {
                    if (stage.status == "completed") {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(PdigV2Colors.Positive, RoundedCornerShape(1.dp)),
                        )
                    }
                }
            }
        }
    }
}

private fun stageLabelShort(key: String): String = when (key) {
    "impact-analysis" -> "影响分析"
    "establish-new-number" -> "建新号"
    "verify-new-number" -> "验证新号"
    "migrate-key-accounts" -> "迁移账户"
    "check-recovery-paths" -> "恢复路径"
    "retire-old-number" -> "停用旧号"
    else -> key
}

/** 投影选择器（当前 / 迁移中 / 完成后；After 显式标注 计划投影）。 */
@Composable
private fun ProjectionSelector(app: VAppState) {
    val projections = listOf("current" to "当前", "transition" to "迁移中", "after" to "完成后")
    Row(horizontalArrangement = Arrangement.spacedBy(VSpacing.Sm), verticalAlignment = Alignment.CenterVertically) {
        Text("投影", color = PdigV2Colors.TextMuted, style = VType.Label)
        projections.forEach { (key, label) ->
            val selected = app.changeProjection == key
            val source = rememberVNextInteractionSource()
            val hover = collectVNextInteraction(source).hovered
            Surface(
                color = when {
                    selected -> PdigV2Colors.PrimaryBright.copy(alpha = 0.14f)
                    hover -> PdigV2Colors.Primary.copy(alpha = 0.16f)
                    else -> PdigV2Colors.SurfaceGlass
                },
                shape = RoundedCornerShape(VRadius.Sm),
                border = BorderStroke(
                    1.dp,
                    when {
                        selected -> PdigV2Colors.PrimaryBright.copy(alpha = 0.3f)
                        hover -> PdigV2Colors.BorderStrong.copy(alpha = 0.7f)
                        else -> PdigV2Colors.BorderSubtle
                    },
                ),
                modifier = Modifier.clickable(
                    interactionSource = source,
                    indication = null,
                    onClick = { app.changeProjection = key },
                ),
            ) {
                Text(
                    label + if (key == "after") "· 计划投影" else "",
                    Modifier.padding(horizontal = VSpacing.Md, vertical = 4.dp),
                    color = if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.TextSecondary,
                    style = VType.Label,
                )
            }
        }
    }
}

/** 下方详情：默认折叠，展开为 Inspector（每服务一行状态）。 */
@Composable
private fun CollapsibleDetails(migrations: List<SceneMigration>) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        color = PdigV2Colors.Surface.copy(alpha = 0.9f),
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(Modifier.padding(VSpacing.Lg), verticalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (expanded) "迁移详情（点击收起）" else "迁移详情（点击展开）",
                    color = PdigV2Colors.TextPrimary,
                    style = VType.Label,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = PdigV2Colors.TextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
            if (expanded) {
                migrations.forEach { m ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(m.service, color = PdigV2Colors.TextSecondary, style = VType.Secondary)
                        Text(statusLabelZh(m.status), color = statusColor(m.status), style = VType.Label)
                    }
                }
            }
        }
    }
}
