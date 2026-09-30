package com.pdig.uivnext.globe

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * PHASE 1C OFFLINE_TEXTURE_EARTH —— bundled 本地纹理资产（offline、deterministic）。
 *
 * 资产：spec/ui-vnext/assets/earth_albedo_2048.png /
 *      earth_night_lights_2048.png / cloud_2048.png
 * 全部由 tools/ui-vnext/assets/gen_earth_assets.py 从仓库自带数据集确定性生成
 * （license-safe 自产资产），SHA256 记录于 ASSET_MANIFEST.json。
 * 加载失败 → renderer 退回 VectorEarthFallbackRenderer。
 */

/** 纹理像素容器（ARGB IntArray，行主序）。 */
class EarthTexture(
    val width: Int,
    val height: Int,
    val pixels: IntArray,
) {
    /** 采样：u,v ∈ [0,1]（u 左→右，v 上→下）。 */
    fun sample(u: Float, v: Float): Int {
        var uu = u - kotlin.math.floor(u)
        var vv = v.coerceIn(0f, 1f)
        if (vv < 0f) vv = 0f
        val x = (uu * (width - 1)).toInt().coerceIn(0, width - 1)
        val y = (vv * (height - 1)).toInt().coerceIn(0, height - 1)
        return pixels[y * width + x]
    }
}

/** 地球材质资产包（单例式加载；失败返回 null → fallback）。 */
class EarthMaterialAssets private constructor(
    val albedo: EarthTexture,
    val nightLights: EarthTexture,
    val clouds: EarthTexture,
) {
    companion object {
        private var cached: EarthMaterialAssets? = null

        /** 从 repo 根目录相对路径加载；不存在/损坏 → null。 */
        fun load(baseDir: File = File(System.getProperty("user.dir"))): EarthMaterialAssets? {
            cached?.let { return it }
            val albedo = readTexture(File(baseDir, "spec/ui-vnext/assets/earth_albedo_2048.png")) ?: return null
            val night = readTexture(File(baseDir, "spec/ui-vnext/assets/earth_night_lights_2048.png")) ?: return null
            val clouds = readTexture(File(baseDir, "spec/ui-vnext/assets/cloud_2048.png")) ?: return null
            val assets = EarthMaterialAssets(albedo, night, clouds)
            cached = assets
            return assets
        }

        private fun readTexture(file: File): EarthTexture? {
            if (!file.isFile) return null
            val img: BufferedImage = try {
                ImageIO.read(file) ?: return null
            } catch (t: Throwable) {
                return null
            }
            val w = img.width
            val h = img.height
            val pixels = IntArray(w * h)
            img.getRGB(0, 0, w, h, pixels, 0, w)
            return EarthTexture(w, h, pixels)
        }
    }
}
