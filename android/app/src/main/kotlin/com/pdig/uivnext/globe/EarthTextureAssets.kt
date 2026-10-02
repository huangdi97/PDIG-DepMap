package com.pdig.uivnext.globe

import android.content.Context
import android.graphics.BitmapFactory
import kotlin.math.floor

/**
 * EarthTextureAssets —— Android 版 bundled 真实地球纹理（offline、deterministic）。
 *
 * 与桌面 EarthMaterialAssets 同源：`spec/ui-vnext/assets/` 的 NASA Public Domain 纹理
 * （earth_albedo_2048 / earth_night_lights_2048 / cloud_2048，sha256 见 ASSET_MANIFEST.json）。
 * 已随 APK 打包进 `assets/earth/`；加载失败 → 返回 null，Globe 退回 VectorEarthFallback（LOW）。
 *
 * 质量档（任务书 §11）：
 *  - HIGH      albedo + night + cloud，渲染边长 ≤ 768px
 *  - BALANCED  albedo + night，渲染边长 ≤ 512px
 *  - LOW       VectorEarthFallback（程序化球体，仅资产缺失/低功耗）
 */
enum class EarthQuality { HIGH, BALANCED, LOW }

/** 纹理像素容器（ARGB IntArray，行主序；采样逻辑与桌面 EarthTexture 一致）。 */
class EarthTexture(
    val width: Int,
    val height: Int,
    val pixels: IntArray,
) {
    /** 采样：u,v ∈ [0,1]（u 左→右，v 上→下）。 */
    fun sample(u: Float, v: Float): Int {
        var uu = u - floor(u)
        var vv = v.coerceIn(0f, 1f)
        if (vv < 0f) vv = 0f
        val x = (uu * (width - 1)).toInt().coerceIn(0, width - 1)
        val y = (vv * (height - 1)).toInt().coerceIn(0, height - 1)
        return pixels[y * width + x]
    }
}

/** 地球材质资产包（albedo 必需；night/cloud 可选，按质量档加载）。 */
class EarthMaterialAssets(
    val albedo: EarthTexture,
    val nightLights: EarthTexture?,
    val clouds: EarthTexture?,
) {
    companion object {
        private var cached: EarthMaterialAssets? = null

        /** 从 APK assets 加载；不存在/损坏 → null（renderer 退回 vector）。 */
        fun load(context: Context, quality: EarthQuality): EarthMaterialAssets? {
            cached?.let { return it }
            val albedo = read(context, "earth/earth_albedo_2048.png") ?: return null
            val night = if (quality == EarthQuality.LOW) null else read(context, "earth/earth_night_lights_2048.png")
            val clouds = if (quality == EarthQuality.HIGH) read(context, "earth/cloud_2048.png") else null
            val assets = EarthMaterialAssets(albedo, night, clouds)
            cached = assets
            return assets
        }

        private fun read(context: Context, name: String): EarthTexture? {
            return try {
                context.assets.open(name).use { input ->
                    val bmp = BitmapFactory.decodeStream(input) ?: return null
                    val w = bmp.width
                    val h = bmp.height
                    val pixels = IntArray(w * h)
                    bmp.getPixels(pixels, 0, w, 0, 0, w, h)
                    bmp.recycle()
                    EarthTexture(w, h, pixels)
                }
            } catch (t: Throwable) {
                null
            }
        }
    }
}

/** 纹理渲染边长上限（性能预算；HIGH ≤768 / BALANCED ≤512）。 */
internal fun earthRenderRect(radiusPx: Float, quality: EarthQuality): Int {
    val cap = if (quality == EarthQuality.HIGH) 768 else 512
    return (radiusPx * 2f).toInt().coerceIn(8, cap)
}
