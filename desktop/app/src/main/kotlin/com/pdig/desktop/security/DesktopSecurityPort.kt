package com.pdig.desktop.security

/**
 * DesktopSecurityPort —— 平台 OS 受保护密钥通道（E6）。
 *
 * Windows 实现用 DPAPI（CurrentUser scope + 固定 entropy），密钥由操作系统
 * 与当前 Windows 用户账户绑定；不落盘任何明文口令/密钥。
 *
 * interface 化只为可测试（内存 fake 不涉及真实密钥）。
 */
interface DesktopSecurityPort {
    /** 保护任意字节（对称于 [unprotect]）；不同用户的输出不可互解。 */
    fun protect(data: ByteArray): ByteArray

    /** 反保护；失败（换用户/会话/损坏）抛异常，绝不 fallback 到明文。 */
    fun unprotect(blob: ByteArray): ByteArray

    /** 该实现是否真的可用（Windows DPAPI 只在 Windows 上可实例化）。 */
    val available: Boolean
}

const val PDIG_DPAPI_ENTROPY: String = "PDIG-Desktop-v1"

/**
 * Windows DPAPI（JNA 平台绑定，jna-platform 5.14 的 Crypt32Util 签名：
 * cryptProtectData(data, entropy, flags, description, prompt) 与
 * cryptUnprotectData(blob, entropy, flags, prompt)）。CurrentUser scope：
 * 只有同一 Windows 用户进程能解，是“OS-protected key”的官方机制。
 */
class WindowsDpapiSecurityPort : DesktopSecurityPort {
    override val available: Boolean
        get() = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)

    override fun protect(data: ByteArray): ByteArray =
        com.sun.jna.platform.win32.Crypt32Util.cryptProtectData(
            data,
            PDIG_DPAPI_ENTROPY.toByteArray(Charsets.UTF_8),
            0,
            null,
            null,
        )

    override fun unprotect(blob: ByteArray): ByteArray =
        com.sun.jna.platform.win32.Crypt32Util.cryptUnprotectData(
            blob,
            PDIG_DPAPI_ENTROPY.toByteArray(Charsets.UTF_8),
            0,
            null,
        )
}

/**
 * 测试/非 Windows 兜底：XOR-rotate 变换，仅证明接口契约，**不用于真实数据**。
 * 任何真实路径不得注入本实现（生产路径一律使用 [WindowsDpapiSecurityPort]）。
 */
class InMemorySecurityPort : DesktopSecurityPort {
    override val available: Boolean = true

    override fun protect(data: ByteArray): ByteArray {
        // 测试专用：非加密，仅可逆混淆。绝不用于生产路径。
        val out = ByteArray(data.size + 1)
        out[0] = 0x5A
        for (i in data.indices) out[i + 1] = ((data[i].toInt() xor 0x5A) and 0xFF).toByte()
        return out
    }

    override fun unprotect(blob: ByteArray): ByteArray {
        if (blob.size < 2 || blob[0] != 0x5A.toByte()) throw IllegalStateException("bad in-memory blob")
        val out = ByteArray(blob.size - 1)
        for (i in out.indices) out[i] = ((blob[i + 1].toInt() xor 0x5A) and 0xFF).toByte()
        return out
    }
}