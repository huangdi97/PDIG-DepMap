package com.pdig.uivnext.ui

import com.pdig.uivnext.model.VScreen

/**
 * Screenshot / recent-task protection policy for Production VNext.
 *
 * This mirrors the existing production intent: overview/help/preferences may stay
 * usable, while concrete Reality details, relationship/review/change execution,
 * records and Reality search are protected.
 *
 * The policy is pure so JVM tests can pin it independently from WindowManager.
 */
internal fun productionVNextRequiresSecureWindow(screen: VScreen): Boolean = when (screen) {
    // Overview / control surfaces. These intentionally match the legacy policy:
    // high-level overview is usable without forcing FLAG_SECURE.
    VScreen.NOW,
    VScreen.ME,
    VScreen.SETTINGS,
    VScreen.PERSONALIZATION,
    VScreen.CHANGE -> false

    // Concrete Reality inventory / dependency surfaces.
    VScreen.INFRASTRUCTURE,
    VScreen.OVERVIEW,
    VScreen.CARDS,
    VScreen.NUMBERS,
    VScreen.ACCOUNTS,
    VScreen.EMAILS,
    VScreen.DEVICES,
    VScreen.SERVICES,
    VScreen.WEAKNESSES,
    VScreen.CARD_DETAIL,
    VScreen.NUMBER_DETAIL,
    VScreen.ACCOUNT_DETAIL,
    VScreen.EMAIL_DETAIL,
    VScreen.DEVICE_DETAIL,
    VScreen.SERVICE_DETAIL,

    // Local appearance editors can contain real identifiers/background images.
    VScreen.CARD_CUSTOMIZATION,
    VScreen.NUMBER_CUSTOMIZATION,

    // Reality history / plans / review / import / source / search.
    VScreen.RECORDS,
    VScreen.REVIEW,
    VScreen.IMPORT,
    VScreen.MANUAL_ADD,
    VScreen.MANUAL_RELATION,
    VScreen.SOURCES,
    VScreen.CHANGE_PHONE,
    VScreen.CHANGE_CARD,
    VScreen.SEARCH -> true
}
