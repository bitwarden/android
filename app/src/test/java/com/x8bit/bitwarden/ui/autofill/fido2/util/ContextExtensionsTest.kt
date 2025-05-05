package com.x8bit.bitwarden.ui.autofill.fido2.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
import androidx.core.graphics.drawable.IconCompat
import com.bumptech.glide.Glide
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException

class ContextExtensionsTest {

    val mockContext = mockk<Context>()

    @BeforeEach
    fun setUp() {
        mockkStatic(
            Glide::class,
            Icon::class,
            IconCompat::class,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            Glide::class,
            Icon::class,
            IconCompat::class,
            Uri::class,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `createFido2IconCompatFromIconDataOrDefault should return default icon when IconData is null`() =
        runTest {
            every { IconCompat.createWithResource(mockContext, 0) } returns IconCompat()
            mockContext.createFido2IconCompatFromIconDataOrDefault(
                iconData = null,
                defaultResourceId = 0,
            )
            verify { IconCompat.createWithResource(mockContext, 0) }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `createFido2IconCompatFromIconDataOrDefault should return default icon when IconData is not Network type`() =
        runTest {
            every { IconCompat.createWithResource(mockContext, 0) } returns IconCompat()
            mockContext.createFido2IconCompatFromIconDataOrDefault(
                iconData = IconData.Local(0),
                defaultResourceId = 0,
            )
            verify { IconCompat.createWithResource(mockContext, 0) }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `createFido2IconCompatFromIconDataOrDefault should load icon from URI when IconData is Network type`() =
        runTest {
            val mockBitmap = mockk<Bitmap>()
            setupMockGlide(mockBitmap)
            every { IconCompat.createWithBitmap(mockBitmap) } returns mockk()
            mockContext.createFido2IconCompatFromIconDataOrDefault(
                iconData = IconData.Network("https://www.mockuri.com", 0),
                defaultResourceId = 0,
            )
            verify { IconCompat.createWithBitmap(mockBitmap) }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `createFido2IconCompatFromIconDataOrDefault should return default icon when loading from URI throws CancellationException`() =
        runTest {
            val mockBitmap = mockk<Bitmap>()
            setupMockGlide(mockBitmap)
            every { IconCompat.createWithResource(mockContext, 0) } returns mockk()
            every { IconCompat.createWithBitmap(mockBitmap) } throws CancellationException()
            mockContext.createFido2IconCompatFromIconDataOrDefault(
                iconData = IconData.Network("https://www.mockuri.com", 0),
                defaultResourceId = 0,
            )
            verify { IconCompat.createWithResource(mockContext, 0) }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `createFido2IconCompatFromIconDataOrDefault should return default icon when loading from URI throws ExecutionException`() =
        runTest {
            val mockBitmap = mockk<Bitmap>()
            setupMockGlide(mockBitmap)
            every { IconCompat.createWithResource(mockContext, 0) } returns mockk()
            every {
                IconCompat.createWithBitmap(mockBitmap)
            } throws ExecutionException("message", Throwable())

            mockContext.createFido2IconCompatFromIconDataOrDefault(
                iconData = IconData.Network("https://www.mockuri.com", 0),
                defaultResourceId = 0,
            )
            verify { IconCompat.createWithResource(mockContext, 0) }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `createFido2IconCompatFromIconDataOrDefault should return default icon when loading from URI throws InterruptedException`() =
        runTest {
            val mockBitmap = mockk<Bitmap>()
            setupMockGlide(mockBitmap)
            every { IconCompat.createWithResource(mockContext, 0) } returns mockk()
            every { IconCompat.createWithBitmap(mockBitmap) } throws InterruptedException()
            mockContext.createFido2IconCompatFromIconDataOrDefault(
                iconData = IconData.Network("https://www.mockuri.com", 0),
                defaultResourceId = 0,
            )
            verify { IconCompat.createWithResource(mockContext, 0) }
        }

    private fun setupMockGlide(mockBitmap: Bitmap) {
        every { Glide.with(mockContext) } returns mockk {
            every { asBitmap() } returns mockk {
                every { load(any<String>()) } returns mockk {
                    every { placeholder(0) } returns mockk {
                        every { submit() } returns mockk {
                            every { get() } returns mockBitmap
                        }
                    }
                }
            }
        }
    }
}
