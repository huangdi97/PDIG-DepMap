package com.pdig.uivnext.ui

import java.io.File
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
        val productionFiles = uiDir.listFiles()
            .orEmpty()
            .filter { it.isFile && it.extension == "kt" && it.name.startsWith("Production") }

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
