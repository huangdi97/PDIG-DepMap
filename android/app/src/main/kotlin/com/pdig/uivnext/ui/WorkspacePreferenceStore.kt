package com.pdig.uivnext.ui

import android.content.Context

/**
 * 工作区本地偏好存储。
 *
 * 仅保存 UI 行为；Evidence tests 不注入本 store，因此不会读取用户本机偏好。
 */
internal class WorkspacePreferenceStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): WorkspacePreferences = WorkspacePreferences(
        privacyMask = prefs.getBoolean(KEY_PRIVACY, true),
        reduceMotion = prefs.getBoolean(KEY_REDUCE_MOTION, false),
        railExpanded = prefs.getBoolean(KEY_RAIL_EXPANDED, true),
        showUpcoming = prefs.getBoolean(KEY_SHOW_UPCOMING, true),
    )

    fun save(value: WorkspacePreferences) {
        prefs.edit()
            .putBoolean(KEY_PRIVACY, value.privacyMask)
            .putBoolean(KEY_REDUCE_MOTION, value.reduceMotion)
            .putBoolean(KEY_RAIL_EXPANDED, value.railExpanded)
            .putBoolean(KEY_SHOW_UPCOMING, value.showUpcoming)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "pdig_ui_vnext_workspace"
        private const val KEY_PRIVACY = "privacy_mask"
        private const val KEY_REDUCE_MOTION = "reduce_motion"
        private const val KEY_RAIL_EXPANDED = "rail_expanded"
        private const val KEY_SHOW_UPCOMING = "show_upcoming"
    }
}
