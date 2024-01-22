package com.x8bit.bitwarden.ui.autofill

import android.content.Context
import android.widget.RemoteViews
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
        every { this@mockk.getColor(R.color.dark_on_surface) } returns DARK_ON_SURFACE_COLOR
        every {
            this@mockk.getColor(R.color.dark_on_surface_variant)
        } returns DARK_ON_SURFACE_VARIANT_COLOR
        every { this@mockk.getColor(R.color.dark_surface) } returns DARK_SURFACE_COLOR
        every { this@mockk.getColor(R.color.on_surface) } returns ON_SURFACE_COLOR
        every { this@mockk.getColor(R.color.on_surface_variant) } returns ON_SURFACE_VARIANT_COLOR
        every { this@mockk.getColor(R.color.surface) } returns SURFACE_COLOR
        every { this@mockk.getString(R.string.app_name) } returns APP_NAME
        every { this@mockk.getString(R.string.go_to_my_vault) } returns GO_TO_MY_VAULT
        every { this@mockk.getString(R.string.vault_is_locked) } returns VAULT_IS_LOCKED
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
        mockkStatic(Context::isSystemDarkMode)
        mockkConstructor(RemoteViews::class)
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(Context::isSystemDarkMode)
        unmockkConstructor(RemoteViews::class)
    }

    @Test
    fun `buildAutofillRemoteViews should set values and light mode colors when not night mode`() {
        // Setup
        every { testContext.isSystemDarkMode } returns false
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
                    SURFACE_COLOR,
                )
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.icon,
                    "setColorFilter",
                    ON_SURFACE_COLOR,
                )
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.title,
                    "setTextColor",
                    ON_SURFACE_COLOR,
                )
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.subtitle,
                    "setTextColor",
                    ON_SURFACE_VARIANT_COLOR,
                )
        }
    }

    @Test
    fun `buildAutofillRemoteViews should set values and dark mode colors when night mode`() {
        // Setup
        every { testContext.isSystemDarkMode } returns true
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
                    DARK_SURFACE_COLOR,
                )
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.icon,
                    "setColorFilter",
                    DARK_ON_SURFACE_COLOR,
                )
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.title,
                    "setTextColor",
                    DARK_ON_SURFACE_COLOR,
                )
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.subtitle,
                    "setTextColor",
                    DARK_ON_SURFACE_VARIANT_COLOR,
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
            iconRes = R.drawable.icon,
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
                    R.drawable.icon,
                )
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.container,
                    "setBackgroundColor",
                    SURFACE_COLOR,
                )
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.title,
                    "setTextColor",
                    ON_SURFACE_COLOR,
                )
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.subtitle,
                    "setTextColor",
                    ON_SURFACE_VARIANT_COLOR,
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
            iconRes = R.drawable.icon,
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
                    R.drawable.icon,
                )
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.container,
                    "setBackgroundColor",
                    DARK_SURFACE_COLOR,
                )
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.title,
                    "setTextColor",
                    DARK_ON_SURFACE_COLOR,
                )
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.subtitle,
                    "setTextColor",
                    DARK_ON_SURFACE_VARIANT_COLOR,
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
                    DARK_SURFACE_COLOR,
                )
        } just runs
        every {
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.icon,
                    "setColorFilter",
                    DARK_ON_SURFACE_COLOR,
                )
        } just runs
        every {
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.icon,
                    "setColorFilter",
                    ON_SURFACE_COLOR,
                )
        } just runs
        every {
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.title,
                    "setTextColor",
                    DARK_ON_SURFACE_COLOR,
                )
        } just runs
        every {
            anyConstructed<RemoteViews>()
                .setInt(
                    R.id.subtitle,
                    "setTextColor",
                    DARK_ON_SURFACE_VARIANT_COLOR,
                )
        } just runs
    }
}

private const val APP_NAME = "Bitwarden"
private const val DARK_ON_SURFACE_COLOR: Int = 321
private const val DARK_ON_SURFACE_VARIANT_COLOR: Int = 654
private const val DARK_SURFACE_COLOR: Int = 987
private const val GO_TO_MY_VAULT = "Go to my vault"
private const val ICON_RES: Int = 41421421
private const val NAME: String = "NAME"
private const val ON_SURFACE_COLOR: Int = 123
private const val ON_SURFACE_VARIANT_COLOR: Int = 456
private const val PACKAGE_NAME: String = "com.x8bit.bitwarden"
private const val SUBTITLE: String = "SUBTITLE"
private const val SURFACE_COLOR: Int = 789
private const val VAULT_IS_LOCKED = "Vault is locked"
