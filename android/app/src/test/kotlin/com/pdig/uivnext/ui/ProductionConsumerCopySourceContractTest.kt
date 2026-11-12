package com.pdig.uivnext.ui

import com.pdig.uivnext.production.VNextProductionDependency
import com.pdig.uivnext.production.VNextProductionImpact
import com.pdig.uivnext.production.VNextProductionImpactTarget
import com.pdig.uivnext.production.VNextProductionObject
import com.pdig.uivnext.production.VNextProductionSurfaceKind
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards consumer-facing Production string literals from regressing to internal
 * implementation vocabulary. Internal comments and diagnostics are not UI copy.
 */
class ProductionConsumerCopySourceContractTest {
    @Test
    fun productionUiStringLiteralsDoNotRegressToEngineeringCopy() {
        val uiDir = locateProductionUiDirectory()
        val productionFiles = uiDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name.startsWith("Production") }
            .toList()

        assertTrue(
            "No Production UI source files found under ${uiDir.absolutePath}",
            productionFiles.isNotEmpty(),
        )

        val forbiddenFragments = listOf(
            "AppContainer Change authority",
            "已确认 subtype",
            "phone subtype",
            "email subtype",
            "subtype 未确认",
            "authority 边界",
            "权威数据快照",
            "权威连续性",
            "权威影响分析",
            "当前权威步骤",
            "权威计划状态",
            "当前权威覆盖",
            "伪造 subtype",
            "权威来源已确认",
            "Preview ",
            "本机 Presentation",
            "PresentationProfile /",
            "已确认 Reality",
            "生产基础设施",
            "生产对象",
            "生产绑定",
            "生产支付工具",
            "生产记录",
            "生产数据源",
            "生产数据搜索",
            "生产数据绑定",
            "没有已确认的 payment_instrument",
            "当前没有已确认的 payment_instrument",
            "done 与 verified",
            "ChangePlan",
        )
        val literalRegex = Regex("\"(?:\\\\.|[^\"\\\\])*\"")
        val leaks = productionFiles.flatMap { file ->
            literalRegex.findAll(file.readText()).mapNotNull { match ->
                val literal = match.value
                val found = forbiddenFragments.firstOrNull { it in literal }
                    ?: return@mapNotNull null
                "${file.name}: ${found} in ${literal}"
            }.toList()
        }

