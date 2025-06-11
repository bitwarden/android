package com.x8bit.bitwarden.data.autofill.builder

import android.service.autofill.FillRequest
import android.service.autofill.SaveInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillPartition
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository

/**
 * The primary implementation of [SaveInfoBuilder].This is used for converting autofill data into
 * a save info.
 */
class SaveInfoBuilderImpl(
    val settingsRepository: SettingsRepository,
) : SaveInfoBuilder {

    override fun build(
        autofillPartition: AutofillPartition,
        fillRequest: FillRequest,
        packageName: String?,
    ): SaveInfo? {
        // Make sure that the save prompt is possible.
        val canPerformSaveRequest = autofillPartition.canPerformSaveRequest
        if (settingsRepository.isAutofillSavePromptDisabled || !canPerformSaveRequest) return null

        // Docs state that password fields cannot be reliably saved
        // in Compat mode since they show as masked values.
        val isInCompatMode = (fillRequest.flags or
            FillRequest.FLAG_COMPATIBILITY_MODE_REQUEST) == fillRequest.flags

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
