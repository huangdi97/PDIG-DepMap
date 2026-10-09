package com.pdig.uivnext.demo

import com.pdig.uivnext.model.ActiveChange
import com.pdig.uivnext.model.AttentionItem
import com.pdig.uivnext.model.ChangeMigration
import com.pdig.uivnext.model.ChangeStage
import com.pdig.uivnext.model.RegionPresentation
import com.pdig.uivnext.model.UiVNextAccount
import com.pdig.uivnext.model.UiVNextCard
import com.pdig.uivnext.model.UiVNextCardLifecycle
import com.pdig.uivnext.model.UiVNextDevice
import com.pdig.uivnext.model.UiVNextEmail
import com.pdig.uivnext.model.UiVNextNumber
import com.pdig.uivnext.model.UiVNextNumberLifecycle
import com.pdig.uivnext.model.UiVNextRelation
import com.pdig.uivnext.model.UiVNextService
import com.pdig.uivnext.model.UpcomingItem

/**
 * PDIG UI vNext Demo Fixture — Android 移植（all synthetic）。
 * 单一真源：spec/ui-vnext/UIVNextDemoFixture.json（人工 port；禁止真实数据）。
 * 冻结：同一 fixture + 同一时间 + 同一 locale + 同一动画状态 + 同一 scale = 截图可复现。
 */
object UiVNextDemoFixture {
    val regions: List<RegionPresentation> = listOf(
        RegionPresentation("CN", "中国大陆", 35.86, 104.19),
        RegionPresentation("HK", "香港", 22.32, 114.17),
        RegionPresentation("MO", "澳门", 22.2, 113.55),
        RegionPresentation("GB", "英国", 54.0, -2.5),
        RegionPresentation("US", "美国", 39.0, -98.0),
        RegionPresentation("SG", "新加坡", 1.35, 103.82),
    )

    val cards: List<UiVNextCard> = listOf(
        UiVNextCard("card-cn-1", "招行储蓄卡", "招商银行", "4417", "****4417", "CN", "CNY", "debit", "physical", "银联", "2028-05", "active", false, listOf("日常消费", "工资入账", "自动扣款"), "deep-space"),
        UiVNextCard("card-cn-2", "工行信用卡", "中国工商银行", "9023", "****9023", "CN", "CNY", "credit", "physical", "银联", "2026-11", "expiring_soon", true, listOf("订阅扣款", "旅行"), "region"),
        UiVNextCard("card-cn-3", "中行数字卡", "中国银行", "6048", "****6048", "CN", "CNY", "debit", "virtual", "银联", "2030-01", "active", false, listOf("数字支付"), "minimal"),
        UiVNextCard("card-hk-1", "汇丰卓越理財", "HSBC 汇丰", "7351", "****7351", "HK", "HKD", "credit", "physical", "Visa", "2027-03", "active", false, listOf("国际消费", "自动扣款"), "city"),
        UiVNextCard("card-hk-2", "中银香港储蓄", "中银香港", "1188", "****1188", "HK", "HKD", "debit", "physical", "银通", "2030-09", "active", false, listOf("本地日常"), "glass"),
        UiVNextCard("card-gb-1", "Monzo 账户卡", "Monzo", "4602", "****4602", "GB", "GBP", "debit", "virtual", "Mastercard", "2029-01", "active", false, listOf("日常消费"), "metal"),
        UiVNextCard("card-gb-2", "Revolut 多币种", "Revolut", "0047", "****0047", "GB", "GBP", "credit", "virtual", "Mastercard", "2027-07", "active", false, listOf("多币种", "订阅"), "abstract"),
        UiVNextCard("card-us-1", "Chase Sapphire", "Chase", "8220", "****8220", "US", "USD", "credit", "physical", "Visa", "2028-12", "active", false, listOf("旅行", "订阅"), "city"),
        UiVNextCard("card-us-2", "虚拟卡 0109", "Capital One", "0109", "****0109", "US", "USD", "credit", "virtual", "Visa", "2027-06", "active", false, listOf("网络订阅"), "minimal"),
        UiVNextCard("card-sg-1", "DBS 银行卡", "DBS", "3391", "****3391", "SG", "SGD", "credit", "physical", "Visa", "2027-08", "active", false, listOf("本地消费"), "glass"),
    )

