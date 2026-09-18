# HarmonyOS native build for the pdiargon2 NAPI bridge.
#
# Trust boundary: this module compiles the unmodified PHC Argon2 reference
# implementation (third_party/argon2 @ 20190702 / 62358ba2) plus a thin NAPI
# wrapper that only marshals arguments. No cryptography is implemented here.
#
# Symbol visibility: libargon2 is compiled with -fvisibility=hidden and WITHOUT
# A2_VISCTL, so that "argon2_*" never enters the dynamic symbol table. Only
# the NAPI module entry point (RegisterPdiArgon2Module) is exported, which is
# what the ArkTS runtime resolves on load. See the long note at
# target_compile_options() below for the measured reason A2_VISCTL must NOT be set.

cmake_minimum_required(VERSION 3.5.0)
project(pdiargon2)

set(CMAKE_C_STANDARD 99)
set(CMAKE_CXX_STANDARD 17)

# third_party/argon2 relative to harmony/entry/src/main/cpp
set(ARGON2_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../../../../../third_party/argon2")
get_filename_component(ARGON2_ROOT "${ARGON2_ROOT}" ABSOLUTE)

if(NOT EXISTS "${ARGON2_ROOT}/include/argon2.h")
  message(FATAL_ERROR "vendored argon2 not found at ${ARGON2_ROOT}; see third_party/argon2/VENDOR.json")
endif()

set(ARGON2_SOURCES
  "${ARGON2_ROOT}/src/argon2.c"
  "${ARGON2_ROOT}/src/core.c"
  "${ARGON2_ROOT}/src/encoding.c"
  "${ARGON2_ROOT}/src/thread.c"
  "${ARGON2_ROOT}/src/blake2/blake2b.c"
)

# Architecture-conditional fill_segment:
#   ref.c is the portable implementation and is REQUIRED for arm64 and x86_64.
#   opt.c is the x86 SSE2 variant and is intentionally NOT vendored, so it
#   cannot be selected by accident (that mistake was observed as
#   "undefined reference to fill_segment"). See third_party/argon2/VENDOR.json.
list(APPEND ARGON2_SOURCES "${ARGON2_ROOT}/src/ref.c")

set(PDI_AR2_SOURCES
  "${ARGON2_SOURCES}"
  "${CMAKE_CURRENT_SOURCE_DIR}/pdi_argon2.cpp"
)

add_library(pdiargon2 SHARED ${PDI_AR2_SOURCES})

target_include_directories(pdiargon2 PRIVATE
  "${ARGON2_ROOT}/include"
  "${ARGON2_ROOT}/src"
)

# 可见性策略（实测校正，勿轻改）：
#
# 上游 Makefile 用 "-fvisibility=hidden -DA2_VISCTL=1" 构建 libargon2.so，
# 其意图是**导出** argon2_* —— A2_VISCTL 把 argon2.h 里的 ARGON2_PUBLIC 定义成
# visibility("default")。本项目是把 libargon2 **静态链接进 NAPI 模块**，
# 不需要也不应该把 argon2_* 暴露给 ArkTS，所以：
#
#   - 不定义 A2_VISCTL        -> ARGON2_PUBLIC 为空
#   - 只加 -fvisibility=hidden -> argon2_* 全部隐藏
#   - 只对 NAPI 入口单独打开 default visibility（见 pdi_argon2.cpp 的 PDI_EXPORT）
#
# 实测（tools/harmony/check-argon2-native-build.mjs）：
#   带 -DA2_VISCTL=1 -> 动态符号表导出 22 个 argon2_*   (FAIL)
#   不带             -> 导出 0 个 argon2_*，入口可见    (PASS)
target_compile_options(pdiargon2 PRIVATE
  -fPIC
  -fvisibility=hidden
  -Wall
  -Wextra
  -Wno-unused-parameter
)

# 只给桥接开 NAPI 入口的可见性。
set_source_files_properties(
  "${CMAKE_CURRENT_SOURCE_DIR}/pdi_argon2.cpp"
  PROPERTIES COMPILE_DEFINITIONS "A2_PDI_EXPORT=1"
)

# libc / libm / pthread are provided by the OHOS sysroot.
target_link_libraries(pdiargon2 PRIVATE
  libace_napi.z.so
  pthread
  m
)

# Only the NAPI entry point is exported; -fvisibility=hidden covers the rest.
set_target_properties(pdiargon2 PROPERTIES
  PREFIX "lib"
  OUTPUT_NAME "pdiargon2"
)
