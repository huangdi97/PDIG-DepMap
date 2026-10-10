package com.pdig.uivnext.ui.screens

import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.serviceKindLabelZh
import com.pdig.uivnext.ui.VAppState

internal data class CommandTarget(
    val screen: VScreen,
    val title: String,
    val hint: String,
    val aliases: List<String> = emptyList(),
)

internal val commandTargets = listOf(
    CommandTarget(VScreen.OVERVIEW, "基础设施总览", "全球基础设施与地区活动", listOf("总览", "基础设施", "地球", "地区")),
    CommandTarget(VScreen.CARDS, "卡片", "支付基础设施与绑定关系", listOf("银行卡", "支付")),
    CommandTarget(VScreen.NUMBERS, "号码", "通信身份与恢复依赖", listOf("手机号", "电话", "sim", "esim")),
    CommandTarget(VScreen.ACCOUNTS, "账户", "账户规模与登录恢复关系", listOf("账号")),
    CommandTarget(VScreen.EMAILS, "邮箱", "邮箱身份与恢复角色", listOf("邮件", "email")),
    CommandTarget(VScreen.DEVICES, "设备", "验证器、可信终端与恢复设备", listOf("手机", "电脑")),
    CommandTarget(VScreen.SERVICES, "服务", "订阅、支付与验证服务", listOf("订阅")),
    CommandTarget(VScreen.WEAKNESSES, "薄弱点", "恢复、到期与迁移风险", listOf("风险", "恢复")),
    CommandTarget(VScreen.CHANGE_PHONE, "更换手机号", "规划并迁移号码", listOf("变更", "迁移", "换号")),
    CommandTarget(VScreen.CARDS, "更换银行卡", "先选择具体卡片，再核对支付依赖与迁移计划", listOf("换卡", "银行卡", "支付迁移")),
    CommandTarget(VScreen.RECORDS, "记录", "已发生事件、验证状态与依据", listOf("历史", "时间线", "验证", "证据")),
    CommandTarget(VScreen.REVIEW, "待复核", "确认系统发现的关系、对象候选与现实漂移", listOf("复核", "确认", "建议", "proposal", "candidate", "drift")),
    CommandTarget(VScreen.IMPORT, "建立基础设施", "本机导入并确认要记录的对象", listOf("导入", "账单", "文件", "建立", "新增来源")),
    CommandTarget(VScreen.MANUAL_ADD, "手工记录", "手工建立明确存在的基础设施对象", listOf("手工", "添加", "新建对象", "录入")),
    CommandTarget(VScreen.MANUAL_RELATION, "手工记录关系", "明确确认两个已记录对象之间的关系语义", listOf("关系", "依赖", "手工关系", "dependency")),
    CommandTarget(VScreen.SOURCES, "数据源", "当前工作区的数据边界", listOf("来源", "数据")),
    CommandTarget(VScreen.PERSONALIZATION, "设置 · 个性化", "外观、隐私与动效偏好", listOf("设置", "隐私", "个性化")),
)

internal sealed class SearchResult {
    abstract val title: String
    abstract val subtitle: String
    abstract val kind: String

    data class NavigationHit(
        val screen: VScreen,
        override val title: String,
        override val subtitle: String,
    ) : SearchResult() {
        override val kind = "页面"
    }

    data class CardHit(val id: String, override val title: String, override val subtitle: String) : SearchResult() {
        override val kind = "卡片"
    }

    data class NumberHit(val id: String, override val title: String, override val subtitle: String) : SearchResult() {
        override val kind = "号码"
    }

    data class ServiceHit(val id: String, override val title: String, override val subtitle: String) : SearchResult() {
        override val kind = "服务"
    }

    data class RegionHit(val code: String, override val title: String, override val subtitle: String) : SearchResult() {
        override val kind = "地区"
    }

    data class AccountHit(val id: String, override val title: String, override val subtitle: String) : SearchResult() {
        override val kind = "账户"
    }

    data class EmailHit(val id: String, override val title: String, override val subtitle: String) : SearchResult() {
        override val kind = "邮箱"
    }

    data class DeviceHit(val id: String, override val title: String, override val subtitle: String) : SearchResult() {
        override val kind = "设备"
    }
}

