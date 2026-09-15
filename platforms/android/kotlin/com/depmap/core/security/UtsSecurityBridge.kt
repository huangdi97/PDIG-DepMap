package com.depmap.core.security

import android.content.Context
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * UTS（uni-app x）友好桥接层。
 *
 * ## 为什么需要这一层（2026-09-15 实测发现，见 `PRODUCTION_RUNTIME_UI_AUDIT.md` §R-3）
 *
 * 本包内所有适配器都以 `suspend fun` 暴露能力（`SecureDatabaseAdapter`、
 * `SecureKeyAdapter`、`BiometricGate`）。UTS 在 Android 上会被编译成 Kotlin，
 * 但 **UTS 语言没有协程概念**，也没有声明 `suspend` 的语法，
 * 因此 `app/uni_modules/<plugin>/utssdk/app-android/index.uts` 中对这些方法的调用，
 * 在首次真实编译时会报：
 *
 * ```
 * Kotlin: Suspension functions can only be called within coroutine body
 * ```
 *
 * 本对象把 suspend 能力包装成「普通函数 + 回调」，与
 * `SqlCipherSecureDatabaseAdapter` 既有的 UTS 调用风格（onSuccess / onError 双回调）保持一致。
 *
 * ## 线程约定
 *
 * 全部回调在**主线程**派发 —— `BiometricPrompt` 必须在主线程调用，
 * 数据库回调也直接驱动 UI 状态更新。
 *
 * ## 失败语义
 *
 * 与项目既有原则一致：**fail-closed**。启动锁不可用时返回 `false` / `"not_available"`，
 * 绝不返回「看起来通过」的结果。
 *
 * 状态：IMPLEMENTED；COMPILED = YES（Gradle 真实编译验证）；DEVICE_VERIFIED = NO（B18）。
 */
object UtsSecurityBridge {

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    // ---------------------------------------------------------------------
    // 启动锁（BiometricPrompt）
    // ---------------------------------------------------------------------

    /**
     * 从任意 Activity 构造启动锁。
     *
     * UTS 侧只能拿到 `android.app.Activity`（`UTSAndroid.getUniActivity()` 的返回类型），
     * 而 `BiometricGateAdapter` 需要 `FragmentActivity`。此处做安全向下转型，
     * **转型失败返回 null 而不是抛异常** —— 宿主不是 FragmentActivity 时启动锁不可用，
     * 由调用层 fail-closed，不伪装成可用。
     */
    fun createBiometricGate(activity: android.app.Activity?): BiometricGateAdapter? =
        (activity as? FragmentActivity)?.let { BiometricGateAdapter(it) }

    fun biometricCanAuthenticate(gate: BiometricGateAdapter?, onResult: (Boolean) -> Unit) {
        if (gate == null) {
            onResult(false)
            return
        }
        scope.launch { onResult(gate.canAuthenticate()) }
    }

    fun biometricAuthenticate(
        gate: BiometricGateAdapter?,
        reason: String,
        onResult: (ok: Boolean, reason: String?) -> Unit
    ) {
        if (gate == null) {
            onResult(false, "not_available")
            return
        }
        scope.launch {
            val result = gate.authenticate(reason)
            onResult(result.ok, result.reason)
        }
    }

    // ---------------------------------------------------------------------
    // 安全数据库
    // ---------------------------------------------------------------------

    fun createSecureKeyAdapter(context: Context): KeystoreSecureKeyAdapter =
        KeystoreSecureKeyAdapter(context)

    fun createSecureDatabase(
        context: Context,
        keyAdapter: KeystoreSecureKeyAdapter
    ): SqlCipherSecureDatabaseAdapter =
        SqlCipherSecureDatabaseAdapter(context, keyAdapter)

    fun dbOpen(
        adapter: SqlCipherSecureDatabaseAdapter,
        dbName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            try {
                adapter.open(SecureDatabaseOpenOptions(dbName))
                onSuccess()
            } catch (t: Throwable) {
                onError(t.message ?: "open_failed")
            }
        }
    }

    fun dbClose(
        adapter: SqlCipherSecureDatabaseAdapter,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            try {
                adapter.close()
                onSuccess()
            } catch (t: Throwable) {
                onError(t.message ?: "close_failed")
            }
        }
    }

    /** @param onSuccess 收到迁移后的 schema 版本号。 */
    fun dbMigrate(
        adapter: SqlCipherSecureDatabaseAdapter,
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            try {
                onSuccess(adapter.migrate())
            } catch (t: Throwable) {
                onError(t.message ?: "migrate_failed")
            }
        }
    }

    fun dbQuery(
        adapter: SqlCipherSecureDatabaseAdapter,
        sql: String,
        params: Array<Any?>,
        onSuccess: (Array<Map<String, Any?>>) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            try {
                val rows = adapter.query(sql, params.toList())
                onSuccess(rows.toTypedArray())
            } catch (t: Throwable) {
                onError(t.message ?: "query_failed")
            }
        }
    }

    /** @param onSuccess 收到受影响行数。 */
    fun dbExecute(
        adapter: SqlCipherSecureDatabaseAdapter,
        sql: String,
        params: Array<Any?>,
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            try {
                onSuccess(adapter.execute(sql, params.toList()))
            } catch (t: Throwable) {
                onError(t.message ?: "execute_failed")
            }
        }
    }

    /**
     * 事务桥接。
     *
     * UTS 侧没有 `suspend () -> T` 的表达能力，因此这里把事务体收敛成**同步**的
     * `body`：UTS 在 body 内部发出的 SQL 调用会以「回调尚未返回」的方式排队执行，
     * 故 **body 内不应依赖前一条 SQL 的回调结果**。当前 App 侧事务仅用于
     * 「Reality mutation 与 revision +1 同事务」，均为串行写且不读回，
     * 满足该约束。若将来需要 body 内串行等待，应改为原生侧批量执行接口。
     */
    fun dbTransaction(
        adapter: SqlCipherSecureDatabaseAdapter,
        body: () -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            try {
                adapter.transaction { body() }
                onSuccess()
            } catch (t: Throwable) {
                onError(t.message ?: "transaction_failed")
            }
        }
    }

    // ---------------------------------------------------------------------
    // 安全密钥
    // ---------------------------------------------------------------------

    fun getOrCreateDatabaseKey(
        adapter: KeystoreSecureKeyAdapter,
        alias: String,
        onSuccess: (ByteArray) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            try {
                onSuccess(adapter.getOrCreateDatabaseKey(alias))
            } catch (t: Throwable) {
                onError(t.message ?: "key_failed")
            }
        }
    }

    fun getOrCreateFpSecret(
        adapter: KeystoreSecureKeyAdapter,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            try {
                onSuccess(adapter.getOrCreateFpSecret())
            } catch (t: Throwable) {
                onError(t.message ?: "fp_secret_failed")
            }
        }
    }

    fun hasKey(
        adapter: KeystoreSecureKeyAdapter,
        alias: String,
        onSuccess: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            try {
                onSuccess(adapter.hasKey(alias))
            } catch (t: Throwable) {
                onError(t.message ?: "has_key_failed")
            }
        }
    }

    fun deleteKey(
        adapter: KeystoreSecureKeyAdapter,
        alias: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            try {
                adapter.deleteKey(alias)
                onSuccess()
            } catch (t: Throwable) {
                onError(t.message ?: "delete_key_failed")
            }
        }
    }
}
