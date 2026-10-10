package com.pdig.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pdig.app.ui.theme.PdigStatus
import com.pdig.app.ui.theme.PdigTokens
import com.pdig.core.domain.ActionVerificationStatus
import com.pdig.core.domain.PlanAction

/**
 * 步骤轨道行（Continuity Rail，UIUX_FREEZE §3 / design-tokens §14）：
 * 左列 = 状态节点（icon/shape）+ 连接竖线；右列 = 标题 + 状态文字 + 闸门/前置说明 + 操作。
 * 状态永不只靠颜色：节点形状 + 文字 + 颜色三通道。
 */
@Composable
internal fun StepRailRow(
    state: StepState,
    isLast: Boolean,
    action: PlanAction,
    allActions: List<PlanAction>,
    onComplete: () -> Unit,
    onVerify: () -> Unit,
) {
    val prereqTitles = action.prerequisiteActionIds.mapNotNull { pid -> allActions.firstOrNull { it.id == pid }?.title }
    val dependsTitles = allActions.filter { action.id in it.prerequisiteActionIds }.map { it.title }
    val preconditionLines = buildPreconditionLines(action, prereqTitles, dependsTitles)
    val gate = gateReasonFor(action)

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PdigTokens.SpaceSm),
    ) {
        // 左列：节点 + 连接线（不引入 Canvas，用色块节点 + 竖线）
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            StepNode(state)
            if (!isLast) {
                Box(Modifier.width(2.dp).height(18.dp).background(connectorColor(state)))
            }
        }
        Column(
            Modifier.weight(1f).padding(bottom = PdigTokens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs),
        ) {
            Text(action.title, style = PdigTokens.BodyStrong)
            StepStateLine(state)
            gate?.let {
                Text(it, style = PdigTokens.Caption, color = PdigStatus.blocked)
            }
            preconditionLines.forEach {
                Text(it, style = PdigTokens.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            VerificationStatusLine(action)

            when (state) {
                StepState.IN_PROGRESS, StepState.UPCOMING -> Button(
                    onClick = onComplete,
                    modifier = Modifier.heightIn(min = PdigTokens.MinTouchTarget),
                ) { Text("标记完成") }
                StepState.BLOCKED -> Button(
                    onClick = onComplete,
                    enabled = false,
                    modifier = Modifier.heightIn(min = PdigTokens.MinTouchTarget),
                ) { Text("等待前置完成后可操作") }
                else -> {
                    // verified / completed：done 且未到终态 → 提供「确认验证」
                    val v = action.verification
                    if (action.done && v != null &&
                        (v.status == ActionVerificationStatus.PENDING ||
                            v.status == ActionVerificationStatus.EVIDENCE_SUGGESTED)
                    ) {
                        Button(
                            onClick = onVerify,
                            modifier = Modifier.heightIn(min = PdigTokens.MinTouchTarget),
                        ) { Text("确认验证") }
                    }
                }
            }
        }
    }
}

/** 步骤节点：verified=实心双勾（success 强）；completed=空心单勾（弱）；blocked=实心「!」。 */
@Composable
private fun StepNode(state: StepState) {
    when (state) {
        StepState.VERIFIED -> Box(
            Modifier.size(28.dp).background(MaterialTheme.colorScheme.secondary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(11.dp),
                )
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(11.dp),
                )
            }
        }
        StepState.COMPLETED -> Box(
            Modifier.size(28.dp).border(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
        }
        StepState.IN_PROGRESS -> Box(
            Modifier.size(28.dp).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        }
        StepState.BLOCKED -> Box(
            Modifier.size(28.dp).background(PdigStatus.blocked, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "!",
                color = MaterialTheme.colorScheme.onError,
                style = PdigTokens.Label,
                fontWeight = FontWeight.Bold,
            )
        }
        StepState.UPCOMING -> Box(
            Modifier.size(28.dp).border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(6.dp).background(MaterialTheme.colorScheme.outline, CircleShape))
        }
    }
}

/** 完成段的连接线用 success 实色，未完成段用中性色（形状不唯一通道）。 */
@Composable
private fun connectorColor(state: StepState): Color = when (state) {
    StepState.VERIFIED, StepState.COMPLETED -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.outline
}

/** 状态文字行：文字 + 颜色 + 小图标（与节点形状互补，保证三通道）。 */
@Composable
private fun StepStateLine(state: StepState) {
    when (state) {
        StepState.VERIFIED -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(12.dp),
            )
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(12.dp),
            )
            Text(stepStateLabel(state), style = PdigTokens.Label, color = MaterialTheme.colorScheme.secondary)
        }
        StepState.COMPLETED -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp),
            )
            Text(stepStateLabel(state), style = PdigTokens.Label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        StepState.BLOCKED -> Text(
            stepStateLabel(state),
            style = PdigTokens.Label,
            color = PdigStatus.blocked,
        )
        StepState.IN_PROGRESS -> Text(
            stepStateLabel(state),
            style = PdigTokens.Label,
            color = MaterialTheme.colorScheme.primary,
        )
        StepState.UPCOMING -> Text(
            stepStateLabel(state),
            style = PdigTokens.Label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 验证子状态行（VerificationScreen 语义）：动作已完成 ≠ 结果已验证。
 * verified 用双勾强色（在步骤态已表达）；这里把待验证 / 证据 / 失败分别用 icon+文字+颜色讲清。
 */
@Composable
private fun VerificationStatusLine(action: PlanAction) {
    val v = action.verification ?: return
    when (v.status) {
        ActionVerificationStatus.VERIFIED -> Unit // 步骤态「已验证」已表达（双勾 + 强色）
        ActionVerificationStatus.NOT_REQUIRED -> Text(
            "验证：无需验证",
            style = PdigTokens.Label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ActionVerificationStatus.PENDING -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = PdigStatus.verifying,
                modifier = Modifier.size(14.dp),
            )
            Column {
                Text("验证：待验证", style = PdigTokens.Label, color = PdigStatus.verifying)
                if (action.done) {
                    Text(
                        "动作已完成，结果尚未验证",
                        style = PdigTokens.Caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        ActionVerificationStatus.EVIDENCE_SUGGESTED -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = PdigStatus.reviewRequired,
                modifier = Modifier.size(14.dp),
            )
            Text(
                "验证：发现可能是变更后的证据",
                style = PdigTokens.Label,
                color = PdigStatus.reviewRequired,
            )
        }
        ActionVerificationStatus.FAILED -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                Icons.Filled.Clear,
                contentDescription = null,
                tint = PdigStatus.blocked,
                modifier = Modifier.size(14.dp),
            )
            Text("验证：验证未通过", style = PdigTokens.Label, color = PdigStatus.blocked)
        }
    }
}