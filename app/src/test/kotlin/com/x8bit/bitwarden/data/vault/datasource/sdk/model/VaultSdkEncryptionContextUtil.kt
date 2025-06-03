package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.vault.Cipher
import com.bitwarden.vault.EncryptionContext
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Default date time used for [ZonedDateTime] properties of mock objects.
 */
private const val DEFAULT_TIMESTAMP = "2023-10-27T12:00:00Z"
private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse(DEFAULT_TIMESTAMP),
    ZoneOffset.UTC,
)

/**
 * Create a mock [EncryptionContext] with a given [number].
 */
fun createMockEncryptionContext(
    number: Int,
    cipher: Cipher = createMockSdkCipher(number, FIXED_CLOCK),
): EncryptionContext =
    EncryptionContext(
        encryptedFor = "mockEncryptedFor-$number",
        cipher = cipher,
    )
