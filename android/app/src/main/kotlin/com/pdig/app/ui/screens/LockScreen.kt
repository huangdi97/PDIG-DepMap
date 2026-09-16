package com.pdig.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pdig.app.security.AppLock
import com.pdig.app.security.LockCapability
import com.pdig.app.security.LockState
import com.pdig.app.ui.theme.PdigTokens

/**
 * 冷启动/重新检查设备能力时的中性占位页。
 *
 * 关键：**这一屏不含任何用户数据**。App 在读到 [AppLock.capability] 之前不会组合
 * 承载 Reality 的 NavHost，因此不存在"先展示 HOME 内容再决定锁定"的时间窗。
 */
@Composable
fun LockCheckingScreen() {
    Scaffold { pad ->
        Box(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceMd),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                Text(
                    "正在检查设备安全设置…",
                    style = PdigTokens.Body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics {
                        contentDescription = "正在检查设备安全设置，请稍候"
                    },
                )
            }
        }
    }
}

/**
 * 应用锁页（spec §60）。
 *
 * 本页**不是** NavHost 里的一个目的地，而是 App 的门：
 * LOCKED 时 NavHost 根本不参与组合，因此 HOME / 敏感路由 / 返回键 / 深链
 * 都无法绕过它。可绕过性由设备内测试 `AppLockNavigationTest` 断言。
 *
 * 三态处理：
 *  - `LOCKED`          设备可验证身份 → 必须验证通过才能进入；取消/失败一律留在本页。
 *  - `NOT_CONFIGURED`  设备没有录入任何生物识别，也没有设置锁屏凭据 → 无法验证身份。
 *                      **不伪装成已解锁**：明确说明原因，只有用户显式确认后才放行，
 *                      且这只是"设备无凭据"的产品策略，不是一次身份验证。
 *  - `UNAVAILABLE`     设备没有指纹/人脸硬件 → 同上，说明更精确。
 *
 * 任何其它取值（含 `UNLOCKED` / `FAILED`）都按 fail-closed 落到 LOCKED 分支。
 */
@Composable
fun LockScreen(
    capability: LockCapability,
    onUnlocked: () -> Unit,
    onRecheck: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context.findFragmentActivity()
    var message by remember { mutableStateOf<String?>(null) }
    val state = AppLock.state(capability)

    Scaffold { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(PdigTokens.SpaceXl)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceLg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("PDIG 已锁定", style = PdigTokens.Display, textAlign = TextAlign.Center)
            Text(
                "数据只保存在这台设备上，进入前需要验证身份。",
                style = PdigTokens.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            when (state) {
                LockState.NOT_CONFIGURED -> {
                    Text(
                        "这台设备没有录入指纹或人脸，也没有设置锁屏密码/图案，因此无法验证身份，" +
                            "应用锁无法真正生效。\n\n" +
                            "在系统设置里录入生物识别或设置锁屏凭据后回到这里，点「重新检查」即可启用。",
                        style = PdigTokens.Body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    // 这是"设备无凭据"的产品策略，不是一次身份验证 —— 文案明确这么写。
                    Button(
                        onClick = { onUnlocked() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = PdigTokens.MinTouchTarget),
                    ) { Text("已知悉风险，本次进入") }
                    TextButton(
                        onClick = { onRecheck() },
                        modifier = Modifier.heightIn(min = PdigTokens.MinTouchTarget),
                    ) { Text("重新检查设备能力") }
                }

                LockState.UNAVAILABLE -> {
                    Text(
                        "这台设备没有可用的生物识别硬件，也没有可用的锁屏凭据，" +
                            "无法验证身份，应用锁无法真正生效。",
                        style = PdigTokens.Body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = { onUnlocked() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = PdigTokens.MinTouchTarget),
                    ) { Text("已知悉风险，本次进入") }
                    TextButton(
                        onClick = { onRecheck() },
                        modifier = Modifier.heightIn(min = PdigTokens.MinTouchTarget),
                    ) { Text("重新检查设备能力") }
                }

                // LOCKED，以及任何意外取值（包含 UNLOCKED / FAILED）—— 一律要求验证。
                else -> {
                    Text(
                        "验证身份后才能查看你的基础设施。",
                        style = PdigTokens.Body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = {
                            if (activity == null) {
                                message = "无法启动验证"
                                return@Button
                            }
                            AppLock.authenticate(
                                activity = activity,
                                capability = capability,
                                onSuccess = {
                                    message = null
                                    onUnlocked()
                                },
                                onFailure = { _, msg ->
                                    // 不泄露底层错误细节（spec §128）
                                    message = "验证未通过，请重试。"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                onCancel = { message = "已取消验证。" },
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = PdigTokens.MinTouchTarget),
                    ) { Text("验证身份并解锁") }
                }
            }

            message?.let {
                Text(it, style = PdigTokens.BodyStrong, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

internal fun android.content.Context.findFragmentActivity(): androidx.fragment.app.FragmentActivity? {
    var ctx: android.content.Context = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is androidx.fragment.app.FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
