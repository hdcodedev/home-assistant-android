package io.homeassistant.companion.android.common.data.shortcuts.entities

sealed interface ShortcutResult<out T> {
    data class Success<T>(val data: T) : ShortcutResult<T>
    data class Error(val error: ShortcutError, val throwable: Throwable? = null) : ShortcutResult<Nothing>
}

enum class ShortcutError {
    ApiNotSupported,
    NoServers,
    SlotsFull,
    InvalidIndex,
    InvalidInput,
    HomeShortcutNotSupported,
    Unknown,
}
