package com.x8bit.bitwarden.data.platform.datasource.sdk.util

import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength
import com.x8bit.bitwarden.data.auth.datasource.sdk.util.toInt
import com.x8bit.bitwarden.data.auth.datasource.sdk.util.toPasswordStrengthOrNull
import com.x8bit.bitwarden.data.auth.datasource.sdk.util.toUByte
import com.x8bit.bitwarden.data.auth.datasource.sdk.util.toUInt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PasswordStrengthExtensionsTest {
    @Nested
    inner class IntegerType {
        @Test
        fun `toPasswordStrengthOrNull returns the correct values in 0 to 4 range`() {
            mapOf(
                0 to PasswordStrength.LEVEL_0,
                1 to PasswordStrength.LEVEL_1,
                2 to PasswordStrength.LEVEL_2,
                3 to PasswordStrength.LEVEL_3,
                4 to PasswordStrength.LEVEL_4,
            )
                .forEach { (intValue, level) ->
                    assertEquals(
                        level,
                        intValue.toPasswordStrengthOrNull(),
                    )
                }
        }

        @Test
        fun `toPasswordStrengthOrNull returns null outside the 0 to 4 range`() {
            listOf(-2, -1, 5, 6).forEach { intValue ->
                assertNull(
                    intValue.toPasswordStrengthOrNull(),
                )
            }
        }

        @Test
        fun `toInt returns the correct Int for each level`() {
            mapOf(
                PasswordStrength.LEVEL_0 to 0,
                PasswordStrength.LEVEL_1 to 1,
                PasswordStrength.LEVEL_2 to 2,
                PasswordStrength.LEVEL_3 to 3,
                PasswordStrength.LEVEL_4 to 4,
            )
                .forEach { (level, intValue) ->
                    assertEquals(
                        intValue,
                        level.toInt(),
                    )
                }
        }
    }

    @Nested
    inner class UByteType {
        @Test
        fun `toPasswordStrengthOrNull returns the correct values in 0 to 4 range`() {
            mapOf(
                0.toUByte() to PasswordStrength.LEVEL_0,
                1.toUByte() to PasswordStrength.LEVEL_1,
                2.toUByte() to PasswordStrength.LEVEL_2,
                3.toUByte() to PasswordStrength.LEVEL_3,
                4.toUByte() to PasswordStrength.LEVEL_4,
            )
                .forEach { (uByteValue, level) ->
                    assertEquals(
                        level,
                        uByteValue.toPasswordStrengthOrNull(),
                    )
                }
        }

        @Test
        fun `toPasswordStrengthOrNull returns null outside the 0 to 4 range`() {
            listOf(5.toUByte(), 6.toUByte()).forEach { uByteValue ->
                assertNull(
                    uByteValue.toPasswordStrengthOrNull(),
                )
            }
        }

        @Test
        fun `toUByte returns the correct UByte for each level`() {
            mapOf(
                PasswordStrength.LEVEL_0 to 0.toUByte(),
                PasswordStrength.LEVEL_1 to 1.toUByte(),
                PasswordStrength.LEVEL_2 to 2.toUByte(),
                PasswordStrength.LEVEL_3 to 3.toUByte(),
                PasswordStrength.LEVEL_4 to 4.toUByte(),
            )
                .forEach { (level, uByteValue) ->
                    assertEquals(
                        uByteValue,
                        level.toUByte(),
                    )
                }
        }
    }

    @Nested
    inner class UIntType {
        @Test
        fun `toPasswordStrengthOrNull returns the correct values in 0 to 4 range`() {
            mapOf(
                0u to PasswordStrength.LEVEL_0,
                1u to PasswordStrength.LEVEL_1,
                2u to PasswordStrength.LEVEL_2,
                3u to PasswordStrength.LEVEL_3,
                4u to PasswordStrength.LEVEL_4,
            )
                .forEach { (uIntValue, level) ->
                    assertEquals(
                        level,
                        uIntValue.toPasswordStrengthOrNull(),
                    )
                }
        }

        @Test
        fun `toPasswordStrengthOrNull returns null outside the 0 to 4 range`() {
            listOf(5u, 6u).forEach { uIntValue ->
                assertNull(
                    uIntValue.toPasswordStrengthOrNull(),
                )
            }
        }

        @Test
        fun `toUInt returns the correct UInt for each level`() {
            mapOf(
                PasswordStrength.LEVEL_0 to 0u,
                PasswordStrength.LEVEL_1 to 1u,
                PasswordStrength.LEVEL_2 to 2u,
                PasswordStrength.LEVEL_3 to 3u,
                PasswordStrength.LEVEL_4 to 4u,
            )
                .forEach { (level, uIntValue) ->
                    assertEquals(
                        uIntValue,
                        level.toUInt(),
                    )
                }
        }
    }
}
