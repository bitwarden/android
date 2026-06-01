package com.x8bit.bitwarden.data.autofill.di

import javax.inject.Qualifier

/**
 * Used to denote that the particular manager is scoped to a single Activity instance.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ActivityScopedManager
