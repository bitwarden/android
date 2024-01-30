package com.x8bit.bitwarden.data.autofill.di

import android.app.Activity
import android.view.autofill.AutofillManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillActivityManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillActivityManagerImpl
import com.x8bit.bitwarden.data.autofill.manager.AutofillEnabledManager
import com.x8bit.bitwarden.data.platform.manager.AppForegroundManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

/**
 * Provides dependencies in the autofill package that must be scoped to a single Activity. These
 * are for dependencies that require a very specific Activity's context to operate.
 */
@Module
@InstallIn(ActivityComponent::class)
object ActivityAutofillModule {

    @ActivityScoped
    @Provides
    fun provideAutofillActivityManager(
        @ActivityScopedManager autofillManager: AutofillManager,
        appForegroundManager: AppForegroundManager,
        autofillEnabledManager: AutofillEnabledManager,
        dispatcherManager: DispatcherManager,
    ): AutofillActivityManager =
        AutofillActivityManagerImpl(
            autofillManager = autofillManager,
            appForegroundManager = appForegroundManager,
            autofillEnabledManager = autofillEnabledManager,
            dispatcherManager = dispatcherManager,
        )

    /**
     * An AutofillManager specific to the given Activity. This wll give more accurate results
     * compared to the global manager.
     */
    @ActivityScoped
    @ActivityScopedManager
    @Provides
    fun provideActivityScopedAutofillManager(
        activity: Activity,
    ): AutofillManager = activity.getSystemService(AutofillManager::class.java)
}
