package com.bitwarden.authenticator.data.platform.util

import java.util.Locale

/**
 * String [Comparator] where the characters are compared giving precedence to
 * special characters.
 */
object SpecialCharWithPrecedenceComparator : Comparator<String> {
    override fun compare(str1: String, str2: String): Int {
        val minLength = minOf(str1.length, str2.length)
        for (i in 0 until minLength) {
            val char1 = str1[i]
            val char2 = str2[i]
            val compareResult = compareCharsSpecialCharsWithPrecedence(char1, char2)
            if (compareResult != 0) {
                return compareResult
            }
        }
        // If all compared chars are the same give precedence to the shorter String.
        return str1.length - str2.length
    }
}

/**
 * Compare two characters, where a special character is considered with higher precedence over
 * letters and numbers. If both characters are a letter and they are equal ignoring the case,
 * give priority to the lowercase instance. If they are both a digit or a non-equal letter
 * use the default [String.compareTo] converting the chars to the [Locale] specific uppercase
 * String.
 */
private fun compareCharsSpecialCharsWithPrecedence(c1: Char, c2: Char): Int {
    return when {
        c1.isLetterOrDigit() && !c2.isLetterOrDigit() -> 1
        !c1.isLetterOrDigit() && c2.isLetterOrDigit() -> -1
        c1.isLetter() && c2.isLetter() && c1.equals(other = c2, ignoreCase = true) -> {
            compareLettersLowerCaseFirst(c1 = c1, c2 = c2)
        }

        else -> {
            val upperCaseStr1 = c1.toString().uppercase(Locale.getDefault())
            val upperCaseStr2 = c2.toString().uppercase(Locale.getDefault())
            upperCaseStr1.compareTo(upperCaseStr2)
        }
    }
}

/**
 * Compare two equal letters ignoring case (i.e. 'A' == 'a'), give precedence to the
 * the character which is lowercase. If both [c1] and [c2] are equal and the
 * same case return 0 to indicate they are the same.
 */
private fun compareLettersLowerCaseFirst(c1: Char, c2: Char): Int {
    require(
        value = c1.isLetter() &&
            c2.isLetter() &&
            c1.equals(other = c2, ignoreCase = true),
    ) {
        "Both character must be the same letter, case does not matter."
    }

    return when {
        !c1.isLowerCase() && c2.isLowerCase() -> 1
        c1.isLowerCase() && !c2.isLowerCase() -> -1
        else -> 0
    }
}