    val numbers: List<UiVNextNumber> = listOf(
        UiVNextNumber("num-cn-1", "主号 中国移动", "+86 138****8823", "CN", "+86", "中国移动", "SIM", "primary", listOf("银行验证", "注册", "2FA"), "active", true, true, preset = "country"),
        UiVNextNumber("num-cn-2", "工作副号", "+86 137****5510", "CN", "+86", "中国联通", "eSIM", "secondary", listOf("工作"), "active", false, false, preset = "work"),
        UiVNextNumber("num-cn-3", "保号副号", "+86 139****2204", "CN", "+86", "中国移动", "SIM", "secondary", listOf("保号"), "active", false, false, preset = "recovery"),
        UiVNextNumber("num-hk-1", "香港主号", "+852 9***4321", "HK", "+852", "3HK", "SIM", "primary", listOf("银行验证", "2FA"), "active", true, true, preset = "banking"),
        UiVNextNumber("num-gb-1", "英国主号", "+44 7911 182***", "GB", "+44", "Vodafone", "eSIM", "primary", listOf("注册", "旅行", "2FA"), "active", false, false, preset = "travel"),
        UiVNextNumber("num-us-1", "美国保号", "+1 415 887 ****", "US", "+1", "T-Mobile", "SIM", "secondary", listOf("恢复"), "active", true, false, preset = "minimal"),
        UiVNextNumber("num-sg-1", "新加坡主号", "+65 9***2214", "SG", "+65", "Singtel", "eSIM", "primary", listOf("银行验证", "工作"), "active", false, false, preset = "city"),
    )

    /**
     * Recorded lifecycle metadata used by the UI reference.
     * Values are synthetic and intentionally live outside Canonical truth.
     */
    val cardLifecycle: Map<String, UiVNextCardLifecycle> = mapOf(
        "card-cn-1" to UiVNextCardLifecycle(
            annualFee = "免年费",
            billingDay = "每月 8 日",
            paymentDueDay = "每月 26 日",
            installmentSummary = "无已记录分期",
            autoPaySummary = "已记录：自动还款",
        ),
        "card-cn-2" to UiVNextCardLifecycle(
            annualFee = "¥100 / 年",
            annualFeeDue = "2026-11",
            billingDay = "每月 12 日",
            paymentDueDay = "每月 30 日",
            installmentSummary = "2 笔 · ¥3,280 剩余",
            autoPaySummary = "已记录：储蓄卡自动还款",
        ),
        "card-hk-1" to UiVNextCardLifecycle(
            annualFee = "HK$1,800 / 年",
            annualFeeDue = "2027-02",
            billingDay = "每月 18 日",
            paymentDueDay = "账单后 23 天",
            installmentSummary = "1 笔 · HK$4,200 剩余",
            autoPaySummary = "未记录",
        ),
        "card-gb-2" to UiVNextCardLifecycle(
            annualFee = "未记录",
            billingDay = "每月 21 日",
            paymentDueDay = "每月 7 日",
            installmentSummary = "无已记录分期",
            autoPaySummary = "未记录",
        ),
        "card-us-1" to UiVNextCardLifecycle(
            annualFee = "US$95 / 年",
            annualFeeDue = "2027-01",
            billingDay = "每月 3 日",
            paymentDueDay = "每月 28 日",
            installmentSummary = "无已记录分期",
            autoPaySummary = "已记录：自动还款",
        ),
    )

    val numberLifecycle: Map<String, UiVNextNumberLifecycle> = mapOf(
        "num-cn-1" to UiVNextNumberLifecycle(
            billingMode = "后付费",
            planCost = "¥59 / 月",
            keepAliveDue = "无需单独保号",
            keepAliveCycle = "持续在用",
            lastKeepAlive = "今天",
            renewalMethod = "运营商套餐",
        ),
        "num-cn-3" to UiVNextNumberLifecycle(
            billingMode = "保号套餐",
            planCost = "¥8 / 月",
            keepAliveDue = "2026-11-18",
            keepAliveCycle = "每 90 天检查",
            lastKeepAlive = "2026-08-20",
            renewalMethod = "充值 / 短信（已记录）",
        ),
        "num-hk-1" to UiVNextNumberLifecycle(
            billingMode = "月费套餐",
            planCost = "HK$68 / 月",
            keepAliveDue = "无需单独保号",
            keepAliveCycle = "持续在用",
            lastKeepAlive = "今天",
            renewalMethod = "运营商套餐",
        ),
        "num-gb-1" to UiVNextNumberLifecycle(
            billingMode = "eSIM 月包",
            planCost = "£10 / 月",
            keepAliveDue = "2026-11-01",
            keepAliveCycle = "每月续费",
            lastKeepAlive = "2026-10-01",
            renewalMethod = "运营商 App（已记录）",
        ),
        "num-us-1" to UiVNextNumberLifecycle(
            billingMode = "预付费保号",
            planCost = "US$5 / 90 天",
            keepAliveDue = "2026-11-05",
            keepAliveCycle = "每 90 天",
            lastKeepAlive = "2026-08-07",
            renewalMethod = "充值（已记录）",
        ),
    )

