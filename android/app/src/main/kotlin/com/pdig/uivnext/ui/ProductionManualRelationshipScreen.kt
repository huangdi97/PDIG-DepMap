package com.pdig.uivnext.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.production.ManualRelationshipDefinitionView
import com.pdig.uivnext.production.ManualRelationshipInput
import com.pdig.uivnext.production.ManualRelationshipResultView
import com.pdig.uivnext.production.ProductionVNextSession
import com.pdig.uivnext.production.VNextProductionObject
import com.pdig.uivnext.production.supportedManualRelationshipDefinitions
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius

/**
 * Production-authoritative manual relationship flow.
 *
 * It exposes only the CURRENT v3 runtime relation registry. Storage-known future
 * relations (verifies / bound_to) are absent because their runtime definitions do
 * not exist. Capability is derived from RelationDefinition, never entered freely.
 */
@Composable
internal fun ProductionManualRelationshipScreen(
    session: ProductionVNextSession,
    modifier: Modifier = Modifier,
) {
    val gateway = session.authorities?.manualRelationship
    val snapshot = session.dataSource.productionSnapshot()
    if (gateway == null || snapshot == null) {
        ProductionManualRelationshipUnavailable(modifier)
        return
    }

    val definitions = remember { supportedManualRelationshipDefinitions() }
    var fromId by rememberSaveable { mutableStateOf("") }
    var relationId by rememberSaveable { mutableStateOf("") }
    var toId by rememberSaveable { mutableStateOf("") }
    var required by rememberSaveable { mutableStateOf(false) }
    var result by remember { mutableStateOf<ManualRelationshipResultView?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val from = snapshot.objects.firstOrNull { it.id == fromId }
    val relation = definitions.firstOrNull { it.relation == relationId }
    val to = snapshot.objects.firstOrNull { it.id == toId }

    val allowedRelations = definitions.filter { definition ->
        from != null && from.kind in definition.allowedFromKinds
    }
    val allowedTargets = snapshot.objects.filter { target ->
        relation != null && target.kind in relation.allowedToKinds
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("pdig.production-vnext.manual-relationship"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "手工记录关系",
                    color = PdigV2Colors.TextPrimary,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "你确认的是一条 Reality 关系。方向、关系类型和关键性都不会由界面猜测。",
                    color = PdigV2Colors.TextMuted,
                    fontSize = 12.sp,
                )
            }
        }

        item {
            ManualRelationBoundary(
                "当前直接写入只覆盖 Canonical v3 已正式注册的 runtime relations：funding_source / merchant_agreement / recovers / authenticates / controls。verifies / bound_to 仍不可创建。"
            )
        }

        item { ManualRelationSection("1 · 从哪个已确认对象开始？") }
        items(snapshot.objects, key = { "from:${it.id}" }) { item ->
            ManualObjectChoice(
                item = item,
                selected = item.id == fromId,
                onClick = {
                    fromId = item.id
                    relationId = ""
                    toId = ""
                    result = null
                    error = null
                },
                tag = "pdig.production-vnext.manual-relation.from.${item.id}",
            )
        }

        if (from != null) {
            item { ManualRelationSection("2 · 选择关系") }
            if (allowedRelations.isEmpty()) {
                item {
                    ManualRelationBoundary(
                        "当前 runtime registry 没有允许从“${productionObjectKindLabel(from.kind)}”出发的关系。"
                    )
                }
            } else {
                items(allowedRelations, key = { "relation:${it.relation}" }) { definition ->
                    ManualRelationDefinitionChoice(
                        definition = definition,
                        selected = definition.relation == relationId,
                        onClick = {
                            relationId = definition.relation
                            toId = ""
                            result = null
                            error = null
                        },
                    )
                }
            }
        }

        if (relation != null) {
            item {
                ManualRelationBoundary(
                    "能力由关系定义确定：${productionCapabilityLabel(relation.capability)}（${relation.capability}）。UI 不能另选一个不匹配的 capability。"
                )
            }
            item { ManualRelationSection("3 · 指向哪个已确认对象？") }
            if (allowedTargets.isEmpty()) {
                item {
                    ManualRelationBoundary("当前没有符合该关系 To-kind 约束的已确认对象。")
                }
            } else {
                items(allowedTargets, key = { "to:${it.id}" }) { item ->
                    ManualObjectChoice(
                        item = item,
                        selected = item.id == toId,
                        onClick = {
                            toId = item.id
                            result = null
                            error = null
                        },
                        tag = "pdig.production-vnext.manual-relation.to.${item.id}",
                    )
                }
            }
        }

        if (to != null && relation != null) {
            item { ManualRelationSection("4 · 关键性") }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ManualCriticalityChoice(
                        title = "未知",
                        body = "默认。unknown 不等于 optional，也不等于安全。",
                        selected = !required,
                        modifier = Modifier.weight(1f),
                    ) {
                        required = false
                        result = null
                    }
                    ManualCriticalityChoice(
                        title = "已确认必需",
                        body = "只有你明确知道它是必须路径时选择。",
                        selected = required,
                        modifier = Modifier.weight(1f),
                    ) {
                        required = true
                        result = null
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = PdigV2Colors.Surface,
                    shape = RoundedCornerShape(VRadius.Lg),
                    border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                ) {
                    Column(
                        Modifier.padding(13.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("确认预览", color = PdigV2Colors.TextPrimary,
                            fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${from.name} → ${productionRelationLabel(relation.relation)} → ${to.name}",
                            color = PdigV2Colors.TextPrimary,
                            fontSize = 11.sp,
                        )
                        Text(
                            "能力：${productionCapabilityLabel(relation.capability)} · 关键性：${if (required) "已确认必需" else "未知"} · 来源：手工确认",
                            color = PdigV2Colors.TextMuted,
                            fontSize = 9.sp,
                        )
                    }
                }
            }

            error?.let { message ->
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = PdigV2Colors.Critical.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(VRadius.Md),
                    ) {
                        Text(message, Modifier.padding(12.dp),
                            color = PdigV2Colors.Critical, fontSize = 10.sp)
                    }
                }
            }

            result?.let { saved ->
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pdig.production-vnext.manual-relation.result"),
                        color = PdigV2Colors.PrimarySoft,
                        shape = RoundedCornerShape(VRadius.Lg),
                    ) {
                        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                when {
                                    saved.created -> "关系已写入已确认数据"
                                    saved.reactivated -> "原关系已重新激活并重新确认"
                                    else -> "已有关系已重新确认"
                                },
                                color = PdigV2Colors.PrimaryText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "${productionRelationLabel(saved.relation)} · ${productionCapabilityLabel(saved.capability)} · ${productionCriticalityLabel(saved.criticality)}",
                                color = PdigV2Colors.TextPrimary,
                                fontSize = 10.sp,
                            )
                            Text(
                                "graphRevision = ${saved.graphRevision}",
                                color = PdigV2Colors.TextMuted,
                                fontSize = 9.sp,
                            )
                        }
                    }
                }
            }

            item {
                val canSubmit = fromId.isNotBlank() && relationId.isNotBlank() && toId.isNotBlank()
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pdig.production-vnext.manual-relation.save")
                        .then(
                            if (canSubmit) {
                                Modifier.clickable {
                                    error = null
                                    result = null
                                    try {
                                        result = gateway.create(
                                            ManualRelationshipInput(
                                                fromNodeId = fromId,
                                                relation = relationId,
                                                toNodeId = toId,
                                                required = required,
                                            ),
                                        )
                                    } catch (_: Throwable) {
                                        error = "关系未写入。输入不符合当前 runtime relation registry，或正式数据写入失败。"
                                    }
                                }
                            } else {
                                Modifier
                            },
                        ),
                    color = if (canSubmit) PdigV2Colors.PrimarySoft else PdigV2Colors.SurfaceRaised,
                    shape = RoundedCornerShape(VRadius.Lg),
                    border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                ) {
                    Text(
                        "确认这条关系并写入",
                        Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                        color = if (canSubmit) PdigV2Colors.PrimaryText else PdigV2Colors.TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        item {
            ManualRelationBoundary(
                "两条关系不等于两条独立路径。FailureDomain / RecoveryCycle / ProviderPolicy 仍由 Continuity engine 判断，手工关系页不会计算安全分或独立路径数。"
            )
        }
    }
}

