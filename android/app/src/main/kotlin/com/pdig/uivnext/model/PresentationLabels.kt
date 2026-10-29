package com.pdig.uivnext.model

/** UI 统一展示标签；只负责 Presentation copy，不参与任何 domain identity。 */
fun regionLabelZh(code: String): String = when (code.trim().uppercase()) {
    "CN" -> "中国大陆"
    "HK" -> "香港"
    "MO" -> "澳门"
    "GB", "UK" -> "英国"
    "US" -> "美国"
    "SG" -> "新加坡"
    else -> code
}

fun serviceKindLabelZh(kind: String): String = when (kind) {
    "payment" -> "支付"
    "banking" -> "银行"
    "subscription" -> "订阅"
    "funding" -> "资金来源"
    "authenticates" -> "登录验证"
    "twoFA" -> "2FA 验证"
    else -> "关联服务"
}


fun relationKindLabelZh(kind: String?): String = when (kind) {
    "funding" -> "资金来源"
    "authenticates" -> "登录验证"
    "twoFA" -> "2FA 验证"
    else -> "关联关系"
}
