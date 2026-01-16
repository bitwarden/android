@file:OmitFromCoverage

package com.bitwarden.core.util

import com.bitwarden.annotation.OmitFromCoverage

private const val KEY_XIAOMI_HYPER_OS_NAME = "ro.mi.os.version.name"
private const val HORIZON_OS_SDK = "horizonos.os.Build\$HorizonOsSdk"

/**
 * Returns true if the device is running Xiaomi HyperOS.
 */
fun isHyperOS(): Boolean = !getSystemProperty(KEY_XIAOMI_HYPER_OS_NAME).isNullOrEmpty()

/**
 * Returns true if the device is running Horizon OS.
 */
fun isHorizonOS(): Boolean {
    return try {
        val horizonOsSdk = Class.forName(HORIZON_OS_SDK)
        val getVersionMethod = horizonOsSdk.getMethod("getVersion")
        getVersionMethod.invoke(null) != null
    } catch (_: Throwable) {
        false
    }
}

/**
 * Reads an Android system property using the android.os.SystemProperties API
 *
 * @param key the name of the system property
 * @return the property value, or null if unavailable
 */
@Suppress("SameParameterValue", "PrivateApi")
private fun getSystemProperty(key: String): String? {
    return try {
        val systemProperties = Class.forName("android.os.SystemProperties")
        val getMethod = systemProperties.getMethod("get", String::class.java)
        getMethod.invoke(null, key) as? String
    } catch (_: Throwable) {
        null
    }
}
