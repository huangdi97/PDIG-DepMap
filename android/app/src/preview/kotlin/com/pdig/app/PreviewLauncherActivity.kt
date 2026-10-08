package com.pdig.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pdig.uivnext.VNextApp
import com.pdig.uivnext.VNextShellViewModel

/**
 * Dedicated launcher for the preview flavor.
 *
 * Normal home-screen launch ALWAYS enters the accepted Light-first UI vNext.
 * This is an isolated synthetic-reference experience; production PersonalReality
 * and the existing lock-gated MainActivity are not entry points in this flavor.
 */
class PreviewLauncherActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val model: VNextShellViewModel = viewModel()
            VNextApp(model.app)
        }
    }
}
