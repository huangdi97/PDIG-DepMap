// VNextDemoFixture —— PDIG UI vNext 演示 fixture（iOS 移植，全 synthetic）。
//
// 单一真源：spec/ui-vnext/UIVNextDemoFixture.json（人工 port；禁止真实数据）。
// 与 desktop/app/.../demo/UiVNextDemoFixture.kt 语义一致：
//  - region 是表现层派生属性；无数据 = 不显示/0，禁止伪造节点；
//  - 弧线只来自真实跨区关系或地区资产摘要，禁止装饰性连线；
//  - 冻结：同一 fixture + 同一时间 + 同一 locale + 同一动画状态 + 同一 scale = 截图可复现。

import Foundation

/// vNext 演示数据（全 synthetic；地区聚合由 fixture 数据计算）。
enum VNextDemoFixture {

    static let regions: [RegionPresentation] = [
        RegionPresentation(regionCode: "CN", displayName: "中国大陆", latitude: 35.86, longitude: 104.19),
        RegionPresentation(regionCode: "HK", displayName: "香港", latitude: 22.32, longitude: 114.17),
        RegionPresentation(regionCode: "MO", displayName: "澳门", latitude: 22.2, longitude: 113.55),
        RegionPresentation(regionCode: "GB", displayName: "英国", latitude: 54.0, longitude: -2.5),
        RegionPresentation(regionCode: "US", displayName: "美国", latitude: 39.0, longitude: -98.0),
        RegionPresentation(regionCode: "SG", displayName: "新加坡", latitude: 1.35, longitude: 103.82),
    ]

    static let cards: [VCard] = [
        VCard(id: "card-cn-1", nickname: "招行储蓄卡", issuer: "招商银行", last4: "4417", masked: "****4417", region: "CN", currency: "CNY", type: "debit", form: "physical", network: "银联", expiry: "2028-05", status: "active", attention: false, usages: ["日常消费", "工资入账", "自动扣款"], preset: "deep-space"),
        VCard(id: "card-cn-2", nickname: "工行信用卡", issuer: "中国工商银行", last4: "9023", masked: "****9023", region: "CN", currency: "CNY", type: "credit", form: "physical", network: "银联", expiry: "2026-11", status: "expiring_soon", attention: true, usages: ["订阅扣款", "旅行"], preset: "region"),
        VCard(id: "card-cn-3", nickname: "中行数字卡", issuer: "中国银行", last4: "6048", masked: "****6048", region: "CN", currency: "CNY", type: "debit", form: "virtual", network: "银联", expiry: "2030-01", status: "active", attention: false, usages: ["数字支付"], preset: "minimal"),
        VCard(id: "card-hk-1", nickname: "汇丰卓越理財", issuer: "HSBC 汇丰", last4: "7351", masked: "****7351", region: "HK", currency: "HKD", type: "credit", form: "physical", network: "Visa", expiry: "2027-03", status: "active", attention: false, usages: ["国际消费", "自动扣款"], preset: "city"),
        VCard(id: "card-hk-2", nickname: "中银香港储蓄", issuer: "中银香港", last4: "1188", masked: "****1188", region: "HK", currency: "HKD", type: "debit", form: "physical", network: "银通", expiry: "2030-09", status: "active", attention: false, usages: ["本地日常"], preset: "glass"),
        VCard(id: "card-gb-1", nickname: "Monzo 账户卡", issuer: "Monzo", last4: "4602", masked: "****4602", region: "GB", currency: "GBP", type: "debit", form: "virtual", network: "Mastercard", expiry: "2029-01", status: "active", attention: false, usages: ["日常消费"], preset: "metal"),
        VCard(id: "card-gb-2", nickname: "Revolut 多币种", issuer: "Revolut", last4: "0047", masked: "****0047", region: "GB", currency: "GBP", type: "credit", form: "virtual", network: "Mastercard", expiry: "2026-07", status: "expiring_soon", attention: true, usages: ["多币种", "订阅"], preset: "abstract"),
        VCard(id: "card-us-1", nickname: "Chase Sapphire", issuer: "Chase", last4: "8220", masked: "****8220", region: "US", currency: "USD", type: "credit", form: "physical", network: "Visa", expiry: "2028-12", status: "active", attention: false, usages: ["旅行", "订阅"], preset: "city"),
        VCard(id: "card-us-2", nickname: "虚拟卡 0109", issuer: "Capital One", last4: "0109", masked: "****0109", region: "US", currency: "USD", type: "credit", form: "virtual", network: "Visa", expiry: "2027-06", status: "active", attention: false, usages: ["网络订阅"], preset: "minimal"),
        VCard(id: "card-sg-1", nickname: "DBS 银行卡", issuer: "DBS", last4: "3391", masked: "****3391", region: "SG", currency: "SGD", type: "credit", form: "physical", network: "Visa", expiry: "2027-08", status: "active", attention: false, usages: ["本地消费"], preset: "glass"),
    ]

