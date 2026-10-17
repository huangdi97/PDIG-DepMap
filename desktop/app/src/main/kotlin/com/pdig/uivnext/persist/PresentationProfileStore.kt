package com.pdig.uivnext.persist

import com.pdig.uivnext.model.PresentationProfile
import java.io.File

/**
 * PresentationProfileStore —— 本地外观偏好存储（PHASE 1E §66）。
 *
 * 铁律：
 *  - 只写 app-managed storage（默认 `~/.pdig/presentation-profiles.json`，测试注入临时目录）；
 *  - 绝不写 PersonalReality / .depmap canonical payload（store 不知道 .depmap 存在）；
 *  - 键 = `targetType + ":" + targetId`；同 key 覆盖，无历史版本。
 *
 * 格式：单文件 JSON-lite（无外部依赖），一行一个 profile：
 *
 *     {"targetType":"card","targetId":"card-cn-1","themeId":"glass","material":"glass",...}
 *
 * 解析失败的行跳过（绝不抛异常破坏启动）；文件损坏时返回空集合并保留原文件（审计需要）。
 */
class PresentationProfileStore(
    private val file: File,
) {
    init {
        file.parentFile?.mkdirs()
    }

    /** 保存并返回持久化的 profile（同 key 覆盖）。 */
    fun save(profile: PresentationProfile) {
        val lines = loadAll().toMutableMap()
        lines[keyOf(profile)] = profile
        file.writeText(lines.values.joinToString("\n") { encode(it) } + "\n")
    }

    fun load(targetType: String, targetId: String): PresentationProfile? =
        loadAll()[keyOf(targetType, targetId)]

    fun loadAll(): Map<String, PresentationProfile> {
        if (!file.isFile) return emptyMap()
        val result = LinkedHashMap<String, PresentationProfile>()
        runCatching { file.readLines() }.getOrDefault(emptyList()).forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEach
            val profile = decode(trimmed) ?: return@forEach
            result[keyOf(profile)] = profile
        }
        return result
    }

    fun delete(targetType: String, targetId: String) {
        val remaining = loadAll().filterKeys { it != keyOf(targetType, targetId) }
        file.writeText(remaining.values.joinToString("\n") { encode(it) } + "\n")
    }

    private fun keyOf(profile: PresentationProfile): String = keyOf(profile.targetType, profile.targetId)

    private fun keyOf(targetType: String, targetId: String): String = "$targetType:$targetId"

    /** 编码：所有值 JSON 转义（引号/反斜杠/换行）。 */
    private fun encode(p: PresentationProfile): String = buildString {
        append("{")
        append("\"targetType\":").append(esc(p.targetType)).append(",")
        append("\"targetId\":").append(esc(p.targetId)).append(",")
        append("\"themeId\":").append(esc(p.themeId)).append(",")
        append("\"material\":").append(esc(p.material)).append(",")
        append("\"accentColor\":").append(esc(p.accentColor)).append(",")
        append("\"backgroundKind\":").append(esc(p.backgroundKind)).append(",")
        append("\"backgroundValue\":").append(esc(p.backgroundValue)).append(",")
        append("\"layout\":").append(esc(p.layout)).append(",")
        append("\"maskSensitive\":").append(p.maskSensitive)
        append("}")
    }

    private fun decode(line: String): PresentationProfile? {
        val body = line.trim().removePrefix("{").removeSuffix("}")
        val fields = parseFields(body) ?: return null
        return PresentationProfile(
            targetType = fields["targetType"] ?: return null,
            targetId = fields["targetId"] ?: return null,
            themeId = fields["themeId"] ?: "minimal",
            material = fields["material"] ?: "glass",
            accentColor = fields["accentColor"] ?: "#4D74FF",
            backgroundKind = fields["backgroundKind"] ?: "preset",
            backgroundValue = fields["backgroundValue"] ?: "",
            layout = fields["layout"] ?: "standard",
            maskSensitive = fields["maskSensitive"]?.toBooleanStrictOrNull() ?: true,
        )
    }

    /** 解析 `"k":"v","k":true` 形式的字段（支持转义，无外部依赖）。 */
    private fun parseFields(body: String): Map<String, String>? {
        val result = LinkedHashMap<String, String>()
        var i = 0
        val n = body.length
        while (i < n) {
            if (body[i] != '"') return null
            val keyEnd = body.indexOf('"', i + 1)
            if (keyEnd < 0) return null
            val k = body.substring(i + 1, keyEnd)
            var j = keyEnd + 1
            while (j < n && (body[j] == ' ' || body[j] == ':')) j++
            if (j >= n) return null
            var value = ""
            var next = j
            if (body[j] == '"') {
                val sb = StringBuilder()
                var p = j + 1
                while (p < n) {
                    val ch = body[p]
                    if (ch == '\\' && p + 1 < n) {
                        sb.append(body[p + 1])
                        p += 2
                        continue
                    }
                    if (ch == '"') break
                    sb.append(ch)
                    p++
                }
                value = sb.toString()
                next = p + 1 // skip closing quote
            } else {
                val end = body.indexOf(',', j).let { if (it < 0) n else it }
                value = body.substring(j, end).trim()
                next = end
            }
            while (next < n && body[next] != ',') next++
            i = if (next < n) next + 1 else n
            result[k] = value
        }
        return result
    }

    private fun esc(value: String): String = buildString {
        append('"')
        value.forEach { ch ->
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(ch)
            }
        }
        append('"')
    }
}

/** 默认 app-managed 存储位置（真实桌面运行时；测试用临时目录注入）。 */
fun defaultProfileStore(): PresentationProfileStore {
    val home = System.getenv("USERPROFILE") ?: System.getProperty("user.home") ?: "."
    return PresentationProfileStore(File(home, ".pdig/presentation-profiles.json"))
}