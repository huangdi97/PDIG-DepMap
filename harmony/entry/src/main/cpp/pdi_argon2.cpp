/*
 * pdi_argon2 — HarmonyOS NAPI bridge for the Argon2id KDF.
 *
 * Scope (deliberately minimal):
 *   This module performs Argon2id key derivation AND NOTHING ELSE.
 *
 *   It must NOT:
 *     - accept a JSON / .depmap container
 *     - parse or validate a container header
 *     - perform AES-256-GCM
 *     - perform RFC 8785 JCS canonicalisation
 *     - hold or transfer the derived key anywhere except back to ArkTS
 *
 *   Rationale: the native trust boundary stays as small as possible. Everything
 *   above (JCS / AAD / container structure / bounds / GCM) lives in ArkTS where
 *   it is covered by the compile gate and the conformance runner. Only the
 *   primitive that ArkTS cannot express (Argon2id) crosses into native code.
 *
 * The KDF itself is the unmodified PHC reference implementation
 * (third_party/argon2 at tag 20190702, commit 62358ba2). No cryptographic
 * arithmetic is implemented here.
 *
 * Exported to ArkTS:
 *   deriveArgon2id(password: Uint8Array, salt: Uint8Array,
 *                  memoryKiB: number, iterations: number, parallelism: number,
 *                  outputLength: number, version: number): Uint8Array
 *
 *   Every argument is explicit. In particular `parallelism` and `version` are
 *   forwarded verbatim; the bridge does NOT hardcode p=1 or v=19, so a test can
 *   prove the parameter is honoured end to end.
 *
 * Errors: any failure (invalid parameters, allocation failure, argon2 status
 * code != ARGON2_OK) throws a JavaScript Error. Failure is fail-closed: no
 * partially filled buffer is ever returned.
 */

#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <stdint.h>

#include "napi/native_api.h"
#include "argon2.h"

/* Upper bounds mirrors spec/security/depmap-container-v1.json `bounds`.
 * They are duplicated here on purpose: the native layer must never be talked
 * into an unbounded memory allocation by a malformed caller. */
#define PDI_ARGON2_MEMORY_KIB_MIN 16384u
#define PDI_ARGON2_MEMORY_KIB_MAX 262144u
#define PDI_ARGON2_ITERATIONS_MIN 1u
#define PDI_ARGON2_ITERATIONS_MAX 10u
#define PDI_ARGON2_PARALLELISM_MIN 1u
#define PDI_ARGON2_PARALLELISM_MAX 4u
#define PDI_ARGON2_SALT_MIN 8u
#define PDI_ARGON2_OUTPUT_MIN 16u
#define PDI_ARGON2_OUTPUT_MAX 64u
#define PDI_ARGON2_VERSION_10 0x10u /* 16 */
#define PDI_ARGON2_VERSION_13 0x13u /* 19 */

static napi_value MakeError(napi_env env, const char *message)
{
    napi_value msg = NULL;
    napi_value err = NULL;
    napi_create_string_utf8(env, message, NAPI_AUTO_LENGTH, &msg);
    napi_create_error(env, NULL, msg, &err);
    return err;
}

static bool ThrowError(napi_env env, const char *message)
{
    napi_value err = MakeError(env, message);
    napi_throw(env, err);
    return false;
}

/* Read a Uint8Array argument into a freshly allocated buffer.
 * Returns NULL and throws when the argument is not a Uint8Array. */
static bool ReadUint8Array(napi_env env, napi_value value, uint8_t **out, size_t *outLen)
{
    bool isTypedArray = false;
    if (napi_is_typedarray(env, value, &isTypedArray) != napi_ok || !isTypedArray) {
        return ThrowError(env, "pdi_argon2: expected a Uint8Array");
    }

    napi_typedarray_type type;
    size_t length = 0;
    void *data = NULL;
    napi_value arrayBuffer = NULL;
    size_t byteOffset = 0;
    if (napi_get_typedarray_info(env, value, &type, &length, &data, &arrayBuffer, &byteOffset) != napi_ok) {
        return ThrowError(env, "pdi_argon2: could not read typed array");
    }
    if (type != napi_uint8_array) {
        return ThrowError(env, "pdi_argon2: expected a Uint8Array (uint8)");
    }

    /* Copy: the JS heap may move / be collected while argon2 runs. */
    uint8_t *copy = (uint8_t *)malloc(length == 0 ? 1 : length);
    if (copy == NULL) {
        return ThrowError(env, "pdi_argon2: out of memory");
    }
    if (length > 0) {
        memcpy(copy, data, length);
    }
    *out = copy;
    *outLen = length;
    return true;
}