    val accounts: List<UiVNextAccount> = listOf(
        UiVNextAccount("acc-cn-1", "微信账户", "腾讯", "wxid_••••8823", "CN", listOf("日常身份", "支付"), listOf("主号", "设备确认"), "主号 + 设备确认", "active", false),
        UiVNextAccount("acc-hk-1", "HSBC 网银", "HSBC", "h••••@mail.com", "HK", listOf("银行", "资产"), listOf("香港主号", "安全设备"), "香港主号", "active", true),
        UiVNextAccount("acc-gb-1", "Amazon UK", "Amazon", "h••••@mail.com", "GB", listOf("购物", "订阅"), listOf("英国主号", "邮箱"), "邮箱 + 英国主号", "active", false),
        UiVNextAccount("acc-us-1", "Apple Account", "Apple", "h••••@icloud.com", "US", listOf("设备", "订阅", "恢复"), listOf("美国保号", "受信任设备"), "美国保号", "active", true),
        UiVNextAccount("acc-sg-1", "DBS digibank", "DBS", "user••••91", "SG", listOf("银行"), listOf("新加坡主号"), "新加坡主号", "active", false),
    )

    val emails: List<UiVNextEmail> = listOf(
        UiVNextEmail("email-cn-1", "主邮箱", "h••••@outlook.com", "Outlook", "CN", listOf("登录", "恢复", "通知"), 5, true, "active"),
        UiVNextEmail("email-gb-1", "海外邮箱", "h••••@gmail.com", "Gmail", "GB", listOf("登录", "订阅"), 4, false, "active"),
        UiVNextEmail("email-us-1", "Apple 恢复邮箱", "h••••@icloud.com", "iCloud", "US", listOf("恢复", "设备"), 3, false, "active"),
    )

    val devices: List<UiVNextDevice> = listOf(
        UiVNextDevice("dev-cn-1", "Pixel 8", "Android", "手机", "CN", listOf("主设备", "验证器"), "受信任", "今天", false),
        UiVNextDevice("dev-hk-1", "MacBook Pro", "macOS", "电脑", "HK", listOf("工作", "受信任设备"), "受信任", "昨天", false),
        UiVNextDevice("dev-gb-1", "YubiKey 5C NFC", "Hardware Key", "安全密钥", "GB", listOf("2FA", "恢复"), "备用", "2026-09-29", false),
        UiVNextDevice("dev-us-1", "旧 iPhone", "iOS", "手机", "US", listOf("恢复设备"), "待检查", "2026-06-18", true),
    )

    val services: List<UiVNextService> = listOf(
        UiVNextService("svc-wxpay", "微信支付", "CN", "payment"),
        UiVNextService("svc-alipay", "支付宝", "CN", "payment"),
        UiVNextService("svc-tencent", "腾讯视频", "CN", "subscription"),
        UiVNextService("svc-hsbc", "HSBC 网银", "HK", "banking"),
        UiVNextService("svc-apple", "Apple 订阅", "US", "subscription"),
        UiVNextService("svc-netflix", "Netflix", "US", "subscription"),
        UiVNextService("svc-amazon", "亚马逊", "GB", "subscription"),
        UiVNextService("svc-microsoft", "Microsoft 365", "US", "subscription"),
        UiVNextService("svc-dbs", "DBS 网银", "SG", "banking"),
        UiVNextService("svc-notion", "Notion", "GB", "subscription"),
    )

    val relations: List<UiVNextRelation> = listOf(
        UiVNextRelation("card-cn-1", "svc-wxpay", "funding"),
        UiVNextRelation("card-cn-1", "svc-tencent", "funding"),
        UiVNextRelation("card-cn-2", "svc-tencent", "funding"),
        UiVNextRelation("card-hk-1", "svc-hsbc", "funding"),
        UiVNextRelation("card-hk-1", "svc-apple", "funding"),
        UiVNextRelation("card-us-1", "svc-netflix", "funding"),
        UiVNextRelation("card-us-1", "svc-microsoft", "funding"),
        UiVNextRelation("card-us-2", "svc-apple", "funding"),
        UiVNextRelation("card-gb-2", "svc-amazon", "funding"),
        UiVNextRelation("num-cn-1", "svc-wxpay", "authenticates"),
        UiVNextRelation("num-cn-1", "svc-alipay", "authenticates"),
        UiVNextRelation("num-hk-1", "svc-hsbc", "twoFA"),
        UiVNextRelation("num-gb-1", "svc-amazon", "authenticates"),
        UiVNextRelation("num-us-1", "svc-apple", "twoFA"),
        UiVNextRelation("num-sg-1", "svc-hsbc", "authenticates"),
        UiVNextRelation("num-sg-1", "svc-dbs", "twoFA"),
    )

