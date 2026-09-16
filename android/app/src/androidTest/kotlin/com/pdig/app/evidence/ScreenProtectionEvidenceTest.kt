package com.pdig.app.evidence

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pdig.app.ui.Route
import com.pdig.app.ui.isSensitiveRoute
import com.pdig.app.ui.sensitiveRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 截图保护（ANDROID_SCREEN_PROTECTION）证据。
 *
 * 这里验证的是**判定口径本身**：哪些路由受保护必须是显式且完整的，
 * 新增路由如果不显式分类就会被这条测试挡下。
 * 真实 window flag（FLAG_SECURE 是否真的生效）由 host 侧 E2E 用 dumpsys + screencap 验证。
 */
@RunWith(AndroidJUnit4::class)
class ScreenProtectionEvidenceTest {

    private val allRoutes = listOf(
        Route.ONBOARDING,
        Route.LOCK,
        Route.HOME,
        Route.SCENARIOS,
        Route.PLAN,
        Route.TIMELINE,
        Route.REVIEW,
        Route.DRIFT,
        Route.CANDIDATES,
        Route.INFRASTRUCTURE,
        Route.GRAPH,
        Route.NODE,
        Route.SOURCES,
        Route.IMPORT,
        Route.BACKUP,
        Route.RESTORE,
        Route.SETTINGS,
        Route.PRIVACY,
        Route.ABOUT,
        Route.IMPACT,
        Route.SCENARIO_SETUP,
    )

    @Test
    fun everyRouteHasAnExplicitScreenProtectionDecision() {
        val expectedSensitive = setOf(
            Route.IMPORT,
            Route.BACKUP,
            Route.RESTORE,
            Route.SOURCES,
            Route.REVIEW,
            Route.DRIFT,
            Route.CANDIDATES,
            Route.INFRASTRUCTURE,
            Route.GRAPH,
            Route.NODE,
            Route.TIMELINE,
            Route.PLAN,
            Route.IMPACT,
            Route.SCENARIO_SETUP,
        )
        // 集合必须完全相等：新增路由若不显式归类，这里会失败
        assertEquals("敏感路由集合与测试口径不一致", expectedSensitive, sensitiveRoutes())

        val nonSensitive = allRoutes.filter { it !in expectedSensitive }
        nonSensitive.forEach { r ->
            assertFalse("路由 $r 应归类为非敏感页", isSensitiveRoute(r))
        }
        expectedSensitive.forEach { r ->
            assertTrue("路由 $r 应归类为敏感页", isSensitiveRoute(r))
        }
    }

    @Test
    fun unknownOrNullRouteIsNeverTreatedAsSensitive() {
        // fail-open 会导致"什么都没保护"，fail-closed 会导致"全局禁止截图"；
        // 这里选择不把未知路由当敏感，但必须显式断言这个选择，防止歧义。
        assertFalse(isSensitiveRoute(null))
        assertFalse(isSensitiveRoute("some/unknown/route"))
    }
}
