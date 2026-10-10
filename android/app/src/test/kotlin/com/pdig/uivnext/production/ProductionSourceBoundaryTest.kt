package com.pdig.uivnext.production

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Architecture guard: production VNext source must fail closed rather than import
 * reference/demo data or Preview-specific R9/R1x screen implementations.
 */
class ProductionSourceBoundaryTest {
    @Test
    fun productionSourceHasNoReferenceFixtureImports() {
        val root = findVNextSourceRoot()
        val productionFiles = buildList {
            addAll(File(root, "production").walkTopDown().filter { it.isFile && it.extension == "kt" })
            addAll(
                File(root, "ui").listFiles().orEmpty()
                    .filter { it.isFile && it.extension == "kt" && it.name.startsWith("Production") },
            )
        }

        assertTrue("expected production VNext source files", productionFiles.isNotEmpty())

        productionFiles.forEach { file ->
            val source = file.readText()
            assertFalse(
                "${file.name} must not import demo/reference fixture code",
                Regex("""(?m)^\s*import\s+com\.pdig\.uivnext\.demo\.""").containsMatchIn(source),
            )
            assertFalse(
                "${file.name} must not import Preview R9/R1x screen implementations",
                Regex("""(?m)^\s*import\s+com\.pdig\.uivnext\.ui\.(?:r9|screens\.R(?:1[0-9]|2[0-9]))""")
                    .containsMatchIn(source),
            )
        }
    }

    private fun findVNextSourceRoot(): File {
        val cwd = File(System.getProperty("user.dir")).absoluteFile
        val roots = generateSequence(cwd) { it.parentFile }
            .flatMap { base ->
                sequenceOf(
                    File(base, "app/src/main/kotlin/com/pdig/uivnext"),
                    File(base, "android/app/src/main/kotlin/com/pdig/uivnext"),
                )
            }
        return roots.firstOrNull { it.isDirectory }
            ?: error("Cannot locate Android VNext source root from ${cwd.path}")
    }
}
