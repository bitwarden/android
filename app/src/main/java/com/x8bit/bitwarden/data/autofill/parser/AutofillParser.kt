package com.x8bit.bitwarden.data.autofill.parser

import android.app.assist.AssistStructure
import android.service.autofill.FillRequest
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillRequest

/**
 * A tool for parsing autofill data from the OS into domain models.
 */
interface AutofillParser {

    /**
     * Parse the useful information from [fillRequest] into an [AutofillRequest].
     *
     * @param autofillAppInfo Provides app context that is required to properly parse the request.
     * @param fillRequest The request that needs parsing.
     */
    fun parse(
        autofillAppInfo: AutofillAppInfo,
        fillRequest: FillRequest,
    ): AutofillRequest

    /**
     * Parse the useful information from [assistStructure] into an [AutofillRequest].
     *
     * @param autofillAppInfo Provides app context that is required to properly parse the request.
     * @param assistStructure The key data from the original request that needs parsing.
     */
    fun parse(
        autofillAppInfo: AutofillAppInfo,
        assistStructure: AssistStructure,
    ): AutofillRequest
}
