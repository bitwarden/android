package com.x8bit.bitwarden.ui.platform.base.util

import android.content.res.Resources
import android.os.Parcelable
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
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

/**
 * Create an [AnnotatedString] with highlighted parts.
 * @param mainString the full string
 * @param highlights parts of the mainString that will be highlighted
 * @param tag the tag that will be used for the annotation
 */
@Composable
fun createAnnotatedString(
    mainString: String,
    highlights: List<String>,
    highlightStyle: SpanStyle = bitwardenClickableTextSpanStyle,
    tag: String? = null,
): AnnotatedString {
    return buildAnnotatedString {
        append(mainString)
        addStyle(
            style = bitwardenDefaultSpanStyle,
            start = 0,
            end = mainString.length,
        )
        for (highlightString in highlights) {
            val startIndex = mainString.indexOf(highlightString, ignoreCase = true)
            val endIndex = startIndex + highlightString.length
            addStyle(
                style = highlightStyle,
                start = startIndex,
                end = endIndex,
            )
            tag?.let {
                addStringAnnotation(
                    tag = it,
                    annotation = highlightString,
                    start = startIndex,
                    end = endIndex,
                )
            }
        }
    }
}

/**
 * Create an [AnnotatedString] with highlighted parts that can be clicked.
 * @param mainString the full string to be processed.
 * @param highlights list of [ClickableTextHighlight]s to be annotated within the [mainString].
 * If a highlighted text is repeated in the [mainString], you must choose which instance to use
 * by setting the [ClickableTextHighlight.instance] property. Only one instance of the text will
 * be annotated.
 */
@Composable
fun createClickableAnnotatedString(
    mainString: String,
    highlights: List<ClickableTextHighlight>,
    style: SpanStyle = bitwardenDefaultSpanStyle,
    highlightStyle: SpanStyle = bitwardenClickableTextSpanStyle,
): AnnotatedString {
    return buildAnnotatedString {
        append(mainString)
        addStyle(
            style = style,
            start = 0,
            end = mainString.length,
        )
        for (highlight in highlights) {
            val text = highlight.textToHighlight
            val startIndex = when (highlight.instance) {
                ClickableTextHighlight.Instance.FIRST -> {
                    mainString.indexOf(text, ignoreCase = true)
                }

                ClickableTextHighlight.Instance.LAST -> {
                    mainString.lastIndexOf(text, ignoreCase = true)
                }
            }
            val endIndex = startIndex + highlight.textToHighlight.length
            val link = LinkAnnotation.Clickable(
                tag = highlight.textToHighlight,
                styles = TextLinkStyles(
                    style = highlightStyle,
                ),
            ) {
                highlight.onTextClick.invoke()
            }
            addLink(
                link,
                start = startIndex,
                end = endIndex,
            )
        }
    }
}

/**
 * Models text that should be highlighted with and associated with a click action.
 * @property textToHighlight the text to highlight and associate with click action.
 * @property onTextClick the click action to perform when the text is clicked.
 * @property instance to denote if there are multiple instances of the [textToHighlight] in the
 * [AnnotatedString] which should be highlighted.
 */
data class ClickableTextHighlight(
    val textToHighlight: String,
    val onTextClick: () -> Unit,
    val instance: Instance = Instance.FIRST,
) {
    /**
     * To denote if a [ClickableTextHighlight.textToHighlight] should highlight the
     * first instance of the text or the last instance.
     * "If you ain't first, you're last" == true
     */
    enum class Instance {
        FIRST,
        LAST,
    }
}

val bitwardenDefaultSpanStyle: SpanStyle
    @Composable
    @ReadOnlyComposable
    get() = SpanStyle(
        color = BitwardenTheme.colorScheme.text.primary,
        fontSize = BitwardenTheme.typography.bodyMedium.fontSize,
        fontFamily = BitwardenTheme.typography.bodyMedium.fontFamily,
    )

val bitwardenBoldSpanStyle: SpanStyle
    @Composable
    @ReadOnlyComposable
    get() = bitwardenDefaultSpanStyle.copy(
        fontWeight = FontWeight.Bold,
    )

val bitwardenClickableTextSpanStyle: SpanStyle
    @Composable
    @ReadOnlyComposable
    get() = bitwardenBoldSpanStyle.copy(
        color = BitwardenTheme.colorScheme.text.interaction,
    )
