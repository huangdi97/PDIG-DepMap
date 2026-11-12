package com.pdig.uivnext.ui

import android.content.Context
import com.pdig.uivnext.model.PresentationProfile
import java.security.MessageDigest
import org.json.JSONObject

/**
 * Android 本机 PresentationProfile 存储。
 *
 * 只保存外观偏好；不会写入 .depmap / PersonalReality / Canonical。
 * Evidence tests 不注入本 store，因此不会读取用户本机偏好。
 */
internal class PresentationProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadAll(): Map<String, PresentationProfile> = buildMap {
        prefs.all
            .filterKeys { it.startsWith(KEY_PREFIX) }
            .values
            .filterIsInstance<String>()
            .mapNotNull(::decode)
            .forEach { profile -> put(runtimeKey(profile.targetType, profile.targetId), profile) }
    }

    fun save(profile: PresentationProfile) {
        prefs.edit()
            .putString(storageKey(profile.targetType, profile.targetId), encode(profile))
            .apply()
    }

    private fun encode(profile: PresentationProfile): String = JSONObject()
        .put("targetType", profile.targetType)
        .put("targetId", profile.targetId)
        .put("themeId", profile.themeId)
        .put("material", profile.material)
        .put("accentColor", profile.accentColor)
        .put("backgroundKind", profile.backgroundKind)
        .put("backgroundValue", profile.backgroundValue)
        .put("layout", profile.layout)
        .put("maskSensitive", profile.maskSensitive)
        .toString()

    private fun decode(raw: String): PresentationProfile? = runCatching {
        val json = JSONObject(raw)
        PresentationProfile(
            targetType = json.getString("targetType"),
            targetId = json.getString("targetId"),
            themeId = json.getString("themeId"),
            material = json.getString("material"),
            accentColor = json.getString("accentColor"),
            backgroundKind = json.getString("backgroundKind"),
            backgroundValue = json.getString("backgroundValue"),
            layout = json.getString("layout"),
            maskSensitive = json.getBoolean("maskSensitive"),
        )
    }.getOrNull()

    private fun storageKey(targetType: String, targetId: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest((targetType + "\u0000" + targetId).toByteArray(Charsets.UTF_8))
        val digest = bytes.joinToString("") { "%02x".format(it) }
        return KEY_PREFIX + digest
    }

    companion object {
        private const val PREFS_NAME = "pdig_ui_vnext_presentation"
        private const val KEY_PREFIX = "profile."
        fun runtimeKey(targetType: String, targetId: String): String = targetType + "::" + targetId
    }
}
