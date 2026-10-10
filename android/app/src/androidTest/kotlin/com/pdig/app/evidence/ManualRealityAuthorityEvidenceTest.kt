package com.pdig.app.evidence

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pdig.app.data.AppContainer
import com.pdig.app.data.ManualNodeCreateRequest
import com.pdig.app.platform.AndroidSqliteDriver
import com.pdig.core.generated.NodeKind
import com.pdig.core.schema.migrate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * R26 production-authority evidence for Manual Establish.
 *
 * This does not make Preview mutable. It proves the AppContainer-facing production
 * authority exists and obeys Canonical runtime-creatable policy / revision rules.
 */
@RunWith(AndroidJUnit4::class)
class ManualRealityAuthorityEvidenceTest {

    private fun tempContainer(name: String): Pair<AppContainer, AndroidSqliteDriver> {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(ctx.filesDir, name).apply { if (exists()) delete() }
        val driver = AndroidSqliteDriver.open(file, "r26-manual-authority-test")
        migrate(driver, "2026-10-10T00:00:00.000Z")
        return AppContainer.forDriver(driver) to driver
    }

    @Test
    fun manualCreateUsesCanonicalPolicyBumpsOnceAndCreatesNoDependency() {
        val (app, driver) = tempContainer("r26-manual-node.db")
        try {
            val before = app.graphRevision()
            val result = app.createManualNode(
                ManualNodeCreateRequest(
                    kind = NodeKind.PAYMENT_INSTRUMENT,
                    name = "  手工信用卡  ",
                    issuer = "示例银行",
                    last4 = "8823",
                ),
            )

            assertEquals(before + 1, result.graphRevision)
            assertEquals(result.graphRevision, app.graphRevision())
            assertEquals("payment_instrument", result.node.kind)
            assertEquals("手工信用卡", result.node.name)
            assertEquals("示例银行", result.node.issuer)
            assertEquals("8823", result.node.last4)
            assertEquals(0, app.dependencies().size)

            val second = app.createManualNode(
                ManualNodeCreateRequest(
                    kind = NodeKind.PAYMENT_INSTRUMENT,
                    name = "手工信用卡",
                ),
            )
            assertNotEquals(
                "same display name must not silently merge two explicitly-created Reality objects",
                result.node.id,
                second.node.id,
            )
            assertEquals(before + 2, second.graphRevision)
            assertEquals(0, app.dependencies().size)
        } finally {
            driver.close()
        }
    }

    @Test
    fun manualCreateRejectsKindsOutsideCanonicalRuntimeCreationSet() {
        val (app, driver) = tempContainer("r26-manual-kind-gate.db")
        try {
            val before = app.graphRevision()
            val error = runCatching {
                app.createManualNode(
                    ManualNodeCreateRequest(
                        kind = NodeKind.DEVICE,
                        name = "不应创建的设备",
                    ),
                )
            }.exceptionOrNull()

            assertTrue(error is IllegalArgumentException)
            assertEquals(before, app.graphRevision())
            assertTrue(app.nodes().isEmpty())
        } finally {
            driver.close()
        }
    }

    @Test
    fun issuerAndLast4CannotLeakOntoNonPaymentObjects() {
        val (app, driver) = tempContainer("r26-manual-payment-fields.db")
        try {
            val before = app.graphRevision()
            val error = runCatching {
                app.createManualNode(
                    ManualNodeCreateRequest(
                        kind = NodeKind.ACCOUNT,
                        name = "账户",
                        issuer = "不应写入",
                    ),
                )
            }.exceptionOrNull()

            assertTrue(error is IllegalArgumentException)
            assertEquals(before, app.graphRevision())
            assertTrue(app.nodes().isEmpty())
        } finally {
            driver.close()
        }
    }
}
