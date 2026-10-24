package com.pdig.uivnext.copy

/**
 * PHASE 1F —— 空态 / healthy 文案契约（§37–§40）。
 * UI 引用本对象；测试断言其内容（禁词 + 诚实健康文案），防漂移。
 */

object Phase1FEmptyCopy {
    // Cards Empty（§38）
    const val CARDS_TITLE = "还没有记录卡片"
    const val CARDS_BODY = "添加第一张卡后，PDIG 可以帮助你查看它绑定了哪些服务、什么时候到期，以及换卡前需要处理什么。"
    const val CARDS_CTA = "添加卡片"
    const val CARDS_SECONDARY = "从数据源发现"

    // Numbers Empty
    const val NUMBERS_TITLE = "还没有记录手机号"
    const val NUMBERS_BODY = "加入常用号码后，可以查看：哪些账户依赖它用于登录、验证或恢复，以及它是否为唯一恢复路径。"
    const val NUMBERS_CTA = "添加号码"

    // Region Empty（§39；未知 ≠ 没有）
    const val REGION_TITLE = "该地区暂无已确认的卡片或号码"
    const val REGION_BODY = "可能只是尚未记录或确认；未知 ≠ 没有。发现后会自动出现在这里。"

    // No Active Change（§40）
    const val NO_CHANGE_TITLE = "没有正在进行中的变更计划"
    const val NO_CHANGE_BODY = "如需更换手机号或银行卡，可从「变更」开始创建模拟影响分析。"

    // No Attention —— healthy 文案必须诚实（§40，禁词见 BANNED_PHRASES）
    const val NO_ATTENTION_TITLE = "目前没有需要立即处理的已确认事项"
    const val NO_ATTENTION_BODY = "仍可能存在尚未记录或确认的关系；发现后会在「需要处理」中如实列出。"

    // No Known Dependencies（Card Detail §13）
    const val NO_DEPS_TITLE = "尚未确认绑定服务"
    const val NO_DEPS_BODY = "未知 ≠ 没有：数据源中未发现该卡的绑定关系，不代表不存在。"
    const val NO_BACKUP_TITLE = "尚未确认备用支付方式"
    const val NO_HISTORY_TITLE = "尚未记录更多变更"

    /** 禁止出现在 healthy 文案中的伪安全表述（§40）。 */
    val BANNED_PHRASES = listOf("一切安全", "100% 正常", "无风险", "绝对安全")

    /** healthy 文案必须包含的诚实表述。 */
    const val HEALTHY_REQUIRED_FRAGMENT = "仍可能存在尚未记录或确认的关系"
}
