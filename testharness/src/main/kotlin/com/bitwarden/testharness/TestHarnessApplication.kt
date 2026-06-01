package com.bitwarden.testharness

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for the Credential Manager test harness.
 *
 * This test application validates the credential provider implementation in the main
 * Bitwarden app by acting as a client application that requests credential operations
 * through the Android CredentialManager API.
 */
@HiltAndroidApp
class TestHarnessApplication : Application()
