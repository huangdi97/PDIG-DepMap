package com.pdig.app.evidence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pdig.app.ui.components.PdigScrollingPage
import com.pdig.app.ui.theme.PDIGTheme
import com.pdig.app.ui.theme.PdigTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P1-A —— 页面末尾交互元素的 hitbox 不变量。
 *
 * 背景：2026-09-16 的 E2E 记录观察到「确认导入」按钮"看得见、点中心没反应"，
 * 当时的推断是底部裁剪导致语义 bounds 与可点击区错位。
 * 本测试把这条**结构不变量**固定下来（不放宽断言、不用 tap offset 绕过）：
 *
 *   视觉区域 == 真实点击区域 == semantics bounds
 *
 * 具体断言：
 *  1. 末尾按钮滚入视野后，其 semantics bounds 完整落在根节点内（未被视口裁剪）
 *  2. 在 bounds **中心**注入点击必须触发回调（等价于 host 侧 `input tap` 中心）
 *  3. 按钮的可点击高度满足 48dp 最小触摸目标（design-tokens）
 *
 * 注意诚实边界：本测试是**结构**证据，不能单独证明真机上某种注入方式可用。
 * 真机侧由 E2E 直接 `input tap` 语义中心做交叉验证（见 closure report P1-A 小节）。
 */
@RunWith(AndroidJUnit4::class)
class ImportHitboxTest {

    @get:Rule
    val compose = createComposeRule()

    private val trailingTag = "hitbox_trailing_button"

    @Composable
    private fun Harness(onTrailingClick: () -> Unit) {
        PDIGTheme {
            PdigScrollingPage(
                modifier = Modifier.padding(PdigTokens.SpaceLg),
                verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceMd),
            ) {
                // 足够多的内容，把末尾按钮顶出首屏
                repeat(60) { index ->
                    Text("第 $index 行占位内容", style = PdigTokens.Body)
                }
                Button(
                    onClick = onTrailingClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = PdigTokens.MinTouchTarget)
                        .testTag(trailingTag),
                ) { Text("确认导入") }
            }
        }
    }

    @Test
    fun trailingButtonBoundsAreNotClipped_andItsCenterIsClickable() {
        var clicks = 0
        compose.setContent { Harness { clicks += 1 } }

        val node = compose.onNodeWithTag(trailingTag)
        node.performScrollTo()
        compose.waitForIdle()

        val rootBounds = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val buttonBounds = node.fetchSemanticsNode().boundsInRoot
        // 用设备真实 density，不依赖 Compose 测试 API 的 density 类型
        val densityScale = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density

        assertTrue(
            "末尾按钮的 semantics bounds 必须完整落在根节点内（未被滚动视口裁剪）: " +
                "button=$buttonBounds root=$rootBounds",
            buttonBounds.top >= rootBounds.top - 1f && buttonBounds.bottom <= rootBounds.bottom + 1f,
        )
        assertEquals(
            "按钮必须横跨内容宽度（与视觉一致）",
            rootBounds.left + densityScale * 16f,
            buttonBounds.left,
            2f,
        )

        val heightDp = buttonBounds.height / densityScale
        assertTrue(
            "可点击高度必须 ≥ 48dp（accessibility §84），实际 $heightDp dp",
            heightDp >= 47.5f,
        )

        node.assertIsDisplayed()
        // 在 semantics bounds 的中心注入点击 —— 与 host 侧 `input tap <center>` 等价
        node.performClick()
        compose.waitForIdle()
        assertEquals("在按钮中心点击必须触发回调", 1, clicks)
    }

    @Test
    fun clickingTheScrolledTrailingButtonStillWorksAfterUserScrollsToTheVeryBottom() {
        var clicks = 0
        compose.setContent { Harness { clicks += 1 } }

        val node = compose.onNodeWithTag(trailingTag)
        // 滚到最底部（用户可能滑动到底部留白区）后再点击，位置仍必须准确
        node.performScrollTo()
        compose.waitForIdle()
        node.performClick()
        compose.waitForIdle()
        node.performClick()
        compose.waitForIdle()

        assertEquals("连续两次点击都必须生效（不存在'第一次点击被吞'）", 2, clicks)
    }
}
