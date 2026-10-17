package com.pdig.uivnext.model

/**
 * UI vNext 长尾基础设施对象。
 *
 * 只服务于 Android Presentation / synthetic fixture，绝不扩展 Canonical schema，
 * 也不会写入 .depmap。
 */
data class UiVNextAccount(
    val id: String,
    val name: String,
    val provider: String,
    val maskedIdentifier: String,
    val region: String,
    val roles: List<String>,
    val authMethods: List<String>,
    val recoveryRoute: String,
    val status: String,
    val attention: Boolean,
)

data class UiVNextEmail(
    val id: String,
    val name: String,
    val maskedAddress: String,
    val provider: String,
    val region: String,
    val roles: List<String>,
    val linkedServiceCount: Int,
    val recoveryOnly: Boolean,
    val status: String,
    val uniqueRecoveryPath: Boolean? = null, // null = 未知；恢复用途不等于唯一恢复
)

data class UiVNextDevice(
    val id: String,
    val name: String,
    val platform: String,
    val kind: String,
    val region: String,
    val roles: List<String>,
    val trust: String,
    val lastSeen: String,
    val attention: Boolean,
)
