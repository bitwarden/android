package com.x8bit.bitwarden.ui.autofill.util

import android.app.PendingIntent
import android.app.slice.Slice
import android.content.Context
import android.content.Intent
import android.graphics.BlendMode
import android.graphics.drawable.Icon
import android.os.Bundle
import android.widget.inline.InlinePresentationSpec
import androidx.autofill.inline.UiVersions
import androidx.autofill.inline.v1.InlineSuggestionUi
import androidx.core.content.ContextCompat
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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
        mockkStatic(ContextCompat::getString)
        mockkStatic(Context::isSystemDarkMode)
        mockkStatic(Icon::class)
        mockkStatic(InlineSuggestionUi::class)
        mockkStatic(PendingIntent::getService)
        mockkStatic(UiVersions::getVersions)
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(ContextCompat::getString)
        unmockkStatic(Context::isSystemDarkMode)
        unmockkStatic(Icon::class)
        unmockkStatic(InlineSuggestionUi::class)
        unmockkStatic(PendingIntent::getService)
        unmockkStatic(UiVersions::getVersions)
    }

    @Test
    fun `createCipherInlinePresentationOrNull should return null if incompatible`() {
        // Setup
        val autofillCipher: AutofillCipher.Card = mockk {
            every { this@mockk.name } returns AUTOFILL_CIPHER_NAME
            every { this@mockk.subtitle } returns AUTOFILL_CIPHER_SUBTITLE
            every { this@mockk.iconRes } returns BitwardenDrawable.ic_payment_card
        }
        val pendingIntent: PendingIntent = mockk()
        every {
            PendingIntent.getService(
                testContext,
                PENDING_INTENT_CODE,
                any<Intent>(),
                PENDING_INTENT_FLAGS,
            )
        } returns pendingIntent
        every {
            UiVersions.getVersions(testStyle)
        } returns emptyList()

        // Test
        val actual = inlinePresentationSpec.createCipherInlinePresentationOrNull(
            autofillAppInfo = autofillAppInfo,
            autofillCipher = autofillCipher,
        )

        // Verify
        assertNull(actual)
        verify(exactly = 1) {
            UiVersions.getVersions(testStyle)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `createCipherInlinePresentationOrNull should return presentation with card icon when card cipher and compatible`() {
        // Setup
        val icon: Icon = mockk()
        val iconRes = BitwardenDrawable.ic_payment_card
        val autofillCipher: AutofillCipher.Card = mockk {
            every { this@mockk.name } returns AUTOFILL_CIPHER_NAME
            every { this@mockk.subtitle } returns AUTOFILL_CIPHER_SUBTITLE
            every { this@mockk.iconRes } returns iconRes
        }
        val pendingIntent: PendingIntent = mockk()
        prepareForCompatibleCipherInlinePresentation(
            iconRes = iconRes,
            icon = icon,
            pendingIntent = pendingIntent,
            isSystemDarkMode = true,
            cipherType = CARD,
        )

        // Test
        val actual = inlinePresentationSpec.createCipherInlinePresentationOrNull(
            autofillAppInfo = autofillAppInfo,
            autofillCipher = autofillCipher,
        )

        // Verify not-null because we can't properly mock Intent constructors.
        assertNotNull(actual)
        verify(exactly = 1) {
            UiVersions.getVersions(testStyle)
            PendingIntent.getService(
                testContext,
                PENDING_INTENT_CODE,
                any<Intent>(),
                PENDING_INTENT_FLAGS,
            )
            testContext.isSystemDarkMode
            Icon.createWithResource(
                testContext,
                iconRes,
            )
                .setTint(COLOR_DARK_ICON_TINT)
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
        every {
            ContextCompat.getString(testContext, BitwardenString.autofill_suggestion)
        } returns AUTOFILL_SUGGESTION
        every {
            ContextCompat.getString(testContext, BitwardenString.type_card)
        } returns CARD
        every {
            ContextCompat.getString(testContext, BitwardenString.type_login)
        } returns LOGIN
        val icon: Icon = mockk()
        val iconRes = BitwardenDrawable.ic_globe
        val autofillCipher: AutofillCipher.Login = mockk {
            every { this@mockk.name } returns AUTOFILL_CIPHER_NAME
            every { this@mockk.subtitle } returns AUTOFILL_CIPHER_SUBTITLE
            every { this@mockk.iconRes } returns BitwardenDrawable.ic_globe
        }
        val pendingIntent: PendingIntent = mockk()
        prepareForCompatibleCipherInlinePresentation(
            iconRes = iconRes,
            icon = icon,
            pendingIntent = pendingIntent,
            isSystemDarkMode = false,
        )

        // Test
        val actual = inlinePresentationSpec.createCipherInlinePresentationOrNull(
            autofillAppInfo = autofillAppInfo,
            autofillCipher = autofillCipher,
        )

        // Verify not-null because we can't properly mock Intent constructors.
        assertNotNull(actual)
        verify(exactly = 1) {
            UiVersions.getVersions(testStyle)
            PendingIntent.getService(
                testContext,
                PENDING_INTENT_CODE,
                any<Intent>(),
                PENDING_INTENT_FLAGS,
            )
            testContext.isSystemDarkMode
            Icon
                .createWithResource(
                    testContext,
                    iconRes,
                )
                .setTint(COLOR_LIGHT_ICON_TINT)
            InlineSuggestionUi.newContentBuilder(pendingIntent)
                .setTitle(AUTOFILL_CIPHER_NAME)
                .setSubtitle(AUTOFILL_CIPHER_SUBTITLE)
                .setStartIcon(icon)
                .build()
                .slice
        }
    }

    @Test
    fun `createVaultItemInlinePresentationOrNull should return null if incompatible`() {
        // Setup
        val pendingIntent: PendingIntent = mockk()
        every {
            UiVersions.getVersions(testStyle)
        } returns emptyList()

        every { testContext.getString(R.string.app_name) } returns APP_NAME
        every { testContext.getString(BitwardenString.vault_is_locked) } returns VAULT_IS_LOCKED
        every { testContext.getString(BitwardenString.my_vault) } returns MY_VAULT

        // Test
        val actual = inlinePresentationSpec.createVaultItemInlinePresentationOrNull(
            autofillAppInfo = autofillAppInfo,
            pendingIntent = pendingIntent,
            isLocked = true,
        )

        // Verify
        assertNull(actual)
        verify(exactly = 1) {
            UiVersions.getVersions(testStyle)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `createVaultItemInlinePresentationOrNull should return presentation with locked vault title when vault is locked and compatible`() {
        // Setup
        val icon: Icon = mockk()
        val pendingIntent: PendingIntent = mockk()
        prepareForCompatibleVaultItemInlinePresentation(
            icon = icon,
            pendingIntent = pendingIntent,
        )

        // Test
        val actual = inlinePresentationSpec.createVaultItemInlinePresentationOrNull(
            autofillAppInfo = autofillAppInfo,
            pendingIntent = pendingIntent,
            isLocked = true,
        )

        // Verify not-null because we can't properly mock Intent constructors.
        assertNotNull(actual)
        verify(exactly = 1) {
            UiVersions.getVersions(testStyle)
            Icon
                .createWithResource(
                    testContext,
                    BitwardenDrawable.logo_bitwarden_icon,
                )
                .setTintBlendMode(BlendMode.DST)
            testContext.getString(R.string.app_name)
            testContext.getString(BitwardenString.vault_is_locked)
            InlineSuggestionUi.newContentBuilder(pendingIntent)
                .setTitle(APP_NAME)
                .setSubtitle(VAULT_IS_LOCKED)
                .setStartIcon(icon)
                .build()
                .slice
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `createVaultItemInlinePresentationOrNull should return presentation with my vault title when vault is locked and compatible`() {
        // Setup
        val icon: Icon = mockk()
        val pendingIntent: PendingIntent = mockk()
        prepareForCompatibleVaultItemInlinePresentation(
            icon = icon,
            pendingIntent = pendingIntent,
        )

        // Test
        val actual = inlinePresentationSpec.createVaultItemInlinePresentationOrNull(
            autofillAppInfo = autofillAppInfo,
            pendingIntent = pendingIntent,
            isLocked = false,
        )

        // Verify not-null because we can't properly mock Intent constructors.
        assertNotNull(actual)
        verify(exactly = 1) {
            UiVersions.getVersions(testStyle)
            Icon
                .createWithResource(
                    testContext,
                    BitwardenDrawable.logo_bitwarden_icon,
                )
                .setTintBlendMode(BlendMode.DST)
            testContext.getString(R.string.app_name)
            testContext.getString(BitwardenString.my_vault)
            InlineSuggestionUi.newContentBuilder(pendingIntent)
                .setTitle(APP_NAME)
                .setSubtitle(MY_VAULT)
                .setStartIcon(icon)
                .build()
                .slice
        }
    }

    private fun prepareForCompatibleCipherInlinePresentation(
        iconRes: Int,
        icon: Icon,
        pendingIntent: PendingIntent,
        isSystemDarkMode: Boolean,
        cipherType: String = LOGIN,
    ) {
        every {
            ContextCompat.getString(testContext, BitwardenString.autofill_suggestion)
        } returns AUTOFILL_SUGGESTION
        every {
            ContextCompat.getString(testContext, BitwardenString.type_card)
        } returns CARD
        every {
            ContextCompat.getString(testContext, BitwardenString.type_login)
        } returns LOGIN
        @Suppress("DEPRECATION")
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
        every { testContext.isSystemDarkMode } returns isSystemDarkMode
        every {
            Icon
                .createWithResource(
                    testContext,
                    iconRes,
                )
                .setTint(COLOR_DARK_ICON_TINT)
        } returns icon
        every {
            Icon
                .createWithResource(
                    testContext,
                    iconRes,
                )
                .setTint(COLOR_LIGHT_ICON_TINT)
        } returns icon
        every {
            InlineSuggestionUi
                .newContentBuilder(pendingIntent)
                .setContentDescription(
                    createMockContentDescription(cipherType),
                )
                .setTitle(AUTOFILL_CIPHER_NAME)
                .setSubtitle(AUTOFILL_CIPHER_SUBTITLE)
                .setStartIcon(icon)
                .build()
                .slice
        } returns slice

        every {
            InlineSuggestionUi
                .newContentBuilder(pendingIntent)
                .setTitle(AUTOFILL_CIPHER_NAME)
                .setSubtitle(AUTOFILL_CIPHER_SUBTITLE)
                .setStartIcon(icon)
                .build()
                .slice
        } returns slice
    }

    private fun prepareForCompatibleVaultItemInlinePresentation(
        icon: Icon,
        pendingIntent: PendingIntent,
    ) {
        @Suppress("DEPRECATION")
        val slice: Slice = mockk()
        every {
            UiVersions.getVersions(testStyle)
        } returns listOf(UiVersions.INLINE_UI_VERSION_1)
        every {
            Icon
                .createWithResource(
                    testContext,
                    BitwardenDrawable.logo_bitwarden_icon,
                )
                .setTintBlendMode(BlendMode.DST)
        } returns icon
        every {
            InlineSuggestionUi
                .newContentBuilder(pendingIntent)
                .setTitle(APP_NAME)
                .setSubtitle(VAULT_IS_LOCKED)
                .setStartIcon(icon)
                .build()
                .slice
        } returns slice
        every {
            InlineSuggestionUi
                .newContentBuilder(pendingIntent)
                .setTitle(APP_NAME)
                .setSubtitle(MY_VAULT)
                .setStartIcon(icon)
                .build()
                .slice
        } returns slice
        every { testContext.getString(R.string.app_name) } returns APP_NAME
        every { testContext.getString(BitwardenString.vault_is_locked) } returns VAULT_IS_LOCKED
        every { testContext.getString(BitwardenString.my_vault) } returns MY_VAULT
    }
}

private fun createMockContentDescription(cipherType: String): String =
    "${AUTOFILL_SUGGESTION}, $cipherType, ${AUTOFILL_CIPHER_NAME}, ${AUTOFILL_CIPHER_SUBTITLE}"

private const val AUTOFILL_SUGGESTION = "Autofill suggestion"
private const val CARD = "Card"
private const val LOGIN = "Login"
private const val APP_NAME = "Bitwarden"
private const val AUTOFILL_CIPHER_NAME = "Cipher1"
private const val AUTOFILL_CIPHER_SUBTITLE = "Subtitle"
private const val COLOR_DARK_ICON_TINT: Int = -6904901
private const val COLOR_LIGHT_ICON_TINT: Int = -10850927
private const val MY_VAULT = "My vault"
private const val PENDING_INTENT_CODE: Int = 0
private const val PENDING_INTENT_FLAGS: Int =
    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
private const val VAULT_IS_LOCKED = "Vault is locked"
