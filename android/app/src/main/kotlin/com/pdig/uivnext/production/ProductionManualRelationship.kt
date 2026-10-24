package com.pdig.uivnext.production

import com.pdig.app.data.AppContainer
import com.pdig.app.data.ManualDependencyCreateRequest
import com.pdig.core.domain.RELATION_DEFINITIONS
import com.pdig.core.domain.getRelationDefinition

internal data class ManualRelationshipDefinitionView(
    val relation: String,
    val capability: String,
    val allowedFromKinds: Set<String>,
    val allowedToKinds: Set<String>,
)

internal data class ManualRelationshipInput(
    val fromNodeId: String,
    val relation: String,
    val toNodeId: String,
    val required: Boolean = false,
)

internal data class ManualRelationshipResultView(
    val dependencyId: String,
    val fromNodeId: String,
    val relation: String,
    val toNodeId: String,
    val capability: String,
    val criticality: String,
    val graphRevision: Int,
    val created: Boolean,
    val reactivated: Boolean,
)

internal fun supportedManualRelationshipDefinitions(): List<ManualRelationshipDefinitionView> =
    RELATION_DEFINITIONS.map { definition ->
        ManualRelationshipDefinitionView(
            relation = definition.id.wire,
            capability = definition.capability.wire,
            allowedFromKinds = definition.fromKinds.mapTo(linkedSetOf()) { it.wire },
            allowedToKinds = definition.toKinds.mapTo(linkedSetOf()) { it.wire },
        )
    }

internal class AppContainerVNextManualRelationshipGateway(
    private val app: AppContainer,
) {
    fun create(input: ManualRelationshipInput): ManualRelationshipResultView {
        val definition = requireNotNull(getRelationDefinition(input.relation)) {
            "Relation '${input.relation}' is not in the current runtime registry"
        }

        val result = app.createManualDependency(
            ManualDependencyCreateRequest(
                fromNodeId = input.fromNodeId,
                relation = definition.id,
                toNodeId = input.toNodeId,
                capability = definition.capability,
                required = input.required,
            ),
        )

        return ManualRelationshipResultView(
            dependencyId = result.dependency.id,
            fromNodeId = result.dependency.from,
            relation = result.dependency.relation,
            toNodeId = result.dependency.to,
            capability = result.dependency.capability,
            criticality = result.dependency.criticality,
            graphRevision = result.graphRevision,
            created = result.created,
            reactivated = result.reactivated,
        )
    }
}
