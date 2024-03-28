package com.x8bit.bitwarden.authenticator.data.platform.datasource.di

import android.content.SharedPreferences
import javax.inject.Qualifier

/**
 * Used to denote an instance of [SharedPreferences] that does not encrypt its data.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class UnencryptedPreferences
