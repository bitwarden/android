package com.bitwarden.core.data.repository.util

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Locale

class SpecialCharWithPrecedenceComparatorTest {

    private lateinit var defaultLocale: Locale

    @BeforeEach
    fun setup() {
        defaultLocale = Locale.getDefault()
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `Sorting with comparator should return expected result of sorted string`() {
        val unsortedList = listOf(
            "__Za",
            "z",
            "___",
            "1a3",
            "aBc",
            "__a",
            "__A",
            "__a",
            "__4",
            "Z",
            "__3",
            "Abc",
        )
        val expectedSortedList = listOf(
            "___",
            "__3",
            "__4",
            "__a",
            "__a",
            "__A",
            "__Za",
            "1a3",
            "aBc",
            "Abc",
            "z",
            "Z",
        )

        assertEquals(
            expectedSortedList,
            unsortedList.sortedWith(SpecialCharWithPrecedenceComparator),
        )
    }

    @Test
    fun `comparator should return consistent values across locales`() {
        val unsortedList = listOf("i", "z", "j")
        val sortedList = listOf("i", "j", "z")
        val locales = listOf(
            Locale.forLanguageTag("tr-TR"),
            Locale.US,
        )

        locales.forEach { locale ->
            Locale.setDefault(locale)
            assertEquals(
                sortedList,
                unsortedList.sortedWith(SpecialCharWithPrecedenceComparator),
            )
        }
    }
}
