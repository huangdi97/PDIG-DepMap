package com.pdig.uivnext.demo

import com.pdig.uivnext.model.ActiveChange
import com.pdig.uivnext.model.AttentionItem
import com.pdig.uivnext.model.RegionPresentation
import com.pdig.uivnext.model.UiVNextCard
import com.pdig.uivnext.model.UiVNextNumber
import com.pdig.uivnext.model.UpcomingItem
import com.pdig.uivnext.ui.VAppState

/**
 * 证据用 fixture 闸门：emptyDemo 模式返回空列表（空态截图确定性）。
 *
 * 只影响表现层数据读取；绝不触碰真实 repos / 用户持久化 profile
 * （任务书 §15：测试 screenshot 不得读用户真实持久化 profile）。
 */
fun VAppState.demoCards(): List<UiVNextCard> =
    if (emptyDemo) emptyList() else UiVNextDemoFixture.cards

fun VAppState.demoNumbers(): List<UiVNextNumber> =
    if (emptyDemo) emptyList() else UiVNextDemoFixture.numbers

fun VAppState.demoRegions(): List<RegionPresentation> =
    if (emptyDemo) emptyList() else UiVNextDemoFixture.regionSummaries()

fun VAppState.demoAttention(): List<AttentionItem> =
    if (emptyDemo) emptyList() else UiVNextDemoFixture.attentionItems

fun VAppState.demoChanges(): List<ActiveChange> =
    if (emptyDemo) emptyList() else UiVNextDemoFixture.activeChanges

fun VAppState.demoUpcoming(): List<UpcomingItem> =
    if (emptyDemo) emptyList() else UiVNextDemoFixture.upcoming
