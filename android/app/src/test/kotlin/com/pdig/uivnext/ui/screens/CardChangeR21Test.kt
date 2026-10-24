package com.pdig.uivnext.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardChangeR21Test {
    @Test
    fun currentProjectionNeverPretendsAPlanExecuted() {
        val stages = cardChangeStages("current", hasReplacement = false)
        assertTrue(stages.all { it.status == "not_started" })
        assertEquals("当前依赖", cardServiceProjectionLabel("current"))
    }

    @Test
    fun transitionBlocksMigrationUntilReplacementIsChosen() {
        val blocked = cardChangeStages("transition", hasReplacement = false)
        assertEquals("completed", blocked[0].status)
        assertEquals("blocked", blocked[1].status)
        assertEquals("blocked", blocked[2].status)

        val selected = cardChangeStages("transition", hasReplacement = true)
        assertEquals("completed", selected[0].status)
        assertEquals("verifying", selected[1].status)
        assertEquals("blocked", selected[2].status)
    }

    @Test
    fun afterProjectionIsPlanOnly() {
        val stages = cardChangeStages("after", hasReplacement = true)
        assertTrue(stages.all { it.status == "plan" })
        assertEquals("计划迁移", cardServiceProjectionLabel("after"))
    }
}
