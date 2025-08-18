package com.x8bit.bitwarden.ui.platform.feature.debugmenu.di

import com.x8bit.bitwarden.data.platform.repository.DebugMenuRepository
import com.x8bit.bitwarden.ui.platform.feature.debugmenu.manager.DebugLaunchManagerImpl
import com.x8bit.bitwarden.ui.platform.feature.debugmenu.manager.DebugMenuLaunchManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Provides dependencies for the debug menu.
 */
@Module
@InstallIn(SingletonComponent::class)
class DebugMenuModule {

    @Provides
    fun provideDebugMenuLaunchManager(
        debugMenuRepository: DebugMenuRepository,
    ): DebugMenuLaunchManager = DebugLaunchManagerImpl(debugMenuRepository = debugMenuRepository)
}
