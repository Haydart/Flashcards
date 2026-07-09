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

    @Test
    fun `domain module has no Android framework imports`() {
        Konsist.scopeFromProject()
            .files
            .filter { it.path.contains("/core/domain/src/") }
            .assertTrue { file ->
                file.imports.none { import -> import.name.startsWith("android.") }
            }
    }

    @Test
    fun `no class uses the Impl suffix`() {
        Konsist.scopeFromProject()
            .classes()
            .assertFalse { it.name.endsWith("Impl") }
    }

    @Test
    fun `DTO classes reside in a data-layer package`() {
        // Firestore DTOs use reflection-based mapping, not kotlinx @Serializable, so we
        // enforce location (data layer) rather than a serialization annotation here.
        Konsist.scopeFromProject()
            .classes()
            .filter { it.name.endsWith("Dto") }
            .assertTrue { koClass -> koClass.resideInPackage("..data..") }
    }

    @Test
    fun `HiltViewModel classes have the ViewModel suffix`() {
        Konsist.scopeFromProject()
            .classes()
            .filter { koClass -> koClass.annotations.any { it.name == "HiltViewModel" } }
            .assertTrue { it.name.endsWith("ViewModel") }
    }
}
