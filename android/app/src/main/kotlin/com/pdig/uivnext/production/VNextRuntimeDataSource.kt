package com.pdig.uivnext.production

/**
 * Runtime source boundary for UI vNext.
 *
 * Reference Preview and production Reality are different authority modes.
 * Production mode is deliberately unable to fall back to synthetic fixture data.
 */
internal enum class VNextRuntimeDataMode {
    REFERENCE_PREVIEW,
    PRODUCTION_REALITY,
}

internal interface VNextRuntimeDataSource {
    val mode: VNextRuntimeDataMode

    /**
     * Current immutable production snapshot. Reference mode returns null by design.
     */
    fun productionSnapshot(): VNextProductionSnapshot?

    /**
     * Consumer-safe inventory projection. Reference mode returns null by design.
     */
    fun productionInventory(): ProductionConsumerInventory?

    /**
     * Production-only authoritative impact projection.
     */
    fun productionImpact(targetNodeId: String): VNextProductionImpact?

    /**
     * Production-only authoritative plan projection.
     */
    fun productionPlan(planId: String): VNextProductionPlan?

    /**
     * Production-only authoritative record trace.
     */
    fun productionRecords(): List<VNextProductionRecordItem>
}

internal object ReferenceVNextRuntimeDataSource : VNextRuntimeDataSource {
    override val mode: VNextRuntimeDataMode = VNextRuntimeDataMode.REFERENCE_PREVIEW

    override fun productionSnapshot(): VNextProductionSnapshot? = null
    override fun productionInventory(): ProductionConsumerInventory? = null
    override fun productionImpact(targetNodeId: String): VNextProductionImpact? = null
    override fun productionPlan(planId: String): VNextProductionPlan? = null
    override fun productionRecords(): List<VNextProductionRecordItem> = emptyList()
}

internal class ProductionVNextRuntimeDataSource(
    private val source: VNextReadModelSource,
    private val nowIso: String? = null,
) : VNextRuntimeDataSource {
    override val mode: VNextRuntimeDataMode = VNextRuntimeDataMode.PRODUCTION_REALITY

    private fun snapshot(): VNextProductionSnapshot = source.snapshot(nowIso)

    override fun productionSnapshot(): VNextProductionSnapshot = snapshot()

    override fun productionInventory(): ProductionConsumerInventory =
        buildProductionConsumerInventory(snapshot())

    override fun productionImpact(targetNodeId: String): VNextProductionImpact =
        source.impact(targetNodeId)

    override fun productionPlan(planId: String): VNextProductionPlan? =
        source.plan(planId)

    override fun productionRecords(): List<VNextProductionRecordItem> =
        source.records()
}