    val attentionItems: List<AttentionItem> = listOf(
        AttentionItem("att-1", "critical", "工行信用卡 11 月到期，绑定 2 项自动扣款", "card-cn-2"),
        AttentionItem("att-2", "warning", "美国保号 11 月需完成保号，且承担账户恢复依赖", "num-us-1"),
        AttentionItem("att-3", "warning", "+86 138****8823 是 2 个账户的唯一恢复路径", "num-cn-1"),
    )

    val activeChanges: List<ActiveChange> = listOf(
        ActiveChange("chg-1", "更换手机号（进行中）", "verify_new_phone", "change-phone"),
    )

    val upcoming: List<UpcomingItem> = listOf(
        UpcomingItem("upc-1", "工行信用卡 2026-11-15 到期", 47),
        UpcomingItem("upc-2", "美国保号 2026-11-05 需要完成保号操作", 27),
    )

    val changeStages: List<ChangeStage> = listOf(
        ChangeStage(1, "impact-analysis", "completed"),
        ChangeStage(2, "establish-new-number", "completed"),
        ChangeStage(3, "verify-new-number", "verifying"),
        ChangeStage(4, "migrate-key-accounts", "not_started"),
        ChangeStage(5, "check-recovery-paths", "not_started"),
        ChangeStage(6, "retire-old-number", "blocked", blockReason = "新手机号验证通过后才能停用旧手机号"),
    )

    val changeMigrations: List<ChangeMigration> = listOf(
        ChangeMigration("微信支付", "waiting"),
        ChangeMigration("支付宝", "waiting"),
        ChangeMigration("招商银行网银", "not_started"),
        ChangeMigration("腾讯视频", "waiting"),
    )

    /** 地区聚合（由真实数据计算；无数据 = 0）。 */
    fun regionSummaries(): List<RegionPresentation> = regions.map { r ->
        val cardsHere = cards.count { it.region == r.regionCode }
        val phonesHere = numbers.count { it.region == r.regionCode }
        val servicesHere = services.count { it.region == r.regionCode }
        val attentionHere = attentionItems.count { item ->
            cards.any { it.id == item.target && it.region == r.regionCode } ||
                numbers.any { it.id == item.target && it.region == r.regionCode }
        }
        val accountsHere = accounts.count { it.region == r.regionCode }
        r.copy(
            cardCount = cardsHere,
            phoneCount = phonesHere,
            serviceCount = servicesHere,
            accountCount = accountsHere,
            attentionCount = attentionHere,
        )
    }

    fun cardById(id: String): UiVNextCard? = cards.firstOrNull { it.id == id }

    fun cardLifecycleFor(id: String): UiVNextCardLifecycle? = cardLifecycle[id]

    fun numberById(id: String): UiVNextNumber? = numbers.firstOrNull { it.id == id }

    fun numberLifecycleFor(id: String): UiVNextNumberLifecycle? = numberLifecycle[id]

    fun servicesForCard(cardId: String): List<UiVNextService> = relations
        .filter { it.from == cardId }
        .map { rel -> services.firstOrNull { it.id == rel.to } }
        .filterNotNull()

    fun servicesForNumber(numberId: String): List<UiVNextService> = relations
        .filter { it.from == numberId }
        .map { rel -> services.firstOrNull { it.id == rel.to } }
        .filterNotNull()

    /** 跨地区真实关系弧线（来源 fixture relations；禁止装饰性弧线）。 */
    fun crossRegionRelations(): List<Pair<String, String>> {
        fun regionOf(id: String): String = when {
            cards.any { it.id == id } -> cards.first { it.id == id }.region
            numbers.any { it.id == id } -> numbers.first { it.id == id }.region
            services.any { it.id == id } -> services.first { it.id == id }.region
            else -> ""
        }
        return relations
            .mapNotNull { rel ->
                val fromRegion = regionOf(rel.from)
                val toRegion = regionOf(rel.to)
                if (fromRegion.isNotEmpty() && toRegion != fromRegion) rel.from to rel.to else null
            }
    }
}
