package com.bitwarden.data.manager.di

import android.content.Context
import com.bitwarden.core.data.manager.BuildInfoManager
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.data.datasource.disk.FlightRecorderDiskSource
import com.bitwarden.data.manager.BitwardenPackageManager
import com.bitwarden.data.manager.BitwardenPackageManagerImpl
import com.bitwarden.data.manager.NativeLibraryManager
import com.bitwarden.data.manager.NativeLibraryManagerImpl
import com.bitwarden.data.manager.file.FileManager
import com.bitwarden.data.manager.file.FileManagerImpl
import com.bitwarden.data.manager.flightrecorder.FlightRecorderManager
import com.bitwarden.data.manager.flightrecorder.FlightRecorderManagerImpl
import com.bitwarden.data.manager.flightrecorder.FlightRecorderWriter
import com.bitwarden.data.manager.flightrecorder.FlightRecorderWriterImpl
import com.bitwarden.network.service.DownloadService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Provides managers in the data module.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataManagerModule {

    @Provides
    @Singleton
    fun provideBitwardenPackageManager(
        @ApplicationContext context: Context,
    ): BitwardenPackageManager = BitwardenPackageManagerImpl(context = context)

    @Provides
    @Singleton
    fun provideFileManager(
        @ApplicationContext context: Context,
        downloadService: DownloadService,
        dispatcherManager: DispatcherManager,
    ): FileManager = FileManagerImpl(
        context = context,
        downloadService = downloadService,
        dispatcherManager = dispatcherManager,
    )

    @Provides
    @Singleton
    fun provideFlightRecorderManager(
        @ApplicationContext context: Context,
        clock: Clock,
        dispatcherManager: DispatcherManager,
        flightRecorderDiskSource: FlightRecorderDiskSource,
        flightRecorderWriter: FlightRecorderWriter,
    ): FlightRecorderManager = FlightRecorderManagerImpl(
        context = context,
        clock = clock,
        dispatcherManager = dispatcherManager,
        flightRecorderDiskSource = flightRecorderDiskSource,
        flightRecorderWriter = flightRecorderWriter,
    )

    @Provides
    @Singleton
    fun provideFlightRecorderWriter(
        clock: Clock,
        fileManager: FileManager,
        dispatcherManager: DispatcherManager,
        buildInfoManager: BuildInfoManager,
    ): FlightRecorderWriter = FlightRecorderWriterImpl(
        clock = clock,
        fileManager = fileManager,
        dispatcherManager = dispatcherManager,
        buildInfoManager = buildInfoManager,
    )

    @Provides
    @Singleton
    fun provideNativeLibraryManager(): NativeLibraryManager = NativeLibraryManagerImpl()
}
