package com.pdig.app.evidence

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pdig.app.data.AppContainer
import com.pdig.app.data.ManualDependencyCreateRequest
import com.pdig.app.data.ManualIdentityAnchorCreateRequest
import com.pdig.app.data.ManualNodeCreateRequest
import com.pdig.app.platform.AndroidSqliteDriver
import com.pdig.core.domain.confirmedIdentityAnchorProfile
import com.pdig.core.generated.Capability
import com.pdig.core.generated.IdentityAnchorSubtype
import com.pdig.core.generated.NodeKind
import com.pdig.core.generated.VerificationBasisType
import com.pdig.core.generated.Relation
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
    fun manualPhoneIdentityCreatesNodeProfileAndIdentifierAtomically() {
        val (app, driver) = tempContainer("r38-manual-phone.db")
        try {
            val before = app.graphRevision()
            val result = app.createManualIdentityAnchor(
                ManualIdentityAnchorCreateRequest(
                    subtype = IdentityAnchorSubtype.PHONE_NUMBER,
                    name = "香港主号",
                    identifierValue = "  +852 6123 4567  ",
                ),
            )

            assertEquals(before + 1, result.graphRevision)
            assertEquals(result.graphRevision, app.graphRevision())
            assertEquals("identity_anchor", result.node.kind)
            assertEquals("香港主号", result.node.name)
            assertTrue(app.dependencies().isEmpty())

            val profile = confirmedIdentityAnchorProfile(
                NodeKind.IDENTITY_ANCHOR,
                result.node.fieldsJson,
            )
            requireNotNull(profile)
            assertEquals(IdentityAnchorSubtype.PHONE_NUMBER, profile.subtype)
            assertEquals(VerificationBasisType.USER_CONFIRMED, profile.verificationBasisType)
            requireNotNull(profile.identifier)
            assertEquals("+852 6123 4567", profile.identifier?.value)
            assertEquals(
                VerificationBasisType.USER_CONFIRMED,
                profile.identifier?.verificationBasisType,
            )
            assertTrue(profile.identifier?.confirmedAt?.isNotBlank() == true)
        } finally {
            driver.close()
        }
    }

    @Test
    fun manualIdentityDoesNotSilentlyMergeSameConfirmedIdentifier() {
        val (app, driver) = tempContainer("r38-manual-identity-duplicate.db")
        try {
            val first = app.createManualIdentityAnchor(
                ManualIdentityAnchorCreateRequest(
                    IdentityAnchorSubtype.EMAIL_ADDRESS,
                    "主邮箱",
                    "user@example.com",
                ),
            )
            val second = app.createManualIdentityAnchor(
                ManualIdentityAnchorCreateRequest(
                    IdentityAnchorSubtype.EMAIL_ADDRESS,
                    "备用记录",
                    "user@example.com",
                ),
            )

            assertNotEquals(first.node.id, second.node.id)
            assertEquals(first.graphRevision + 1, second.graphRevision)
            assertEquals(2, app.nodes().count { it.kind == "identity_anchor" })
            assertTrue(app.dependencies().isEmpty())
        } finally {
            driver.close()
        }
    }

    @Test
    fun invalidManualIdentityFailsClosedWithoutRevisionMutation() {
        val (app, driver) = tempContainer("r38-manual-identity-invalid.db")
        try {
            val before = app.graphRevision()

            val otherSubtype = runCatching {
                app.createManualIdentityAnchor(
                    ManualIdentityAnchorCreateRequest(
                        IdentityAnchorSubtype.OTHER_IDENTITY,
                        "不应创建",
                        "identifier",
                    ),
                )
            }.exceptionOrNull()
            assertTrue(otherSubtype is IllegalArgumentException)

            val blankIdentifier = runCatching {
                app.createManualIdentityAnchor(
                    ManualIdentityAnchorCreateRequest(
                        IdentityAnchorSubtype.PHONE_NUMBER,
                        "空号码",
                        "   ",
                    ),
                )
            }.exceptionOrNull()
            assertTrue(blankIdentifier is IllegalArgumentException)

            val controlIdentifier = runCatching {
                app.createManualIdentityAnchor(
                    ManualIdentityAnchorCreateRequest(
                        IdentityAnchorSubtype.EMAIL_ADDRESS,
                        "控制字符",
                        "a@example.com\u0000",
                    ),
                )
            }.exceptionOrNull()
            assertTrue(controlIdentifier is IllegalArgumentException)

            assertEquals(before, app.graphRevision())
            assertTrue(app.nodes().isEmpty())
        } finally {
            driver.close()
        }
    }

    @Test
    fun manualRelationshipUsesV3RuntimeRegistryAndBumpsOnce() {
        val (app, driver) = tempContainer("r31-manual-relationship.db")
        try {
            val card = app.createManualNode(
                ManualNodeCreateRequest(
                    kind = NodeKind.PAYMENT_INSTRUMENT,
                    name = "主卡",
                ),
            ).node
            val service = app.createManualNode(
                ManualNodeCreateRequest(
                    kind = NodeKind.SERVICE,
                    name = "视频服务",
                ),
            ).node
            val before = app.graphRevision()

            val created = app.createManualDependency(
                ManualDependencyCreateRequest(
                    fromNodeId = card.id,
                    relation = Relation.MERCHANT_AGREEMENT,
                    toNodeId = service.id,
                    capability = Capability.PAYMENT,
                ),
            )

            assertEquals(before + 1, created.graphRevision)
            assertTrue(created.created)
            assertTrue(!created.reactivated)
            assertEquals("merchant_agreement", created.dependency.relation)
            assertEquals("payment", created.dependency.capability)
            assertEquals("unknown", created.dependency.criticality)
            assertEquals("active", created.dependency.state)

            val raw = driver.prepare(
                "SELECT origin, verification_basis_type FROM dependencies WHERE id = ?",
            ).get(created.dependency.id)
            assertEquals("manual", raw?.str("origin"))
            assertEquals("user_confirmed", raw?.str("verification_basis_type"))
        } finally {
            driver.close()
        }
    }

    @Test
    fun manualRelationshipReconfirmKeepsLogicalRowAndExplicitRequiredCanUpgrade() {
        val (app, driver) = tempContainer("r31-manual-relationship-reconfirm.db")
        try {
            val account = app.createManualNode(
                ManualNodeCreateRequest(NodeKind.ACCOUNT, "账户"),
            ).node
            val service = app.createManualNode(
                ManualNodeCreateRequest(NodeKind.SERVICE, "服务"),
            ).node

            val first = app.createManualDependency(
                ManualDependencyCreateRequest(
                    fromNodeId = account.id,
                    relation = Relation.MERCHANT_AGREEMENT,
                    toNodeId = service.id,
                    capability = Capability.PAYMENT,
                ),
            )
            val revisionAfterFirst = first.graphRevision
            val second = app.createManualDependency(
                ManualDependencyCreateRequest(
                    fromNodeId = account.id,
                    relation = Relation.MERCHANT_AGREEMENT,
                    toNodeId = service.id,
                    capability = Capability.PAYMENT,
                    required = true,
                ),
            )

            assertEquals(first.dependency.id, second.dependency.id)
            assertTrue(!second.created)
            assertEquals("required", second.dependency.criticality)
            assertEquals(revisionAfterFirst + 1, second.graphRevision)
            assertEquals(1, app.dependencies().count { it.id == first.dependency.id })
        } finally {
            driver.close()
        }
    }

    @Test
    fun manualRelationshipRejectsCapabilityMismatchAndStorageOnlyRelation() {
        val (app, driver) = tempContainer("r31-manual-relationship-invalid.db")
        try {
            val account = app.createManualNode(
                ManualNodeCreateRequest(NodeKind.ACCOUNT, "账户"),
            ).node
            val service = app.createManualNode(
                ManualNodeCreateRequest(NodeKind.SERVICE, "服务"),
            ).node
            val before = app.graphRevision()

            val mismatch = runCatching {
                app.createManualDependency(
                    ManualDependencyCreateRequest(
                        fromNodeId = account.id,
                        relation = Relation.MERCHANT_AGREEMENT,
                        toNodeId = service.id,
                        capability = Capability.RECOVERY,
                    ),
                )
            }.exceptionOrNull()
            assertTrue(mismatch is IllegalArgumentException)
            assertEquals(before, app.graphRevision())

            val storageOnly = runCatching {
                app.createManualDependency(
                    ManualDependencyCreateRequest(
                        fromNodeId = account.id,
                        relation = Relation.VERIFIES,
                        toNodeId = service.id,
                        capability = Capability.AUTHENTICATION,
                    ),
                )
            }.exceptionOrNull()
            assertTrue(storageOnly is IllegalArgumentException)
            assertEquals(before, app.graphRevision())
            assertTrue(app.dependencies().isEmpty())
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
