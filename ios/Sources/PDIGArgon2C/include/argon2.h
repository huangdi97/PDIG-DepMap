/*
 * 转发头：把 `argon2.h` 的解析指向 **vendored 单一真源**。
 *
 * 为什么不复制 third_party/argon2 到包内：
 *   复制件会让 VENDOR.json 的哈希校验失去意义 —— 改了 vendored 原件而副本不动，
 *   哈希门禁照样通过。所以这里只做一层 include 转发，源码仍然只有一份。
 *
 * 为什么需要这一层：
 *   SwiftPM 禁止把 header search path 指到包外
 *   （"header search path should not be outside the package root"），
 *   因此 -I 方案不可用；而 vendored 的 `src/argon2.c` 里写的是
 *   `#include "argon2.h"`，只会先在**自己所在目录**找、再沿 -I 找。
 *   本文件位于 target 的 include/ 下（SwiftPM 自动加入搜索路径），
 *   于是那句 include 会解析到这里，再由这里指向真源。
 */

#include "../../../../third_party/argon2/include/argon2.h"
