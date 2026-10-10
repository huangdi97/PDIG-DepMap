package com.pdig.uivnext.globe

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import com.pdig.uivnext.model.RegionPresentation

/**
 * EarthRenderer —— 地球渲染编排（PHASE 1C 架构拆分）。
 *
 *  - TextureEarthRenderer：默认（OFFLINE_TEXTURE_EARTH；bundled 纹理）
 *  - VectorEarthFallbackRenderer：fallback（LOW_POWER / VECTOR_EARTH_FALLBACK）
 *  - EarthMaterialAssets / EarthLighting / EarthAtmosphere / EarthRegionOverlay 各自独立文件
 *
 * 全部离屏、确定性、离线。
 */

/** 地球渲染输入（与相机/交互状态解耦，便于离屏截图确定性）。 */
data class EarthRenderInput(
    val camera: GlobeCamera,
    val regions: List<RegionPresentation>,
    val arcingPairs: List<Pair<String, String>>,
    val selectedRegion: String?,
    val hoveredRegion: RegionPresentation?,
    val showRegionLabels: Boolean,
    val lowPower: Boolean,
    val textMeasurer: TextMeasurer,
)

/** 地球渲染器接口（显式传入 DrawScope，避免成员扩展接收者歧义）。 */
interface EarthRenderer {
    fun render(scope: DrawScope, input: EarthRenderInput)
}

/** 工厂：纹理资产可用 → TextureEarthRenderer；否则 → VectorEarthFallbackRenderer。 */
fun createEarthRenderer(assets: EarthMaterialAssets?): EarthRenderer {
    return if (assets != null) TextureRendererAdapter(assets) else VectorRendererAdapter
}

private class TextureRendererAdapter(private val assets: EarthMaterialAssets) : EarthRenderer {
    override fun render(scope: DrawScope, input: EarthRenderInput) = with(scope) {
        val (center, radius) = globeMetrics(size, input.camera.zoom)
        drawStars()
        drawAtmosphereGlow(center, radius)
        drawTextureEarthBody(assets, center, radius, input.camera)
        if (!input.lowPower) {
            drawOceanSpecular(center, radius, input.camera)
        }
        drawGraticule(center, radius, input.camera)
        drawArcs(center, radius, input.camera, input.regions, input.arcingPairs, input.selectedRegion)
        drawAtmosphereRim(center, radius)
        drawAnchors(center, radius, input.camera, input.regions, input.selectedRegion, input.hoveredRegion, input.showRegionLabels, input.textMeasurer)
    }
}

private object VectorRendererAdapter : EarthRenderer {
    override fun render(scope: DrawScope, input: EarthRenderInput) = with(scope) {
        val (center, radius) = globeMetrics(size, input.camera.zoom)
        drawStars()
        drawAtmosphereGlow(center, radius)
        drawVectorEarthBody(center, radius, input.camera, input.lowPower)
        drawGraticule(center, radius, input.camera)
        drawArcs(center, radius, input.camera, input.regions, input.arcingPairs, input.selectedRegion)
        drawAtmosphereRim(center, radius)
        drawAnchors(center, radius, input.camera, input.regions, input.selectedRegion, input.hoveredRegion, input.showRegionLabels, input.textMeasurer)
    }
}
