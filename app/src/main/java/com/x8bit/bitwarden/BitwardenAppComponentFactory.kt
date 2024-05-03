package com.x8bit.bitwarden

import android.app.Service
import android.content.Intent
import androidx.core.app.AppComponentFactory
import com.x8bit.bitwarden.data.autofill.BitwardenAutofillService
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

private const val LEGACY_AUTOFILL_SERVICE_NAME = "com.x8bit.bitwarden.Autofill.AutofillService"

/**
 * A factory class that allows us to intercept when a manifest element is being instantiated
 * and modify various characteristics before initialization.
 */
@Suppress("unused")
@OmitFromCoverage
class BitwardenAppComponentFactory : AppComponentFactory() {
    /**
     * Used to intercept when the [BitwardenAutofillService] is being instantiated and modify which
     * service is created. This is required because the [className] used in the manifest must match
     * the legacy Xamarin app service name but the service name in this app is different.
     */
    override fun instantiateServiceCompat(
        cl: ClassLoader,
        className: String,
        intent: Intent?,
    ): Service = when (className) {
        LEGACY_AUTOFILL_SERVICE_NAME -> {
            super.instantiateServiceCompat(cl, BitwardenAutofillService::class.java.name, intent)
        }

        else -> super.instantiateServiceCompat(cl, className, intent)
    }
}
