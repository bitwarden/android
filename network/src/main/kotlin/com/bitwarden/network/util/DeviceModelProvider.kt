package com.bitwarden.network.util

import android.os.Build
import com.bitwarden.annotation.OmitFromCoverage

/**
 * Provides device model string. Useful for mocking static [Build.MODEL] call tests.
 */
@OmitFromCoverage
internal class DeviceModelProvider {

    /**
     * Device model.
     */
    val deviceModel: String = Build.MODEL
}
