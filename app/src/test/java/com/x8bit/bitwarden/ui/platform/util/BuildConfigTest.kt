package com.x8bit.bitwarden.ui.platform.util

import android.os.Build
import com.x8bit.bitwarden.BuildConfig
import com.x8bit.bitwarden.data.platform.util.deviceData
import com.x8bit.bitwarden.data.platform.util.isFdroid
import com.x8bit.bitwarden.data.platform.util.versionData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BuildConfigTest {
    @Test
    fun `deviceData should be formatted correctly`() {
        val deviceBrandModel = "\uD83D\uDCF1 ${Build.BRAND} ${Build.MODEL}"
        val osInfo = "\uD83E\uDD16 ${Build.VERSION.RELEASE}@${Build.VERSION.SDK_INT}"
        val buildInfo = "\uD83D\uDCE6 dev"

        if (isFdroid) {
            assertEquals("$deviceBrandModel $osInfo $buildInfo -fdroid", deviceData)
        } else {
            assertEquals("$deviceBrandModel $osInfo $buildInfo", deviceData)
        }
    }

    @Test
    fun `versionData should be formatted correctly`() {
        val versionName = BuildConfig.VERSION_NAME
        val versionCode = BuildConfig.VERSION_CODE

        assertEquals("$versionName ($versionCode)", versionData)
    }
}
