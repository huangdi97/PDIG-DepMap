package com.pdig.uivnext.ui

/** Android UI vNext 工作区本地偏好；只影响表现与交互，不进入 Canonical / .depmap。 */
data class WorkspacePreferences(
    val privacyMask: Boolean = false,
    val reduceMotion: Boolean = false,
    val railExpanded: Boolean = true,
    val showUpcoming: Boolean = true,
)
