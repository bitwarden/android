package com.bitwarden.data.manager

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BitwardenPackageManagerTest {

    private val mockPackageManager: PackageManager = mockk()
    private val context: Context = mockk {
        every { packageManager } returns mockPackageManager
    }
    private val bitwardenPackageManager = BitwardenPackageManagerImpl(context)

    @Test
    fun `isPackageInstalled returns true for installed package`() {
        val packageName = "com.example.installed"
        every { mockPackageManager.getApplicationInfo(packageName, 0) } returns ApplicationInfo()
        val result = bitwardenPackageManager.isPackageInstalled(packageName)
        assertTrue(result)
    }

    @Test
    fun `isPackageInstalled returns false for non existent package`() {
        val packageName = "com.example.nonexistent"
        every {
            mockPackageManager.getApplicationInfo(packageName, 0)
        } throws PackageManager.NameNotFoundException()
        val result = bitwardenPackageManager.isPackageInstalled(packageName)
        assertFalse(result)
    }

    @Test
    fun `isPackageInstalled handles empty package name`() {
        val packageName = ""
        every {
            mockPackageManager.getApplicationInfo(packageName, 0)
        } throws PackageManager.NameNotFoundException()
        val result = bitwardenPackageManager.isPackageInstalled(packageName)
        assertFalse(result)
    }

    @Test
    fun `isPackageInstalled handles package name with special characters`() {
        val packageName = "com.example.invalid name!"
        every {
            mockPackageManager.getApplicationInfo(packageName, 0)
        } throws PackageManager.NameNotFoundException()
        val result = bitwardenPackageManager.isPackageInstalled(packageName)
        assertFalse(result)
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
}
