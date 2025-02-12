package com.bitwarden.authenticator

import android.app.Application
import com.bitwarden.authenticator.data.platform.manager.CrashLogsManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Custom application class.
 */
@HiltAndroidApp
class AuthenticatorApplication : Application() {
    // Inject classes here that must be triggered on startup but are not otherwise consumed by
    // other callers.

    @Inject
    lateinit var crashLogsManager: CrashLogsManager
}
