// VNextCopy —— UI vNext 演示文案（「新增 copy key：uiVNext.*」，不修改 copy-zh.json）。
// 复用语义在注释标注对应 copy-zh / desktop key；仅作为演示固定文案存在。

import Foundation

/// vNext 演示文案（「新增 copy key：uiVNext.*」；复用语义用注释标注）。
enum VCopy {
    // nav（components/nav.md 建议文案）
    static let navNow = "现在"
    static let navInfrastructure = "基础设施"
    static let navChange = "变更"
    static let navRecords = "记录"
    static let navSources = "数据源"
    static let navSettings = "设置"
    static let navInfraOverview = "总览"
    static let navInfraCards = "卡片"
    static let navInfraNumbers = "号码"
    static let navInfraAccounts = "账户"
    static let navInfraEmails = "邮箱"
    static let navInfraDevices = "设备"
    static let navInfraServices = "服务"
    static let navInfraWeaknesses = "薄弱点"

    // now（uiuxV031 语义参考）
    static let nowTitle = "现在"
    static let needAttention = "需要你处理" // home.needsAction
    static let activeChanges = "进行中的变更"
    static let upcoming = "即将到来" // home.upcoming
    static let viewAll = "查看全部"
    static let healthyUnknown = "以下范围仍然未知" // uiuxV031.healthyUnknown
    static let noImmediate = "当前没有需要立即处理的事项" // uiuxV031.healthyTitle

    // overview / globe
    static let myInfrastructure = "我的基础设施" // uiuxV031.myInfrastructure
    static let globeHint = "点击地区聚焦 · 双指缩放 · 再次点击打开地区抽屉"
    static let regionListTitle = "地区（Region List）"
    static let quickCards = "查看卡片" // uiVNext.overview.quick.cards
    static let quickNumbers = "查看号码" // uiVNext.overview.quick.numbers
    static let quickWeaknesses = "查看薄弱点" // uiVNext.overview.quick.weaknesses
    static let quickChangePhone = "更换手机号"
    static let backToGlobal = "返回全球（Escape）"
    static let drawerViewAll = "查看全部"
    static let drawerViewCards = "查看卡片"
    static let drawerViewNumbers = "查看号码"
    static let drawerViewHintAll = "该地区所有基础设施"
    static let drawerViewHintCards = "该地区卡片列表"
    static let drawerViewHintNumbers = "该地区号码列表"

    // cards / asset-card
    static let cardsTitle = "卡片"
    static let filterAll = "全部"
    static let filterExpiring = "即将到期"
    static let viewGrid = "网格视图"
    static let viewList = "列表视图"
    static let toggleToList = "切换：紧凑列表"
    static let toggleToGrid = "切换：视觉网格"
    static let notSet = "未设置" // uiVNext.notSet
    static let unknown = "未知" // uiVNext.unknown
    static let credit = "信用" // uiVNext.card.type.credit
    static let debit = "储蓄" // uiVNext.card.type.debit
    static let physical = "实体" // uiVNext.card.physical
    static let virtual = "虚拟" // uiVNext.card.virtual

    // card detail
    static let cardDetailIdentity = "卡片信息" // uiVNext.card.detail.identity
    static let cardDetailUsage = "使用场景" // uiVNext.card.detail.usage
    static let cardDetailServices = "绑定服务"
    static let cardDetailRisk = "风险"
    static let cardDetailRecovery = "恢复与替代"
    static let cardDetailHistory = "变更历史"
    static let customizeCardFace = "定制卡面"
    static let currentTheme = "当前主题"
    static let presentationNote = "外观设置只改变显示方式，不会改变卡片事实、依赖关系或确认状态。"

    // numbers / phone-card
    static let numbersTitle = "号码"
    static let filterSim = "SIM 类型"
    static let filterRole = "主副号"
    static let simPhysical = "SIM" // uiVNext.phone.sim.physical
    static let simEsim = "eSIM" // uiVNext.phone.sim.esim
    static let rolePrimary = "主号" // uiVNext.phone.role.primary
    static let roleSecondary = "副号" // uiVNext.phone.role.secondary
    static let roleKeep = "保号" // uiVNext.phone.role.keep
    static let recoveryOnly = "唯一恢复路径"
    static let recoveryOnlyShort = "恢复唯一"
    static let selectNumberHint = "选择一个号码查看详情"
    static let numberDetailTitle = "号码详情"
    static let relatedServices = "关联服务"
    static let viewFullDetail = "查看完整详情"
    static let maskNote = "号码已隐藏" // uiVNext.phone.maskNote
    static let customizeNumberFace = "定制号码面"

    // number detail
    static let loginUsage = "登录用途"
    static let twoFA = "两步验证" // uiVNext.inspector.twoFA
    static let recoveryUsage = "恢复用途"
    static let backupPath = "备用路径"
    static let history = "历史"

