package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.model.UiVNextCard
import com.pdig.uivnext.model.hexColorOrNull
import com.pdig.uivnext.ui.components.AssetCard
import com.pdig.uivnext.ui.components.CardIdentityThumbnail
import com.pdig.uivnext.ui.r9.R10CardFace

/**
 * Adaptive bridge for the R10/R19 consumer card-image decision.
 *
 * Default cards may retain the established adaptive identity renderer, but once
 * the user chooses an R10 built-in picture or a private local image, the same
 * selected picture must immediately appear in list/detail/inspector surfaces.
 */
@Composable
internal fun R19PresentedCardFace(
    card: UiVNextCard,
    profile: PresentationProfile?,
    privacyMask: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    presentationLayout: String? = profile?.layout,
    onClick: () -> Unit,
) {
    val imageBacked = profile?.backgroundKind == "r10-art" ||
        profile?.backgroundKind == "local-image"
    if (imageBacked) {
        Box(modifier.clickable(onClick = onClick)) {
            R10CardFace(
                card = card,
                privacyMask = privacyMask,
                profile = profile,
                modifier = Modifier.fillMaxWidth(),
                compact = compact,
            )
        }
    } else {
        AssetCard(
            card = card.copy(preset = profile?.themeId ?: card.preset),
            privacyMask = privacyMask,
            onClick = onClick,
            modifier = modifier,
            presentationMaterial = profile?.material,
            presentationAccent = hexColorOrNull(profile?.accentColor ?: "default"),
            presentationLayout = presentationLayout,
        )
    }
}

@Composable
internal fun R19PresentedCardThumbnail(
    card: UiVNextCard,
    profile: PresentationProfile?,
    privacyMask: Boolean,
    modifier: Modifier = Modifier,
) {
    val imageBacked = profile?.backgroundKind == "r10-art" ||
        profile?.backgroundKind == "local-image"
    if (imageBacked) {
        R10CardFace(
            card = card,
            privacyMask = privacyMask,
            profile = profile,
            modifier = modifier,
            compact = true,
        )
    } else {
        CardIdentityThumbnail(
            card = card.copy(preset = profile?.themeId ?: card.preset),
            privacyMask = privacyMask,
            modifier = modifier,
        )
    }
}
