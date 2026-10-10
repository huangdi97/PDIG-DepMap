package com.pdig.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.pdig.app.ui.Route
import com.pdig.app.ui.components.PdigCard
import com.pdig.app.ui.components.PdigTopBar
import com.pdig.app.ui.components.SectionHeader
import com.pdig.app.ui.theme.PdigTokens

/** 每个场景的 Three-Answer 文案：什么时候用 / 检查什么 / 大约步骤（不泄漏内部枚举）。 */
internal data class ScenarioThreeAnswer(val whenToUse: String, val checks: String, val steps: String)

internal fun scenarioThreeAnswer(templateId: String): ScenarioThreeAnswer = when (templateId) {
    "replace_payment_card" -> ScenarioThreeAnswer(
        whenToUse = "换卡前，先弄清楚哪些钱包、自动扣款和订阅依赖这张卡。",
        checks = "找出所有已确认使用这张卡付款的对象，标出换卡后可能中断的部分。",
        steps = "选择要更换的卡 → 查看影响 → 创建变更计划 → 按步骤迁移并验证。",
    )
    "expiring_payment_card" -> ScenarioThreeAnswer(
        whenToUse = "卡片到期前，提前确认还有哪些支付路径依赖它。",
        checks = "检查仍依赖这张卡的支付关系与自动扣款，评估到期后的影响。",
        steps = "选择到期的卡 → 查看影响 → 创建变更计划 → 在到期前完成迁移。",
    )
    "close_payment_instrument" -> ScenarioThreeAnswer(
        whenToUse = "注销前确认哪些支付关系需要先迁移或切换。",
        checks = "找出依赖这张卡的所有已确认支付关系，确认迁移或替代路径。",
        steps = "选择要注销的卡 → 查看影响 → 迁移关键支付 → 最后执行注销。",
    )
    "replace_phone_number" -> ScenarioThreeAnswer(
        whenToUse = "换号前，先弄清这个手机号承担了哪些登录验证、账号恢复与通讯能力。",
        checks = "检查该手机号关联的账户、恢复路径与共享故障点，确认换号后哪些会受影响。",
        steps = "选择旧手机号 → 查看影响与恢复路径 → 建立新路径并验证 → 迁移关键账户 → 停用旧手机号。",
    )
    else -> ScenarioThreeAnswer("", "", "")
}

/**
 * 场景中心：按用户意图分「支付 / 身份与恢复」两区（UIUX_FREEZE §13）。
 * 每场景回答「什么时候用 / 系统会帮你检查什么 / 大约有哪些步骤」；不显示未来功能。
 * 场景清单唯一来源是 ScenarioRegistry.active（planned 模板不展示）。
 */
@Composable
fun ScenarioCenterScreen(nav: NavController) {
    val payment = com.pdig.core.scenario.ScenarioRegistry.active.filter { it.category == "payment" }
    val identity = com.pdig.core.scenario.ScenarioRegistry.active.filter { it.category != "payment" }

    Scaffold(topBar = { PdigTopBar("场景", onBack = { nav.popBackStack() }) }) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(PdigTokens.SpaceLg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceMd),
        ) {
            if (payment.isNotEmpty()) {
                SectionHeader("支付")
                payment.forEach { t -> ScenarioCard(t.id, t.title, t.description, nav) }
            }
            if (identity.isNotEmpty()) {
                SectionHeader("身份与恢复")
                identity.forEach { t -> ScenarioCard(t.id, t.title, t.description, nav) }
        }
    }
}
}

@Composable
private fun ScenarioCard(templateId: String, title: String, description: String, nav: NavController) {
    val copy = scenarioThreeAnswer(templateId)
    PdigCard(onClick = { nav.navigate(Route.SCENARIO_SETUP.replace("{templateId}", templateId)) }) {
        Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs)) {
            Text(title, style = PdigTokens.BodyStrong)
            if (description.isNotBlank()) {
                Text(description, style = PdigTokens.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (copy.whenToUse.isNotBlank()) {
                Text("什么时候用", style = PdigTokens.Label, color = MaterialTheme.colorScheme.primary)
                Text(copy.whenToUse, style = PdigTokens.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (copy.checks.isNotBlank()) {
                Text("系统会帮你检查什么", style = PdigTokens.Label, color = MaterialTheme.colorScheme.primary)
                Text(copy.checks, style = PdigTokens.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (copy.steps.isNotBlank()) {
                Text("大约有哪些步骤", style = PdigTokens.Label, color = MaterialTheme.colorScheme.primary)
                Text(copy.steps, style = PdigTokens.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}