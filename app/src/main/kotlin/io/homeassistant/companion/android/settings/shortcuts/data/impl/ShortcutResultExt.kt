package io.homeassistant.companion.android.settings.shortcuts.data.impl

import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutError
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutResult
import kotlin.coroutines.cancellation.CancellationException
import timber.log.Timber

/**
 * Runs [block] and wraps any unexpected failure in a [ShortcutResult.Error].
 *
 * Cancellation is propagated untouched so coroutine cancellation is never swallowed.
 *
 * @param errorMessage Message logged alongside the caught exception for debugging.
 * @param block Operation producing a [ShortcutResult].
 * @return The result of [block], or a [ShortcutResult.Error] on failure.
 */
internal inline fun <T> runCatchingShortcut(errorMessage: String, block: () -> ShortcutResult<T>): ShortcutResult<T> =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, errorMessage)
        ShortcutResult.Error(ShortcutError.Unknown)
    }
