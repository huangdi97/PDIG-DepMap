package com.pdig.uivnext.ui

import android.content.Context

/**
 * User-editable number display labels. Unlike Canonical phone identity, this
 * local presentation alias may be changed or cleared without an audit event.
 * Empty alias means display the best phone number currently recorded.
 */
internal class NumberDisplayNameStore(context: Context) {
    private val preferences = context.getSharedPreferences("pdig_ui_vnext_number_labels", Context.MODE_PRIVATE)
    fun loadAll(): Map<String, String> = preferences.all.entries.mapNotNull { (id, value) ->
        val alias = (value as? String)?.trim()?.takeIf(String::isNotEmpty) ?: return@mapNotNull null
        id to alias
    }.toMap()
    fun save(numberId: String, label: String) {
        val normalized = label.trim().take(32)
        val editor = preferences.edit()
        if (normalized.isEmpty()) editor.remove(numberId)
        else editor.putString(numberId, normalized)
        editor.apply()
    }
}

/** Do not replace an unlabelled number with a made-up "primary" fixture nickname. */
internal fun displayNameForNumber(recordedNumber: String, alias: String?): String =
    alias?.trim()?.takeIf { it.isNotEmpty() } ?: recordedNumber
