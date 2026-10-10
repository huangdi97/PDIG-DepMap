package com.pdig.uivnext.persist

import com.pdig.uivnext.model.PresentationProfile

/**
 * Studio 初始 profile 的确定性解析（证据 harness 与 UI 共用同一逻辑，避免 drift）。
 *
 * 优先级固定为：
 *  1. 已持久化本地偏好（store.load）—— 产品行为（用户改过则尊重用户）；
 *  2. evidence customTheme override（截图证据指定主题）—— 仅当无持久化偏好时生效；
 *  3. 目标默认视觉 fallback。
 *
 * 证据 harness 必须注入隔离 store（不读用户主目录 `~/.pdig/presentation-profiles.json`），
 * 使 override 确定性生效，防止用户持久化 profile 污染 deterministic screenshot fixture。
 */
fun resolveStudioProfile(
    store: PresentationProfileStore,
    targetType: String,
    targetId: String,
    themeOverride: String?,
    fallback: () -> PresentationProfile,
): PresentationProfile {
    store.load(targetType, targetId)?.let { return it }
    themeOverride?.let { return PresentationProfile.defaultFor(targetType, targetId, it) }
    return fallback()
}