    static let numbers: [VNumber] = [
        VNumber(id: "num-cn-1", nickname: "主号 中国移动", maskedNumber: "+86 138****8823", region: "CN", countryCode: "+86", carrier: "中国移动", simKind: "SIM", role: "primary", usages: ["银行验证", "注册", "2FA"], status: "active", recoveryOnly: true, attention: true),
        VNumber(id: "num-cn-2", nickname: "工作副号", maskedNumber: "+86 137****5510", region: "CN", countryCode: "+86", carrier: "中国联通", simKind: "eSIM", role: "secondary", usages: ["工作"], status: "active", recoveryOnly: false, attention: false),
        VNumber(id: "num-cn-3", nickname: "保号副号", maskedNumber: "+86 139****2204", region: "CN", countryCode: "+86", carrier: "中国移动", simKind: "SIM", role: "secondary", usages: ["保号"], status: "active", recoveryOnly: false, attention: false),
        VNumber(id: "num-hk-1", nickname: "香港主号", maskedNumber: "+852 9***4321", region: "HK", countryCode: "+852", carrier: "3HK", simKind: "SIM", role: "primary", usages: ["银行验证", "2FA"], status: "active", recoveryOnly: true, attention: true),
        VNumber(id: "num-gb-1", nickname: "英国主号", maskedNumber: "+44 7911 182***", region: "GB", countryCode: "+44", carrier: "Vodafone", simKind: "eSIM", role: "primary", usages: ["注册", "旅行", "2FA"], status: "active", recoveryOnly: false, attention: false),
        VNumber(id: "num-us-1", nickname: "美国保号", maskedNumber: "+1 415 887 ****", region: "US", countryCode: "+1", carrier: "T-Mobile", simKind: "SIM", role: "secondary", usages: ["恢复"], status: "active", recoveryOnly: true, attention: false),
        VNumber(id: "num-sg-1", nickname: "新加坡主号", maskedNumber: "+65 9***2214", region: "SG", countryCode: "+65", carrier: "Singtel", simKind: "eSIM", role: "primary", usages: ["银行验证", "工作"], status: "active", recoveryOnly: false, attention: false),
    ]

    static let services: [VService] = [
        VService(id: "svc-wxpay", name: "微信支付", region: "CN", kind: "payment"),
        VService(id: "svc-alipay", name: "支付宝", region: "CN", kind: "payment"),
        VService(id: "svc-tencent", name: "腾讯视频", region: "CN", kind: "subscription"),
        VService(id: "svc-hsbc", name: "HSBC 网银", region: "HK", kind: "banking"),
        VService(id: "svc-apple", name: "Apple 订阅", region: "US", kind: "subscription"),
        VService(id: "svc-netflix", name: "Netflix", region: "US", kind: "subscription"),
        VService(id: "svc-amazon", name: "亚马逊", region: "GB", kind: "subscription"),
        VService(id: "svc-microsoft", name: "Microsoft 365", region: "US", kind: "subscription"),
        VService(id: "svc-dbs", name: "DBS 网银", region: "SG", kind: "banking"),
        VService(id: "svc-notion", name: "Notion", region: "GB", kind: "subscription"),
    ]

