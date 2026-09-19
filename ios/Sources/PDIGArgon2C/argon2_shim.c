/*
 * PDIG —— vendored Argon2 (third_party/argon2) 的 SwiftPM 编译单元。
 *
 * 为什么用 `#include "...c"` 把源码拉进一个 TU，而不是让 SwiftPM 直接编译
 * vendored 目录：
 *   - SwiftPM 的 target path 必须落在包内；third_party/ 在仓库根的**包外**。
 *   - 复制到包内 = 制造第二份 argon2 源码，VENDOR.json 的哈希校验就失去意义
 *     （改了 vendored 原件而副本不动，也能"通过"）。
 * 因此这里保留单一真源：只加一个 -I 指向 vendored 根目录，把需要的 .c 拉进来。
 *
 * 编译单元清单与 upstream 的 Makefile 一致（ref 实现 + 线程层）。
 */

#include "pdig_argon2.h"

#include "src/argon2.c"
#include "src/core.c"
#include "src/encoding.c"
#include "src/ref.c"
#include "src/thread.c"
#include "src/blake2/blake2b.c"

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
) {
    return argon2id_hash_raw(
        iterations,
        memory_kib,
        parallelism,
        pwd,
        pwdlen,
        salt,
        saltlen,
        out,
        outlen
    );
}
