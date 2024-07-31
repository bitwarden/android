package com.x8bit.bitwarden.data.autofill.builder

import android.annotation.SuppressLint
import android.os.Build
import android.service.autofill.FillRequest
import android.service.autofill.SaveInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillPartition
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository

/**
 * The primary implementation of [SaveInfoBuilder].This is used for converting autofill data into
 * a save info.
 */
class SaveInfoBuilderImpl(
    val settingsRepository: SettingsRepository,
) : SaveInfoBuilder {

    @SuppressLint("InlinedApi")
    override fun build(
        autofillAppInfo: AutofillAppInfo,
        autofillPartition: AutofillPartition,
        fillRequest: FillRequest,
        packageName: String?,
    ): SaveInfo? {
        // Make sure that the save prompt is possible.
        val canPerformSaveRequest = autofillPartition.canPerformSaveRequest
        if (settingsRepository.isAutofillSavePromptDisabled || !canPerformSaveRequest) return null

        // Docs state that password fields cannot be reliably saved
        // in Compat mode since they show as masked values.
        val isInCompatMode = if (autofillAppInfo.sdkInt >= Build.VERSION_CODES.Q) {
            // Attempt to automatically establish compat request mode on Android 10+
            (fillRequest.flags or FillRequest.FLAG_COMPATIBILITY_MODE_REQUEST) == fillRequest.flags
        } else {
            COMPAT_BROWSERS.contains(packageName)
        }

        // If login and compat mode, the password might be obfuscated,
        // in which case we should skip the save request.
        return if (autofillPartition is AutofillPartition.Login && isInCompatMode) {
            null
        } else {
            SaveInfo
                .Builder(
                    autofillPartition.saveType,
                    autofillPartition.requiredSaveIds.toTypedArray(),
                )
                .apply {
                    // setOptionalIds will throw an IllegalArgumentException if the array is empty
                    autofillPartition
                        .optionalSaveIds
                        .takeUnless { it.isEmpty() }
                        ?.let { setOptionalIds(it.toTypedArray()) }
                    if (isInCompatMode) setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE)
                }
                .build()
        }
    }
}

/**
 * These browsers function using the compatibility shim for the Autofill Framework.
 *
 * Ensure that these entries are sorted alphabetically and keep this list synchronized with the
 * values in /xml/autofill_service_configuration.xml and
 * /xml-v30/autofill_service_configuration.xml.
 */
private val COMPAT_BROWSERS: List<String> = emptyList()
