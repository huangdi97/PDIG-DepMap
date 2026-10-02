package com.pdig.uivnext.layout

/**
 * PHASE 1F —— 确定性布局契约（纯函数，供 UI 渲染、probe、测试共用，防漂移）。
 * 全部为表现层几何；不触碰 domain / canonical / schema。
 */

/** ContinuityScene 比例常量（§29/§31：Old/New 主、服务次级、路径最弱）。 */
data class ContinuitySceneMetrics(
    val oldXFraction: Float,
    val newXFraction: Float,
    val numWFraction: Float,
    val numHFraction: Float,
    val nodeWFraction: Float,
    val nodeHFraction: Float,
    val colFractions: List<Float>,
    val rowFractions: List<Float>,
    val heightPx: Int,
)

object Phase1FLayout {
    /** Cards 网格 @1920 列数（§11）。 */
    const val CARDS_COLUMNS_1920 = 3

    /** 卡面宽高比（固定资产身份）。 */
    const val CARD_ASPECT_RATIO = 1.586f

    /** Card Studio 预览卡宽（§15：660–740px；center 占 0.72）。 */
    fun studioPreviewWidthPx(centerPaneWidthPx: Float): Float =
        (centerPaneWidthPx * 0.72f).coerceIn(660f, 740f)

    /** ContinuityScene 几何（§29/§30）。 */
    fun continuityScene(): ContinuitySceneMetrics = ContinuitySceneMetrics(
        oldXFraction = 0.155f,
        newXFraction = 0.845f,
        numWFraction = 0.15f,
        numHFraction = 0.42f,
        nodeWFraction = 0.085f,
        nodeHFraction = 0.30f,
        colFractions = listOf(0.375f, 0.625f),
        rowFractions = listOf(0.20f, 0.72f),
        heightPx = 430,
    )

    /** OLD/NEW 号码面宽（§29：250–300px @1920 场景宽 ≈1684px）。 */
    fun sceneNumberWidthPx(sceneWidthPx: Float): Float = sceneWidthPx * 0.15f

    /** 服务节点宽（§29：130–170px @1920）。 */
    fun sceneNodeWidthPx(sceneWidthPx: Float): Float = sceneWidthPx * 0.085f

    /** 路径层级（§32：primary 2.5 / secondary 2.0 / ghost 1.5）。 */
    const val PATH_PRIMARY_PX = 2.5f
    const val PATH_SECONDARY_PX = 2.0f
    const val PATH_GHOST_PX = 1.5f
}
