package com.x8bit.bitwarden.data.autofill.di

import android.app.Activity
import android.view.autofill.AutofillManager
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.x8bit.bitwarden.data.autofill.manager.AutofillActivityManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillActivityManagerImpl
import com.x8bit.bitwarden.data.autofill.manager.AutofillEnabledManager
import com.x8bit.bitwarden.data.autofill.manager.browser.BrowserThirdPartyAutofillEnabledManager
import com.x8bit.bitwarden.data.autofill.manager.browser.BrowserThirdPartyAutofillManager
import com.x8bit.bitwarden.data.autofill.manager.browser.BrowserThirdPartyAutofillManagerImpl
import com.x8bit.bitwarden.data.platform.manager.AppStateManager
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
    @ActivityScopedManager
    @Provides
    fun provideActivityScopedBrowserThirdPartyAutofillManager(
        activity: Activity,
    ): BrowserThirdPartyAutofillManager = BrowserThirdPartyAutofillManagerImpl(
        context = activity.baseContext,
    )

    @ActivityScoped
    @Provides
    fun provideAutofillActivityManager(
        @ActivityScopedManager autofillManager: AutofillManager,
        @ActivityScopedManager browserThirdPartyAutofillManager: BrowserThirdPartyAutofillManager,
        appStateManager: AppStateManager,
        autofillEnabledManager: AutofillEnabledManager,
        lifecycleScope: LifecycleCoroutineScope,
        browserThirdPartyAutofillEnabledManager: BrowserThirdPartyAutofillEnabledManager,
    ): AutofillActivityManager =
        AutofillActivityManagerImpl(
            autofillManager = autofillManager,
            browserThirdPartyAutofillManager = browserThirdPartyAutofillManager,
            appStateManager = appStateManager,
            autofillEnabledManager = autofillEnabledManager,
            lifecycleScope = lifecycleScope,
            browserThirdPartyAutofillEnabledManager = browserThirdPartyAutofillEnabledManager,
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

    @ActivityScoped
    @Provides
    fun provideLifecycleCoroutineScope(
        activity: Activity,
    ): LifecycleCoroutineScope = (activity as LifecycleOwner).lifecycleScope
}
