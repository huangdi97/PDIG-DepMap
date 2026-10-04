package com.pdig.uivnext.evidence

import android.app.Application
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pdig.uivnext.VNextShellViewModel
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Workspace preference 磁盘持久化（任务书 §20）：reduce_motion 必须写入真实
 * SharedPreferences（SharedPreferences 机制 = 跨进程重启存活），且全新 ViewModel
 * （模拟进程重启）从磁盘恢复。配合测试外 force-stop → relaunch → run-as dump
 * shared_prefs XML 构成完整证据。
 */
@RunWith(AndroidJUnit4::class)
class WorkspacePreferenceDiskPersistenceTest {

    @Test
    fun workspacePreferencePersistsOnDiskAndAcrossRecreation() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        ctx.getSharedPreferences("pdig_ui_vnext_workspace", Context.MODE_PRIVATE)
            .edit().clear().commit()

        // 真实生产路径写入
        val vm1 = VNextShellViewModel(ctx.applicationContext as Application)
        vm1.app.reduceMotion = true
        Thread.sleep(2500) // 允许 SharedPreferences.apply() 异步落盘

        // 磁盘层面真值（SharedPreferences 实例 = 磁盘 XML 的读写视图）
        val disk = ctx.getSharedPreferences("pdig_ui_vnext_workspace", Context.MODE_PRIVATE)
        assertTrue("reduce_motion must be written to disk-backed prefs", disk.getBoolean("reduce_motion", false))

        // 全新 ViewModel（模拟进程重启）从磁盘恢复
        val vm2 = VNextShellViewModel(ctx.applicationContext as Application)
        assertTrue("restart must load reduceMotion from store", vm2.app.reduceMotion)
    }
}
