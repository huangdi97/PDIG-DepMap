package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/**
 * 共享 UI 修饰符助手（package 级唯一定义；禁止在各屏幕文件里重复定义同名扩展，
 * 否则同包会产生 conflicting overloads / overload resolution ambiguity）。
 */
internal fun Modifier.testTagLocal(tag: String): Modifier = this.then(testTag(tag))

internal fun Modifier.clickableLocal(onClick: () -> Unit): Modifier = this.then(clickable(onClick = onClick))
