package com.x8bit.bitwarden.data.autofill.accessibility.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.x8bit.bitwarden.LEGACY_ACCESSIBILITY_SERVICE_NAME
import com.x8bit.bitwarden.LEGACY_SHORT_ACCESSIBILITY_SERVICE_NAME
import com.x8bit.bitwarden.data.autofill.accessibility.BitwardenAccessibilityService
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ContextExtensionsTest {

    @BeforeEach
    fun setup() {
        mockkStatic(Settings.Secure::getString)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Settings.Secure::getString)
    }

    @Test
    fun `isAccessibilityServiceEnabled with null package name returns false`() {
        val context: Context = mockk {
            every { applicationContext } returns this
            every { packageName } returns null
            every { contentResolver } returns mockk()
            every { getSystemService(AccessibilityManager::class.java) } returns null
        }

        assertFalse(context.isAccessibilityServiceEnabled)
    }

    @Test
    fun `isAccessibilityServiceEnabled with null secure string returns false`() {
        val context: Context = mockk {
            every { applicationContext } returns this
            every { packageName } returns "com.x8bit.bitwarden"
            every { contentResolver } returns mockk()
            every { getSystemService(AccessibilityManager::class.java) } returns null
        }
        mockkSettingsSecureGetString(value = null)

        assertFalse(context.isAccessibilityServiceEnabled)
    }

    @Test
    fun `isAccessibilityServiceEnabled with incorrect secure string returns false`() {
        val context: Context = mockk {
            every { applicationContext } returns this
            every { packageName } returns "com.x8bit.bitwarden"
            every { contentResolver } returns mockk()
            every { getSystemService(AccessibilityManager::class.java) } returns null
        }
        @Suppress("MaxLineLength")
        mockkSettingsSecureGetString(
            value = "com.x8bit.bitwarden.dev/com.x8bit.bitwarden.Accessibility.AccessibilityService",
        )

        assertFalse(context.isAccessibilityServiceEnabled)
    }

    @Test
    fun `isAccessibilityServiceEnabled with correct secure string returns true`() {
        val context: Context = mockk {
            every { applicationContext } returns this
            every { packageName } returns "com.x8bit.bitwarden"
            every { contentResolver } returns mockk()
            every { getSystemService(AccessibilityManager::class.java) } returns null
        }
        mockkSettingsSecureGetString(
            value = "com.x8bit.bitwarden/com.x8bit.bitwarden.Accessibility.AccessibilityService",
        )

        assertTrue(context.isAccessibilityServiceEnabled)
    }

    @Test
    fun `isAccessibilityServiceEnabled with correct abbreviated secure string returns true`() {
        val context: Context = mockk {
            every { applicationContext } returns this
            every { packageName } returns "com.x8bit.bitwarden"
            every { contentResolver } returns mockk()
            every { getSystemService(AccessibilityManager::class.java) } returns null
        }
        mockkSettingsSecureGetString(
            value = "com.x8bit.bitwarden/.Accessibility.AccessibilityService",
        )

        assertTrue(context.isAccessibilityServiceEnabled)
    }

    @Test
    fun `isAccessibilityServiceEnabled with matching enabled service returns true`() {
        val context: Context = mockk {
            every { applicationContext } returns this
            every { packageName } returns "com.x8bit.bitwarden"
            every { contentResolver } returns mockk()
            every {
                getSystemService(AccessibilityManager::class.java)
            } returns mockkAccessibilityManager(
                enabledServices = listOf(
                    createAccessibilityServiceInfo(
                        servicePackageName = "com.x8bit.bitwarden",
                        serviceName = BitwardenAccessibilityService::class.java.name,
                    ),
                ),
            )
        }

        assertTrue(context.isAccessibilityServiceEnabled)
    }

    @Test
    fun `isAccessibilityServiceEnabled with legacy enabled service name returns true`() {
        val context: Context = mockk {
            every { applicationContext } returns this
            every { packageName } returns "com.x8bit.bitwarden"
            every { contentResolver } returns mockk()
            every {
                getSystemService(AccessibilityManager::class.java)
            } returns mockkAccessibilityManager(
                enabledServices = listOf(
                    createAccessibilityServiceInfo(
                        servicePackageName = "com.x8bit.bitwarden",
                        serviceName = LEGACY_ACCESSIBILITY_SERVICE_NAME,
                    ),
                ),
            )
        }

        assertTrue(context.isAccessibilityServiceEnabled)
    }

    @Test
    fun `isAccessibilityServiceEnabled with legacy short enabled service name returns true`() {
        val context: Context = mockk {
            every { applicationContext } returns this
            every { packageName } returns "com.x8bit.bitwarden"
            every { contentResolver } returns mockk()
            every {
                getSystemService(AccessibilityManager::class.java)
            } returns mockkAccessibilityManager(
                enabledServices = listOf(
                    createAccessibilityServiceInfo(
                        servicePackageName = "com.x8bit.bitwarden",
                        serviceName = LEGACY_SHORT_ACCESSIBILITY_SERVICE_NAME,
                    ),
                ),
            )
        }

        assertTrue(context.isAccessibilityServiceEnabled)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isAccessibilityServiceEnabled with non-matching enabled service falls back to secure string`() {
        val context: Context = mockk {
            every { applicationContext } returns this
            every { packageName } returns "com.x8bit.bitwarden"
            every { contentResolver } returns mockk()
            every {
                getSystemService(AccessibilityManager::class.java)
            } returns mockkAccessibilityManager(
                enabledServices = listOf(
                    createAccessibilityServiceInfo(
                        servicePackageName = "com.other.app",
                        serviceName = "com.other.app.SomeService",
                    ),
                ),
            )
        }
        mockkSettingsSecureGetString(value = null)

        assertFalse(context.isAccessibilityServiceEnabled)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isAccessibilityServiceEnabled with matching package but wrong service name falls back to secure string`() {
        val context: Context = mockk {
            every { applicationContext } returns this
            every { packageName } returns "com.x8bit.bitwarden"
            every { contentResolver } returns mockk()
            every {
                getSystemService(AccessibilityManager::class.java)
            } returns mockkAccessibilityManager(
                enabledServices = listOf(
                    createAccessibilityServiceInfo(
                        servicePackageName = "com.x8bit.bitwarden",
                        serviceName = "com.x8bit.bitwarden.SomeOtherService",
                    ),
                ),
            )
        }
        mockkSettingsSecureGetString(
            value = "com.x8bit.bitwarden/com.x8bit.bitwarden.Accessibility.AccessibilityService",
        )

        assertTrue(context.isAccessibilityServiceEnabled)
    }

    @Test
    fun `isAccessibilityServiceEnabled with empty enabled service list returns false`() {
        val context: Context = mockk {
            every { applicationContext } returns this
            every { packageName } returns "com.x8bit.bitwarden"
            every { contentResolver } returns mockk()
            every {
                getSystemService(AccessibilityManager::class.java)
            } returns mockkAccessibilityManager(enabledServices = emptyList())
        }
        mockkSettingsSecureGetString(value = null)

        assertFalse(context.isAccessibilityServiceEnabled)
    }

    @Test
    fun `isAccessibilityServiceEnabled with null enabled service list falls back to secure string`() {
        val context: Context = mockk {
            every { applicationContext } returns this
            every { packageName } returns "com.x8bit.bitwarden"
            every { contentResolver } returns mockk()
            every {
                getSystemService(AccessibilityManager::class.java)
            } returns mockk {
                every {
                    getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                } returns null
            }
        }
        mockkSettingsSecureGetString(value = null)

        assertFalse(context.isAccessibilityServiceEnabled)
    }

    @Test
    fun `isAccessibilityServiceEnabled with null serviceInfo falls back to secure string`() {
        val context: Context = mockk {
            every { applicationContext } returns this
            every { packageName } returns "com.x8bit.bitwarden"
            every { contentResolver } returns mockk()
            every {
                getSystemService(AccessibilityManager::class.java)
            } returns mockkAccessibilityManager(
                enabledServices = listOf(
                    mockk<AccessibilityServiceInfo> {
                        every { resolveInfo } returns ResolveInfo()
                    },
                ),
            )
        }
        mockkSettingsSecureGetString(value = null)

        assertFalse(context.isAccessibilityServiceEnabled)
    }

    @Test
    fun `isAccessibilityServiceEnabled with null resolveInfo falls back to secure string`() {
        val context: Context = mockk {
            every { applicationContext } returns this
            every { packageName } returns "com.x8bit.bitwarden"
            every { contentResolver } returns mockk()
            every {
                getSystemService(AccessibilityManager::class.java)
            } returns mockkAccessibilityManager(
                enabledServices = listOf(
                    mockk<AccessibilityServiceInfo> {
                        every { resolveInfo } returns null
                    },
                ),
            )
        }
        mockkSettingsSecureGetString(value = null)

        assertFalse(context.isAccessibilityServiceEnabled)
    }

    private fun mockkAccessibilityManager(
        enabledServices: List<AccessibilityServiceInfo>,
    ): AccessibilityManager =
        mockk {
            every {
                getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            } returns enabledServices
        }

    private fun createAccessibilityServiceInfo(
        servicePackageName: String,
        serviceName: String,
    ): AccessibilityServiceInfo =
        mockk {
            every { resolveInfo } returns ResolveInfo().apply {
                serviceInfo = ServiceInfo().apply {
                    packageName = servicePackageName
                    name = serviceName
                }
            }
        }

    private fun mockkSettingsSecureGetString(value: String?) {
        every {
            Settings.Secure.getString(any(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        } returns value
    }
}
