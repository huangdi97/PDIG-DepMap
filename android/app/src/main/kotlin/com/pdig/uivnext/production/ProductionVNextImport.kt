package com.pdig.uivnext.production

import com.pdig.app.data.AppContainer
import com.pdig.app.data.ImportCommitResult
import com.pdig.app.data.ImportPreview
import com.pdig.core.sources.Observation

/**
 * Consumer-safe VNext projection over the existing production import pipeline.
 *
 * File picking, lock/re-auth survival and parsing stay owned by the existing
 * FileWorkflowCoordinator + AppContainer. This seam begins only after observations
 * exist in memory; it does not create a second picker or parser implementation.
 */
internal data class VNextImportPreviewView(
    val sourceLabel: String,
    val adapterId: String,
    val observationCount: Int,
    val skippedRowCount: Int,
    val detectedPaymentInstruments: List<String>,
    val detectedCounterparties: List<String>,
) {
    val detectedObjectCount: Int
        get() = detectedPaymentInstruments.size + detectedCounterparties.size
}

internal data class VNextImportCommitView(
    val sourceInstanceId: String,
    val rawCount: Int,
    val newUniqueCount: Int,
    val duplicateCount: Int,
    val nodeCount: Int,
    val proposalCount: Int,
    val errorCount: Int,
) {
    /** A successful import can create review work, never a confirmed dependency directly. */
    val nextStep: String
        get() = if (proposalCount > 0) "review" else "sources"
}

internal fun mapImportPreview(preview: ImportPreview): VNextImportPreviewView =
    VNextImportPreviewView(
        sourceLabel = preview.sourceLabel,
        adapterId = preview.adapterId,
        observationCount = preview.observations.size,
        skippedRowCount = preview.errors.size,
        detectedPaymentInstruments = preview.instruments.map { it.label },
        detectedCounterparties = preview.counterparties.map { it.label },
    )

internal fun mapImportCommit(result: ImportCommitResult): VNextImportCommitView =
    VNextImportCommitView(
        sourceInstanceId = result.sourceInstanceId,
        rawCount = result.rawCount,
        newUniqueCount = result.newUniqueCount,
        duplicateCount = result.duplicateCount,
        nodeCount = result.nodeCount,
        proposalCount = result.proposalCount,
        errorCount = result.errorCount,
    )

/**
 * Domain-authoritative portion of the VNext import flow.
 *
 * - previewImport writes nothing.
 * - commitImport is the existing transaction that can create Source/Evidence/Node
 *   plus Proposal/Candidate/Drift work.
 * - it never creates a confirmed Dependency directly.
 *
 * The caller must reuse the hardened FileWorkflowCoordinator for file selection,
 * background lock/re-auth and in-memory preview lifetime.
 */
internal class AppContainerVNextImportAuthority(
    private val app: AppContainer,
) {
    suspend fun parseFile(
        bytes: ByteArray,
        adapterId: String,
        mapping: com.pdig.core.sources.MappingProfile?,
    ): com.pdig.app.data.ParseOutcome =
        app.parseFile(bytes, adapterId, mapping)

    fun preview(
        observations: List<Observation>,
        errors: List<String>,
        adapterId: String,
        sourceLabel: String,
    ): Pair<ImportPreview, VNextImportPreviewView> {
        val domainPreview = app.previewImport(
            observations = observations,
            errors = errors,
            adapterId = adapterId,
            sourceLabel = sourceLabel,
        )
        return domainPreview to mapImportPreview(domainPreview)
    }

    fun commitAuthoritative(
        preview: ImportPreview,
    ): Pair<ImportCommitResult, VNextImportCommitView> {
        val result = app.commitImport(preview)
        return result to mapImportCommit(result)
    }

    fun commit(preview: ImportPreview): VNextImportCommitView =
        commitAuthoritative(preview).second
}
