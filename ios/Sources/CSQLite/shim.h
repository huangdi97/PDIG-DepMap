// 系统 sqlite3 的转发头。
//
// 只用于 **conformance host harness**（macOS runner 上跑 canonical 用例）。
// App 的持久化层必须用 SQLCipher；本文件不构成那层实现，也不得被 App target 引用。
//
// 为什么 host 用系统 sqlite 而不是 SQLCipher：
// 这两条用例（backup 往返 / schema 迁移）验证的是 payload 序列化与迁移逻辑，
// 与 at-rest 加密无关 —— `.depmap` 容器加密由 CryptoKit 的 AES-GCM 单独覆盖，
// 且已在 depmap-golden-v1 用例中做了字节级 golden 校验。
#ifndef PDIG_CSQLITE_SHIM_H
#define PDIG_CSQLITE_SHIM_H

#include <sqlite3.h>

#endif
