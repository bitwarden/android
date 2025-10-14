package com.bitwarden.ui.platform.manager.share

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.net.Uri
import com.bitwarden.core.data.manager.BuildInfoManager
import com.bitwarden.ui.platform.manager.share.model.ShareData
import com.bitwarden.ui.platform.model.FileData
import com.bitwarden.ui.platform.util.getLocalFileData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ShareManagerTest {

    private val context: Context = mockk()
    private val buildInfoManager: BuildInfoManager = mockk {
        every { applicationId } returns APP_ID
    }

    private val shareManager: ShareManager = ShareManagerImpl(
        context = context,
        buildInfoManager = buildInfoManager,
    )

    @BeforeEach
    fun setup() {
        mockkStatic(Uri::class)
        mockkStatic(context::getLocalFileData)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Uri::class)
        unmockkStatic(context::getLocalFileData)
    }

    @Test
    fun `getShareDataOrNull with non-send action returns null`() {
        val intent = mockk<Intent> {
            every { action } returns Intent.ACTION_EDIT
        }

        val result = shareManager.getShareDataOrNull(intent = intent)

        assertNull(result)
    }

    @Test
    fun `getShareDataOrNull with send action, text type, and null text extra returns null`() {
        val intent = mockk<Intent> {
            every { action } returns Intent.ACTION_SEND
            every { type } returns "text/plain"
            every { getStringExtra(Intent.EXTRA_SUBJECT) } returns "subject"
            every { getStringExtra(Intent.EXTRA_TEXT) } returns null
        }

        val result = shareManager.getShareDataOrNull(intent = intent)

        assertNull(result)
    }

    @Test
    fun `getShareDataOrNull with send action, text type, and valid text extra returns TextSend`() {
        val subject = "subject"
        val text = "text"
        val intent = mockk<Intent> {
            every { action } returns Intent.ACTION_SEND
            every { type } returns "text/plain"
            every { getStringExtra(Intent.EXTRA_SUBJECT) } returns subject
            every { getStringExtra(Intent.EXTRA_TEXT) } returns text
        }

        val result = shareManager.getShareDataOrNull(intent = intent)

        assertEquals(ShareData.TextSend(subject = subject, text = text), result)
    }

    @Test
    fun `getShareDataOrNull with send action, non-text type, and no clip data returns null`() {
        val intent = mockk<Intent> {
            every { action } returns Intent.ACTION_SEND
            every { type } returns "image/jpeg"
            every { clipData } returns null
        }

        val result = shareManager.getShareDataOrNull(intent = intent)

        assertNull(result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getShareDataOrNull with send action, non-text type, and clip data from internal provider returns null`() {
        val packageInfo = PackageInfo().apply {
            providers = arrayOf(ProviderInfo().apply { authority = AUTHORITY })
        }
        val packageManager = mockk<PackageManager> {
            every { getPackageInfo(APP_ID, PackageManager.GET_PROVIDERS) } returns packageInfo
        }
        every { context.packageManager } returns packageManager
        val intent = mockk<Intent> {
            every { action } returns Intent.ACTION_SEND
            every { type } returns "image/jpeg"
            every { clipData } returns mockk {
                every { getItemAt(0) } returns mockk {
                    every { uri } returns createMockUri(url = AUTHORITY)
                }
            }
        }

        val result = shareManager.getShareDataOrNull(intent = intent)

        assertNull(result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getShareDataOrNull with send action, non-text type, clip data from external provider, and no local file data returns null`() {
        val packageInfo = PackageInfo().apply {
            providers = arrayOf(ProviderInfo().apply { authority = "com.other.provider" })
        }
        val packageManager = mockk<PackageManager> {
            every { getPackageInfo(APP_ID, PackageManager.GET_PROVIDERS) } returns packageInfo
        }
        every { context.packageManager } returns packageManager
        val mockUri = createMockUri(url = AUTHORITY)
        every { context.getLocalFileData(uri = mockUri) } returns null
        val intent = mockk<Intent> {
            every { action } returns Intent.ACTION_SEND
            every { type } returns "image/jpeg"
            every { clipData } returns mockk {
                every { getItemAt(0) } returns mockk {
                    every { uri } returns mockUri
                }
            }
        }

        val result = shareManager.getShareDataOrNull(intent = intent)

        assertNull(result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getShareDataOrNull with send action, non-text type, clip data from external provider, and local file data returns FileData`() {
        val packageInfo = PackageInfo().apply {
            providers = arrayOf(ProviderInfo().apply { authority = "com.other.provider" })
        }
        val packageManager = mockk<PackageManager> {
            every { getPackageInfo(APP_ID, PackageManager.GET_PROVIDERS) } returns packageInfo
        }
        every { context.packageManager } returns packageManager
        val mockUri = createMockUri(url = AUTHORITY)
        val fileData = FileData(
            fileName = "file.jpg",
            uri = mockUri,
            sizeBytes = 1024,
        )
        every { context.getLocalFileData(uri = mockUri) } returns fileData
        val intent = mockk<Intent> {
            every { action } returns Intent.ACTION_SEND
            every { type } returns "image/jpeg"
            every { clipData } returns mockk {
                every { getItemAt(0) } returns mockk {
                    every { uri } returns mockUri
                }
            }
        }

        val result = shareManager.getShareDataOrNull(intent = intent)

        assertEquals(ShareData.FileSend(fileData = fileData), result)
    }
}

private const val APP_ID: String = "com.x8bit.bitwarden"
private const val AUTHORITY: String = "$APP_ID.fileprovider"

private fun createMockUri(url: String): Uri {
    val mockUri = mockk<Uri> {
        every { this@mockk.toString() } returns url
    }
    every { Uri.parse(url) } returns mockUri
    return mockUri
}
