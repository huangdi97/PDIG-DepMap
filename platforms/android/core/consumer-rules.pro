# DepMap Android core — consumer ProGuard/R8 规则
#
# 本模块对外暴露 SQLCipher / Keystore / Biometric 适配器与 .depmap 容器解析。
# 当前无需要保留的反射入口；规则文件的存在性由 core/build.gradle.kts
# 的 consumerProguardFiles("consumer-rules.pro") 引用。
#
# 注意：新增任何通过反射访问的类（例如序列化绑定）时，必须在此补充 keep 规则。