    static let accounts: [VAccount] = [
        VAccount(id: "acc-cn-1", name: "微信账户", provider: "腾讯", maskedIdentifier: "wxid_••••8823", region: "CN", roles: ["日常身份", "支付"], authMethods: ["主号", "设备确认"], recoveryRoute: "主号 + 设备确认", status: "active", attention: false),
        VAccount(id: "acc-hk-1", name: "HSBC 网银", provider: "HSBC", maskedIdentifier: "h••••@mail.com", region: "HK", roles: ["银行", "资产"], authMethods: ["香港主号", "安全设备"], recoveryRoute: "香港主号", status: "active", attention: true),
        VAccount(id: "acc-gb-1", name: "Amazon UK", provider: "Amazon", maskedIdentifier: "h••••@mail.com", region: "GB", roles: ["购物", "订阅"], authMethods: ["英国主号", "邮箱"], recoveryRoute: "邮箱 + 英国主号", status: "active", attention: false),
        VAccount(id: "acc-us-1", name: "Apple Account", provider: "Apple", maskedIdentifier: "h••••@icloud.com", region: "US", roles: ["设备", "订阅", "恢复"], authMethods: ["美国保号", "受信任设备"], recoveryRoute: "美国保号", status: "active", attention: true),
        VAccount(id: "acc-sg-1", name: "DBS digibank", provider: "DBS", maskedIdentifier: "user••••91", region: "SG", roles: ["银行"], authMethods: ["新加坡主号"], recoveryRoute: "新加坡主号", status: "active", attention: false),
    ]

    static let emails: [VEmail] = [
        VEmail(id: "email-cn-1", name: "主邮箱", maskedAddress: "h••••@outlook.com", provider: "Outlook", region: "CN", roles: ["登录", "恢复", "通知"], linkedServiceCount: 5, recoveryOnly: true, status: "active"),
        VEmail(id: "email-gb-1", name: "海外邮箱", maskedAddress: "h••••@gmail.com", provider: "Gmail", region: "GB", roles: ["登录", "订阅"], linkedServiceCount: 4, recoveryOnly: false, status: "active"),
        VEmail(id: "email-us-1", name: "Apple 恢复邮箱", maskedAddress: "h••••@icloud.com", provider: "iCloud", region: "US", roles: ["恢复", "设备"], linkedServiceCount: 3, recoveryOnly: false, status: "active"),
    ]

    static let devices: [VDevice] = [
        VDevice(id: "dev-cn-1", name: "Pixel 8", platform: "Android", kind: "手机", region: "CN", roles: ["主设备", "验证器"], trust: "受信任", lastSeen: "今天", attention: false),
        VDevice(id: "dev-hk-1", name: "MacBook Pro", platform: "macOS", kind: "电脑", region: "HK", roles: ["工作", "受信任设备"], trust: "受信任", lastSeen: "昨天", attention: false),
        VDevice(id: "dev-gb-1", name: "YubiKey 5C NFC", platform: "Hardware Key", kind: "安全密钥", region: "GB", roles: ["2FA", "恢复"], trust: "备用", lastSeen: "2026-09-29", attention: false),
        VDevice(id: "dev-us-1", name: "旧 iPhone", platform: "iOS", kind: "手机", region: "US", roles: ["恢复设备"], trust: "待检查", lastSeen: "2026-06-18", attention: true),
    ]

    static let relations: [VRelation] = [
        VRelation(from: "card-cn-1", to: "svc-wxpay", kind: "funding"),
        VRelation(from: "card-cn-1", to: "svc-tencent", kind: "funding"),
        VRelation(from: "card-cn-2", to: "svc-tencent", kind: "funding"),
        VRelation(from: "card-hk-1", to: "svc-hsbc", kind: "funding"),
        VRelation(from: "card-hk-1", to: "svc-apple", kind: "funding"),
        VRelation(from: "card-us-1", to: "svc-netflix", kind: "funding"),
        VRelation(from: "card-us-1", to: "svc-microsoft", kind: "funding"),
        VRelation(from: "card-us-2", to: "svc-apple", kind: "funding"),
        VRelation(from: "card-gb-2", to: "svc-amazon", kind: "funding"),
        VRelation(from: "num-cn-1", to: "svc-wxpay", kind: "authenticates"),
        VRelation(from: "num-cn-1", to: "svc-alipay", kind: "authenticates"),
        VRelation(from: "num-hk-1", to: "svc-hsbc", kind: "twoFA"),
        VRelation(from: "num-gb-1", to: "svc-amazon", kind: "authenticates"),
        VRelation(from: "num-us-1", to: "svc-apple", kind: "twoFA"),
        VRelation(from: "num-sg-1", to: "svc-hsbc", kind: "authenticates"),
        VRelation(from: "num-sg-1", to: "svc-dbs", kind: "twoFA"),
    ]

    static let attentionItems: [VAttentionItem] = [
        VAttentionItem(id: "att-1", severity: "critical", title: "工行信用卡 11 月到期，绑定 2 项自动扣款", target: "card-cn-2"),
        VAttentionItem(id: "att-2", severity: "warning", title: "Revolut 多币种卡 7 月到期，建议更新订阅支付方式", target: "card-gb-2"),
        VAttentionItem(id: "att-3", severity: "warning", title: "+86 138****8823 是 2 个账户的唯一恢复路径", target: "num-cn-1"),
    ]

