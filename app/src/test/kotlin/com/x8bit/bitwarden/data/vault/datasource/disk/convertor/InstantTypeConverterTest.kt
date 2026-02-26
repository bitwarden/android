package com.x8bit.bitwarden.data.vault.datasource.disk.convertor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class InstantTypeConverterTest {

    private val typeConverter = InstantTypeConverter()

    @Test
    fun `fromTimestamp should return null when value is null`() {
        val value: Long? = null

        val result = typeConverter.fromTimestamp(value)

        assertNull(result)
    }

    @Test
    fun `fromTimestamp should return correct Instant when value is not null`() {
        val expected = Instant.parse("2023-12-15T20:38:06Z")
        val value = expected.epochSecond

        val result = typeConverter.fromTimestamp(value)

        assertEquals(expected, result)
    }

    @Test
    fun `toTimestamp should return null when value is null`() {
        val value: Instant? = null

        val result = typeConverter.toTimestamp(value)

        assertNull(result)
    }

    @Test
    fun `toTimestamp should return correct Long when value is not null`() {
        val value = Instant.parse("2023-12-15T20:38:06Z")
        val expected = value.epochSecond

        val result = typeConverter.toTimestamp(value)

        assertEquals(expected, result)
    }
}
