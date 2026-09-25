package com.pdig.desktop.ui

/**
 * Desktop 导航模型：路由即顶层枚举 + 可选参数。
 * 与 Android 的 Route 常量一一对应（对应关系见下），Graph 不是首页（产品铁律）。
 */
enum class Screen(val route: String, val title: String) {
    HOME("home", "首页"),
    ATTENTION("attention", "需要处理"),
    SOURCES("sources", "数据来源"),
    IMPORT("import", "导入"),
    MAPPING("mapping", "CSV 映射"),
    REVIEW("review", "待确认"),
    PROPOSALS("proposals", "待确认关系"),
    CANDIDATES("candidates", "待确认服务"),
    DRIFTS("drifts", "可能发生了变化"),
    INFRA("infrastructure", "基础设施"),
    NODE("node", "对象详情"),
    SCENARIOS("scenarios", "场景中心"),
    SCENARIO_SETUP("scenario-setup", "场景设置"),
    IMPACT("impact", "影响分析"),
    PLAN("plan", "变更计划"),
    ACTIONS("actions", "行动计划"),
    VERIFICATION("verification", "验证"),
    TIMELINE("timeline", "时间线"),
    BACKUP("backup", "备份"),
    RESTORE("restore", "恢复"),
    SETTINGS("settings", "设置"),
    SECURITY("security", "安全"),
    ABOUT("about", "关于"),
}

/** 左侧导航里固定的顶层入口（不依赖参数的屏面；其余屏面从内容区进入）。 */
val TOP_LEVEL_SCREENS: List<Screen> = listOf(
    Screen.HOME, Screen.ATTENTION, Screen.SOURCES, Screen.INFRA,
    Screen.SCENARIOS, Screen.TIMELINE, Screen.SETTINGS,
)