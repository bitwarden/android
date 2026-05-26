package com.x8bit.bitwarden.data.autofill.di

import android.content.SharedPreferences
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.data.datasource.disk.di.UnencryptedPreferences
import com.bitwarden.data.repository.ServerConfigRepository
import com.bitwarden.network.service.FillAssistService
import com.x8bit.bitwarden.data.autofill.datasource.disk.FillAssistDiskSource
import com.x8bit.bitwarden.data.autofill.datasource.disk.FillAssistDiskSourceImpl
import com.x8bit.bitwarden.data.autofill.manager.FillAssistManager
import com.x8bit.bitwarden.data.autofill.manager.FillAssistManagerImpl
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * Provides fill-assist dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object FillAssistModule {

    @Provides
    @Singleton
    fun providesFillAssistDiskSource(
        @UnencryptedPreferences sharedPreferences: SharedPreferences,
        json: Json,
    ): FillAssistDiskSource =
        FillAssistDiskSourceImpl(
            sharedPreferences = sharedPreferences,
            json = json,
        )

    @Provides
    @Singleton
    fun providesFillAssistManager(
        fillAssistService: FillAssistService,
        fillAssistDiskSource: FillAssistDiskSource,
        featureFlagManager: FeatureFlagManager,
        serverConfigRepository: ServerConfigRepository,
        clock: Clock,
        dispatcherManager: DispatcherManager,
    ): FillAssistManager =
        FillAssistManagerImpl(
            fillAssistService = fillAssistService,
            fillAssistDiskSource = fillAssistDiskSource,
            featureFlagManager = featureFlagManager,
            serverConfigRepository = serverConfigRepository,
            clock = clock,
            dispatcherManager = dispatcherManager,
        )
}
