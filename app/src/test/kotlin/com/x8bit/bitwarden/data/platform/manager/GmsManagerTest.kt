package com.x8bit.bitwarden.data.platform.manager

import android.content.Context
import com.google.android.gms.common.GoogleApiAvailabilityLight
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GmsManagerTest {

    private val context: Context = mockk()
    private val mockGoogleApiAvailabilityLight: GoogleApiAvailabilityLight = mockk()
    private val gmsManager = GmsManagerImpl(context = context)

    @BeforeEach
    fun setUp() {
        mockkStatic(GoogleApiAvailabilityLight::class)
        every {
            GoogleApiAvailabilityLight.getInstance()
        } returns mockGoogleApiAvailabilityLight
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(GoogleApiAvailabilityLight::class)
    }

    @Test
    fun `isVersionAtLeast should return true when installed version equals required version`() {
        every { mockGoogleApiAvailabilityLight.getApkVersion(context) } returns 261031035
        assertTrue(gmsManager.isVersionAtLeast(261031035))
    }

    @Test
    fun `isVersionAtLeast should return true when installed version exceeds required version`() {
        every { mockGoogleApiAvailabilityLight.getApkVersion(context) } returns 261031036
        assertTrue(gmsManager.isVersionAtLeast(261031035))
    }

    @Test
    fun `isVersionAtLeast should return false when installed version is below required version`() {
        every { mockGoogleApiAvailabilityLight.getApkVersion(context) } returns 261031034
        assertFalse(gmsManager.isVersionAtLeast(261031035))
    }

    @Test
    fun `isVersionAtLeast should return false when GMS is not installed`() {
        every { mockGoogleApiAvailabilityLight.getApkVersion(context) } returns 0
        assertFalse(gmsManager.isVersionAtLeast(261031035))
    }
}
