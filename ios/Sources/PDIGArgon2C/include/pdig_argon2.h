#ifndef PDIG_ARGON2_H
#define PDIG_ARGON2_H

#include <stddef.h>
#include <stdint.h>

/**
 * Argon2id 原始密钥派生（对应 upstream `argon2id_hash_raw`）。
 *
 * password 传入的是**精确 UTF-8 字节**：调用方不得做 Unicode 归一化
 * （spec §38 / DEP-03：combining 与 NFC 必须派生出不同密钥）。
 *
 * 返回 0 表示成功；非 0 为 upstream `argon2_error_codes` 的错误码。
 */
int pdig_argon2id(
    const uint8_t *pwd,
    size_t pwdlen,
    const uint8_t *salt,
    size_t saltlen,
    uint32_t memory_kib,
    uint32_t iterations,
    uint32_t parallelism,
    uint8_t *out,
    size_t outlen
);

#endif /* PDIG_ARGON2_H */
