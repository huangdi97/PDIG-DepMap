package com.pdig.uivnext.globe

/**
 * Bundled 主要城市坐标（DESIGN_TOKENS.json globe.bundledData.cityLights）。
 *
 * 用途：夜间城市灯光（nightCityLight 点 + nightCityGlow 光晕），按 terminator
 * 在暗面增强、亮面淡出。纯数据、离线、确定性；禁止远程数据源。
 */

/** 城市灯光点：name 仅用于调试/日志。 */
data class CityLight(val name: String, val lat: Double, val lon: Double)

val WORLD_CITY_LIGHTS: List<CityLight> = listOf(
    // 东亚（中国）
    CityLight("北京", 39.9, 116.4),
    CityLight("上海", 31.2, 121.5),
    CityLight("广州", 23.1, 113.3),
    CityLight("深圳", 22.5, 114.1),
    CityLight("香港", 22.3, 114.2),
    CityLight("成都", 30.6, 104.1),
    CityLight("武汉", 30.6, 114.3),
    CityLight("西安", 34.3, 108.9),
    // 东亚（日韩）
    CityLight("东京", 35.7, 139.7),
    CityLight("大阪", 34.7, 135.5),
    CityLight("首尔", 37.6, 127.0),
    CityLight("台北", 25.0, 121.5),
    // 南亚
    CityLight("孟买", 19.1, 72.9),
    CityLight("新德里", 28.6, 77.2),
    CityLight("加尔各答", 22.6, 88.4),
    CityLight("达卡", 23.8, 90.4),
    CityLight("卡拉奇", 24.9, 67.0),
    CityLight("班加罗尔", 13.0, 77.6),
    CityLight("科伦坡", 6.9, 79.9),
    // 东南亚
    CityLight("新加坡", 1.35, 103.8),
    CityLight("曼谷", 13.8, 100.5),
    CityLight("雅加达", -6.2, 106.8),
    CityLight("马尼拉", 14.6, 121.0),
    CityLight("吉隆坡", 3.1, 101.7),
    CityLight("胡志明市", 10.8, 106.7),
    // 中东
    CityLight("迪拜", 25.2, 55.3),
    CityLight("利雅得", 24.7, 46.7),
    CityLight("特拉维夫", 32.1, 34.8),
    CityLight("德黑兰", 35.7, 51.4),
    CityLight("巴格达", 33.3, 44.4),
    // 欧洲
    CityLight("伦敦", 51.5, -0.1),
    CityLight("巴黎", 48.9, 2.35),
    CityLight("柏林", 52.5, 13.4),
    CityLight("马德里", 40.4, -3.7),
    CityLight("罗马", 41.9, 12.5),
    CityLight("里斯本", 38.7, -9.1),
    CityLight("阿姆斯特丹", 52.4, 4.9),
    CityLight("布鲁塞尔", 50.8, 4.4),
    CityLight("维也纳", 48.2, 16.4),
    CityLight("华沙", 52.2, 21.0),
    CityLight("斯德哥尔摩", 59.3, 18.1),
    CityLight("奥斯陆", 59.9, 10.8),
    CityLight("赫尔辛基", 60.2, 24.9),
    CityLight("哥本哈根", 55.7, 12.6),
    CityLight("都柏林", 53.3, -6.3),
    CityLight("雅典", 38.0, 23.7),
    CityLight("莫斯科", 55.8, 37.6),
    CityLight("基辅", 50.5, 30.5),
    CityLight("伊斯坦布尔", 41.0, 29.0),
    // 非洲
    CityLight("开罗", 30.0, 31.2),
    CityLight("拉各斯", 6.5, 3.4),
    CityLight("内罗毕", -1.3, 36.8),
    CityLight("约翰内斯堡", -26.2, 28.0),
    CityLight("卡萨布兰卡", 33.6, -7.6),
    CityLight("阿尔及尔", 36.8, 3.1),
    CityLight("阿克拉", 5.6, -0.2),
    CityLight("亚的斯亚贝巴", 9.0, 38.7),
    // 北美洲
    CityLight("纽约", 40.7, -74.0),
    CityLight("洛杉矶", 34.1, -118.2),
    CityLight("芝加哥", 41.9, -87.6),
    CityLight("西雅图", 47.6, -122.3),
    CityLight("旧金山", 37.8, -122.4),
    CityLight("多伦多", 43.7, -79.4),
    CityLight("温哥华", 49.3, -123.1),
    CityLight("墨西哥城", 19.4, -99.1),
    CityLight("迈阿密", 25.8, -80.2),
    CityLight("休斯顿", 29.8, -95.4),
    CityLight("波士顿", 42.4, -71.1),
    CityLight("华盛顿", 38.9, -77.0),
    CityLight("丹佛", 39.7, -105.0),
    CityLight("蒙特利尔", 45.5, -73.6),
    // 南美洲
    CityLight("圣保罗", -23.6, -46.6),
    CityLight("里约热内卢", -22.9, -43.2),
    CityLight("布宜诺斯艾利斯", -34.6, -58.4),
    CityLight("利马", -12.0, -77.0),
    CityLight("波哥大", 4.7, -74.1),
    CityLight("圣地亚哥", -33.5, -70.7),
    // 大洋洲
    CityLight("悉尼", -33.9, 151.2),
    CityLight("墨尔本", -37.8, 144.9),
    CityLight("珀斯", -32.0, 115.9),
    CityLight("布里斯班", -27.5, 153.0),
    CityLight("奥克兰", -36.8, 174.8),
)
