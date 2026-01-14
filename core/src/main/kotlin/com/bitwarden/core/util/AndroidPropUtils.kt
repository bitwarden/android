@file:OmitFromCoverage

package com.bitwarden.core.util

import com.bitwarden.annotation.OmitFromCoverage

private const val KEY_XIAOMI_HYPER_OS_NAME = "ro.mi.os.version.name"

/**
 * Returns true if the device is running Xiaomi HyperOS.
 */
fun isHyperOS(): Boolean = !getSystemProperty(KEY_XIAOMI_HYPER_OS_NAME).isNullOrEmpty()

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
