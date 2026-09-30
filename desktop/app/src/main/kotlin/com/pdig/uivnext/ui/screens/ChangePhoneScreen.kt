package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VType
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.ContinuityRail
import com.pdig.uivnext.ui.components.LabelChip
import com.pdig.uivnext.ui.components.SectionHeader
import com.pdig.uivnext.ui.components.StatusBadge

/**
 * Change Phone（G12 flagship）：
 * 顶部 6-stage progress（ContinuityRail）；中央 旧手机号 → migration → 新手机号；
 * 左 = 仍依赖旧号的 services/accounts；右 = 已迁移 / 等待验证。
 * 状态至少 migrated / waiting / manual / blocked / not-started；
 * 一眼看出：旧号承担什么、新号接管什么、什么还不能停。
 */
@Composable
fun ChangePhoneScreen(app: VAppState) {
    val old = UiVNextDemoFixture.numberById("num-cn-1")
    val new = UiVNextDemoFixture.numberById("num-cn-3")
    val stages = UiVNextDemoFixture.changeStages
    val done = stages.count { it.status == "completed" }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(VSpacing.Xxl),
        verticalArrangement = Arrangement.spacedBy(VSpacing.Xxl),
    ) {
        PageHeader(
            title = "更换手机号",
            subtitle = "让旧号码承担的，由新号码接管 · 验证通过前旧号码不能停用（make-before-break）",
            trailing = {
                Surface(color = PdigV2Colors.PrimarySoft, shape = RoundedCornerShape(VRadius.Md)) {
                    Text(
                        "进度 $done / ${stages.size}",
                        Modifier.padding(horizontal = VSpacing.Lg, vertical = VSpacing.Sm),
                        color = PdigV2Colors.PrimaryBright,
                        style = VType.Label,
                    )
                }
            },
        )

        // 顶部 6-stage 进度（ContinuityRail）
        ContinuityRail(
            stages = stages,
            modifier = Modifier.testTagLocal(VTestIds.CHANGE_PROGRESS),
        )

        // 中央：旧手机号 → migration → 新手机号
        SectionHeader("旧手机号 → 关键服务 → 新手机号")
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VSpacing.Md),
            verticalAlignment = Alignment.Top,
        ) {
            if (old != null) {
                Surface(
                    Modifier.weight(1f),
                    color = PdigV2Colors.Surface.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(VRadius.Lg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                ) {
                    Column(Modifier.padding(VSpacing.Lg)) {
                        LabelChip("旧号码")
                        Spacer(Modifier.height(VSpacing.Sm))
                        Text(old.maskedNumber, style = VType.SectionTitle, color = PdigV2Colors.TextPrimary)
                        Text("${old.carrier} · 主号", color = PdigV2Colors.TextMuted, style = VType.Meta)
                        Spacer(Modifier.height(VSpacing.Sm))
                        Text("承担：银行验证 / 注册 / 2FA（${old.usages.size} 类用途）", color = PdigV2Colors.TextSecondary, style = VType.Secondary)
                    }
                }
            }
            Text("→", color = PdigV2Colors.TextMuted, style = VType.SectionTitle, modifier = Modifier.padding(top = 8.dp))
            Surface(
                Modifier.weight(1.3f),
                color = PdigV2Colors.Surface.copy(alpha = 0.96f),
                shape = RoundedCornerShape(VRadius.Lg),
                border = androidx.compose.foundation.BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Column(Modifier.padding(VSpacing.Lg), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LabelChip("关键服务与账户")
                    Spacer(Modifier.height(4.dp))
                    UiVNextDemoFixture.changeMigrations.forEach { m ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(m.service, color = PdigV2Colors.TextSecondary, style = VType.Secondary)
                            StatusBadge(m.status)
                        }
                    }
                }
            }
            Text("→", color = PdigV2Colors.TextMuted, style = VType.SectionTitle, modifier = Modifier.padding(top = 8.dp))
            if (new != null) {
                Surface(
                    Modifier.weight(1f),
                    color = PdigV2Colors.Primary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(VRadius.Lg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PdigV2Colors.PrimaryBright),
                ) {
                    Column(Modifier.padding(VSpacing.Lg)) {
                        LabelChip("新号码", highlight = true)
                        Spacer(Modifier.height(VSpacing.Sm))
                        Text(new.maskedNumber, style = VType.SectionTitle, color = PdigV2Colors.TextPrimary)
                        Text("${new.carrier} · 副号", color = PdigV2Colors.TextMuted, style = VType.Meta)
                        Spacer(Modifier.height(VSpacing.Sm))
                        Text("待验证通过后接管关键账户绑定", color = PdigV2Colors.Warning, style = VType.Secondary)
                    }
                }
            }
        }

        // 左 = 仍依赖旧号 / 右 = 等待验证与已迁移
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VSpacing.Xxl)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
                SectionHeader("仍依赖旧号码")
                val stillOnOld = UiVNextDemoFixture.changeMigrations.filter { it.status != "migrated" }
                if (stillOnOld.isEmpty()) {
                    Text("暂无 — 旧号码已完成接管", color = PdigV2Colors.TextMuted, style = VType.Secondary)
                } else {
                    stillOnOld.forEach { m ->
                        MigrationRow(m.service, m.status, "仍以旧号码为验证/扣款路径")
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
                SectionHeader("等待验证 / 已迁移")
                val waiting = UiVNextDemoFixture.changeMigrations.filter { it.status == "waiting" }
                val migrated = UiVNextDemoFixture.changeMigrations.filter { it.status == "migrated" }
                if (migrated.isEmpty() && waiting.isEmpty()) {
                    Text("暂无 — 验证新号码后迁移将在此列出", color = PdigV2Colors.TextMuted, style = VType.Secondary)
                } else {
                    waiting.forEach { m -> MigrationRow(m.service, m.status, "等待验证新号码后迁移") }
                    migrated.forEach { m -> MigrationRow(m.service, m.status, "已由新号码接管") }
                }
            }
        }

        SectionHeader("风险提示")
        Surface(color = PdigV2Colors.Warning.copy(alpha = 0.12f), shape = RoundedCornerShape(VRadius.Md), modifier = Modifier.fillMaxWidth()) {
            Text(
                "旧号码是 2 个账户的唯一恢复路径：迁移完成前不要停用；验证阶段未完成时「停用旧号码」必须保持禁用。",
                Modifier.padding(VSpacing.Lg),
                color = PdigV2Colors.TextPrimary,
                style = VType.Secondary,
            )
        }
    }
}

@Composable
private fun MigrationRow(name: String, status: String, hint: String) {
    Surface(
        color = PdigV2Colors.Surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(VRadius.Md),
        border = androidx.compose.foundation.BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = VSpacing.Lg, vertical = VSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, color = PdigV2Colors.TextPrimary, style = VType.Label)
                Text(hint, color = PdigV2Colors.TextMuted, style = VType.Meta)
            }
            Spacer(Modifier.width(VSpacing.Md))
            StatusBadge(status)
        }
    }
}
