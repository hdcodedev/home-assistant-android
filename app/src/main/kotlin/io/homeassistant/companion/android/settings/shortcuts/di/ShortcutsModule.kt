package io.homeassistant.companion.android.settings.shortcuts.di

import androidx.core.content.pm.ShortcutInfoCompat
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.homeassistant.companion.android.settings.shortcuts.HaShortcutManager
import io.homeassistant.companion.android.settings.shortcuts.data.ShortcutInfoFactory
import io.homeassistant.companion.android.settings.shortcuts.data.ShortcutIntentSerializer
import javax.inject.Singleton

/** Binds the app-owned [HaShortcutManager] to the shortcut contracts declared in `:common`. */
@Module
@InstallIn(SingletonComponent::class)
internal object ShortcutsModule {
    @Provides
    @Singleton
    fun provideShortcutInfoFactory(shortcutManager: HaShortcutManager): ShortcutInfoFactory =
        ShortcutInfoFactory { id, draft -> shortcutManager.buildShortcutInfo(shortcutId = id, draft = draft) }

    @Provides
    @Singleton
    fun provideShortcutIntentSerializer(shortcutManager: HaShortcutManager): ShortcutIntentSerializer =
        object : ShortcutIntentSerializer {
            override suspend fun decode(shortcut: ShortcutInfoCompat, defaultServerId: Int) =
                shortcutManager.decode(shortcut = shortcut, defaultServerId = defaultServerId)

            override suspend fun decodeListItem(shortcut: ShortcutInfoCompat) = shortcutManager.decodeListItem(shortcut)
        }
}
