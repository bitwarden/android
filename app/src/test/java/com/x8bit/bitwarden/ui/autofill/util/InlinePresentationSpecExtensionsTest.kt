package com.x8bit.bitwarden.ui.autofill.util

import android.app.PendingIntent
import android.app.slice.Slice
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Bundle
import android.widget.inline.InlinePresentationSpec
import androidx.autofill.inline.UiVersions
import androidx.autofill.inline.v1.InlineSuggestionUi
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InlinePresentationSpecExtensionsTest {
    private val testContext: Context = mockk()
    private val autofillAppInfo: AutofillAppInfo = mockk {
        every { this@mockk.context } returns testContext
    }
    private val testStyle: Bundle = mockk()
    private val inlinePresentationSpec: InlinePresentationSpec = mockk {
        every { this@mockk.style } returns testStyle
    }

    @BeforeEach
    fun setup() {
        mockkStatic(Context::isSystemDarkMode)
        mockkStatic(Icon::class)
        mockkStatic(InlineSuggestionUi::class)
        mockkStatic(PendingIntent::getService)
        mockkStatic(UiVersions::getVersions)
    }

    @AfterEach
    fun teardown() {
        mockkStatic(Context::isSystemDarkMode)
        unmockkStatic(Icon::class)
        unmockkStatic(InlineSuggestionUi::class)
        unmockkStatic(PendingIntent::getService)
        unmockkStatic(UiVersions::getVersions)
    }

    @Test
    fun `createCipherInlinePresentationOrNull should return null if incompatible`() {
        // Setup
        every {
            UiVersions.getVersions(testStyle)
        } returns emptyList()

        // Test
        val actual = inlinePresentationSpec.createCipherInlinePresentationOrNull(
            autofillAppInfo = autofillAppInfo,
            autofillCipher = mockk(),
        )

        // Verify
        Assertions.assertNull(actual)
        verify(exactly = 1) {
            UiVersions.getVersions(testStyle)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `createCipherInlinePresentationOrNull should return presentation with card icon when card cipher and compatible`() {
        // Setup
        val icon: Icon = mockk()
        val autofillCipher: AutofillCipher.Card = mockk {
            every { this@mockk.name } returns AUTOFILL_CIPHER_NAME
            every { this@mockk.subtitle } returns AUTOFILL_CIPHER_SUBTITLE
            every { this@mockk.iconRes } returns R.drawable.ic_card_item
        }
        val pendingIntent: PendingIntent = mockk()
        val slice: Slice = mockk()
        every {
            UiVersions.getVersions(testStyle)
        } returns listOf(UiVersions.INLINE_UI_VERSION_1)
        every {
            PendingIntent.getService(
                testContext,
                PENDING_INTENT_CODE,
                any<Intent>(),
                PENDING_INTENT_FLAGS,
            )
        } returns pendingIntent
        every { testContext.isSystemDarkMode } returns true
        every { testContext.getColor(R.color.dark_on_surface) } returns ICON_TINT
        every {
            Icon.createWithResource(
                testContext,
                R.drawable.ic_card_item,
            )
                .setTint(ICON_TINT)
        } returns icon
        every {
            InlineSuggestionUi.newContentBuilder(pendingIntent)
                .setTitle(AUTOFILL_CIPHER_NAME)
                .setSubtitle(AUTOFILL_CIPHER_SUBTITLE)
                .setStartIcon(icon)
                .build()
                .slice
        } returns slice

        // Test
        val actual = inlinePresentationSpec.createCipherInlinePresentationOrNull(
            autofillAppInfo = autofillAppInfo,
            autofillCipher = autofillCipher,
        )

        // Verify not-null because we can't properly mock Intent constructors.
        Assertions.assertNotNull(actual)
        verify(exactly = 1) {
            UiVersions.getVersions(testStyle)
            PendingIntent.getService(
                testContext,
                PENDING_INTENT_CODE,
                any<Intent>(),
                PENDING_INTENT_FLAGS,
            )
            testContext.isSystemDarkMode
            testContext.getColor(R.color.dark_on_surface)
            Icon.createWithResource(
                testContext,
                R.drawable.ic_card_item,
            )
                .setTint(ICON_TINT)
            InlineSuggestionUi.newContentBuilder(pendingIntent)
                .setTitle(AUTOFILL_CIPHER_NAME)
                .setSubtitle(AUTOFILL_CIPHER_SUBTITLE)
                .setStartIcon(icon)
                .build()
                .slice
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `createCipherInlinePresentationOrNull should return presentation with login icon when login cipher and compatible`() {
        // Setup
        val icon: Icon = mockk()
        val autofillCipher: AutofillCipher.Login = mockk {
            every { this@mockk.name } returns AUTOFILL_CIPHER_NAME
            every { this@mockk.subtitle } returns AUTOFILL_CIPHER_SUBTITLE
            every { this@mockk.iconRes } returns R.drawable.ic_login_item
        }
        val pendingIntent: PendingIntent = mockk()
        val slice: Slice = mockk()
        every {
            UiVersions.getVersions(testStyle)
        } returns listOf(UiVersions.INLINE_UI_VERSION_1)
        every {
            PendingIntent.getService(
                testContext,
                PENDING_INTENT_CODE,
                any<Intent>(),
                PENDING_INTENT_FLAGS,
            )
        } returns pendingIntent
        every { testContext.isSystemDarkMode } returns false
        every { testContext.getColor(R.color.on_surface) } returns ICON_TINT
        every {
            Icon.createWithResource(
                testContext,
                R.drawable.ic_login_item,
            )
                .setTint(ICON_TINT)
        } returns icon
        every {
            InlineSuggestionUi.newContentBuilder(pendingIntent)
                .setTitle(AUTOFILL_CIPHER_NAME)
                .setSubtitle(AUTOFILL_CIPHER_SUBTITLE)
                .setStartIcon(icon)
                .build()
                .slice
        } returns slice

        // Test
        val actual = inlinePresentationSpec.createCipherInlinePresentationOrNull(
            autofillAppInfo = autofillAppInfo,
            autofillCipher = autofillCipher,
        )

        // Verify not-null because we can't properly mock Intent constructors.
        Assertions.assertNotNull(actual)
        verify(exactly = 1) {
            UiVersions.getVersions(testStyle)
            PendingIntent.getService(
                testContext,
                PENDING_INTENT_CODE,
                any<Intent>(),
                PENDING_INTENT_FLAGS,
            )
            testContext.isSystemDarkMode
            testContext.getColor(R.color.on_surface)
            Icon.createWithResource(
                testContext,
                R.drawable.ic_login_item,
            )
                .setTint(ICON_TINT)
            InlineSuggestionUi.newContentBuilder(pendingIntent)
                .setTitle(AUTOFILL_CIPHER_NAME)
                .setSubtitle(AUTOFILL_CIPHER_SUBTITLE)
                .setStartIcon(icon)
                .build()
                .slice
        }
    }
}

private const val AUTOFILL_CIPHER_NAME = "Cipher1"
private const val AUTOFILL_CIPHER_SUBTITLE = "Subtitle"
private const val ICON_TINT: Int = 6123751
private const val PENDING_INTENT_CODE: Int = 0
private const val PENDING_INTENT_FLAGS: Int =
    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
