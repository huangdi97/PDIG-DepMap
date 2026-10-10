package com.pdig.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pdig.uivnext.VNextApp
import com.pdig.uivnext.VNextShellViewModel

/**
 * Dedicated launcher for the preview flavor.
 *
 * Normal home-screen launch ALWAYS enters the current Light-first UI vNext review candidate.
 * This is an isolated synthetic-reference experience; production PersonalReality
 * and the existing lock-gated MainActivity are not entry points in this flavor.
 */
class PreviewLauncherActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("pdig_preview_guide", MODE_PRIVATE)
        setContent {
            val model: VNextShellViewModel = viewModel()
            var showGuide by androidx.compose.runtime.saveable.rememberSaveable {
                androidx.compose.runtime.mutableStateOf(shouldShowPreviewGuide(prefs.getInt("completed_version", 0)))
            }
            if (showGuide) {
                PreviewWelcomeFlow(onFinish = {
                    prefs.edit().putInt("completed_version", PREVIEW_GUIDE_VERSION).apply()
                    showGuide = false
                })
            } else {
                VNextApp(model.app, onHelp = { showGuide = true })
            }
        }
    }
}