    static let activeChanges: [VActiveChange] = [
        VActiveChange(id: "chg-1", title: "更换手机号（进行中）", phase: "verify_new_phone", target: "change-phone"),
    ]

    static let upcoming: [VUpcomingItem] = [
        VUpcomingItem(id: "upc-1", title: "工行信用卡 2026-11-15 到期", days: 47),
        VUpcomingItem(id: "upc-2", title: "Revolut 卡 2026-07-20 到期", days: 20),
    ]

    static let changeStages: [VChangeStage] = [
        VChangeStage(stage: 1, key: "impact-analysis", status: "completed", blockReason: nil),
        VChangeStage(stage: 2, key: "establish-new-number", status: "completed", blockReason: nil),
        VChangeStage(stage: 3, key: "verify-new-number", status: "verifying", blockReason: nil),
        VChangeStage(stage: 4, key: "migrate-key-accounts", status: "not_started", blockReason: nil),
        VChangeStage(stage: 5, key: "check-recovery-paths", status: "not_started", blockReason: nil),
        VChangeStage(stage: 6, key: "retire-old-number", status: "blocked", blockReason: "新手机号验证通过后才能停用旧手机号"),
    ]

    static let changeMigrations: [VMigration] = [
        VMigration(service: "微信支付", status: "waiting"),
        VMigration(service: "支付宝", status: "waiting"),
        VMigration(service: "招商银行网银", status: "not_started"),
        VMigration(service: "腾讯视频", status: "waiting"),
    ]

    /// 地区聚合（由 fixture 数据计算；无数据 = 0）。
    static func regionSummaries() -> [RegionPresentation] {
        regions.map { r in
            let cardsHere = cards.filter { $0.region == r.regionCode }.count
            let phonesHere = numbers.filter { $0.region == r.regionCode }.count
            let servicesHere = services.filter { $0.region == r.regionCode }.count
            let attentionHere = attentionItems.filter { item in
                cards.contains { $0.id == item.target && $0.region == r.regionCode }
                    || numbers.contains { $0.id == item.target && $0.region == r.regionCode }
            }.count
            let accountsHere = accounts.filter { $0.region == r.regionCode }.count
            return RegionPresentation(
                regionCode: r.regionCode,
                displayName: r.displayName,
                latitude: r.latitude,
                longitude: r.longitude,
                cardCount: cardsHere,
                phoneCount: phonesHere,
                accountCount: accountsHere,
                serviceCount: servicesHere,
                attentionCount: attentionHere
            )
        }
    }

    static func cardById(_ id: String) -> VCard? {
        cards.first { $0.id == id }
    }

    static func numberById(_ id: String) -> VNumber? {
        numbers.first { $0.id == id }
    }

    static func servicesForCard(_ cardId: String) -> [VService] {
        relations
            .filter { $0.from == cardId }
            .compactMap { rel in services.first { $0.id == rel.to } }
    }

    static func servicesForNumber(_ numberId: String) -> [VService] {
        relations
            .filter { $0.from == numberId }
            .compactMap { rel in services.first { $0.id == rel.to } }
    }

    static func relationKind(_ fromId: String, _ toId: String) -> String? {
        relations.first { $0.from == fromId && $0.to == toId }?.kind
    }

    /// 跨区真实关系 → 地区对（去重；来源 fixture relations；禁止装饰性弧线）。
    static func crossRegionPairs() -> [(String, String)] {
        func regionOf(_ id: String) -> String? {
            if let c = cards.first(where: { $0.id == id }) { return c.region }
            if let n = numbers.first(where: { $0.id == id }) { return n.region }
            if let s = services.first(where: { $0.id == id }) { return s.region }
            return nil
        }
        var seen = Set<String>()
        var result: [(String, String)] = []
        for rel in relations {
            guard let from = regionOf(rel.from), let to = regionOf(rel.to), from != to else { continue }
            let key = from < to ? "\(from)|\(to)" : "\(to)|\(from)"
            if seen.contains(key) { continue }
            seen.insert(key)
            result.append((from, to))
        }
        return result
    }

    /// 换号旗舰流程：旧号码 / 新号码（fixture changePhone 段）。
    static let changeOldNumberId = "num-cn-1"
    static let changeNewNumberId = "num-cn-3"
}
