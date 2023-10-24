package com.x8bit.bitwarden

import android.app.Application
import com.x8bit.bitwarden.data.platform.repository.NetworkConfigRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Custom application class.
 */
@HiltAndroidApp
class BitwardenApplication : Application() {
    // Inject classes here that must be triggered on startup but are not otherwise consumed by
    // other callers.
    @Inject
    lateinit var networkConfigRepository: NetworkConfigRepository
}