    // change-phone（scenarioV030.* / changePlanV030.* / uiuxV031.blockedReason 语义）
    static let changePhoneTitle = "更换手机号" // uiVNext.change.title
    static let planProjectionNote = "计划投影：以下步骤为当前执行计划；完成状态只在实际验证后标记。"
    static let oldToNewModel = "旧号码 → 关键服务 → 新号码"
    static let oldNumber = "旧手机号" // uiVNext.change.oldNumber
    static let newNumber = "新手机号" // uiVNext.change.newNumber
    static let keyServices = "关联服务与账户" // uiVNext.change.services
    static let stageDetailTitle = "阶段明细"
    static let riskTitle = "风险提示"
    static let riskBody = "旧号码是 2 个账户的唯一恢复路径：迁移完成前不要停用；验证阶段未完成时「停用旧号码」必须保持禁用。"
    static let plan = "计划" // uiVNext.plan
    static let upcomingStage = "后续阶段" // uiVNext.change.upcoming
    static let impactAnalysis = "影响分析" // uiVNext.change.impactAnalysis
    static let establishNewNumber = "建立新号码" // scenarioV030.addNewPhone
    static let verifyNewNumber = "验证新号码" // scenarioV030.verifyNewPhone
    static let migrateAccounts = "迁移关键账户" // scenarioV030.migrateAccounts
    static let checkRecoveryPaths = "检查恢复路径" // scenarioV030.reviewRecoveryPaths
    static let retireOldNumber = "停用旧号码" // scenarioV030.retireOldPhone
    static let blockedReason = "新手机号验证通过后才能停用旧手机号" // uiuxV031.blockedReason
    static let verifying = "待验证" // planWorkflow.verifying
    static let completed = "已完成" // planWorkflow.completed
    static let notStarted = "未开始"
    static let waiting = "等待中" // changePlanV030.waitingVerification
    static let migrated = "已迁移" // changePlanV030.newPathVerified
    static let statusActive = "使用中"

    // customization（customization-studio 建议文案）
    static let customizationCardTitle = "卡面定制" // uiVNext.customization.card.title
    static let customizationNumberTitle = "号码面定制" // uiVNext.customization.number.title
    static let presetLibrary = "预设" // uiVNext.customization.library
    static let livePreview = "实时预览" // uiVNext.customization.preview
    static let propertyInspector = "属性与样式" // uiVNext.customization.inspector
    static let savedLocal = "已保存（本地偏好）"
    static let save = "保存" // common.save
    static let done = "完成" // common.done
    static let reset = "重置为默认" // uiVNext.customization.reset
    static let presetNote = "素材全部来自 bundled local / procedural；不加载远程图片。主题只影响外观，不影响实际角色。" // uiVNext.customization.presetNote
    static let fieldTheme = "主题" // uiVNext.customization.field.theme
    static let fieldMaterial = "材质" // uiVNext.customization.field.material
    static let fieldAccent = "主色" // uiVNext.customization.field.accent
    static let fieldBackground = "背景" // uiVNext.customization.field.background
    static let fieldLayout = "布局" // uiVNext.customization.field.layout
    static let fieldMask = "号码遮罩" // uiVNext.customization.field.mask
    static let maskOn = "已开启"
    static let maskOff = "已关闭"

    // personalization（personalization-center 建议文案）
    static let personalizationTitle = "个性化" // uiVNext.settings.personalization
    static let personalizationNote = "以下均为本地偏好（Presentation Layer）；不影响依赖/证据/确认，也不写入 .depmap 备份。"
    static let workspaceTheme = "工作区主题" // uiVNext.settings.workspaceTheme
    static let workspaceThemeValue = "亮色 · iOS 原生材质"
    static let globeTheme = "星球主题" // uiVNext.settings.globeTheme
    static let idleRotation = "极慢空闲旋转"
    static let arcAnimation = "弧线动画"
    static let navDensity = "导航密度" // uiVNext.settings.navDensity
    static let cardDefaults = "卡片默认" // uiVNext.settings.cardDefaults
    static let numberDefaults = "号码默认" // uiVNext.settings.numberDefaults
    static let privacyMask = "隐私遮罩" // uiVNext.settings.privacyMask
    static let homeModules = "首页模块" // uiVNext.settings.homeModules
    static let regionGrouping = "地区分组" // uiVNext.settings.regionGrouping
    static let motion = "动效" // uiVNext.settings.motion
    static let reducedEffects = "减弱动态效果" // uiVNext.settings.reducedEffects
    static let p0Protected = "关键操作不可隐藏" // uiVNext.settings.p0Protected
    static let p0Note = "P0 提示：首页「需要你处理」模块在存在必须处理事项时不可隐藏。"

    // region tooltip（globe.md 建议文案格式「{displayName}，{cardCount} 张卡，{phoneCount} 个号码」）
    static func regionTooltip(_ r: RegionPresentation) -> String {
        "\(r.displayName)，\(r.cardCount) 张卡，\(r.phoneCount) 个号码"
    }
}