static bool ReadUint32(napi_env env, napi_value value, const char *name, uint32_t *out)
{
    /* Read as a JS number, then range-check. napi_get_value_uint32 rejects
     * negative / non-integer values, which is what we want. */
    uint32_t v = 0;
    if (napi_get_value_uint32(env, value, &v) != napi_ok) {
        char buf[128];
        /* Keep the message static-ish: no user data. */
        int n = snprintf(buf, sizeof(buf), "pdi_argon2: %s must be a non-negative integer", name);
        if (n < 0) {
            buf[0] = '\0';
        }
        return ThrowError(env, buf);
    }
    *out = v;
    return true;
}

/*
 * deriveArgon2id(password, salt, memoryKiB, iterations, parallelism,
 *                outputLength, version) -> Uint8Array
 */
static napi_value DeriveArgon2id(napi_env env, napi_callback_info info)
{
    size_t argc = 7;
    napi_value argv[7] = {NULL};

    if (napi_get_cb_info(env, info, &argc, argv, NULL, NULL) != napi_ok) {
        ThrowError(env, "pdi_argon2: could not read arguments");
        return NULL;
    }
    if (argc < 7) {
        ThrowError(env, "pdi_argon2: deriveArgon2id requires 7 arguments");
        return NULL;
    }

    uint8_t *password = NULL;
    size_t passwordLen = 0;
    uint8_t *salt = NULL;
    size_t saltLen = 0;
    uint8_t *out = NULL;

    if (!ReadUint8Array(env, argv[0], &password, &passwordLen)) {
        return NULL;
    }
    if (!ReadUint8Array(env, argv[1], &salt, &saltLen)) {
        free(password);
        return NULL;
    }

    uint32_t memoryKiB = 0;
    uint32_t iterations = 0;
    uint32_t parallelism = 0;
    uint32_t outputLength = 0;
    uint32_t version = 0;
    bool ok = ReadUint32(env, argv[2], "memoryKiB", &memoryKiB) &&
              ReadUint32(env, argv[3], "iterations", &iterations) &&
              ReadUint32(env, argv[4], "parallelism", &parallelism) &&
              ReadUint32(env, argv[5], "outputLength", &outputLength) &&
              ReadUint32(env, argv[6], "version", &version);

    if (ok) {
        /* Fail closed *before* touching argon2 / allocating large memory. */
        char buf[192];
        int n = 0;
        if (passwordLen == 0) {
            n = snprintf(buf, sizeof(buf), "pdi_argon2: password must not be empty");
        } else if (saltLen < PDI_ARGON2_SALT_MIN) {
            n = snprintf(buf, sizeof(buf), "pdi_argon2: salt must be at least %u bytes", PDI_ARGON2_SALT_MIN);
        } else if (outputLength < PDI_ARGON2_OUTPUT_MIN || outputLength > PDI_ARGON2_OUTPUT_MAX) {
            n = snprintf(buf, sizeof(buf), "pdi_argon2: outputLength must be in [%u,%u]",
                         PDI_ARGON2_OUTPUT_MIN, PDI_ARGON2_OUTPUT_MAX);
        } else if (memoryKiB < PDI_ARGON2_MEMORY_KIB_MIN || memoryKiB > PDI_ARGON2_MEMORY_KIB_MAX) {
            n = snprintf(buf, sizeof(buf), "pdi_argon2: memoryKiB must be in [%u,%u]",
                         PDI_ARGON2_MEMORY_KIB_MIN, PDI_ARGON2_MEMORY_KIB_MAX);
        } else if (iterations < PDI_ARGON2_ITERATIONS_MIN || iterations > PDI_ARGON2_ITERATIONS_MAX) {
            n = snprintf(buf, sizeof(buf), "pdi_argon2: iterations must be in [%u,%u]",
                         PDI_ARGON2_ITERATIONS_MIN, PDI_ARGON2_ITERATIONS_MAX);
        } else if (parallelism < PDI_ARGON2_PARALLELISM_MIN || parallelism > PDI_ARGON2_PARALLELISM_MAX) {
            n = snprintf(buf, sizeof(buf), "pdi_argon2: parallelism must be in [%u,%u]",
                         PDI_ARGON2_PARALLELISM_MIN, PDI_ARGON2_PARALLELISM_MAX);
        } else if (version != PDI_ARGON2_VERSION_10 && version != PDI_ARGON2_VERSION_13) {
            n = snprintf(buf, sizeof(buf), "pdi_argon2: version must be 16 or 19");
        }
        if (n > 0) {
            ok = ThrowError(env, buf);
        }
    }

    if (!ok) {
        free(password);
        free(salt);
        return NULL;
    }

    out = (uint8_t *)malloc(outputLength);
    if (out == NULL) {
        free(password);
        free(salt);
        ThrowError(env, "pdi_argon2: out of memory");
        return NULL;
    }

    /* ARGON2_OK == 0. Reject anything else: do not return a partial buffer. */
    int rc = argon2id_hash_raw(iterations, memoryKiB, parallelism,
                               password, passwordLen,
                               salt, saltLen,
                               out, outputLength);

    free(password);
    free(salt);

    if (rc != ARGON2_OK) {
        free(out);
        const char *reason = argon2_error_message(rc);
        char buf[192];
        int n = snprintf(buf, sizeof(buf), "pdi_argon2: argon2id failed (code %d: %s)", rc,
                         reason == NULL ? "unknown" : reason);
        if (n < 0) {
            buf[0] = '\0';
        }
        ThrowError(env, buf);
        return NULL;
    }

    napi_value result = NULL;
    void *resultData = NULL;
    if (napi_create_arraybuffer(env, outputLength, &resultData, &result) != napi_ok || resultData == NULL) {
        free(out);
        ThrowError(env, "pdi_argon2: could not allocate result buffer");
        return NULL;
    }
    memcpy(resultData, out, outputLength);
    free(out);

    napi_value typed = NULL;
    if (napi_create_typedarray(env, napi_uint8_array, outputLength, result, 0, &typed) != napi_ok) {
        ThrowError(env, "pdi_argon2: could not create result typed array");
        return NULL;
    }
    return typed;
}

