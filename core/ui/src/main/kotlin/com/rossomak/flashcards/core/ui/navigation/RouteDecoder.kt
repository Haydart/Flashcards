package com.rossomak.flashcards.core.ui.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute

/**
 * Ergonomic, test-seamable stand-in for [toRoute]. Call it exactly like the bare `toRoute`,
 * passing the destination type as the generic argument:
 *
 * ```
 * private val route = savedStateHandle.decodeRoute<SubcategoryDetailsRoute>()
 * ```
 *
 * In production it delegates to the real reified [toRoute]. The actual decode is funnelled
 * through the non-inline [RouteDecoder.decode] seam, which JVM unit tests stub
 * (`mockkObject(RouteDecoder)`) to return a fake route — so the android `Bundle` that
 * [toRoute] requires off-device is never touched.
 */
inline fun <reified T : Any> SavedStateHandle.decodeRoute(): T =
    RouteDecoder.decode { toRoute<T>() }

/**
 * Single indirection point behind [decodeRoute]. Kept as a non-inline object method so tests
 * can replace the whole decode via MockK; production simply invokes [producer].
 */
object RouteDecoder {
    fun <T> decode(producer: () -> T): T = producer()
}
