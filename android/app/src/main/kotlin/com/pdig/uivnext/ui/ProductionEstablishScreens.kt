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
import com.pdig.core.generated.IdentityAnchorSubtype
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.production.ManualEstablishInput
import com.pdig.uivnext.production.ManualIdentityEstablishInput
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
            EstablishEntry(
                title = "手工记录关系",
                body = "在两个已确认对象之间明确确认当前 runtime registry 支持的关系；不会自动判断独立路径。",
                state = if (session.authorities?.manualRelationship != null) "正式关系能力可用" else "正式关系能力待接",
                enabled = session.authorities?.manualRelationship != null,
            ) {
                app.navigate(VScreen.MANUAL_RELATION)
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
                "导入完成不等于依赖已确认；对象建立与关系确认仍是两个独立 Reality 动作。"
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
    var identifierValue by rememberSaveable { mutableStateOf("") }
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
            Text(
                "身份对象",
                color = PdigV2Colors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "phone_number" to "手机号",
                    "email_address" to "邮箱",
                ).forEach { (kind, label) ->
                    val selected = selectedKind == kind
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedKind = kind
                                issuer = ""
                                last4 = ""
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

        if (selectedKind == "phone_number" || selectedKind == "email_address") {
            item {
                OutlinedTextField(
                    value = identifierValue,
                    onValueChange = {
                        identifierValue = it.take(320)
                        result = null
                        error = null
                    },
                    label = {
                        Text(
                            if (selectedKind == "phone_number")
                                "号码（按你确认的原始值保存）"
                            else
                                "邮箱地址（按你确认的原始值保存）"
                        )
                    },
                    supportingText = {
                        Text(
                            "这里确认的是标识值本身；不会据此推断运营商、地区、恢复角色或唯一恢复路径。"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                        .testTag("pdig.production-vnext.manual.identity.value"),
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
                        created.identitySubtype?.let { subtype ->
                            Text(
                                if (subtype == "phone_number") "手机号身份" else "邮箱身份",
                                color = PdigV2Colors.TextSecondary,
                                fontSize = 10.sp,
                            )
                            created.identityIdentifierValue?.let { value ->
                                Text(
                                    if (app.privacyMask) "标识值已遮蔽" else value,
                                    color = PdigV2Colors.TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        Text(
                            "graphRevision=${created.graphRevision}",
                            color = PdigV2Colors.TextMuted,
                            fontSize = 9.sp,
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
            val isIdentity = selectedKind == "phone_number" || selectedKind == "email_address"
            val canSave = name.trim().isNotEmpty() && (
                selectedKind in supported ||
                    (isIdentity && identifierValue.trim().isNotEmpty())
                )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pdig.production-vnext.manual.save")
                    .then(
                        if (canSave) Modifier.clickable {
                            error = null
                            result = null
                            try {
                                result = when (selectedKind) {
                                    "phone_number" -> gateway.createIdentity(
                                        ManualIdentityEstablishInput(
                                            subtype = IdentityAnchorSubtype.PHONE_NUMBER,
                                            name = name.trim(),
                                            identifierValue = identifierValue.trim(),
                                        ),
                                    )
                                    "email_address" -> gateway.createIdentity(
                                        ManualIdentityEstablishInput(
                                            subtype = IdentityAnchorSubtype.EMAIL_ADDRESS,
                                            name = name.trim(),
                                            identifierValue = identifierValue.trim(),
                                        ),
                                    )
                                    else -> gateway.create(
                                        ManualEstablishInput(
                                            kind = selectedKind,
                                            name = name.trim(),
                                            issuer = issuer.trim().takeIf { it.isNotEmpty() },
                                            last4 = last4.takeIf { it.isNotEmpty() },
                                        ),
                                    )
                                }
                                if (result != null) {
                                    name = ""
                                    issuer = ""
                                    last4 = ""
                                    identifierValue = ""
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
                    if (canSave) "确认对象与已填写事实并保存"
                    else if (isIdentity) "填写名称与标识值后可提交"
                    else "填写名称后可提交",
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
                    title = "继续记录关系",
                    body = "对象已建立。关系仍需单独确认，并且只允许当前 Canonical runtime registry 已正式支持的关系。",
                    state = if (session.authorities?.manualRelationship != null) "可进入正式关系确认" else "关系 authority 待接",
                    enabled = session.authorities?.manualRelationship != null,
                ) {
                    app.navigate(VScreen.MANUAL_RELATION)
                }
            }
        }

        item {
            EstablishBoundary(
                "手机号/邮箱仅在 subtype 与 identifier 同一次权威事务中被用户确认后开放；其他通用身份、设备、会员与自定义对象仍不会因为界面能显示就擅自开放创建。"
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
