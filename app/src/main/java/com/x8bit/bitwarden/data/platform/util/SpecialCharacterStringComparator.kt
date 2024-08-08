package com.x8bit.bitwarden.data.platform.util

import java.util.Locale

/**
 * Compare two characters, where a special character is considered with higher precedence over
 * letters and numbers. If both characters are a letter or a digit use the default
 * [Char.compareTo].
 */
private fun compareCharsSpecialCharsWithPrecedence(c1: Char, c2: Char): Int {
    return when {
        c1.isLetterOrDigit() && !c2.isLetterOrDigit() -> 1
        !c1.isLetterOrDigit() && c2.isLetterOrDigit() -> -1
        else -> c1.compareTo(c2)
    }
}

/**
 * String [Comparator] where the characters are compared giving precedence to
 * special characters.
 */
object CompareStringSpecialCharWithPrecedence : Comparator<String> {
    override fun compare(str1: String, str2: String): Int {
        val uppercaseStr1 = str1.uppercase(Locale.getDefault())
        val uppercaseStr2 = str2.uppercase(Locale.getDefault())
        val minLength = minOf(uppercaseStr1.length, uppercaseStr2.length)
        for (i in 0 until minLength) {
            val char1 = uppercaseStr1[i]
            val char2 = uppercaseStr2[i]
            val compareResult = compareCharsSpecialCharsWithPrecedence(char1, char2)
            if (compareResult != 0) {
                return compareResult
            }
        }
        // If all compared chars are the same give precedence to the shorter String.
        return uppercaseStr1.length - uppercaseStr2.length
    }
}
