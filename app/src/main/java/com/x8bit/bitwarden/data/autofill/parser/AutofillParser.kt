package com.x8bit.bitwarden.data.autofill.parser

import android.service.autofill.FillRequest
import com.x8bit.bitwarden.data.autofill.model.AutofillRequest

/**
 * A tool for parsing autofill data from the OS into domain models.
 */
interface AutofillParser {

    /**
     * Parse the useful information from [fillRequest] into an [AutofillRequest].
     */
    fun parse(fillRequest: FillRequest): AutofillRequest
}
