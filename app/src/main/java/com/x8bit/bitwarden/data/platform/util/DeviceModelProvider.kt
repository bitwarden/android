package com.x8bit.bitwarden.data.platform.util

import android.os.Build

/**
 * Provides device model string. Useful for mocking static [Build.model] call tests.
 */
class DeviceModelProvider {

    /**
     * Device model.
     */
    val deviceModel: String = Build.MODEL
}
