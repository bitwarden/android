package com.x8bit.bitwarden.ui.autofill

import android.content.res.Resources
import android.widget.RemoteViews
import com.x8bit.bitwarden.R
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.unmockkConstructor
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BitwardenRemoteViewsTest {
    private val testResources: Resources = mockk()

    @BeforeEach
    fun setup() {
        mockkConstructor(RemoteViews::class)
    }

    @AfterEach
    fun teardown() {
        unmockkConstructor(RemoteViews::class)
        confirmVerified(
            testResources,
        )
    }

    @Test
    fun `buildAutofillRemoteViews should set text`() {
        // Setup
        val title = "Bitwarden"
        every {
            anyConstructed<RemoteViews>()
                .setTextViewText(
                    R.id.text,
                    title,
                )
        } just runs

        // Test
        buildAutofillRemoteViews(
            title = title,
            packageName = PACKAGE_NAME,
        )

        // Note: impossible to do a useful test of the returned RemoteViews due to mockking
        // constraints of the [RemoteViews] constructor. Our best bet is to make sure the correct
        // operations are performed on the constructed [RemoteViews].

        // Verify
        verify(exactly = 1) {
            anyConstructed<RemoteViews>()
                .setTextViewText(
                    R.id.text,
                    title,
                )
        }
    }

    companion object {
        private const val PACKAGE_NAME: String = "com.x8bit.bitwarden"
    }
}
