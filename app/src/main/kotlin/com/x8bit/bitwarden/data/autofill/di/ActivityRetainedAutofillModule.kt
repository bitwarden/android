package com.x8bit.bitwarden.data.autofill.di

import com.x8bit.bitwarden.MainActivity
import com.x8bit.bitwarden.data.autofill.manager.AutofillSelectionManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillSelectionManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped

/**
 * Provides dependencies in the autofill package that must be scoped to a retained Activity. These
 * are for dependencies that must operate independently in different application tasks that contain
 * unique [MainActivity] instances.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
object ActivityRetainedAutofillModule {

    @ActivityRetainedScoped
    @Provides
    fun provideAutofillSelectionManager(): AutofillSelectionManager =
        AutofillSelectionManagerImpl()
}
