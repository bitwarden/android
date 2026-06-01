package com.bitwarden.ui.platform.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LongExtensionsTest {
    @Test
    fun `formatBytes should return string in appropriate format`() {
        // Bytes
        assertEquals("0 B", 0L.formatBytes())
        assertEquals("500 B", 500L.formatBytes())
        assertEquals("1023 B", 1_023L.formatBytes())
        // Kibibytes
        assertEquals("1.00 KB", 1_024L.formatBytes())
        assertEquals("21.51 KB", 22_024L.formatBytes())
        assertEquals("591.82 KB", 606_024L.formatBytes())
        // Mebibytes
        assertEquals("1.00 MB", 1_048_576L.formatBytes())
        assertEquals("3.27 MB", 3_425_346L.formatBytes())
        assertEquals("477.24 MB", 500_425_346L.formatBytes())
        // Gibibytes
        assertEquals("1.00 GB", 1_073_741_824L.formatBytes())
        assertEquals("1.07 GB", 1_151_461_496L.formatBytes())
        assertEquals("52.08 GB", 55_917_186_986L.formatBytes())
        // Tebibytes
        assertEquals("1.00 TB", 1_099_511_627_776L.formatBytes())
        assertEquals("12.49 TB", 13_732_900_230_922L.formatBytes())
        assertEquals("2000.00 TB", 2_199_023_255_552_000L.formatBytes())
    }
}
