package com.pdig.uivnext.ui

/**
 * Presentation-only geographic anchors for the GPU Globe.
 *
 * IMPORTANT:
 * - this catalog is NOT RegionFact authority;
 * - it never creates geography;
 * - it is consulted only after a governed confirmed territory code already exists;
 * - unsupported territory codes remain valid Production Region facts and are shown
 *   in textual Region surfaces, but are not plotted at a fabricated coordinate.
 *
 * The current anchors preserve the human-selected world-reference geometry already
 * used by Android Preview. Expanding visual coverage is a presentation-data change,
 * not a Canonical migration.
 */
internal data class TerritoryPresentationAnchor(
    val territoryCode: String,
    val displayNameZh: String,
    val latitude: Double,
    val longitude: Double,
)

internal object TerritoryPresentationCatalog {
    private val anchors = listOf(
        TerritoryPresentationAnchor("CN", "中国大陆", 35.86, 104.19),
        TerritoryPresentationAnchor("HK", "香港", 22.32, 114.17),
        TerritoryPresentationAnchor("MO", "澳门", 22.20, 113.55),
        TerritoryPresentationAnchor("GB", "英国", 54.00, -2.50),
        TerritoryPresentationAnchor("US", "美国", 39.00, -98.00),
        TerritoryPresentationAnchor("SG", "新加坡", 1.35, 103.82),
    ).associateBy { it.territoryCode }

    fun anchor(territoryCode: String): TerritoryPresentationAnchor? =
        anchors[territoryCode]

    fun displayName(territoryCode: String): String =
        anchors[territoryCode]?.displayNameZh ?: territoryCode

    fun coveredTerritoryCodes(): Set<String> = anchors.keys
}
