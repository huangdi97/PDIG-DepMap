package com.pdig.uivnext.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class R20ServiceRelationPresentationTest {
    @Test
    fun serviceDependenciesRespectUserControlledPrivacyMask() {
        assertEquals(
            "支付工具（已遮蔽）",
            r20VisibleServiceRelationSource("旅行银行卡", null, "card-secret", true),
        )
        assertEquals(
            "手机号身份（已遮蔽）",
            r20VisibleServiceRelationSource(null, "香港主号", "phone-secret", true),
        )
        assertEquals(
            "关联对象（已遮蔽）",
            r20VisibleServiceRelationSource(null, null, "internal-identity-id", true),
        )
    }

    @Test
    fun unmaskedServiceRelationsUseRecordedNamesWithoutInventingAnIdentity() {
        assertEquals(
            "旅行银行卡",
            r20VisibleServiceRelationSource("旅行银行卡", null, "card-secret", false),
        )
        assertEquals(
            "香港主号",
            r20VisibleServiceRelationSource(null, "香港主号", "phone-secret", false),
        )
        assertEquals(
            "source-recorded-id",
            r20VisibleServiceRelationSource(null, null, "source-recorded-id", false),
        )
    }
}
