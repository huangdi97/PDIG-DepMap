package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.pdig.uivnext.model.regionLabelZh

/** 屏幕共享 UI 助手。 */
internal fun Modifier.testTagLocal(tag: String): Modifier = this.then(testTag(tag))

internal fun Modifier.clickableLocal(onClick: () -> Unit): Modifier = this.then(clickable(onClick = onClick))

/** Consumer-facing 地区名；内部继续使用稳定 regionCode。 */
internal fun regionLabel(code: String): String = regionLabelZh(code)
