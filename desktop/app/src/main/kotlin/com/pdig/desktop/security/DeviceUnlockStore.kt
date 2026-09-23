package com.pdig.desktop.security

import java.io.File
import java.nio.charset.StandardCharsets

/**
 * “记住本机解锁”（opt-in）：把数据文件口令用 Windows DPAPI 保护后存到
 * %APPDATA%/PDIG/device-unlock.blob。**默认关闭**；关闭时删除 blob。
 *
 * SAFETY:
 *  - 磁盘上只有 DPAPI 加密的 blob（CurrentUser scope，OS 持钥），绝无明文口令；
 *  - 换 Windows 用户 / 机器 / 上下文后 unprotect 失败 → 回退到手动输入口令，不绕过；
 *  - 该机制只是便利（本机免输），不是安全降级：.depmap 文件本身始终由口令 + Argon2id 保护。
 */
class DeviceUnlockStore(
    private val port: DesktopSecurityPort,
    private val dir: File,
) {
    private val blobFile: File get() = File(dir, "device-unlock.blob")

    fun enabled(): Boolean = blobFile.isFile

    /** 记住（覆盖旧 blob）。失败即抛，绝不写入半截文件。 */
    fun remember(masterPassword: String) {
        dir.mkdirs()
        val blob = port.protect(masterPassword.toByteArray(StandardCharsets.UTF_8))
        val tmp = File(dir, ".device-unlock.tmp")
        tmp.writeBytes(blob)
        if (!tmp.renameTo(blobFile)) {
            tmp.copyTo(blobFile, overwrite = true)
            tmp.delete()
        }
    }

    /** 取回口令；unprotect 失败返回 null（调用方回退到手动输入）。 */
    fun recall(): String? {
        if (!blobFile.isFile) return null
        return try {
            String(port.unprotect(blobFile.readBytes()), StandardCharsets.UTF_8)
        } catch (_: Throwable) {
            null
        }
    }

    /** 忘记：删除 blob（不触碰 .depmap 数据）。 */
    fun forget() {
        blobFile.delete()
    }
}