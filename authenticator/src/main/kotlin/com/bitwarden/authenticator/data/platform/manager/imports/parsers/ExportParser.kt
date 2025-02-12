package com.bitwarden.authenticator.data.platform.manager.imports.parsers

import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.bitwarden.authenticator.data.platform.manager.imports.model.ExportParseResult
import com.bitwarden.authenticator.ui.platform.base.util.asText
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException
import java.io.IOException

/**
 * Responsible for transforming exported authenticator data to a format consumable by this
 * application.
 */
abstract class ExportParser {
    /**
     * Converts the given [byteArray] content of a file to a collection of
     * [AuthenticatorItemEntity].
     */
    protected abstract fun parse(byteArray: ByteArray): ExportParseResult

    /**
     * Parses the given byte array into an [ExportParseResult].
     *
     * This method attempts to deserialize the input data and return a successful result.
     * If deserialization fails due to various exceptions, an appropriate error result
     * is returned instead.
     *
     * Exceptions handled include:
     * - [MissingFieldException]: If required fields are missing in the input data.
     * - [SerializationException]: If the input data cannot be processed due to invalid format.
     * - [IllegalArgumentException]: If an argument provided to a method is invalid.
     * - [IOException]: If an I/O error occurs during processing.
     *
     * @param byteArray The input data to be parsed as a [ByteArray].
     * @return [ExportParseResult] indicating success or a specific error result.
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun parseForResult(byteArray: ByteArray): ExportParseResult = try {
        parse(byteArray = byteArray)
    } catch (error: MissingFieldException) {
        ExportParseResult.Error(
            title = R.string.required_information_missing.asText(),
            message = R.string.required_information_missing_message.asText(),
        )
    } catch (error: SerializationException) {
        ExportParseResult.Error(
            title = R.string.file_could_not_be_processed.asText(),
            message = R.string.file_could_not_be_processed_message.asText(),
        )
    } catch (error: IllegalArgumentException) {
        ExportParseResult.Error(message = error.message?.asText())
    } catch (error: IOException) {
        ExportParseResult.Error(message = error.message?.asText())
    }
}
