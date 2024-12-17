package com.x8bit.bitwarden.ui.platform.base.util

import android.text.Annotation
import android.text.SpannableStringBuilder
import android.text.SpannedString
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.core.text.getSpans
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Creates an [AnnotatedString] from a string resource allowing for optional arguments
 * to be applied.
 * @param args Optional arguments to be applied to the string resource, must already be a
 * [String]
 * @param style Style to apply to the entire string
 * @param emphasisHighlightStyle Style to apply to part of the resource that has been annotated
 * with the "emphasis" annotation.
 * @param linkHighlightStyle Style to apply to part of the resource that has been annotated with
 * the "link" annotation.
 * @param onAnnotationClick Callback to invoke when a link annotation is clicked. Will pass back
 * the value of the annotation as a string to allow for delineation if there are multiple callbacks
 * to be applied.
 *
 * In order for the styles to be applied the resource must contain custom annotations for example:
 *
 * If a word or phrase is to have the [emphasisHighlightStyle] applied then it must be annotated
 * with the custom XML tag: <annotation emphasis="anything"> where the value does not matter.
 * <string foo>Foo <annotation emphasis="anything">bar</annotation> baz</string>
 *
 * If a word or phrase is to have the [linkHighlightStyle] applied then it must be annotated
 * with the custom XML tag: <annotation link="callBackKey"> where the value will be passed back
 * in [onAnnotationClick] and used to delineate which annotation was clicked.
 * <string foo>Foo <annotation link="onBarClick">bar</annotation> baz</string>
 *
 * If the <string> contains a format argument (%1$s) then that argument should be wrapped in the
 * following custom XML tag: <annotation arg="0"> where the value is the index of the argument,
 * starting at 0.
 */
@Suppress("LongMethod")
@Composable
fun @receiver:StringRes Int.toAnnotatedString(
    vararg args: String,
    style: SpanStyle = bitwardenDefaultSpanStyle,
    emphasisHighlightStyle: SpanStyle = bitwardenBoldSpanStyle,
    linkHighlightStyle: SpanStyle = bitwardenClickableTextSpanStyle,
    onAnnotationClick: ((annotationKey: String) -> Unit)? = null,
): AnnotatedString {
    val resources = LocalContext.current.resources
    val spannableBuilder = try {
        SpannableStringBuilder(resources.getText(this) as SpannedString)
    } catch (e: ClassCastException) {
        return stringResource(id = this).toAnnotatedString()
    }
    spannableBuilder.applyArgAnnotations(args = args)
    val annotatedStringBuilder = AnnotatedString.Builder()
    annotatedStringBuilder.append(spannableBuilder)
    annotatedStringBuilder.addStyle(
        style = style,
        start = 0,
        end = spannableBuilder.length,
    )
    spannableBuilder.getSpans<Annotation>(0, spannableBuilder.length)
        .forEach { annotation ->
            val start = spannableBuilder.getSpanStart(annotation)
            val end = spannableBuilder.getSpanEnd(annotation)
            when (annotation.key) {
                "emphasis" -> {
                    annotatedStringBuilder.addStyle(
                        style = emphasisHighlightStyle,
                        start = start,
                        end = end,
                    )
                }

                "link" -> {
                    val link = LinkAnnotation.Clickable(
                        tag = annotation.value.orEmpty(),
                        styles = TextLinkStyles(
                            style = linkHighlightStyle,
                        ),
                    ) {
                        onAnnotationClick?.invoke(annotation.value.orEmpty())
                    }
                    annotatedStringBuilder.addLink(
                        link,
                        start = start,
                        end = end,
                    )
                }
            }
        }
    return annotatedStringBuilder.toAnnotatedString()
}

private fun SpannableStringBuilder.applyArgAnnotations(
    vararg args: String,
) {
    val annotations = getSpans<Annotation>()
    annotations
        .filter {
            it.key == "arg"
        }.forEach { annotation ->
            val argIndex = Integer.parseInt(annotation.value)
            this.replace(
                this.getSpanStart(annotation),
                this.getSpanEnd(annotation),
                args[argIndex],
            )
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
