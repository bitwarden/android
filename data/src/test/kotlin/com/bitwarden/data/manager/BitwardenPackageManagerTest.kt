package com.bitwarden.data.manager

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.bitwarden.core.util.isBuildVersionAtLeast
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BitwardenPackageManagerTest {

    private val mockPackageManager: PackageManager = mockk()
    private val context: Context = mockk {
        every { packageManager } returns mockPackageManager
    }
    private val bitwardenPackageManager = BitwardenPackageManagerImpl(context)

    @BeforeEach
    fun setUp() {
        mockkStatic(
            PackageManager.ApplicationInfoFlags::of,
            ::isBuildVersionAtLeast,
        )
        // Set the default API level to simulate the latest version
        every { isBuildVersionAtLeast(any()) } returns true
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            PackageManager.ApplicationInfoFlags::of,
            ::isBuildVersionAtLeast,
        )
    }

    @Test
    fun `isPackageInstalled returns true for installed package`() {
        val packageName = "com.example.installed"
        every {
            mockPackageManager.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(0L),
            )
        } returns ApplicationInfo()
        val result = bitwardenPackageManager.isPackageInstalled(packageName)
        assertTrue(result)
    }

    @Test
    fun `isPackageInstalled returns false for non existent package`() {
        val packageName = "com.example.nonexistent"
        every {
            mockPackageManager.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(0L),
            )
        } throws PackageManager.NameNotFoundException()
        val result = bitwardenPackageManager.isPackageInstalled(packageName)
        assertFalse(result)
    }

    @Test
    fun `isPackageInstalled handles empty package name`() {
        val packageName = ""
        every {
            mockPackageManager.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(0L),
            )
        } throws PackageManager.NameNotFoundException()
        val result = bitwardenPackageManager.isPackageInstalled(packageName)
        assertFalse(result)
    }

    @Test
    fun `isPackageInstalled handles package name with special characters`() {
        val packageName = "com.example.invalid name!"
        every {
            mockPackageManager.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(0L),
            )
        } throws PackageManager.NameNotFoundException()
        val result = bitwardenPackageManager.isPackageInstalled(packageName)
        assertFalse(result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isPackageInstalled invokes correct getApplicationInfo overload when API level is below 33`() {
        val packageName = "com.example.installed"
        every { isBuildVersionAtLeast(33) } returns false
        every {
            mockPackageManager.getApplicationInfo(packageName, 0)
        } returns mockk()
        bitwardenPackageManager.isPackageInstalled(packageName)
        verify { mockPackageManager.getApplicationInfo(packageName, 0) }
    }

    @Test
    fun `getAppLabelForPackageOrNull returns correct label for installed package`() {
        val packageName = "com.example.installed"
        val appLabel = "Example App"
        val applicationInfo = ApplicationInfo()
        every { mockPackageManager.getApplicationInfo(packageName, 0) } returns applicationInfo
        every { mockPackageManager.getApplicationLabel(applicationInfo) } returns appLabel
        val result = bitwardenPackageManager.getAppLabelForPackageOrNull(packageName)
        assertEquals(appLabel, result)
    }

    @Test
    fun `getAppLabelForPackageOrNull returns null for non existent package`() {
        val packageName = "com.example.nonexistent"
        every {
            mockPackageManager.getApplicationInfo(packageName, 0)
        } throws PackageManager.NameNotFoundException()
        val result = bitwardenPackageManager.getAppLabelForPackageOrNull(packageName)
        assertNull(result)
    }

    @Test
    fun `getAppLabelForPackageOrNull handles package with no label`() {
        val packageName = "com.example.nolabel"
        val applicationInfo = ApplicationInfo()
        every { mockPackageManager.getApplicationInfo(packageName, 0) } returns applicationInfo
        every { mockPackageManager.getApplicationLabel(applicationInfo) } returns ""
        val result = bitwardenPackageManager.getAppLabelForPackageOrNull(packageName)
        assertEquals("", result)
    }

    @Test
    fun `getAppLabelForPackageOrNull handles empty package name`() {
        val packageName = ""
        every {
            mockPackageManager.getApplicationInfo(packageName, 0)
        } throws PackageManager.NameNotFoundException()
        val result = bitwardenPackageManager.getAppLabelForPackageOrNull(packageName)
        assertNull(result)
    }

    @Test
    fun `getAppLabelForPackageOrNull handles package name with special characters`() {
        val packageName = "com.example.invalid name!"
        every {
            mockPackageManager.getApplicationInfo(packageName, 0)
        } throws PackageManager.NameNotFoundException()
        val result = bitwardenPackageManager.getAppLabelForPackageOrNull(packageName)
        assertNull(result)
    }

    @Test
    fun `getPackageInstallationSourceOrNull returns correct source for installed package`() {
        val packageName = "com.example.installed"
        val installationSource = "com.example.source"
        every { mockPackageManager.getInstallSourceInfo(packageName) } returns mockk {
            every { installingPackageName } returns installationSource
        }
        val result = bitwardenPackageManager.getPackageInstallationSourceOrNull(packageName)
        assertEquals(installationSource, result)
    }

    @Test
    fun `getPackageInstallationSourceOrNull returns null when installation source is null`() {
        val packageName = "com.example.installed"
        every {
            mockPackageManager.getInstallSourceInfo(packageName)
        } returns mockk {
            every { installingPackageName } returns null
        }
        val result = bitwardenPackageManager.getPackageInstallationSourceOrNull(packageName)
        assertNull(result)
    }

    @Test
    fun `getPackageInstallationSourceOrNull returns null for non existent package`() {
        val packageName = "com.example.nonexistent"
        every {
            mockPackageManager.getInstallSourceInfo(packageName)
        } throws PackageManager.NameNotFoundException()
        val result = bitwardenPackageManager.getPackageInstallationSourceOrNull(packageName)
        assertNull(result)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `getPackageInstallationSourceOrNull invokes getInstallerPackageName on API 29 or lower`() {
        val packageName = "com.example.installed"
        val installationSource = "com.example.source"
        every { isBuildVersionAtLeast(30) } returns false
        every { mockPackageManager.getInstallerPackageName(packageName) } returns installationSource
        bitwardenPackageManager.getPackageInstallationSourceOrNull(packageName)
        verify { mockPackageManager.getInstallerPackageName(packageName) }
    }

    @Test
    fun `getPackageInstallationSourceOrNull invokes getInstallSourceInfo on API 30 or higher`() {
        val packageName = "com.example.installed"
        val installationSource = "com.example.source"
        every { mockPackageManager.getInstallSourceInfo(packageName) } returns mockk {
            every { installingPackageName } returns installationSource
        }
        bitwardenPackageManager.getPackageInstallationSourceOrNull(packageName)
        verify { mockPackageManager.getInstallSourceInfo(packageName) }
    }
}
