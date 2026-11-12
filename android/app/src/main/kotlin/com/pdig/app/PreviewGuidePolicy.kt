package com.pdig.app

/** This preview guide never writes to PersonalReality, Canonical, or .depmap. */
internal const val PREVIEW_GUIDE_VERSION = 1
internal fun shouldShowPreviewGuide(completedVersion: Int): Boolean =
    completedVersion < PREVIEW_GUIDE_VERSION
