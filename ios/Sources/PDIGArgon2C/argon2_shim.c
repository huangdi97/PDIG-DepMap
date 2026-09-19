/*
 * PDIG —— vendored Argon2（third_party/argon2）的 SwiftPM 编译单元。
 *
 * 为什么用 `#include "...c"` 把源码拉进一个 TU，而不是让 SwiftPM 直接编译
 * vendored 目录：
 *   - SwiftPM 的 target path / header search path 都不能指到包外；
 *   - 复制到包内 = 制造第二份 argon2 源码，VENDOR.json 的哈希校验就失去意义。
 * 因此这里保留单一真源：相对路径 include 真源，配合 include/argon2.h 转发头。
 *
 * 编译单元清单与 upstream 的 Makefile 一致（ref 实现 + 线程层）。
 */

#include "pdig_argon2.h"

#include "../../../third_party/argon2/src/argon2.c"
#include "../../../third_party/argon2/src/core.c"
#include "../../../third_party/argon2/src/encoding.c"
#include "../../../third_party/argon2/src/ref.c"
#include "../../../third_party/argon2/src/thread.c"
#include "../../../third_party/argon2/src/blake2/blake2b.c"

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
