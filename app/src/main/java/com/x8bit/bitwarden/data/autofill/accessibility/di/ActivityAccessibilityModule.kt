package com.x8bit.bitwarden.data.autofill.accessibility.di

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityActivityManager
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityActivityManagerImpl
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityEnabledManager
import com.x8bit.bitwarden.data.platform.manager.AppStateManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped

/**
 * Provides dependencies within the accessibility package scoped to the activity.
 */
@Module
@InstallIn(ActivityComponent::class)
object ActivityAccessibilityModule {
    @ActivityScoped
    @Provides
    fun providesAccessibilityActivityManager(
        @ApplicationContext context: Context,
        accessibilityEnabledManager: AccessibilityEnabledManager,
        appStateManager: AppStateManager,
        lifecycleScope: LifecycleCoroutineScope,
    ): AccessibilityActivityManager =
        AccessibilityActivityManagerImpl(
            context = context,
            accessibilityEnabledManager = accessibilityEnabledManager,
            appStateManager = appStateManager,
            lifecycleScope = lifecycleScope,
        )
}
