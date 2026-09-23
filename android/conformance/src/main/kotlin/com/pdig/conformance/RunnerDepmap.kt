package com.pdig.conformance

import com.pdig.core.crypto.DepmapContainer
import com.pdig.core.crypto.Jcs
import com.pdig.core.json.Json

// ---------------------------------------------------------------------------
// depmap —— .depmap 容器（Cutover 核心要求）
// ---------------------------------------------------------------------------

internal fun runDepmap(id: String, input: Json.Obj): Json = when (id) {
    "depmap-golden-v1" -> {
        val salt = hexToBytes(str(input, "saltHex"))
        val nonce = hexToBytes(str(input, "nonceHex"))
        val plaintext = str(input, "plaintext").toByteArray(Charsets.UTF_8)
        val kdf = requireObj(input.require("kdf"))
        val created = DepmapContainer.create(
            plaintext,
            str(input, "password"),
            DepmapContainer.CreateOptions(
                salt = salt,
                nonce = nonce,
                memoryKiB = int(kdf, "memoryKiB"),
                iterations = int(kdf, "iterations"),
                parallelism = int(kdf, "parallelism"),
            ),
        )
        val reopened = String(DepmapContainer.openContainer(created.json, str(input, "password")), Charsets.UTF_8)
        val wrongPassword = try {
            DepmapContainer.openContainer(created.json, "wrong-password"); "opened"
        } catch (e: DepmapContainer.DepmapException) {
            e.code
        }
        val tampered = created.json.replace(
            Regex("\"ciphertext\":\"[^\"]+\""),
            "\"ciphertext\":\"AAAAAAAAAAAAAAAAAAAAAA==\"",
        )
        val tamperOutcome = try {
            DepmapContainer.openContainer(tampered, str(input, "password")); "opened"
        } catch (e: DepmapContainer.DepmapException) {
            e.code
        }
        Json.Obj(
            listOf(
                "derivedKeyHex" to Json.Str(created.derivedKeyHex),
                "ciphertextBase64" to Json.Str(created.header.ciphertextB64),
                "tagBase64" to Json.Str(created.header.tagB64),
                "containerJson" to Json.Str(created.json),
                "reopenedPlaintext" to Json.Str(reopened),
                "wrongPasswordOutcome" to Json.Str(wrongPassword),
                "tamperedCiphertextOutcome" to Json.Str(tamperOutcome),
            ),
        )
    }

    "depmap-utf8-password-normalization" -> {
        val salt = hexToBytes("00112233445566778899aabbccddeeff")
        val nonce = hexToBytes("a1b2c3d4e5f60718293a4b5c")
        val plaintext = str(input, "plaintext").toByteArray(Charsets.UTF_8)
        val cases = arr(input, "cases")
        val entries = cases.map { c ->
            val o = requireObj(c)
            val caseId = str(o, "id")
            val derived = DepmapContainer.create(
                plaintext,
                str(o, "password"),
                DepmapContainer.CreateOptions(salt = salt, nonce = nonce),
            ).derivedKeyHex
            caseId to Json.Str(derived)
        }
        val combining = requireStr(entries.first { it.first == "combining" }.second).value
        val nfc = requireStr(entries.first { it.first == "nfc" }.second).value
        Json.Obj(
            listOf(
                "derivedKeyHexByCase" to Json.Obj(entries),
                "combiningDiffersFromNfc" to Json.Bool(combining != nfc),
            ),
        )
    }

    "depmap-bounds-and-structure-rejection" -> {
        val mutations = arr(input, "mutations")
        Json.Obj(
            mutations.map { m ->
                val o = requireObj(m)
                val caseId = str(o, "id")
                val json = str(o, "json")
                val code = try {
                    DepmapContainer.openContainer(json, "depmap-test"); "opened"
                } catch (e: DepmapContainer.DepmapException) {
                    e.code
                }
                caseId to Json.Str(code)
            },
        )
    }

    else -> throw NotImplementedError("no depmap runner for $id")
}

// ---------------------------------------------------------------------------
// jcs —— RFC 8785 序列化契约
// ---------------------------------------------------------------------------

internal fun runJcs(input: Json.Obj): Json {
    val canonical = arr(input, "cases").map { c ->
        val o = requireObj(c)
        val caseId = str(o, "id")
        caseId to Json.Str(Jcs.stringify(o.require("input")))
    }
    val rejections = arr(input, "rejectCases").map { c ->
        val o = requireObj(c)
        val caseId = str(o, "id")
        val outcome = try {
            Jcs.stringify(o.require("input")); "serialized"
        } catch (e: Jcs.JcsError) {
            "Error"
        }
        caseId to Json.Str(outcome)
    }
    return Json.Obj(
        listOf(
            "canonical" to Json.Obj(canonical),
            "rejections" to Json.Obj(rejections),
        ),
    )
}