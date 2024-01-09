package com.x8bit.bitwarden.data.autofill.parser

import android.app.assist.AssistStructure
import com.x8bit.bitwarden.data.autofill.model.AutofillRequest

/**
 * A tool for parsing autofill data from the OS into domain models.
 */
interface AutofillParser {

    /**
     * Parse the useful information from [assistStructure] into an [AutofillRequest].
     */
    fun parse(assistStructure: AssistStructure): AutofillRequest
}
