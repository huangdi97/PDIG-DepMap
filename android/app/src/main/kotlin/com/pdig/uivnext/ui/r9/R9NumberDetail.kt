package com.pdig.uivnext.ui.r9

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.model.hexColorOrNull
import com.pdig.uivnext.model.relationKindLabelZh
import com.pdig.uivnext.model.serviceKindLabelZh
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.NumberFace

/** R9 communication-identity detail; never conflates account recovery with payment assets. */
@Composable
internal fun R9NumberDetailScreen(app: VAppState) {
    if (app.emptyDemo) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text("没有已记录号码。未知不等于安全。", color = R9.Muted, fontSize = 12.sp)
        }
        return
    }
    val number = app.selectedNumberId?.let { UiVNextDemoFixture.numberById(it) }
    if (number == null) {
        Text("未选择有效的号码对象。请从号码列表进入详情。",
            Modifier.fillMaxWidth().padding(20.dp), color = R9.Muted, fontSize = 12.sp)
        return
    }
    val related = UiVNextDemoFixture.servicesForNumber(number.id)
    val lifecycle = UiVNextDemoFixture.numberLifecycleFor(number.id)
    val profile = app.savedPresentationProfile("phoneNumber", number.id)
    val title = app.numberDisplayNameForScreen(number.id, number.maskedNumber)
    var editingName by remember(number.id) { mutableStateOf(false) }
    var proposedName by remember(number.id) { mutableStateOf(app.numberAlias(number.id)) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 13.dp, vertical = 12.dp)
            .testTag("pdig.r9.screen.number-detail"),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        R9SectionTitle(title, "修改名称 →") {
            proposedName = app.numberAlias(number.id)
            editingName = true
        }
        Text("未命名时显示记录中的号码", color = R9.Muted, fontSize = 10.sp)
        NumberFace(
            number = number.copy(nickname = title, preset = profile?.themeId ?: number.preset),
            privacyMask = app.privacyMask || (profile?.maskSensitive == true),
            onClick = {},
            modifier = Modifier.fillMaxWidth().testTag(VTestIds.NUMBER_DETAIL_HERO),
            presentationMaterial = profile?.material,
            presentationAccent = hexColorOrNull(profile?.accentColor ?: "default"),
            presentationLayout = profile?.layout,
        )
        Surface(color = Color.White, shape = RoundedCornerShape(17.dp),
            border = BorderStroke(1.dp, R9.Line),
            modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(vertical = 13.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("地区 / 运营商", color = R9.Muted, fontSize = 10.sp)
                    Text("${regionFlag(number.region)} ${number.carrier}", color = R9.Ink,
                        fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("角色 / SIM", color = R9.Muted, fontSize = 10.sp)
                    Text(
                        (when(number.role) {
                            "primary" -> "主号码"
                            "keep" -> "保号"
                            "secondary" -> "副号"
                            else -> number.role
                        }) + " · " + number.simKind,
                        color = R9.Ink, fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Column(horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("关联服务", color = R9.Muted, fontSize = 10.sp)
                    Text("${related.size} 项", color = R9.Blue, fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().testTag("pdig.r18.number.lifecycle"),
            color = Color.White,
            shape = RoundedCornerShape(17.dp),
            border = BorderStroke(1.dp, R9.Line),
        ) {
            Column(
                Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("号码生命周期", color = R9.Ink, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold)
                    Text("已记录资料", color = R9.Muted, fontSize = 9.sp)
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    R18NumberFact("资费", lifecycle?.planCost, Modifier.weight(1f))
                    R18NumberFact("下次保号", lifecycle?.keepAliveDue, Modifier.weight(1f))
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    R18NumberFact("保号周期", lifecycle?.keepAliveCycle, Modifier.weight(1f))
                    R18NumberFact("最近操作", lifecycle?.lastKeepAlive, Modifier.weight(1f))
                }
                lifecycle?.billingMode?.let { R9DetailLineNumber("计费方式", it) }
                lifecycle?.renewalMethod?.let { R9DetailLineNumber("续费 / 保号方式", it) }
                Text("保号日期来自用户记录，不代表运营商实时状态；未知字段保持“未记录”。",
                    color = R9.Muted, fontSize = 9.sp, lineHeight = 14.sp)
            }
        }

        R9SectionTitle("关联服务（${related.size}）")
        if (related.isEmpty()) {
            Text("没有已核验的服务关联，不代表该号码没有依赖。",
                color = R9.Muted, fontSize = 12.sp)
        } else {
            related.forEach { service ->
                val rel = UiVNextDemoFixture.relations.firstOrNull {
                    it.from == number.id && it.to == service.id
                }
                Surface(color = Color.White, shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, R9.Line),
                    modifier = Modifier.fillMaxWidth().testTag(VTestIds.NUMBER_DETAIL_SERVICES)) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(color = R9.Blue, shape = RoundedCornerShape(11.dp)) {
                            Text(service.name.take(1),
                                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(service.name, color = R9.Ink, fontWeight = FontWeight.Bold,
                                fontSize = 12.sp)
                            Text(serviceKindLabelZh(service.kind), color = R9.Muted, fontSize = 10.sp)
                        }
                        R9Badge(relationKindLabelZh(rel?.kind), R9.Blue)
                    }
                }
            }
        }
        R9SectionTitle("显示设置", "号码外观 →") { app.openNumberCustomization(number.id) }
        R9SectionTitle("安全与恢复")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if(number.recoveryOnly) Color(0xFFFFF1E4) else R9.Ice,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if(number.recoveryOnly) Color(0xFFF1D3AE) else R9.Line),
        ) {
            Text(
                if(number.recoveryOnly)
                    "该号码承担恢复功能；注销或迁移之前必须逐项验证替代恢复路径。不能因已添加新号码而自动标记安全。"
                else
                    "尚未确认的恢复关系仍为未知。请先检查已记录的关联服务，再执行停用或转移。",
                Modifier.padding(14.dp), color = R9.Ink, fontSize = 12.sp, lineHeight = 20.sp,
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth().height(48.dp)
                .clickable { app.navigate(com.pdig.uivnext.model.VScreen.CHANGE_PHONE) },
            color = R9.Blue, shape = RoundedCornerShape(15.dp)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("分析更换号码影响 →", color = Color.White,
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(9.dp))
    }
    if (editingName) {
        AlertDialog(
            onDismissRequest = { editingName = false },
            title = { Text("给号码命名", color = R9.Ink) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = proposedName, onValueChange = { proposedName = it.take(32) },
                        label = { Text("自定义名称（可留空）") },
                        placeholder = { Text("如：香港主号") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("pdig.r10.number.alias.input"),
                    )
                    Text("留空则显示 ${number.maskedNumber}；名称仅保存在本机。",
                        color = R9.Muted, fontSize = 11.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    app.renameNumber(number.id, proposedName)
                    editingName = false
                }) { Text("保存名称") }
            },
            dismissButton = {
                TextButton(onClick = { editingName = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun R18NumberFact(
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.heightIn(min = 55.dp),
        color = R9.Ice,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, R9.Line.copy(alpha = .75f)),
    ) {
        Column(
            Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(label, color = R9.Muted, fontSize = 9.sp)
            Text(value?.takeIf { it.isNotBlank() } ?: "未记录",
                color = R9.Ink, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 2, lineHeight = 13.sp)
        }
    }
}

@Composable
private fun R9DetailLineNumber(key: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(key, color = R9.Muted, fontSize = 10.sp)
        Text(value, color = R9.Ink, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 2)
    }
}
