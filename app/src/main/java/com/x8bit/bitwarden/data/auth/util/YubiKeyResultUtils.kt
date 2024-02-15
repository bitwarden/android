package com.x8bit.bitwarden.data.auth.util

import android.content.Intent
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.regex.Pattern

private const val YUBI_KEY_DATA_LENGTH: Int = 44
private const val YUBI_KEY_PATTERN: String = "^.*?([cbdefghijklnrtuv]{32,64})\$"
private val YubiKeyPattern: Pattern = Pattern.compile(YUBI_KEY_PATTERN)

/**
 * Retrieves an [YubiKeyResult] from an [Intent]. There are two possible cases.
 *
 * - `null`: Intent is not an Yubi key callback, or data is null or invalid.
 * - [YubiKeyResult]: Intent is the Yubi key callback with correct data.
 */
fun Intent.getYubiKeyResultOrNull(): YubiKeyResult? {
    val value = this.dataString ?: return null
    val otpMatch = YubiKeyPattern.matcher(value)
    return if (otpMatch.matches()) {
        otpMatch
            .group(1)
            ?.takeUnless { it.length != YUBI_KEY_DATA_LENGTH }
            ?.let { YubiKeyResult(it) }
    } else {
        null
    }
}

/**
 * Represents a Yubi Key result object with the necessary [token] to log in to the app.
 */
@Parcelize
data class YubiKeyResult(
    val token: String,
) : Parcelable
