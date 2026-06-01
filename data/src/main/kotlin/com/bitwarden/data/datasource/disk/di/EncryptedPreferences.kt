package com.bitwarden.data.datasource.disk.di

import javax.inject.Qualifier

/**
 * Used to denote an instance of [android.content.SharedPreferences] that encrypts its data.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class EncryptedPreferences
