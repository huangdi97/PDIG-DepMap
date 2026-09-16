# PDIG Android — R8 / ProGuard 规则
#
# 状态说明（2026-09-16）：
#   `app/build.gradle.kts` 的 release 构建类型目前 `isMinifyEnabled = false`，
#   因此本文件**当前不会生效**。它此前被 `proguardFiles(...)` 引用但文件并不存在
#   （悬空引用），这里补上，避免"引用了不存在的规则文件"这种隐性不确定性。
#
# 什么时候会生效：一旦打开 minify/缩码，下面这些规则就是必需的。
# 打开前必须先跑一遍 release 冒烟（启动 + 导入 + 导出 + 恢复），
# 并且不得以"构建通过"代替"运行通过"。

# 领域模型 / 序列化 / 容器：靠反射或跨端契约读取的成员不得被重命名。
# `-keep` 只保护必要面，不做 "keep everything"（那等于关闭缩码收益）。
-keep class com.pdig.core.domain.** { *; }
-keep class com.pdig.core.serialize.** { *; }
-keep class com.pdig.core.schema.** { *; }
-keep class com.pdig.core.generated.** { *; }
-keep class com.pdig.core.crypto.** { *; }

# BouncyCastle Argon2id（反射加载 provider / 算法实现）
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# SQLCipher / SQLite JDBC 风格驱动
-keep class net.zetetic.** { *; }
-keep class net.sqlcipher.** { *; }
-dontwarn net.zetetic.**
-dontwarn net.sqlcipher.**

# 枚举按 wire 值序列化（`fromWire`），名字一旦被改成 a/b/c 就会破坏容器兼容性
-keepclassmembers enum com.pdig.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留异常与注解元数据，便于真机排障时不丢信息
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable
