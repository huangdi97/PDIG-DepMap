package com.pdig.uivnext.production

import com.pdig.app.data.AppContainer
import com.pdig.app.data.ManualIdentityAnchorCreateRequest
import com.pdig.app.data.ManualNodeCreateRequest
import com.pdig.core.domain.confirmedIdentityAnchorProfile
import com.pdig.core.generated.CanonicalSpec
import com.pdig.core.generated.IdentityAnchorSubtype
import com.pdig.core.generated.NodeKind

/**
 * Authoritative production seam for R24 Manual Establish.
 *
 * Preview remains read-only. A future production VNext form may call this gateway,
 * but the gateway itself owns no optimistic UI state and never creates a relation
 * together with the object.
 */
internal data class ManualEstablishInput(
    val kind: String,
    val name: String,
    val issuer: String? = null,
    val last4: String? = null,
)

internal data class ManualIdentityEstablishInput(
    val subtype: IdentityAnchorSubtype,
    val name: String,
    val identifierValue: String,
)

internal data class ManualEstablishResultView(
    val objectId: String,
    val kind: String,
    val name: String,
    val graphRevision: Int,
    val issuer: String?,
    val last4: String?,
    val identitySubtype: String? = null,
    val identityIdentifierValue: String? = null,
)

internal fun supportedManualEstablishKinds(): Set<String> =
    CanonicalSpec.RUNTIME_CREATABLE_NODE_KINDS

internal class AppContainerVNextManualEstablishGateway(
    private val app: AppContainer,
) {
    fun create(input: ManualEstablishInput): ManualEstablishResultView {
        val kind = requireNotNull(NodeKind.fromWire(input.kind)) {
            "Unknown Canonical node kind: ${input.kind}"
        }
        require(kind.wire in CanonicalSpec.RUNTIME_CREATABLE_NODE_KINDS) {
            "Node kind '${kind.wire}' is not runtime-creatable"
        }

        val result = app.createManualNode(
            ManualNodeCreateRequest(
                kind = kind,
                name = input.name,
                issuer = input.issuer,
                last4 = input.last4,
            ),
        )
        return ManualEstablishResultView(
            objectId = result.node.id,
            kind = result.node.kind,
            name = result.node.name,
            graphRevision = result.graphRevision,
            issuer = result.node.issuer,
            last4 = result.node.last4,
        )
    }

    fun createIdentity(input: ManualIdentityEstablishInput): ManualEstablishResultView {
        require(
            input.subtype == IdentityAnchorSubtype.PHONE_NUMBER ||
                input.subtype == IdentityAnchorSubtype.EMAIL_ADDRESS
        ) {
            "Production manual identity establish supports phone/email only"
        }

        val result = app.createManualIdentityAnchor(
            ManualIdentityAnchorCreateRequest(
                subtype = input.subtype,
                name = input.name,
                identifierValue = input.identifierValue,
            ),
        )
        val profile = confirmedIdentityAnchorProfile(
            NodeKind.IDENTITY_ANCHOR,
            result.node.fieldsJson,
        )
        requireNotNull(profile) {
            "Governed identity profile missing after authoritative create"
        }
        require(profile.subtype == input.subtype) {
            "Identity subtype drifted during authoritative create"
        }
        val identifier = requireNotNull(profile.identifier) {
            "Confirmed identifier missing after authoritative create"
        }

        return ManualEstablishResultView(
            objectId = result.node.id,
            kind = result.node.kind,
            name = result.node.name,
            graphRevision = result.graphRevision,
            issuer = null,
            last4 = null,
            identitySubtype = profile.subtype.wire,
            identityIdentifierValue = identifier.value,
        )
    }
}
