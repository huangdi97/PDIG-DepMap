package com.pdig.uivnext.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
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
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.production.ManualEstablishInput
import com.pdig.uivnext.production.ManualEstablishResultView
import com.pdig.uivnext.production.ProductionVNextSession
import com.pdig.uivnext.production.supportedManualEstablishKinds
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius

@Composable
internal fun ProductionEstablishScreen(
    session: ProductionVNextSession,
    modifier: Modifier = Modifier,
) {
    val app = session.appState
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("pdig.production-vnext.establish"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("建立基础设施", color = PdigV2Colors.TextPrimary, fontSize = 23.sp,
                    fontWeight = FontWeight.Bold)
                Text(
                    "对象建立、文件导入与关系确认是三个独立步骤。",
                    color = PdigV2Colors.TextMuted,
                    fontSize = 12.sp,
                )
            }
        }

        item {
            EstablishEntry(
                title = "手工记录",
                body = "只创建当前正式规则允许直接建立的对象；不会同时创建依赖关系。",
                state = "正式创建能力可用",
                enabled = session.authorities?.manualEstablish != null,
            ) {
                app.navigate(VScreen.MANUAL_ADD)
            }
        }

        item {
            val requestImport = session.hostActions.requestFileImport
            EstablishEntry(
                title = "文件导入",
                body = "真实文件选择与重新解锁继续由现有安全导入流程负责；新版界面只请求应用启动该流程。",
                state = if (requestImport != null) "可启动正式导入" else "正式入口待接",
                enabled = requestImport != null,
            ) {
                requestImport?.invoke()
            }
        }

        item {
            EstablishBoundary(
                "导入完成不等于依赖已确认；手工创建对象也不等于手工确认关系。"
            )
        }
    }
}

