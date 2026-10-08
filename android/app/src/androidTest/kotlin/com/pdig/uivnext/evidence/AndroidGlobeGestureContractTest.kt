package com.pdig.uivnext.evidence

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pdig.uivnext.VNextApp
import com.pdig.uivnext.createVNextAppState
import com.pdig.uivnext.model.VTestIds
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This uses two actual injected pointers on the globe Canvas. A Kotlin
 * camera-math unit test alone cannot prove that Compose receives a pinch.
 * The test deliberately verifies the controller, not a fake CSS/Bitmap scale.
 */
@RunWith(AndroidJUnit4::class)
class AndroidGlobeGestureContractTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun pinchOutThenPinchInActuallyMovesGlobeCameraZoom() {
        val app = createVNextAppState().apply { reduceMotion = true }
        compose.setContent { VNextApp(app) }
        compose.waitForIdle()
        val initial = app.globe.camera.zoom

        compose.onNodeWithTag(VTestIds.GLOBE_CANVAS, useUnmergedTree = true)
            .performTouchInput {
                val c = center
                pinch(
                    start0 = Offset(c.x - width * .08f, c.y),
                    end0 = Offset(c.x - width * .23f, c.y),
                    start1 = Offset(c.x + width * .08f, c.y),
                    end1 = Offset(c.x + width * .23f, c.y),
                    durationMillis = 560,
                )
            }
        compose.runOnIdle {
            assertTrue("Two-finger pinch OUT must change actual globe zoom",
                app.globe.camera.zoom > initial * 1.15f)
        }
        val expanded = app.globe.camera.zoom
        compose.onNodeWithTag(VTestIds.GLOBE_CANVAS, useUnmergedTree = true)
            .performTouchInput {
                val c = center
                pinch(
                    start0 = Offset(c.x - width * .23f, c.y),
                    end0 = Offset(c.x - width * .08f, c.y),
                    start1 = Offset(c.x + width * .23f, c.y),
                    end1 = Offset(c.x + width * .08f, c.y),
                    durationMillis = 560,
                )
            }
        compose.runOnIdle {
            assertTrue("Two-finger pinch IN must reduce actual globe zoom",
                app.globe.camera.zoom < expanded * .90f)
        }
    }
}
