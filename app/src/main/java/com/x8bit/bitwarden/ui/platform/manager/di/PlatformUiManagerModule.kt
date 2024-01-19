package com.x8bit.bitwarden.ui.platform.manager.di

import android.content.Context
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

/**
 * Provides UI-based managers in the platform package.
 */
@Module
@InstallIn(SingletonComponent::class)
class PlatformUiManagerModule {
    @Provides
    fun provideIntentManager(
        @ApplicationContext context: Context,
    ): IntentManager =
        IntentManagerImpl(
            context = context,
        )
}