@Composable
internal fun ProductionManualEstablishScreen(
    session: ProductionVNextSession,
    modifier: Modifier = Modifier,
) {
    val app = session.appState
    val gateway = session.authorities?.manualEstablish
    if (gateway == null) {
        ProductionManualUnavailable(modifier)
        return
    }

    val supported = supportedManualEstablishKinds()
    val choices = listOf(
        "payment_instrument" to "支付工具",
        "account" to "账户",
        "service" to "服务",
    ).filter { it.first in supported }

    var selectedKind by rememberSaveable {
        mutableStateOf(choices.firstOrNull()?.first.orEmpty())
    }
    var name by rememberSaveable { mutableStateOf("") }
    var issuer by rememberSaveable { mutableStateOf("") }
    var last4 by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<ManualEstablishResultView?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("pdig.production-vnext.manual-establish"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("手工记录", color = PdigV2Colors.TextPrimary, fontSize = 23.sp,
                    fontWeight = FontWeight.Bold)
                Text(
                    "你确认的是“这个对象存在”；关系、恢复路径与依赖仍需单独确认。",
                    color = PdigV2Colors.TextMuted,
                    fontSize = 12.sp,
                )
            }
        }

        item {
            EstablishBoundary(
                "提交会通过 AppContainer authoritative transaction 创建 Node 并更新 graphRevision；不会写入任何 Dependency。"
            )
        }

        item {
            Text("对象类型", color = PdigV2Colors.TextPrimary, fontSize = 13.sp,
                fontWeight = FontWeight.Bold)
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                choices.forEach { (kind, label) ->
                    val selected = selectedKind == kind
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedKind = kind
                                result = null
                                error = null
                            }
                            .testTag("pdig.production-vnext.manual.kind.$kind"),
                        color = if (selected) PdigV2Colors.PrimarySoft else PdigV2Colors.Surface,
                        shape = RoundedCornerShape(VRadius.Md),
                        border = BorderStroke(
                            1.dp,
                            if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.BorderSubtle,
                        ),
                    ) {
                        Text(
                            label,
                            Modifier.padding(horizontal = 10.dp, vertical = 11.dp),
                            color = PdigV2Colors.TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it.take(80)
                    result = null
                    error = null
                },
                label = { Text("名称") },
                modifier = Modifier.fillMaxWidth().testTag("pdig.production-vnext.manual.name"),
                singleLine = true,
            )
        }

        if (selectedKind == "payment_instrument") {
            item {
                OutlinedTextField(
                    value = issuer,
                    onValueChange = { issuer = it.take(80) },
                    label = { Text("发行方（可留空）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            item {
                OutlinedTextField(
                    value = last4,
                    onValueChange = { input ->
                        last4 = input.filter(Char::isDigit).takeLast(4)
                    },
                    label = { Text("尾号（可留空，最多 4 位数字）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        }

        error?.let { message ->
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = PdigV2Colors.Critical.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(VRadius.Md),
                ) {
                    Text(message, Modifier.padding(12.dp), color = PdigV2Colors.Critical,
                        fontSize = 11.sp)
                }
            }
        }

        result?.let { created ->
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().testTag("pdig.production-vnext.manual.result"),
                    color = PdigV2Colors.PrimarySoft,
                    shape = RoundedCornerShape(VRadius.Lg),
                ) {
                    Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("已写入已确认数据", color = PdigV2Colors.PrimaryText,
                            fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(created.name, color = PdigV2Colors.TextPrimary, fontSize = 12.sp)
                        Text(
                            "kind=${created.kind} · graphRevision=${created.graphRevision}",
                            color = PdigV2Colors.TextMuted,
                            fontSize = 10.sp,
                        )
                        Text(
                            "没有自动创建关系。下一步如需连接对象，必须单独确认关系。",
                            color = PdigV2Colors.TextSecondary,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }

        item {
            val canSave = name.trim().isNotEmpty() && selectedKind in supported
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pdig.production-vnext.manual.save")
                    .then(
                        if (canSave) Modifier.clickable {
                            error = null
                            result = null
                            try {
                                result = gateway.create(
                                    ManualEstablishInput(
                                        kind = selectedKind,
                                        name = name.trim(),
                                        issuer = issuer.trim().takeIf { it.isNotEmpty() },
                                        last4 = last4.takeIf { it.isNotEmpty() },
                                    ),
                                )
                                if (result != null) {
                                    name = ""
                                    issuer = ""
                                    last4 = ""
                                }
                            } catch (t: Throwable) {
                                error = "创建未完成；界面没有自行伪造已确认数据。请检查输入或正式创建能力。"
                            }
                        } else Modifier
                    ),
                color = if (canSave) PdigV2Colors.PrimarySoft else PdigV2Colors.SurfaceRaised,
                shape = RoundedCornerShape(VRadius.Lg),
                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Text(
                    if (canSave) "确认对象存在并保存" else "填写名称后可提交",
                    Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                    color = if (canSave) PdigV2Colors.PrimaryText else PdigV2Colors.TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (result != null) {
            item {
                EstablishEntry(
                    title = "手工记录关系",
                    body = "对象已建立；关系仍需单独确认。当前原生数据格式尚不能安全写入完整关系词汇。",
                    state = "底层格式升级前暂不可提交",
                    enabled = true,
                ) {
                    app.navigate(VScreen.MANUAL_RELATION)
                }
            }
        }

        item {
            EstablishBoundary(
                "身份对象、设备、会员与自定义对象不会因为界面能显示就擅自开放手工创建；必须遵守当前正式创建规则。"
            )
        }
    }
}

@Composable
internal fun ProductionManualRelationshipHoldScreen(
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("pdig.production-vnext.manual-relation-hold"),
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
                    "关系录入设计已完成；正式写入必须等新数据格式和跨端一致性验证完成。",
                    color = PdigV2Colors.TextMuted,
                    fontSize = 12.sp,
                )
            }
        }

        item {
            EstablishBoundary(
                "当前原生数据格式仍是第 3 版。只开放一小部分旧版关系会制造平台分叉，因此这里不会提供“确认关系”按钮。"
            )
        }

        item {
            Text("已冻结的关系输入", color = PdigV2Colors.TextPrimary,
                fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        listOf(
            "From / Relation / To" to "关系必须连接两个已确认对象",
            "Capability" to "payment / access / authentication / recovery / communication",
            "Criticality" to "默认 unknown；机器不能擅自设为 required",
            "来源" to "明确人工陈述可作为手工确认；模型或数据来源给出的建议必须先进入待复核",
            "确认" to "只有明确确认后的关系才能进入已确认数据；未确认建议不是正式依赖",
        ).forEach { (title, body) ->
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = PdigV2Colors.Surface,
                    shape = RoundedCornerShape(VRadius.Lg),
                    border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                ) {
                    Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(title, color = PdigV2Colors.TextPrimary, fontSize = 12.sp,
                            fontWeight = FontWeight.Bold)
                        Text(body, color = PdigV2Colors.TextSecondary, fontSize = 10.sp)
                    }
                }
            }
        }

        item {
            EstablishBoundary(
                "开放条件：新数据格式迁移、运行时校验、正反例测试、Android/iOS/Harmony 一致性验证，以及正式关系写入能力全部完成。缺一项都不开放。"
            )
        }
    }
}

@Composable
private fun EstablishEntry(
    title: String,
    body: String,
    state: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, color = PdigV2Colors.TextPrimary, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold)
                Text(state, color = if (enabled) PdigV2Colors.PrimaryText else PdigV2Colors.TextMuted,
                    fontSize = 9.sp)
            }
            Text(body, color = PdigV2Colors.TextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun EstablishBoundary(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.PrimarySoft.copy(alpha = 0.65f),
        shape = RoundedCornerShape(VRadius.Lg),
    ) {
        Text(text, Modifier.padding(13.dp), color = PdigV2Colors.TextSecondary,
            fontSize = 10.sp)
    }
}

@Composable
private fun ProductionManualUnavailable(modifier: Modifier) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("手工创建能力尚未接入", color = PdigV2Colors.TextPrimary,
            fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.padding(4.dp))
        Text("不会提供假的保存按钮。", color = PdigV2Colors.TextSecondary, fontSize = 12.sp)
    }
}
