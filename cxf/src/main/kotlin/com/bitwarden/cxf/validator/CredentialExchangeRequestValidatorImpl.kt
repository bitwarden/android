package com.bitwarden.cxf.validator

import android.app.Activity
import com.bitwarden.cxf.model.ImportCredentialsRequestData

private const val GMS_PACKAGE_NAME = "com.google.android.gms"

/**
 * Default implementation of [CredentialExchangeRequestValidator].
 */
internal class CredentialExchangeRequestValidatorImpl(
    private val activity: Activity,
) : CredentialExchangeRequestValidator {

    /**
     * Validates the incoming [ImportCredentialsRequestData].
     *
     * This implementation ensures that the request originates from Google Mobile Services (GMS),
     * which is the expected caller for credential exchange flows on Android.
     *
     * Note that [importCredentialsRequestData] is not currently evaluated. It will be used to
     * perform additional validation once the implementation is finalized.
     *
     * @param importCredentialsRequestData The data associated with the import credentials request.
     * @return `true` if the calling package is GMS, `false` otherwise.
     */
    override fun validate(
        importCredentialsRequestData: ImportCredentialsRequestData,
    ): Boolean = activity.callingPackage == GMS_PACKAGE_NAME
}
