package io.homeassistant.companion.android.settings.shortcuts.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.homeassistant.companion.android.settings.shortcuts.data.AppShortcutsRepository
import io.homeassistant.companion.android.settings.shortcuts.data.HomeShortcutsRepository
import io.homeassistant.companion.android.settings.shortcuts.data.ShortcutServersRepository
import io.homeassistant.companion.android.settings.shortcuts.data.impl.AppShortcutsDataSource
import io.homeassistant.companion.android.settings.shortcuts.data.impl.HomeShortcutsDataSource
import io.homeassistant.companion.android.settings.shortcuts.data.impl.ServersDataSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ShortcutsRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAppShortcutsRepository(impl: AppShortcutsDataSource): AppShortcutsRepository

    @Binds
    @Singleton
    abstract fun bindHomeShortcutsRepository(impl: HomeShortcutsDataSource): HomeShortcutsRepository

    @Binds
    @Singleton
    abstract fun bindShortcutServersRepository(impl: ServersDataSource): ShortcutServersRepository
}
