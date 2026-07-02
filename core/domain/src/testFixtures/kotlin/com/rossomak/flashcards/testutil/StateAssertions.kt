package com.rossomak.flashcards.testutil

import kotlinx.coroutines.flow.StateFlow

/**
 * Runs [block] with the current `value` as receiver, so state fields can be
 * asserted without the `state.` prefix:
 *
 * ```
 * viewModel.state.assertValue {
 *     isLoading shouldBe false
 *     displayName shouldBe "Alex"
 * }
 * ```
 */
inline fun <T> StateFlow<T>.assertValue(block: T.() -> Unit) {
    value.block()
}
