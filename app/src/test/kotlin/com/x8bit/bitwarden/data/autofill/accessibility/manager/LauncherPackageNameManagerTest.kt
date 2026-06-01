package com.x8bit.bitwarden.data.autofill.accessibility.manager

import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class LauncherPackageNameManagerTest {
    private val packageManager: PackageManager = mockk()
    private var mutableClock: Clock = FIXED_CLOCK

    private val launcherPackageNameManager: LauncherPackageNameManager =
        LauncherPackageNameManagerImpl(
            clockProvider = { mutableClock },
            packageManager = packageManager,
        )

    @Suppress("MaxLineLength")
    @Test
    fun `launcherPackages should populate cache on first attempt and use cached value for second attempt and repopulate the cache after an hour`() {
        val testPackageName = "testPackageName"
        val testActivityInfo = ActivityInfo().apply {
            packageName = testPackageName
        }
        val resolveInfo = ResolveInfo().apply {
            activityInfo = testActivityInfo
        }
        val packages = listOf(resolveInfo)
        every { packageManager.queryIntentActivities(any(), any<Int>()) } returns packages

        val firstResult = launcherPackageNameManager.launcherPackages

        assertEquals(listOf(testPackageName), firstResult)
        verify(exactly = 1) {
            packageManager.queryIntentActivities(any(), any<Int>())
        }
        clearMocks(packageManager)

        val secondResult = launcherPackageNameManager.launcherPackages

        assertEquals(listOf(testPackageName), secondResult)
        verify(exactly = 0) {
            packageManager.queryIntentActivities(any(), any<Int>())
        }
        clearMocks(packageManager)

        every { packageManager.queryIntentActivities(any(), any<Int>()) } returns emptyList()
        mutableClock = Clock.offset(FIXED_CLOCK, Duration.ofMinutes(61))
        val thirdResult = launcherPackageNameManager.launcherPackages

        assertEquals(emptyList<String>(), thirdResult)
        verify(exactly = 1) {
            packageManager.queryIntentActivities(any(), any<Int>())
        }
    }
}

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)
