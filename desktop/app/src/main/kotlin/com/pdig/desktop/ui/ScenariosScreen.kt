package com.pdig.desktop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pdig.core.scenario.ScenarioRegistry
import com.pdig.core.scenario.ScenarioTemplate
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.SectionHeader
import com.pdig.desktop.ui.theme.PdigDesktopTokens as T
import com.pdig.desktop.ui.theme.PdigType

/**
 * 场景中心（spec §30）：按真实用户意图组织 —— 支付 / 身份与恢复。
 * 每个场景回答：什么时候用 / 系统会帮你检查什么 / 大约有哪些步骤。
 * 未来功能不显示。
 */
@Composable
fun ScenariosScreen(ui: UiState) {
    PdigPage(
        title = "场景中心",
        subtitle = "模拟更换 / 注销一张卡、更换手机号前，先看清会影响到谁",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        Column(Modifier.fillMaxWidth()) {
            SectionHeader("支付")
            ScenarioRegistry.active.filter { isPaymentScenario(it) }.forEach { t ->
                ScenarioCard(t, Icons.Filled.Contactless, ui)
            }
            SectionHeader("身份与恢复")
            ScenarioRegistry.active.filter { !isPaymentScenario(it) }.forEach { t ->
                ScenarioCard(t, Icons.Filled.PhoneAndroid, ui)
            }
        }
    }
}

private fun isPaymentScenario(t: ScenarioTemplate): Boolean =
    t.id.startsWith("replace_payment") || t.id.contains("card") || t.id.contains("payment")

@Composable
private fun ScenarioCard(t: ScenarioTemplate, icon: ImageVector, ui: UiState) {
    Surface(
        Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(T.RadiusMd),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = T.SpaceLg, vertical = T.SpaceMd)) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(T.IconSize))
                Spacer(Modifier.width(T.SpaceSm))
                Text(t.title, style = PdigType.Body, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(4.dp))
            Text("什么时候用：${t.description}", style = PdigType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)
            t.recommendedLeadTimeDays?.let {
                Text("建议提前 $it 天准备", style = PdigType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "开始准备",
                Modifier.clickable {
                    ui.selectedScenarioId = t.id
                    ui.screen = Screen.SCENARIO_SETUP
                },
                style = PdigType.Label,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
    Spacer(Modifier.height(T.SpaceSm))
}
