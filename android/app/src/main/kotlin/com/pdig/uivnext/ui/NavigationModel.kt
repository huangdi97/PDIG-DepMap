package com.pdig.uivnext.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import com.pdig.uivnext.model.VScreen

/** 一级入口（rail / bottom nav 共用）。 */
internal data class NavEntry(val screen: VScreen, val icon: ImageVector)

internal val PRIMARY_ENTRIES = listOf(
    NavEntry(VScreen.NOW, Icons.Filled.Home),
    NavEntry(VScreen.INFRASTRUCTURE, Icons.Filled.Place),
    NavEntry(VScreen.CHANGE, Icons.Filled.Refresh),
    NavEntry(VScreen.RECORDS, Icons.Filled.DateRange),
    NavEntry(VScreen.ME, Icons.Filled.Person),
)

// Product decision: "我" is an intentional fifth primary destination.
// Low-frequency utilities remain below the primary rail on wide layouts.
internal val SECONDARY_ENTRIES = listOf(
    NavEntry(VScreen.SOURCES, Icons.Filled.List),
    NavEntry(VScreen.SETTINGS, Icons.Filled.Settings),
)

/** 基础设施二级：Phone 由 Overview 管理 Hub 承载；wide 在内容区使用 sibling navigation。 */
internal val INFRA_ENTRIES = listOf(
    NavEntry(VScreen.OVERVIEW, Icons.Filled.LocationOn),
    NavEntry(VScreen.CARDS, Icons.Filled.ShoppingCart),
    NavEntry(VScreen.NUMBERS, Icons.Filled.Phone),
    NavEntry(VScreen.ACCOUNTS, Icons.Filled.Person),
    NavEntry(VScreen.EMAILS, Icons.Filled.Email),
    NavEntry(VScreen.DEVICES, Icons.Filled.Build),
    NavEntry(VScreen.SERVICES, Icons.Filled.Star),
    NavEntry(VScreen.WEAKNESSES, Icons.Filled.Warning),
)



internal fun isEntrySelected(entry: VScreen, current: VScreen): Boolean = when (entry) {
    VScreen.NOW -> current == VScreen.NOW || current == VScreen.REVIEW
    VScreen.INFRASTRUCTURE -> current in setOf(
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
        VScreen.CARD_CUSTOMIZATION,
        VScreen.NUMBER_CUSTOMIZATION,
    )
    VScreen.CARDS -> current == VScreen.CARDS ||
        current == VScreen.CARD_DETAIL ||
        current == VScreen.CARD_CUSTOMIZATION
    VScreen.NUMBERS -> current == VScreen.NUMBERS ||
        current == VScreen.NUMBER_DETAIL ||
        current == VScreen.NUMBER_CUSTOMIZATION
    VScreen.ACCOUNTS -> current == VScreen.ACCOUNTS || current == VScreen.ACCOUNT_DETAIL
    VScreen.EMAILS -> current == VScreen.EMAILS || current == VScreen.EMAIL_DETAIL
    VScreen.DEVICES -> current == VScreen.DEVICES || current == VScreen.DEVICE_DETAIL
    VScreen.SERVICES -> current == VScreen.SERVICES || current == VScreen.SERVICE_DETAIL
    VScreen.CHANGE -> current == VScreen.CHANGE ||
        current == VScreen.CHANGE_PHONE ||
        current == VScreen.CHANGE_CARD
    VScreen.ME -> current == VScreen.ME || current == VScreen.SETTINGS ||
        current == VScreen.PERSONALIZATION || current == VScreen.SOURCES ||
        current == VScreen.IMPORT || current == VScreen.MANUAL_ADD ||
        current == VScreen.MANUAL_RELATION
    VScreen.SETTINGS -> current == VScreen.SETTINGS || current == VScreen.PERSONALIZATION
    else -> current == entry
}


internal fun isInfraRootScreen(screen: VScreen): Boolean = screen in setOf(
    VScreen.INFRASTRUCTURE,
    VScreen.OVERVIEW,
    VScreen.CARDS,
    VScreen.NUMBERS,
    VScreen.ACCOUNTS,
    VScreen.EMAILS,
    VScreen.DEVICES,
    VScreen.SERVICES,
    VScreen.WEAKNESSES,
)
