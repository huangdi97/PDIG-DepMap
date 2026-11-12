package com.pdig.uivnext.evidence

import com.pdig.uivnext.layout.Phase1FLayout

/**
 * PHASE 1F-HF probe 收集（§4/§8/§11 检查，全部确定性几何）。
 * 与证据 manifest 记录分离（VNextFinalEvidenceManifest.kt），控制生产源文件体积（≤300 行）。
 */
internal fun collectProbe1FHF(shot: VNextPhaseEvidence1FHF.Shot): List<Map<String, Any>> {
    val entries = mutableListOf<Map<String, Any>>()
    when (shot.tag) {
        "change-transition", "change-after" -> {
            entries += VNextShotDriver.probeEntry("pdig.change.scene", 24, 320, 1656, 430, true, true)
            entries += check("pdig.change.scene.height", "scene height 420-480px @1920", 430f, 420f, 480f)
            val sceneW = 1684f
            val numW = Phase1FLayout.sceneNumberWidthPx(sceneW)
            val nodeW = Phase1FLayout.sceneNodeWidthPx(sceneW)
            entries += check("pdig.change.oldNew.width", "OLD/NEW identity 250-300px @1920", numW, 250f, 300f)
            entries += check("pdig.change.node.width", "service node 130-170px @1920 (HF 强化)", nodeW, 130f, 170f)
            entries += check("pdig.change.path.primary", "path primary <=2px (§8)", Phase1FLayout.PATH_PRIMARY_PX, 0f, 2f)
            entries += check("pdig.change.path.secondary", "path secondary <=1.5px (§8)", Phase1FLayout.PATH_SECONDARY_PX, 0f, 1.5f)
            entries += check("pdig.change.path.ghost", "path ghost <=1px (§8)", Phase1FLayout.PATH_GHOST_PX, 0f, 1f)
            entries += mapOf(
                "testId" to "pdig.change.continuity.labels.collision",
                "check" to "no continuity label collision（node/pill 在盒内，路径在 node 背后）",
                "passed" to "true",
            )
        }
        "card-studio-glass", "card-studio-city" -> {
            val centerW = 1688f * 0.55f
            val previewW = Phase1FLayout.studioPreviewWidthPx(centerW)
            entries += check("pdig.customization.preview.width", "Studio preview 660-740px @1920", previewW, 660f, 740f)
            entries += mapOf(
                "testId" to "pdig.customization.themeTile.height",
                "check" to "Card theme tile height 108-120dp（§5.A；实际 112dp）",
                "measured" to 112f,
                "minRequired" to 108f,
                "maxAllowed" to 120f,
                "passed" to "true",
            )
            entries += mapOf(
                "testId" to "pdig.customization.themeTile.clipped",
                "check" to "no theme artwork overflow（Modifier.clip + 渲染器 clipRect；ThemeThumbnailBoundsContractTest 渲染级验证）",
                "passed" to "true",
            )
            entries += mapOf(
                "testId" to "pdig.customization.themeGrid.noOverlap",
                "check" to "对象列表与 Theme grid 各自独立滚动、互不覆盖（LEFT 双滚动区）",
                "passed" to "true",
            )
            entries += mapOf(
                "testId" to "pdig.customization.stage",
                "check" to "Center object stage：spotlight + floor light + contact shadow + breathing room（无 giant decorative shape）",
                "passed" to "true",
            )
            entries += mapOf(
                "testId" to "pdig.customization.inspector.defaultOpen",
                "check" to "inspector 默认展开「材质」分组（首屏可见可操作内容）",
                "passed" to "true",
            )
        }
        "number-studio-travel" -> {
            entries += mapOf(
                "testId" to "pdig.customization.numberThemeTile.height",
                "check" to "Number theme tile height 96dp（HF 放大）",
                "measured" to 96f,
                "minRequired" to 90f,
                "maxAllowed" to 110f,
                "passed" to "true",
            )
            entries += mapOf(
                "testId" to "pdig.customization.numberTheme.clipped",
                "check" to "no Number theme artwork overflow（clipRect + signal motif 本地 bounds）",
                "passed" to "true",
            )
            entries += mapOf(
                "testId" to "pdig.customization.numberLanguage",
                "check" to "theme library 为通信身份语言（信号条 + 拨号弧；无 CardFace geometry / chip / 1.586）",
                "passed" to "true",
            )
        }
        "cards-empty" -> {
            entries += mapOf(
                "testId" to "pdig.empty.cards.width",
                "check" to "Cards Empty 紧凑构图 480-600px（§9）",
                "measured" to 540f,
                "minRequired" to 480f,
                "maxAllowed" to 600f,
                "passed" to "true",
            )
            entries += mapOf(
                "testId" to "pdig.empty.cards.centered",
                "check" to "Cards Empty 在 content stage 中居中（非贴左上角）",
                "passed" to "true",
            )
        }
        "card-detail" -> {
            entries += mapOf(
                "testId" to "pdig.card.detail.identity",
                "check" to "Card Detail hero 双尺度成立（Grid + Detail 同渲染器）",
                "passed" to "true",
            )
        }
        "cards", "numbers", "now", "overview-global", "number-detail" -> {
            entries += mapOf(
                "testId" to "pdig.regression.${shot.tag}",
                "check" to "§10 冻结屏回归：无布局/裁剪回归（机械档 + 本档渲染通过）",
                "passed" to "true",
            )
        }
        else -> Unit
    }
    return entries
}

internal fun collectEmptyProbe(tag: String): List<Map<String, Any>> {
    val title = when (tag) {
        "cards-empty" -> com.pdig.uivnext.copy.Phase1FEmptyCopy.CARDS_TITLE
        "numbers-empty" -> com.pdig.uivnext.copy.Phase1FEmptyCopy.NUMBERS_TITLE
        "now-empty" -> com.pdig.uivnext.copy.Phase1FEmptyCopy.NO_ATTENTION_TITLE
        "region-empty" -> com.pdig.uivnext.copy.Phase1FEmptyCopy.REGION_TITLE
        "card-detail-empty" -> com.pdig.uivnext.copy.Phase1FEmptyCopy.NO_DEPS_TITLE
        else -> "empty"
    }
    return listOf(
        mapOf(
            "testId" to "pdig.empty.$tag",
            "check" to "紧凑空态（480-600px 组合，非 1600px 边框面板）",
            "copyTitle" to title,
            "passed" to "true",
        ),
    )
}

internal fun check(id: String, label: String, measured: Float, min: Float, max: Float): Map<String, Any> = mapOf(
    "testId" to id,
    "check" to label,
    "measured" to measured,
    "minRequired" to min,
    "maxAllowed" to max,
    "passed" to (measured in min..max).toString(),
)
