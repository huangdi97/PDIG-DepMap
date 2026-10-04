package com.pdig.uivnext

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.pdig.uivnext.ui.PresentationProfileStore
import com.pdig.uivnext.ui.WorkspacePreferenceStore

/**
 * UI vNext 状态宿主。
 *
 * Activity 重建期间保持导航/选择/投影；已保存的 PresentationProfile 通过本机 store
 * 跨进程重启恢复。未保存的 Studio 编辑与搜索词不会持久化。
 */
class VNextShellViewModel(application: Application) : AndroidViewModel(application) {
    private val presentationStore = PresentationProfileStore(application)
    private val workspaceStore = WorkspacePreferenceStore(application)

    val app = createVNextAppState(
        initialPresentationProfiles = presentationStore.loadAll(),
        initialWorkspacePreferences = workspaceStore.load(),
        onPresentationProfileSaved = presentationStore::save,
        onWorkspacePreferencesSaved = workspaceStore::save,
    )
}
