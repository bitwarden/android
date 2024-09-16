package com.x8bit.bitwarden

import android.app.Service
import android.content.Intent
import android.os.Build
import androidx.annotation.Keep
import androidx.core.app.AppComponentFactory
import com.x8bit.bitwarden.data.autofill.BitwardenAutofillService
import com.x8bit.bitwarden.data.autofill.accessibility.BitwardenAccessibilityService
import com.x8bit.bitwarden.data.autofill.fido2.BitwardenFido2ProviderService
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.tiles.BitwardenAutofillTileService
import com.x8bit.bitwarden.data.tiles.BitwardenGeneratorTileService
import com.x8bit.bitwarden.data.tiles.BitwardenVaultTileService

/**
 * A factory class that allows us to intercept when a manifest element is being instantiated
 * and modify various characteristics before initialization.
 */
@Suppress("unused")
@Keep
@OmitFromCoverage
class BitwardenAppComponentFactory : AppComponentFactory() {
    /**
     * Used to intercept when certain legacy services are being instantiated and modify which
     * service is created. This is required because the [className] used in the manifest must match
     * the legacy Xamarin app service name but the service name in this app is different.
     *
     * Services currently being managed:
     * * [BitwardenAccessibilityService]
     * * [BitwardenAutofillService]
     * * [BitwardenAutofillTileService]
     * * [BitwardenFido2ProviderService]
     * * [BitwardenVaultTileService]
     * * [BitwardenGeneratorTileService]
     */
    override fun instantiateServiceCompat(
        cl: ClassLoader,
        className: String,
        intent: Intent?,
    ): Service = when (className) {
        LEGACY_ACCESSIBILITY_SERVICE_NAME -> {
            super.instantiateServiceCompat(
                cl,
                BitwardenAccessibilityService::class.java.name,
                intent,
            )
        }

        LEGACY_AUTOFILL_SERVICE_NAME -> {
            super.instantiateServiceCompat(cl, BitwardenAutofillService::class.java.name, intent)
        }

        LEGACY_AUTOFILL_TILE_SERVICE_NAME -> {
            super.instantiateServiceCompat(
                cl,
                BitwardenAutofillTileService::class.java.name,
                intent,
            )
        }

        LEGACY_CREDENTIAL_SERVICE_NAME -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                super.instantiateServiceCompat(
                    cl,
                    BitwardenFido2ProviderService::class.java.name,
                    intent,
                )
            } else {
                throw UnsupportedOperationException(
                    "The CredentialProviderService requires API 34 or higher.",
                )
            }
        }

        LEGACY_VAULT_TILE_SERVICE_NAME -> {
            super.instantiateServiceCompat(cl, BitwardenVaultTileService::class.java.name, intent)
        }

        LEGACY_GENERATOR_TILE_SERVICE_NAME -> {
            super.instantiateServiceCompat(
                cl,
                BitwardenGeneratorTileService::class.java.name,
                intent,
            )
        }

        else -> super.instantiateServiceCompat(cl, className, intent)
    }
}
