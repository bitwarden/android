package com.x8bit.bitwarden.data.autofill.accessibility.di

import android.content.pm.PackageManager
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityAutofillManager
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityAutofillManagerImpl
import com.x8bit.bitwarden.data.autofill.accessibility.manager.LauncherPackageNameManager
import com.x8bit.bitwarden.data.autofill.accessibility.manager.LauncherPackageNameManagerImpl
import com.x8bit.bitwarden.data.autofill.accessibility.parser.AccessibilityParser
import com.x8bit.bitwarden.data.autofill.accessibility.parser.AccessibilityParserImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Provides dependencies within the accessibility package.
 */
@Module
@InstallIn(SingletonComponent::class)
object AccessibilityModule {
    @Singleton
    @Provides
    fun providesAccessibilityInvokeManager(): AccessibilityAutofillManager =
        AccessibilityAutofillManagerImpl()

    @Singleton
    @Provides
    fun providesAccessibilityParser(): AccessibilityParser = AccessibilityParserImpl()

    @Singleton
    @Provides
    fun providesLauncherPackageNameManager(
        clock: Clock,
        packageManager: PackageManager,
    ): LauncherPackageNameManager =
        LauncherPackageNameManagerImpl(
            clockProvider = { clock },
            packageManager = packageManager,
        )
}