/* Reports the version of the linked reference implementation. Used by the
 * on-device self-check to assert ARGON2_VERSION_NUMBER == 19 (0x13). */
static napi_value Argon2Version(napi_env env, napi_callback_info info)
{
    napi_value out = NULL;
    napi_create_uint32(env, (uint32_t)ARGON2_VERSION_NUMBER, &out);
    return out;
}

EXTERN_C_START
static napi_value Init(napi_env env, napi_value exports)
{
    napi_property_descriptor desc[] = {
        { "deriveArgon2id", NULL, DeriveArgon2id, NULL, NULL, NULL, napi_default, NULL },
        { "argon2Version", NULL, Argon2Version, NULL, NULL, NULL, napi_default, NULL },
    };
    napi_define_properties(env, exports, sizeof(desc) / sizeof(desc[0]), desc);
    return exports;
}
EXTERN_C_END

static napi_module pdiArgon2Module = {
    .nm_version = 1,
    .nm_flags = 0,
    .nm_filename = NULL,
    .nm_register_func = Init,
    .nm_modname = "pdiargon2",
    .nm_priv = NULL,
    .reserved = { 0 },
};

/*
 * 可见性：整个模块以 -fvisibility=hidden 构建（见 CMakeLists.txt），
 * 目的是让静态链接进来的 argon2_* 不出现在动态符号表里。
 * 但 NAPI 的模块注册入口**必须**可见，否则 ArkTS 侧
 * `import nativeArgon2 from 'libpdiargon2.so'` 会找不到模块。
 * 所以这里对入口单独打开 default visibility。
 */
#if defined(A2_PDI_EXPORT)
#define PDI_EXPORT __attribute__((visibility("default")))
#else
#define PDI_EXPORT
#endif

extern "C" PDI_EXPORT __attribute__((constructor)) void RegisterPdiArgon2Module(void)
{
    napi_module_register(&pdiArgon2Module);
}
