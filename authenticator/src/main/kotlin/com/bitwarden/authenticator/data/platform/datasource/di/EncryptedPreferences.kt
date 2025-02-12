package com.bitwarden.authenticator.data.platform.datasource.di

import android.content.SharedPreferences
import javax.inject.Qualifier

/**
 * Used to denote an instance of [SharedPreferences] that encrypts its data.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class EncryptedPreferences
