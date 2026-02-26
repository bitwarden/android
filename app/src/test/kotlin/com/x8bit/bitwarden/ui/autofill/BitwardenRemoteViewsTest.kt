package com.x8bit.bitwarden.ui.autofill

import android.content.Context
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import com.x8bit.bitwarden.ui.autofill.util.isSystemDarkMode
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BitwardenRemoteViewsTest {
    private val testContext: Context = mockk {
        every { this@mockk.getString(R.string.app_name) } returns APP_NAME
        every { this@mockk.getString(BitwardenString.go_to_my_vault) } returns GO_TO_MY_VAULT
        every { this@mockk.getString(BitwardenString.vault_is_locked) } returns VAULT_IS_LOCKED
    }
    private val autofillAppInfo: AutofillAppInfo = mockk {
        every { this@mockk.context } returns testContext
        every { this@mockk.packageName } returns PACKAGE_NAME
    }
    private val autofillCipher: AutofillCipher = mockk {
        every { this@mockk.iconRes } returns ICON_RES
        every { this@mockk.name } returns NAME
        every { this@mockk.subtitle } returns SUBTITLE
    }

    @BeforeEach
    fun setup() {
        mockkStatic(ContextCompat::getString)
        mockkStatic(Context::isSystemDarkMode)
        mockkConstructor(RemoteViews::class)
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(ContextCompat::getString)
        unmockkStatic(Context::isSystemDarkMode)
        unmockkConstructor(RemoteViews::class)
    }

    @Test
    fun `buildAutofillRemoteViews should set values and light mode colors when not night mode`() {
        // Setup
        every { testContext.isSystemDarkMode } returns false
        every {
            ContextCompat.getString(testContext, BitwardenString.autofill_suggestion)
        } returns "Autofill suggestion"
        every {
            ContextCompat.getString(testContext, BitwardenString.type_card)
        } returns "Card"
        every {
            ContextCompat.getString(testContext, BitwardenString.type_login)
        } returns "Login"
        prepareRemoteViews(
            name = NAME,
            subtitle = SUBTITLE,
            iconRes = ICON_RES,
        )

        // Test
        buildAutofillRemoteViews(
            autofillAppInfo = autofillAppInfo,
            autofillCipher = autofillCipher,
        )

        // Note: impossible to do a useful test of the returned RemoteViews due to mockking
        // constraints of the [RemoteViews] constructor. Our best bet is to make sure the correct
        // operations are performed on the constructed [RemoteViews].

        // Verify
        verify(exactly = 1) {
            anyConstructed<RemoteViews>()
                .setTextViewText(
                    R.id.title,
                    NAME,
                )
            anyConstructed<RemoteViews>()
                .setTextViewText(
                    R.id.subtitle,
                    SUBTITLE,
                )
            anyConstructed<RemoteViews>()
                .setImageViewResource(
                    R.id.icon,
                    ICON_RES,
                )
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.container,
                    "setBackgroundColor",
                    COLOR_LIGHT_BACKGROUND,
                )
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.icon,
                    "setColorFilter",
                    COLOR_LIGHT_ICON_TINT,
                )
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.title,
                    "setTextColor",
                    COLOR_LIGHT_TEXT_PRIMARY,
                )
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.subtitle,
                    "setTextColor",
                    COLOR_LIGHT_TEXT_SECONDARY,
                )
        }
    }

    @Test
    fun `buildAutofillRemoteViews should set values and dark mode colors when night mode`() {
        // Setup
        every { testContext.isSystemDarkMode } returns true
        every {
            ContextCompat.getString(testContext, BitwardenString.autofill_suggestion)
        } returns "Autofill suggestion"
        every {
            ContextCompat.getString(testContext, BitwardenString.type_card)
        } returns "Card"
        every {
            ContextCompat.getString(testContext, BitwardenString.type_login)
        } returns "Login"
        prepareRemoteViews(
            name = NAME,
            subtitle = SUBTITLE,
            iconRes = ICON_RES,
        )

        // Test
        buildAutofillRemoteViews(
            autofillAppInfo = autofillAppInfo,
            autofillCipher = autofillCipher,
        )

        // Note: impossible to do a useful test of the returned RemoteViews due to mockking
        // constraints of the [RemoteViews] constructor. Our best bet is to make sure the correct
        // operations are performed on the constructed [RemoteViews].

        // Verify
        verify(exactly = 1) {
            anyConstructed<RemoteViews>()
                .setTextViewText(
                    R.id.title,
                    NAME,
                )
            anyConstructed<RemoteViews>()
                .setTextViewText(
                    R.id.subtitle,
                    SUBTITLE,
                )
            anyConstructed<RemoteViews>()
                .setImageViewResource(
                    R.id.icon,
                    ICON_RES,
                )
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.container,
                    "setBackgroundColor",
                    COLOR_DARK_BACKGROUND,
                )
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.icon,
                    "setColorFilter",
                    COLOR_DARK_ICON_TINT,
                )
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.title,
                    "setTextColor",
                    COLOR_DARK_TEXT_PRIMARY,
                )
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.subtitle,
                    "setTextColor",
                    COLOR_DARK_TEXT_SECONDARY,
                )
        }
    }

    @Test
    fun `buildVaultItemAutofillRemoteViews should set values properly when vault is locked`() {
        // Setup
        every { testContext.isSystemDarkMode } returns false
        prepareRemoteViews(
            name = APP_NAME,
            subtitle = VAULT_IS_LOCKED,
            iconRes = BitwardenDrawable.logo_bitwarden_icon,
        )

        // Test
        buildVaultItemAutofillRemoteViews(
            autofillAppInfo = autofillAppInfo,
            isLocked = true,
        )

        // Note: impossible to do a useful test of the returned RemoteViews due to mockking
        // constraints of the [RemoteViews] constructor. Our best bet is to make sure the correct
        // operations are performed on the constructed [RemoteViews].

        // Verify
        verify(exactly = 1) {
            anyConstructed<RemoteViews>()
                .setTextViewText(
                    R.id.title,
                    APP_NAME,
                )
            anyConstructed<RemoteViews>()
                .setTextViewText(
                    R.id.subtitle,
                    VAULT_IS_LOCKED,
                )
            anyConstructed<RemoteViews>()
                .setImageViewResource(
                    R.id.icon,
                    BitwardenDrawable.logo_bitwarden_icon,
                )
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.container,
                    "setBackgroundColor",
                    COLOR_LIGHT_BACKGROUND,
                )
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.title,
                    "setTextColor",
                    COLOR_LIGHT_TEXT_PRIMARY,
                )
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.subtitle,
                    "setTextColor",
                    COLOR_LIGHT_TEXT_SECONDARY,
                )
        }
    }

    @Test
    fun `buildVaultItemAutofillRemoteViews should set values properly when vault is unlocked`() {
        // Setup
        every { testContext.isSystemDarkMode } returns true
        prepareRemoteViews(
            name = APP_NAME,
            subtitle = GO_TO_MY_VAULT,
            iconRes = BitwardenDrawable.logo_bitwarden_icon,
        )

        // Test
        buildVaultItemAutofillRemoteViews(
            autofillAppInfo = autofillAppInfo,
            isLocked = false,
        )

        // Note: impossible to do a useful test of the returned RemoteViews due to mockking
        // constraints of the [RemoteViews] constructor. Our best bet is to make sure the correct
        // operations are performed on the constructed [RemoteViews].

        // Verify
        verify(exactly = 1) {
            anyConstructed<RemoteViews>()
                .setTextViewText(
                    R.id.title,
                    APP_NAME,
                )
            anyConstructed<RemoteViews>()
                .setTextViewText(
                    R.id.subtitle,
                    GO_TO_MY_VAULT,
                )
            anyConstructed<RemoteViews>()
                .setImageViewResource(
                    R.id.icon,
                    BitwardenDrawable.logo_bitwarden_icon,
                )
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.container,
                    "setBackgroundColor",
                    COLOR_DARK_BACKGROUND,
                )
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.title,
                    "setTextColor",
                    COLOR_DARK_TEXT_PRIMARY,
                )
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.subtitle,
                    "setTextColor",
                    COLOR_DARK_TEXT_SECONDARY,
                )
        }
    }

    private fun prepareRemoteViews(
        name: String,
        subtitle: String,
        iconRes: Int,
    ) {
        every {
            anyConstructed<RemoteViews>()
                .setTextViewText(
                    R.id.title,
                    name,
                )
        } just runs
        every {
            anyConstructed<RemoteViews>()
                .setTextViewText(
                    R.id.subtitle,
                    subtitle,
                )
        } just runs
        every {
            anyConstructed<RemoteViews>()
                .setImageViewResource(
                    R.id.icon,
                    iconRes,
                )
        } just runs
        every {
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.container,
                    "setBackgroundColor",
                    COLOR_LIGHT_BACKGROUND,
                )
        } just runs
        every {
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.icon,
                    "setColorFilter",
                    COLOR_LIGHT_ICON_TINT,
                )
        } just runs
        every {
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.icon,
                    "setColorFilter",
                    COLOR_LIGHT_ICON_TINT,
                )
        } just runs
        every {
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.title,
                    "setTextColor",
                    COLOR_LIGHT_TEXT_PRIMARY,
                )
        } just runs
        every {
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.subtitle,
                    "setTextColor",
                    COLOR_LIGHT_TEXT_SECONDARY,
                )
        } just runs
    }
}

private const val APP_NAME = "Bitwarden"
private const val COLOR_DARK_BACKGROUND: Int = -15590873
private const val COLOR_DARK_ICON_TINT: Int = -6904901
private const val COLOR_DARK_TEXT_PRIMARY: Int = -788743
private const val COLOR_DARK_TEXT_SECONDARY: Int = -7825227
private const val COLOR_LIGHT_BACKGROUND: Int = -788743
private const val COLOR_LIGHT_ICON_TINT: Int = -10850927
private const val COLOR_LIGHT_TEXT_PRIMARY: Int = -14999511
private const val COLOR_LIGHT_TEXT_SECONDARY: Int = -10850927
private const val GO_TO_MY_VAULT = "Go to my vault"
private const val ICON_RES: Int = 41421421
private const val NAME: String = "NAME"
private const val PACKAGE_NAME: String = "com.x8bit.bitwarden"
private const val SUBTITLE: String = "SUBTITLE"
private const val VAULT_IS_LOCKED = "Vault is locked"
