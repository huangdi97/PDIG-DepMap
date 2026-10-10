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
import androidx.compose.foundation.layout.weight
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
                    "对象建立、文件导入与关系确认保持为不同 authority。",
                    color = PdigV2Colors.TextMuted,
                    fontSize = 12.sp,
                )
            }
        }

        item {
            EstablishEntry(
                title = "手工记录",
                body = "只创建受当前 Canonical runtime policy 支持的对象；不会同时创建依赖关系。",
                state = "正式 authority 可用",
                enabled = session.authorities?.manualEstablish != null,
            ) {
                app.navigate(VScreen.MANUAL_ADD)
            }
        }

        item {
            EstablishEntry(
                title = "文件导入",
                body = "生产 Import authority 已存在，但真实文件 picker / lock-re-auth 生命周期仍由现有 FileWorkflowCoordinator 持有。",
                state = "Host binding 待接",
                enabled = false,
            ) {}
        }

        item {
            EstablishBoundary(
                "Import commit != Dependency confirmation；手工创建对象 != 手工确认关系。"
            )
        }
    }
}

@Composable
internal fun ProductionManualEstablishScreen(
    session: ProductionVNextSession,
    modifier: Modifier = Modifier,
) {
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
                        Text("已写入 Reality", color = PdigV2Colors.PrimaryText,
                            fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(created.name, color = PdigV2Colors.TextPrimary, fontSize = 12.sp)
                        Text(
                            "kind=${created.kind} · graphRevision=${created.graphRevision}",
                            color = PdigV2Colors.TextMuted,
                            fontSize = 10.sp,
                        )
                        Text(
                            "没有自动创建关系。下一步如需连接对象，必须走独立的关系确认 authority。",
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
                                error = "创建未完成；没有在 UI 本地伪造 Reality。请检查输入或生产 authority。"
                            }
                        } else Modifier
                    ),
                color = if (canSave) PdigV2Colors.PrimarySoft else PdigV2Colors.SurfaceRaised,
                shape = RoundedCornerShape(VRadius.Lg),
                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Text(
                    if (canSave) "确认对象存在并写入 Reality" else "填写名称后可提交",
                    Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                    color = if (canSave) PdigV2Colors.PrimaryText else PdigV2Colors.TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        item {
            EstablishBoundary(
                "identity_anchor / device / membership / custom 不会因为 UI 能画出来就被偷偷开放；必须服从 generated runtime-creatable policy。"
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
        Text("手工建立 authority 未绑定", color = PdigV2Colors.TextPrimary,
            fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.padding(4.dp))
        Text("不会提供假的保存按钮。", color = PdigV2Colors.TextSecondary, fontSize = 12.sp)
    }
}
