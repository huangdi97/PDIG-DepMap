package com.pdig.app

import android.app.Application

/**
 * 无 analytics、无 crash SDK、无业务网络（spec §130/§240/§241）。
 * 本地 diagnostics 只输出非敏感信息：app version / schema / error code / platform / counts。
 */
class PdigApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // SQLCipher for Android（net.zetetic:sqlcipher-android）的 native 库不会自动加载。
        // 不显式 loadLibrary 时首次打开本地库会抛：
        //   java.lang.UnsatisfiedLinkError:
        //     No implementation found for long net.zetetic.database.sqlcipher.SQLiteConnection.nativeOpen(...)
        // 这是 2026-09-15 在 emulator(API 34 / x86_64) 上实测到的首启崩溃（E2E-02）。
        // 必须在任何 SqliteDriver 使用之前完成加载。
        System.loadLibrary("sqlcipher")
    }
}
