package com.pdig.app.workflow

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

/**
 * 当前 [FileWorkflowCoordinator] 的组合局部提供者。
 *
 * 由 `MainActivity` 在 `setContent` 最外层注入 **Activity 作用域**的实例；
 * 默认值只在"脱离 MainActivity 单独渲染某个页面"的场景（例如 Compose UI 测试
 * 直接 `setContent { ImportScreen(nav) }`）下生效，此时没有 launcher 挂载，
 * 任何 `launchPicker()` 都会返回 false —— 这是**故意**的：
 * 不允许退化成"页面自己再注册一套 launcher"，那正是 D-16 的成因。
 */
val LocalFileWorkflow: ProvidableCompositionLocal<FileWorkflowCoordinator> =
    compositionLocalOf { FileWorkflowCoordinator() }
