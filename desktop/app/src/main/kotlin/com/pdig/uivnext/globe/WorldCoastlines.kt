package com.pdig.uivnext.globe

/**
 * Bundled 简化世界海岸线（DESIGN_TOKENS.json globe.bundledData.coastlines）。
 *
 * 规则：
 *  - 每块 8–20 个顶点（欧亚大陆块稍多，保证轮廓可识别）；
 *  - 顶点为 (纬度, 经度)，多边形由渲染端自动闭合；
 *  - 纯数据、离线、确定性；禁止远程 tiles / 地图 API。
 *
 * 用途：Canvas 内按当前相机正交投影填充大陆（front hemisphere 裁剪），
 * 配合 ocean/terminator/city-lights/atmosphere-rim 形成真实地球感（G1）。
 */

/** 简化海岸线多边形：name 仅用于调试/日志。 */
data class Coastline(val name: String, val points: List<Pair<Double, Double>>)

val WORLD_COASTLINES: List<Coastline> = listOf(
    Coastline(
        "north-america",
        listOf(
            71.0 to -157.0, 55.0 to -160.0, 40.0 to -124.0, 27.0 to -110.0,
            16.0 to -95.0, 21.0 to -87.0, 29.0 to -89.0, 25.0 to -82.0,
            36.0 to -76.0, 44.0 to -66.0, 55.0 to -58.0, 60.0 to -70.0,
            57.0 to -88.0, 52.0 to -98.0, 58.0 to -112.0, 61.0 to -126.0,
            59.0 to -142.0, 64.0 to -158.0, 71.0 to -157.0,
        ),
    ),
    Coastline(
        "greenland",
        listOf(
            60.0 to -44.0, 66.0 to -52.0, 70.0 to -56.0, 75.0 to -60.0,
            80.0 to -64.0, 82.0 to -55.0, 77.0 to -40.0, 70.0 to -32.0,
            64.0 to -38.0, 60.0 to -44.0,
        ),
    ),
    Coastline(
        "south-america",
        listOf(
            12.0 to -71.0, 6.0 to -77.0, -1.0 to -80.0, -8.0 to -79.0,
            -15.0 to -75.0, -25.0 to -70.0, -31.0 to -71.0, -34.0 to -58.0,
            -39.0 to -62.0, -45.0 to -66.0, -52.0 to -70.0, -55.0 to -66.0,
            -40.0 to -62.0, -23.0 to -41.0, -8.0 to -35.0, -3.0 to -40.0,
            2.0 to -51.0, 6.0 to -58.0, 11.0 to -62.0, 12.0 to -71.0,
        ),
    ),
    Coastline(
        "africa",
        listOf(
            37.0 to -10.0, 33.0 to 11.0, 28.0 to 34.0, 18.0 to 39.0,
            12.0 to 44.0, 8.0 to 48.0, 2.0 to 45.0, -5.0 to 39.0,
            -12.0 to 40.0, -20.0 to 35.0, -25.0 to 33.0, -34.0 to 20.0,
            -30.0 to 16.0, -22.0 to 14.0, -15.0 to 12.0, -8.0 to 13.0,
            -2.0 to 9.0, 5.0 to 5.0, 10.0 to -14.0, 14.0 to -17.0,
            21.0 to -17.0, 30.0 to -10.0, 37.0 to -10.0,
        ),
    ),
    Coastline(
        "eurasia",
        listOf(
            36.0 to -9.0, 43.0 to -2.0, 48.0 to -1.0, 51.0 to 2.0,
            55.0 to 8.0, 59.0 to 5.0, 63.0 to 7.0, 70.0 to 22.0,
            70.0 to 48.0, 72.0 to 78.0, 72.0 to 110.0, 69.0 to 140.0,
            65.0 to 165.0, 62.0 to 178.0, 56.0 to 162.0, 52.0 to 158.0,
            47.0 to 143.0, 43.0 to 131.0, 38.0 to 120.0, 31.0 to 122.0,
            26.0 to 120.0, 22.0 to 114.0, 16.0 to 108.0, 10.0 to 107.0,
            1.0 to 104.0, 8.0 to 98.0, 16.0 to 95.0, 22.0 to 90.0,
            16.0 to 82.0, 8.0 to 77.0, 13.0 to 74.0, 20.0 to 71.0,
            25.0 to 66.0, 25.0 to 61.0, 24.0 to 55.0, 13.0 to 45.0,
            28.0 to 34.0, 36.0 to 35.0, 38.0 to 23.0, 42.0 to 16.0,
            43.0 to 7.0, 41.0 to 2.0, 36.0 to -9.0,
        ),
    ),
    Coastline(
        "australia",
        listOf(
            -12.0 to 131.0, -15.0 to 136.0, -19.0 to 146.0, -26.0 to 153.0,
            -32.0 to 152.0, -35.0 to 150.0, -38.0 to 146.0, -35.0 to 137.0,
            -32.0 to 132.0, -25.0 to 114.0, -20.0 to 117.0, -16.0 to 123.0,
            -12.0 to 131.0,
        ),
    ),
    Coastline(
        "british-isles",
        listOf(
            58.0 to -4.0, 57.0 to -6.0, 53.0 to -5.0, 50.0 to -5.0,
            51.0 to 1.0, 54.0 to 0.0, 56.0 to -3.0, 58.0 to -4.0,
        ),
    ),
    Coastline(
        "japan",
        listOf(
            45.0 to 142.0, 41.0 to 140.0, 37.0 to 137.0, 35.0 to 137.0,
            33.0 to 131.0, 33.0 to 133.0, 36.0 to 138.0, 40.0 to 141.0,
            43.0 to 143.0, 45.0 to 142.0,
        ),
    ),
    Coastline(
        "madagascar",
        listOf(
            -12.0 to 49.0, -15.0 to 50.0, -20.0 to 48.0, -24.0 to 47.0,
            -25.0 to 44.0, -22.0 to 43.0, -18.0 to 44.0, -13.0 to 48.0,
            -12.0 to 49.0,
        ),
    ),
    Coastline(
        "new-zealand",
        listOf(
            -34.0 to 173.0, -37.0 to 176.0, -41.0 to 176.0, -44.0 to 171.0,
            -42.0 to 170.0, -38.0 to 172.0, -34.0 to 173.0,
        ),
    ),
)
