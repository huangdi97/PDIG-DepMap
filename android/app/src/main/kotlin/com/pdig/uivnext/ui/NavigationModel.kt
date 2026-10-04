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
)

internal val SECONDARY_ENTRIES = listOf(
    NavEntry(VScreen.SOURCES, Icons.Filled.List),
    NavEntry(VScreen.SETTINGS, Icons.Filled.Settings),
)

/** 基础设施二级（rail 内嵌小节 / compact 顶部 chip 行；卡片/号码为最高优先二级页）。 */
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
        VScreen.CARD_CUSTOMIZATION,
        VScreen.NUMBER_CUSTOMIZATION,
    )
    VScreen.CARDS -> current == VScreen.CARDS ||
        current == VScreen.CARD_DETAIL ||
        current == VScreen.CARD_CUSTOMIZATION
    VScreen.NUMBERS -> current == VScreen.NUMBERS ||
        current == VScreen.NUMBER_DETAIL ||
        current == VScreen.NUMBER_CUSTOMIZATION
    VScreen.CHANGE -> current == VScreen.CHANGE || current == VScreen.CHANGE_PHONE
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
