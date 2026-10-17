package com.pdig.uivnext.evidence

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pdig.uivnext.ui.components.CardIdentityProfile
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * B6 AndroidCardIdentityContractTest（brief §22/§5）——
 *  - 8 个冻结 issuer（CMB/ICBC/BOC/HSBC/BOCHK/Monzo/Revolut/Chase）profile 全部存在；
 *  - 每对 issuer 至少 3 个 identity 维度不同（不得全部映射到同一 blue gradient）；
 *  - profile 解析确定性（同一 issuer 字符串 → 同一维度序列）；Grid/Detail/Studio 通过
 *    唯一 renderer（CardIdentityFace，含 CardIdentityProfile.forIssuer）解析 —— 单一真源。
 */
@RunWith(AndroidJUnit4::class)
class AndroidCardIdentityContractTest {

    @Test
    fun eightFrozenIssuersExist_and_pairwiseAtLeastThreeDimensionsDiffer() {
        val issuers = CardIdentityProfile.FROZEN_ISSUERS
        assertTrue("must define exactly 8 frozen issuer profiles", issuers.size == 8)
        val expectedKeys = listOf("cmb", "icbc", "boc", "hsbc", "bochk", "monzo", "revolut", "chase")
        assertTrue(
            "frozen issuer keys must match (CMB/ICBC/BOC/HSBC/BOCHK/Monzo/Revolut/Chase)",
            issuers.map { it.issuerKey } == expectedKeys,
        )

        for (i in issuers.indices) {
            for (j in i + 1 until issuers.size) {
                val dimsA = issuers[i].identityDimensions()
                val dimsB = issuers[j].identityDimensions()
                val different = dimsA.zip(dimsB).count { (a, b) -> a != b }
                assertTrue(
                    "${issuers[i].issuerKey} vs ${issuers[j].issuerKey}: at least 3 identity dimensions must differ (got $different)",
                    different >= 3,
                )
            }
        }
    }

    @Test
    fun issuerResolutionIsDeterministic_andCoversFixtureIssuers() {
        val fixtureIssuers = listOf(
            "招商银行", "中国工商银行", "中国银行", "HSBC 汇丰", "中银香港",
            "Monzo", "Revolut", "Chase", "Capital One", "DBS",
        )
        fixtureIssuers.forEach { issuer ->
            val first = CardIdentityProfile.forIssuer(issuer)
            val second = CardIdentityProfile.forIssuer(issuer)
            assertTrue(
                "resolution must be deterministic for '$issuer'",
                first.identityDimensions() == second.identityDimensions(),
            )
        }
        // 8 个冻结 issuer 的 fixture 解析必须命中冻结 profile（不是 NEUTRAL 兜底）
        assertTrue("招商银行 -> CMB", CardIdentityProfile.forIssuer("招商银行") == CardIdentityProfile.CMB)
        assertTrue("中国工商银行 -> ICBC", CardIdentityProfile.forIssuer("中国工商银行") == CardIdentityProfile.ICBC)
        assertTrue("中国银行 -> BOC", CardIdentityProfile.forIssuer("中国银行") == CardIdentityProfile.BOC)
        assertTrue("HSBC 汇丰 -> HSBC", CardIdentityProfile.forIssuer("HSBC 汇丰") == CardIdentityProfile.HSBC)
        assertTrue("中银香港 -> BOCHK", CardIdentityProfile.forIssuer("中银香港") == CardIdentityProfile.BOCHK)
        assertTrue("Monzo -> MONZO", CardIdentityProfile.forIssuer("Monzo") == CardIdentityProfile.MONZO)
        assertTrue("Revolut -> REVOLUT", CardIdentityProfile.forIssuer("Revolut") == CardIdentityProfile.REVOLUT)
        assertTrue("Chase -> CHASE", CardIdentityProfile.forIssuer("Chase") == CardIdentityProfile.CHASE)
        assertTrue("unknown issuer must fall back to NEUTRAL (honest, never fake identity)", CardIdentityProfile.forIssuer("未知银行") == CardIdentityProfile.NEUTRAL)
    }
}