        assertTrue(
            "Production UI leaked engineering copy:\n${leaks.joinToString("\n")}",
            leaks.isEmpty(),
        )
    }

    @Test
    fun impactMetricsNeverTurnUnknownIntoAZeroCount() {
        val impact = VNextProductionImpact(
            targetNodeId = "phone-a",
            targets = listOf(
                VNextProductionImpactTarget(
                    nodeId = "account-a",
                    nodeName = "账户 A",
                    capability = "recovery",
                    status = "must_change",
                    depth = 1,
                    reasonCode = "required_edge_no_alternative",
                    reasonText = "required",
                ),
                VNextProductionImpactTarget(
                    nodeId = "account-b",
                    nodeName = "账户 B",
                    capability = "authentication",
                    status = "needs_review",
                    depth = 1,
                    reasonCode = "criticality_unknown",
                    reasonText = "review",
                ),
                VNextProductionImpactTarget(
                    nodeId = "service-a",
                    nodeName = "服务 A",
                    capability = "recovery",
                    status = "backup_path",
                    depth = 1,
                    reasonCode = "confirmed_group_covered",
                    reasonText = "backup",
                ),
                VNextProductionImpactTarget(
                    nodeId = "service-b",
                    nodeName = "服务 B",
                    capability = "authentication",
                    status = "degraded",
                    depth = 1,
                    reasonCode = "upstream_uncertain",
                    reasonText = "degraded",
                ),
            ),
            checklist = emptyList(),
        )

        assertEquals(
            listOf(
                1 to "必须处理",
                1 to "需要核对",
                2 to "备用 / 降级",
            ),
            productionImpactMetrics(impact),
        )
        assertTrue(productionImpactMetrics(impact).none { it.second == "未知" })
    }

    @Test
    fun secondaryObjectRoleSummariesUseConfirmedEdgesWithoutTreatingZeroAsAbsence() {
        val account = VNextProductionObject(
            id = "account-a",
            kind = "account",
            name = "账户 A",
            surfaceKind = VNextProductionSurfaceKind.ACCOUNT,
        )
        val email = VNextProductionObject(
            id = "email-a",
            kind = "identity_anchor",
            name = "邮箱 A",
            surfaceKind = VNextProductionSurfaceKind.EMAIL_IDENTITY,
        )
        val service = VNextProductionObject(
            id = "service-a",
            kind = "service",
            name = "服务 A",
            surfaceKind = VNextProductionSurfaceKind.SERVICE,
        )
        fun dep(
            id: String,
            from: String,
            to: String,
            capability: String,
        ) = VNextProductionDependency(
            id = id,
            fromId = from,
            fromName = from,
            relation = "linked",
            toId = to,
            toName = to,
            capability = capability,
            criticality = "unknown",
        )

        val accountSummary = productionSecondaryObjectRoleSummary(
            account,
            listOf(
                dep("a-auth", "account-a", "service-a", "authentication"),
                dep("a-recovery", "email-a", "account-a", "recovery"),
            ),
        )
        assertEquals("访问 / 认证", accountSummary.firstLabel)
        assertEquals(1, accountSummary.firstCount)
        assertEquals("恢复此账户", accountSummary.secondLabel)
        assertEquals(1, accountSummary.secondCount)
        assertTrue(accountSummary.note.contains("0 不代表不存在未确认关系"))

        val emailSummary = productionSecondaryObjectRoleSummary(
            email,
            listOf(
                dep("e-recovery", "email-a", "account-a", "recovery"),
                // An inbound recovery edge is not evidence that this email is
                // itself being used as the recovery factor.
                dep("e-inbound", "account-a", "email-a", "recovery"),
            ),
        )
        assertEquals(1, emailSummary.firstCount)
        assertEquals(0, emailSummary.secondCount)
        assertTrue(emailSummary.note.contains("0 不代表不存在"))

        val serviceSummary = productionSecondaryObjectRoleSummary(
            service,
            listOf(
                dep("s-in", "account-a", "service-a", "access"),
                dep("s-out", "service-a", "email-a", "recovery"),
            ),
        )
        assertEquals(1, serviceSummary.firstCount)
        assertEquals(1, serviceSummary.secondCount)
    }

    @Test
    fun secondaryObjectChangeEntryNeverInventsUnsupportedScenarioAuthority() {
        val cases = listOf(
            VNextProductionSurfaceKind.ACCOUNT to "账户变更尚未开放正式可执行模板",
            VNextProductionSurfaceKind.EMAIL_IDENTITY to "邮箱更换尚未开放正式可执行模板",
            VNextProductionSurfaceKind.DEVICE to "设备更换尚未开放正式可执行模板",
            VNextProductionSurfaceKind.SERVICE to "服务迁移尚未开放正式可执行模板",
        )
        cases.forEach { (surface, expectedPrefix) ->
            val text = productionSecondaryObjectChangeUnavailableText(surface)
            assertTrue(text.startsWith(expectedPrefix))
            assertTrue("Unsupported change copy must remain fail-closed: $text",
                "不会" in text || "尚未" in text)
        }
    }

    @Test
    fun globalPrivacyMasksEveryProductionObjectIdentityLabel() {
        val cases = listOf(
            VNextProductionSurfaceKind.PAYMENT_ASSET to "支付工具（已遮蔽）",
            VNextProductionSurfaceKind.PHONE_IDENTITY to "手机号身份（已遮蔽）",
            VNextProductionSurfaceKind.EMAIL_IDENTITY to "邮箱身份（已遮蔽）",
            VNextProductionSurfaceKind.ACCOUNT to "账户（已遮蔽）",
            VNextProductionSurfaceKind.SERVICE to "服务（已遮蔽）",
            VNextProductionSurfaceKind.DEVICE to "设备（已遮蔽）",
            VNextProductionSurfaceKind.MEMBERSHIP to "会员 / 资格（已遮蔽）",
            VNextProductionSurfaceKind.IDENTITY_ANCHOR_GENERIC to "身份对象（已遮蔽）",
            VNextProductionSurfaceKind.CUSTOM_GENERIC to "自定义对象（已遮蔽）",
        )

        cases.forEach { (surface, expected) ->
            val item = VNextProductionObject(
                id = surface.name.lowercase(),
                kind = "custom",
                name = "敏感自定义名称",
                surfaceKind = surface,
            )
            assertEquals(expected, productionVisibleObjectName(item, privacyMask = true))
            assertEquals("敏感自定义名称", productionVisibleObjectName(item, privacyMask = false))
        }
    }

    @Test
    fun unknownWireValuesNeverEchoRawDomainTokens() {
        val raw = "RAW_PRIVATE_ENUM_42"
        val labels = listOf(
            productionObjectKindLabel(raw),
            productionIdentityBasisLabel(raw),
            productionRelationLabel(raw),
            productionCapabilityLabel(raw),
            productionCriticalityLabel(raw),
            productionImpactStatusLabel(raw),
            productionWorkflowStateLabel(raw),
            productionReadinessLabel(raw),
            productionActionPhaseLabel(raw),
            productionScenarioLabel(raw),
            productionSourceStateLabel(raw),
        )

        assertTrue(
            "Unknown wire values must remain unknown without exposing raw domain tokens: $labels",
            labels.none { raw in it },
        )
    }

    private fun locateProductionUiDirectory(): File {
        val root = File(System.getProperty("user.dir"))
        val relative = "src/main/kotlin/com/pdig/uivnext/ui"
        val candidates = listOf(
            File(root, relative),
            File(root, "app/$relative"),
            File(root, "android/app/$relative"),
            File(root.parentFile ?: root, "app/$relative"),
            File(root.parentFile ?: root, "android/app/$relative"),
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("Could not locate Production UI sources from ${root.absolutePath}")
    }
}
