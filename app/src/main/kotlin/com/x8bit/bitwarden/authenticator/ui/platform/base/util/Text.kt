package com.x8bit.bitwarden.authenticator.ui.platform.base.util

import android.content.res.Resources
import android.os.Parcelable
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalContext
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * Interface for sending strings to the UI layer.
 */
@Immutable
interface Text : Parcelable {
    /**
     * Returns the string representation of [Text].
     */
    @Composable
    operator fun invoke(): String {
        return toString(LocalContext.current.resources)
    }

    /**
     * Returns the string representation of [Text].
     */
    operator fun invoke(res: Resources): CharSequence

    /**
     * Helper method to call [invoke] and then [toString].
     */
    fun toString(res: Resources): String = invoke(res).toString()
}

/**
 * Implementation of [Text] backed by a string resource.
 */
@Parcelize
private data class ResText(@StringRes private val id: Int) : Text {
    override fun invoke(res: Resources): CharSequence = res.getText(id)
}

/**
 * Implementation of [Text] backed by an array of [Text]s. This makes it easy to concatenate texts.
 */
@Parcelize
private data class TextConcatenation(private val args: List<Text>) : Text {
    override fun invoke(
        res: Resources,
    ): CharSequence = args.joinToString(separator = "") { it.invoke(res) }
}

/**
 * Implementation of [Text] that formats a string resource with arguments.
 */
@Parcelize
private data class ResArgsText(
    @StringRes
    private val id: Int,
    private val args: @RawValue List<Any>,
) : Text {
    override fun invoke(res: Resources): String =
        res.getString(id, *convertArgs(res, args).toTypedArray())

    override fun toString(): String = "ResArgsText(id=$id, args=${args.contentToString()})"
}

/**
 * Implementation of [Text] that formats a plurals resource.
 */
@Parcelize
@Suppress("UnusedPrivateClass")
private data class PluralsText(
    @PluralsRes
    private val id: Int,
    private val quantity: Int,
    private val args: @RawValue List<Any>,
) : Text {
    override fun invoke(res: Resources): String =
        res.getQuantityString(id, quantity, *convertArgs(res, args).toTypedArray())

    override fun toString(): String =
        "PluralsText(id=$id, quantity=$quantity, args=${args.contentToString()})"
}

private fun List<Any>.contentToString() = joinToString(separator = ",", prefix = "(", postfix = ")")

private fun convertArgs(res: Resources, args: List<Any>): List<Any> =
    args.map { if (it is Text) it.invoke(res) else it }

/**
 * Implementation of [Text] backed by a raw string. For use with server responses.
 */
@Parcelize
private data class StringText(private val string: String) : Text {
    override fun invoke(res: Resources): String = string
}

/**
 * Convert a [String] to [Text].
 */
fun String.asText(): Text = StringText(this)

/**
 * Concatenates multiple [Text]s into a singular [Text].
 */
fun Text.concat(vararg args: Text): Text = TextConcatenation(listOf(this, *args))

/**
 * Convert a resource Id to [Text].
 */
fun @receiver:StringRes Int.asText(): Text = ResText(this)

/**
 * Convert a resource Id to [Text] with format args.
 */
fun @receiver:StringRes Int.asText(vararg args: Any): Text = ResArgsText(this, args.asList())