internal fun searchResults(q: String, app: VAppState? = null): List<SearchResult> {
    val query = q.lowercase()
    fun hit(values: List<String>): Boolean = values.any { it.lowercase().contains(query) }

    val out = mutableListOf<SearchResult>()

    commandTargets.forEach { target ->
        if (hit(listOf(target.title, target.hint) + target.aliases)) {
            out.add(SearchResult.NavigationHit(target.screen, target.title, target.hint))
        }
    }

    UiVNextDemoFixture.cards.forEach { card ->
        val region = regionLabel(card.region)
        val lifecycle = UiVNextDemoFixture.cardLifecycleFor(card.id)
        val lifecycleTerms = listOfNotNull(
            lifecycle?.annualFee,
            lifecycle?.annualFeeDue,
            lifecycle?.billingDay,
            lifecycle?.paymentDueDay,
            lifecycle?.installmentSummary,
            lifecycle?.autoPaySummary,
        )
        if (hit(
                listOf(card.nickname, card.issuer, card.region, region, card.masked,
                    card.network, card.currency) + lifecycleTerms,
            )
        ) {
            out.add(SearchResult.CardHit(card.id, card.nickname,
                "${card.issuer} · $region · ${card.masked}"))
        }
    }

    UiVNextDemoFixture.numbers.forEach { number ->
        val region = regionLabel(number.region)
        val lifecycle = UiVNextDemoFixture.numberLifecycleFor(number.id)
        val displayName = app?.numberDisplayNameForScreen(number.id, number.maskedNumber)
            ?: number.nickname
        val roleLabel = when (number.role) {
            "primary" -> "主号"
            "keep" -> "保号"
            "secondary" -> "副号"
            else -> number.role
        }
        val lifecycleTerms = listOfNotNull(
            lifecycle?.billingMode,
            lifecycle?.planCost,
            lifecycle?.keepAliveDue,
            lifecycle?.keepAliveCycle,
            lifecycle?.lastKeepAlive,
            lifecycle?.renewalMethod,
        )
        if (hit(
                listOf(displayName, number.nickname, number.carrier, number.region, region,
                    number.countryCode, number.maskedNumber, roleLabel) + lifecycleTerms,
            )
        ) {
            out.add(
                SearchResult.NumberHit(
                    number.id,
                    displayName,
                    "${number.countryCode} · ${number.carrier} · ${number.maskedNumber}",
                ),
            )
        }
    }

    UiVNextDemoFixture.services.forEach { service ->
        val region = regionLabel(service.region)
        val role = serviceKindLabelZh(service.kind)
        if (hit(listOf(service.name, service.region, region, service.kind, role))) {
            out.add(SearchResult.ServiceHit(service.id, service.name, "$region · $role"))
        }
    }

    UiVNextDemoFixture.accounts.forEach { account ->
        if (hit(listOf(account.name, account.provider, account.maskedIdentifier, regionLabel(account.region)) + account.roles + account.authMethods)) {
            out.add(
                SearchResult.AccountHit(
                    account.id,
                    account.name,
                    account.provider + " · " + regionLabel(account.region) + " · " +
                        if (app?.privacyMask == true) "标识已遮蔽" else account.maskedIdentifier,
                ),
            )
        }
    }

    UiVNextDemoFixture.emails.forEach { email ->
        if (hit(listOf(email.name, email.provider, email.maskedAddress, regionLabel(email.region)) + email.roles)) {
            out.add(
                SearchResult.EmailHit(
                    email.id,
                    email.name,
                    (if (app?.privacyMask == true) "邮箱已遮蔽" else email.maskedAddress) +
                        " · " + email.provider,
                ),
            )
        }
    }

    UiVNextDemoFixture.devices.forEach { device ->
        if (hit(listOf(device.name, device.platform, device.kind, regionLabel(device.region)) + device.roles)) {
            out.add(
                SearchResult.DeviceHit(
                    device.id,
                    device.name,
                    device.platform + " · " + device.trust + " · " + device.lastSeen,
                ),
            )
        }
    }

    UiVNextDemoFixture.regionSummaries().forEach { region ->
        if (hit(listOf(region.displayName, region.regionCode))) {
            out.add(
                SearchResult.RegionHit(
                    region.regionCode,
                    region.displayName,
                    "${region.cardCount} 张卡 · ${region.phoneCount} 个号码",
                ),
            )
        }
    }

    return out.distinctBy { "${it.kind}:${it.title}" }
}

internal fun openSearchResult(result: SearchResult, app: VAppState) {
    when (result) {
        is SearchResult.CardHit -> app.openCard(result.id)
        is SearchResult.NumberHit -> app.openNumber(result.id)
        is SearchResult.ServiceHit -> app.openSecondaryObject(VScreen.SERVICE_DETAIL, result.id)
        is SearchResult.AccountHit -> app.openSecondaryObject(VScreen.ACCOUNT_DETAIL, result.id)
        is SearchResult.EmailHit -> app.openSecondaryObject(VScreen.EMAIL_DETAIL, result.id)
        is SearchResult.DeviceHit -> app.openSecondaryObject(VScreen.DEVICE_DETAIL, result.id)
        is SearchResult.NavigationHit -> app.navigateFromSearch(result.screen)
        is SearchResult.RegionHit -> {
            app.selectRegion(result.code)
            app.navigateFromSearch(VScreen.OVERVIEW)
        }
    }
}
