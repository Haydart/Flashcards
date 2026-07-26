package com.rossomak.flashcards.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/**
 * Architecture rules enforced across the whole project (ADR-0020 arg-order stays in the
 * Python script for now). Rules are intentionally conservative in this first pass so the
 * suite is green on existing code; tighten in follow-up PRs.
 */
class ArchitectureKonsistTest {

    private val projectScope = Konsist.scopeFromProject()

    @Test
    fun `domain module has no Android framework imports`() {
        projectScope
            .files
            .filter { it.path.contains("/core/domain/src/") }
            .assertTrue { file ->
                file.imports.none { import -> import.name.startsWith("android.") || import.name.startsWith("androidx.") }
            }
    }

    @Test
    fun `no class uses the Impl suffix`() {
        projectScope
            .classes()
            .assertFalse { it.name.endsWith("Impl") }
    }

    @Test
    fun `DTO classes reside in a data-layer package`() {
        // Firestore DTOs use reflection-based mapping, not kotlinx @Serializable, so we
        // enforce location (data layer) rather than a serialization annotation here.
        projectScope
            .classes()
            .filter { it.name.endsWith("Dto") }
            .assertTrue { koClass -> koClass.resideInPackage("..data..") }
    }

    @Test
    fun `HiltViewModel classes have the ViewModel suffix`() {
        projectScope
            .classes()
            .filter { koClass -> koClass.annotations.any { it.name == "HiltViewModel" } }
            .assertTrue { it.name.endsWith("ViewModel") }
    }

    @Test
    fun `design-system composables use no raw dp literals`() {
        // Reusable components in :core:ui composables/ must read dimensions from theme
        // tokens (spacing / sizes / cornerRadius), never hardcode `dp` (see core/ui/README.md
        // and ADR-0020). Opt out per-function with @RawDimensions("reason"). Colors are never
        // exempt. Token *definitions* live in the theme/ package, so scoping to composables/
        // keeps them out of this check.
        val rawDpLiteral = Regex("""\b\d+(\.\d+)?\.dp\b""")
        projectScope
            .functions()
            .filter { function -> function.path.contains("/core/ui/") && function.path.contains("/composables/") }
            .filter { function -> function.annotations.none { it.name == "RawDimensions" } }
            .assertFalse { function -> rawDpLiteral.containsMatchIn(function.text) }
    }

    @Test
    fun `core modules never import feature modules`() {
        projectScope
            .files
            .filter { it.path.contains("/core/") && it.path.contains("/src/") }
            .assertTrue { file ->
                file.imports.none { import -> import.name.startsWith("com.rossomak.flashcards.feature.") }
            }
    }
}
