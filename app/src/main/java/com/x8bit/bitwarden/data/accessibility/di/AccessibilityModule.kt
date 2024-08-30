package com.x8bit.bitwarden.data.accessibility.di

import com.x8bit.bitwarden.data.accessibility.manager.AccessibilityAutofillManager
import com.x8bit.bitwarden.data.accessibility.manager.AccessibilityAutofillManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
}
