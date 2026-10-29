package com.pdig.uivnext.evidence

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pdig.uivnext.VNextShellViewModel
import com.pdig.uivnext.ui.WorkspacePreferenceStore
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Production VNext uses the same store-backed VNextShellViewModel as Preview.
 *
 * This proves privacy/motion/upcoming/rail preferences survive a new ViewModel
 * instance without entering Canonical Reality. The original test-app preferences
 * are restored in finally so the contract is non-destructive.
 */
@RunWith(AndroidJUnit4::class)
class ProductionPresentationPersistenceContractTest {
    @Test
    fun workspacePreferencesRoundTripAcrossViewModelInstances() {
        val application = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as Application
        val store = WorkspacePreferenceStore(application)
        val original = store.load()

        try {
            val first = VNextShellViewModel(application)
            first.app.privacyMask = !original.privacyMask
            first.app.reduceMotion = !original.reduceMotion
            first.app.showUpcoming = !original.showUpcoming
            first.app.railExpanded = !original.railExpanded

            val persisted = store.load()
            assertEquals(!original.privacyMask, persisted.privacyMask)
            assertEquals(!original.reduceMotion, persisted.reduceMotion)
            assertEquals(!original.showUpcoming, persisted.showUpcoming)
            assertEquals(!original.railExpanded, persisted.railExpanded)

            val second = VNextShellViewModel(application)
            assertEquals(persisted.privacyMask, second.app.privacyMask)
            assertEquals(persisted.reduceMotion, second.app.reduceMotion)
            assertEquals(persisted.showUpcoming, second.app.showUpcoming)
            assertEquals(persisted.railExpanded, second.app.railExpanded)
        } finally {
            store.save(original)
        }
    }
}
