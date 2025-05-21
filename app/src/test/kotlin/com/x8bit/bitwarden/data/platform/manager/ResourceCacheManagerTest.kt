package com.x8bit.bitwarden.data.platform.manager

import android.content.Context
import android.content.res.Resources
import com.x8bit.bitwarden.R
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ResourceCacheManagerTest {
    private val resources: Resources = mockk {
        every { getStringArray(R.array.exception_suffixes) } returns arrayOf("uk")
        every { getStringArray(R.array.normal_suffixes) } returns arrayOf("co.uk")
        every { getStringArray(R.array.wild_card_suffixes) } returns arrayOf("amazonaws.com")
    }
    private val context: Context = mockk {
        every { this@mockk.resources } returns this@ResourceCacheManagerTest.resources
    }

    private val resourceCacheManager: ResourceCacheManager = ResourceCacheManagerImpl(
        context = context,
    )

    @Test
    fun `domainExceptionSuffixes should return cached value the second time`() {
        val expected = listOf("uk")

        val firstResult = resourceCacheManager.domainExceptionSuffixes

        assertEquals(expected, firstResult)
        verify(exactly = 1) {
            context.resources
            resources.getStringArray(R.array.exception_suffixes)
        }
        clearMocks(context, resources)

        val secondResult = resourceCacheManager.domainExceptionSuffixes

        assertEquals(expected, secondResult)
        verify(exactly = 0) {
            context.resources
            resources.getStringArray(R.array.exception_suffixes)
        }
    }

    @Test
    fun `domainNormalSuffixes should return cached value the second time`() {
        val expected = listOf("co.uk")

        val firstResult = resourceCacheManager.domainNormalSuffixes

        assertEquals(expected, firstResult)
        verify(exactly = 1) {
            context.resources
            resources.getStringArray(R.array.normal_suffixes)
        }
        clearMocks(context, resources)

        val secondResult = resourceCacheManager.domainNormalSuffixes

        assertEquals(expected, secondResult)
        verify(exactly = 0) {
            context.resources
            resources.getStringArray(R.array.normal_suffixes)
        }
    }

    @Test
    fun `domainWildCardSuffixes should return cached value the second time`() {
        val expected = listOf("amazonaws.com")

        val firstResult = resourceCacheManager.domainWildCardSuffixes

        assertEquals(expected, firstResult)
        verify(exactly = 1) {
            context.resources
            resources.getStringArray(R.array.wild_card_suffixes)
        }
        clearMocks(context, resources)

        val secondResult = resourceCacheManager.domainWildCardSuffixes

        assertEquals(expected, secondResult)
        verify(exactly = 0) {
            context.resources
            resources.getStringArray(R.array.wild_card_suffixes)
        }
    }
}