@Composable
private fun ManualObjectChoice(
    item: VNextProductionObject,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(tag),
        color = if (selected) PdigV2Colors.PrimarySoft else PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(
            1.dp,
            if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.BorderSubtle,
        ),
    ) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.name, color = PdigV2Colors.TextPrimary,
                fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(productionObjectKindLabel(item.kind), color = PdigV2Colors.TextMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun ManualRelationDefinitionChoice(
    definition: ManualRelationshipDefinitionView,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("pdig.production-vnext.manual-relation.relation.${definition.relation}"),
        color = if (selected) PdigV2Colors.PrimarySoft else PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(
            1.dp,
            if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.BorderSubtle,
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(productionRelationLabel(definition.relation),
                    color = PdigV2Colors.TextPrimary, fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold)
                Text(definition.relation, color = PdigV2Colors.TextMuted, fontSize = 8.sp)
            }
            Text(productionCapabilityLabel(definition.capability),
                color = PdigV2Colors.PrimaryText, fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ManualCriticalityChoice(
    title: String,
    body: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = if (selected) PdigV2Colors.PrimarySoft else PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(
            1.dp,
            if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.BorderSubtle,
        ),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = PdigV2Colors.TextPrimary, fontSize = 10.sp,
                fontWeight = FontWeight.Bold)
            Text(body, color = PdigV2Colors.TextMuted, fontSize = 8.sp)
        }
    }
}

@Composable
private fun ManualRelationSection(title: String) {
    Text(title, color = PdigV2Colors.TextPrimary, fontSize = 13.sp,
        fontWeight = FontWeight.Bold)
}

@Composable
private fun ManualRelationBoundary(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.PrimarySoft.copy(alpha = 0.58f),
        shape = RoundedCornerShape(VRadius.Lg),
    ) {
        Text(text, Modifier.padding(12.dp), color = PdigV2Colors.TextSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun ProductionManualRelationshipUnavailable(modifier: Modifier) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("手工关系 authority 当前不可用", color = PdigV2Colors.TextPrimary,
            fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            "不会提供假的确认按钮，也不会直接写 SQL。",
            color = PdigV2Colors.TextSecondary,
            fontSize = 12.sp,
        )
    }
}
