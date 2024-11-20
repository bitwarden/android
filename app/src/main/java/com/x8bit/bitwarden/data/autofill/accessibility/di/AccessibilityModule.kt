package com.x8bit.bitwarden.data.autofill.accessibility.di

import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
import android.view.accessibility.AccessibilityManager
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityAutofillManager
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityAutofillManagerImpl
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityCompletionManager
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityCompletionManagerImpl
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityEnabledManager
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityEnabledManagerImpl
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityNodeInfoManager
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityNodeInfoManagerImpl
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilitySelectionManager
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilitySelectionManagerImpl
import com.x8bit.bitwarden.data.autofill.accessibility.manager.LauncherPackageNameManager
import com.x8bit.bitwarden.data.autofill.accessibility.manager.LauncherPackageNameManagerImpl
import com.x8bit.bitwarden.data.autofill.accessibility.parser.AccessibilityParser
import com.x8bit.bitwarden.data.autofill.accessibility.parser.AccessibilityParserImpl
import com.x8bit.bitwarden.data.autofill.accessibility.processor.BitwardenAccessibilityProcessor
import com.x8bit.bitwarden.data.autofill.accessibility.processor.BitwardenAccessibilityProcessorImpl
import com.x8bit.bitwarden.data.autofill.manager.AutofillTotpManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
    fun providesAccessibilityCompletionManager(
        accessibilityAutofillManager: AccessibilityAutofillManager,
        totpManager: AutofillTotpManager,
        dispatcherManager: DispatcherManager,
    ): AccessibilityCompletionManager =
        AccessibilityCompletionManagerImpl(
            accessibilityAutofillManager = accessibilityAutofillManager,
            totpManager = totpManager,
            dispatcherManager = dispatcherManager,
        )

    @Singleton
    @Provides
    fun providesAccessibilityAutofillManager(): AccessibilityAutofillManager =
        AccessibilityAutofillManagerImpl()

    @Singleton
    @Provides
    fun providesAccessibilityEnabledManager(
        accessibilityManager: AccessibilityManager,
    ): AccessibilityEnabledManager =
        AccessibilityEnabledManagerImpl(
            accessibilityManager = accessibilityManager,
        )

    @Singleton
    @Provides
    fun providesAccessibilityNodeInfoManager(): AccessibilityNodeInfoManager =
        AccessibilityNodeInfoManagerImpl()

    @Singleton
    @Provides
    fun providesAccessibilityParser(
        accessibilityNodeInfoManager: AccessibilityNodeInfoManager,
    ): AccessibilityParser = AccessibilityParserImpl(
        accessibilityNodeInfoManager = accessibilityNodeInfoManager,
    )

    @Singleton
    @Provides
    fun providesAccessibilitySelectionManager(): AccessibilitySelectionManager =
        AccessibilitySelectionManagerImpl()

    @Singleton
    @Provides
    fun providesBitwardenAccessibilityProcessor(
        @ApplicationContext context: Context,
        accessibilityParser: AccessibilityParser,
        accessibilityAutofillManager: AccessibilityAutofillManager,
        launcherPackageNameManager: LauncherPackageNameManager,
        powerManager: PowerManager,
    ): BitwardenAccessibilityProcessor =
        BitwardenAccessibilityProcessorImpl(
            context = context,
            accessibilityParser = accessibilityParser,
            accessibilityAutofillManager = accessibilityAutofillManager,
            launcherPackageNameManager = launcherPackageNameManager,
            powerManager = powerManager,
        )

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

    @Singleton
    @Provides
    fun providesPackageManager(
        @ApplicationContext context: Context,
    ): PackageManager = context.packageManager

    @Singleton
    @Provides
    fun provideAccessibilityManager(
        @ApplicationContext context: Context,
    ): AccessibilityManager = context.getSystemService(AccessibilityManager::class.java)

    @Singleton
    @Provides
    fun providesPowerManager(
        @ApplicationContext context: Context,
    ): PowerManager = context.getSystemService(PowerManager::class.java)
}
