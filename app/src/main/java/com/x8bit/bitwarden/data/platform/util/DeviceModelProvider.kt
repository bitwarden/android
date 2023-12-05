package com.x8bit.bitwarden.data.platform.util

import android.os.Build
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

/**
 * Provides device model string. Useful for mocking static [Build.MODEL] call tests.
 */
@OmitFromCoverage
class DeviceModelProvider {

    /**
     * Device model.
     */
    val deviceModel: String = Build.MODEL
}
