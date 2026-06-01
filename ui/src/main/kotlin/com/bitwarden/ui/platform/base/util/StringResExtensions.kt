package com.bitwarden.ui.platform.base.util

import android.content.res.Resources
import android.text.Annotation
import android.text.SpannableStringBuilder
import android.text.SpannedString
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.core.text.getSpans

/**
 * Creates an [AnnotatedString] from a string resource allowing for optional arguments
 * to be applied.
 *
 * @param args Optional arguments to be applied to the string resource, must already be a
 * [String]
 * @param style Style to apply to the entire string
 * @param emphasisHighlightStyle Style to apply to part of the resource that has been annotated
 * with the "emphasis" annotation.
 * @param linkHighlightStyle Style to apply to part of the resource that has been annotated with
 * the "link" annotation.
 * @param resources The resources used to access the strings.
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
@Composable
fun @receiver:StringRes Int.toAnnotatedString(
    vararg args: CharSequence,
    style: SpanStyle = bitwardenDefaultSpanStyle,
    emphasisHighlightStyle: SpanStyle = bitwardenBoldSpanStyle,
    linkHighlightStyle: SpanStyle = bitwardenClickableTextSpanStyle,
    resources: Resources = LocalResources.current,
    onAnnotationClick: ((annotationKey: String) -> Unit)? = null,
): AnnotatedString = remember(
    this,
    args,
    style,
    emphasisHighlightStyle,
    linkHighlightStyle,
    onAnnotationClick,
) {
    val spannedString = try {
        resources.getText(this) as SpannedString
    } catch (_: ClassCastException) {
        // the resource did not contain any valid spans so we just return the raw string.
        return@remember resources.getString(this, *args).toAnnotatedString()
    }
    spannedString.buildAnnotatedString(
        args = args,
        style = style,
        emphasisHighlightStyle = emphasisHighlightStyle,
        linkHighlightStyle = linkHighlightStyle,
        onAnnotationClick = onAnnotationClick,
    )
}

/**
 * Creates an [AnnotatedString] from a plurals resource allowing for optional arguments to be
 * applied.
 *
 * @param quantity The quantity used to select the appropriate plural form from the resource.
 * @param args Optional arguments to be applied to the string resource, must already be a
 * [CharSequence]. Integer plural quantity arguments (`%1$d`) should be passed as a [CharSequence]
 * (for example, `"$count"`).
 * @param style Style to apply to the entire string.
 * @param emphasisHighlightStyle Style to apply to part of the resource that has been annotated
 * with the "emphasis" annotation.
 * @param linkHighlightStyle Style to apply to part of the resource that has been annotated with
 * the "link" annotation.
 * @param resources The resources used to access the strings.
 * @param onAnnotationClick Callback to invoke when a link annotation is clicked. Will pass back
 * the value of the annotation as a string to allow for delineation if there are multiple callbacks
 * to be applied.
 *
 * Annotation conventions match those documented on [Int.toAnnotatedString]; the resource must
 * declare `<annotation arg="N">` wrappers around any format arguments that should be replaced.
 */
@Composable
fun @receiver:PluralsRes Int.toAnnotatedPluralsString(
    quantity: Int,
    vararg args: CharSequence,
    style: SpanStyle = bitwardenDefaultSpanStyle,
    emphasisHighlightStyle: SpanStyle = bitwardenBoldSpanStyle,
    linkHighlightStyle: SpanStyle = bitwardenClickableTextSpanStyle,
    resources: Resources = LocalResources.current,
    onAnnotationClick: ((annotationKey: String) -> Unit)? = null,
): AnnotatedString = remember(
    this,
    quantity,
    args,
    style,
    emphasisHighlightStyle,
    linkHighlightStyle,
    onAnnotationClick,
) {
    val spannedString = try {
        resources.getQuantityText(this, quantity) as SpannedString
    } catch (_: ClassCastException) {
        // the resource did not contain any valid spans so we just return the raw string.
        return@remember resources
            .getQuantityString(this, quantity, *args)
            .toAnnotatedString()
    }
    spannedString.buildAnnotatedString(
        args = args,
        style = style,
        emphasisHighlightStyle = emphasisHighlightStyle,
        linkHighlightStyle = linkHighlightStyle,
        onAnnotationClick = onAnnotationClick,
    )
}

/**
 * Shared annotation-processing pipeline for both string and plurals resources. Replaces any
 * `arg` annotations with the corresponding [args] entry then applies emphasis and link styles
 * to the resulting [AnnotatedString].
 */
private fun SpannedString.buildAnnotatedString(
    args: Array<out CharSequence>,
    style: SpanStyle,
    emphasisHighlightStyle: SpanStyle,
    linkHighlightStyle: SpanStyle,
    onAnnotationClick: ((annotationKey: String) -> Unit)?,
): AnnotatedString {
    // The spannableBuilder is used to help parse through the annotations in the string resource.
    val spannableBuilder = SpannableStringBuilder(this)
    // Replace any format arguments with the provided arguments.
    spannableBuilder.applyArgAnnotations(args = args)

    // The annotatedStringBuilder is used to apply the styles to the string resource.
    val annotatedStringBuilder = AnnotatedString.Builder()

    // Add the entire string to the annotated string builder and apply the style.
    annotatedStringBuilder.append(spannableBuilder)
    annotatedStringBuilder.addStyle(
        style = style,
        start = 0,
        end = spannableBuilder.length,
    )
    val annotations = spannableBuilder.getSpans<Annotation>()
    // Iterate through the annotations and apply the appropriate style. If the [Annotation.key]
    // does not match a [ValidAnnotationType] an exception will be thrown.
    for (annotation in annotations) {
        // Skip the annotation if it does not have a valid start in the spanned string.
        val start = spannableBuilder.getSpanStart(annotation).takeIf { it >= 0 } ?: continue
        val end = spannableBuilder.getSpanEnd(annotation)
        when (ValidAnnotationType.valueOf(annotation.key.uppercase())) {
            ValidAnnotationType.EMPHASIS -> {
                annotatedStringBuilder.addStyle(
                    style = emphasisHighlightStyle,
                    start = start,
                    end = end,
                )
            }

            ValidAnnotationType.LINK -> {
                val link = LinkAnnotation.Clickable(
                    tag = annotation.value.orEmpty(),
                    styles = TextLinkStyles(
                        style = linkHighlightStyle,
                    ),
                ) {
                    onAnnotationClick?.invoke(annotation.value.orEmpty())
                }
                annotatedStringBuilder.addLink(
                    clickable = link,
                    start = start,
                    end = end,
                )
            }
            // Handled prior to this point, not styling to be applied.
            ValidAnnotationType.ARG -> Unit
        }
    }
    return annotatedStringBuilder.toAnnotatedString()
}

/**
 * The span between the <annotation arg="0"> and </annotation> tags in the string resource is
 * replaced with the index value in the provided [args].
 */
private fun SpannableStringBuilder.applyArgAnnotations(
    args: Array<out CharSequence>,
) {
    val argAnnotations = getSpans<Annotation>()
        .filter { it.isArgAnnotation() }
    for (annotation in argAnnotations) {
        // Skip the annotation if it does not have a valid start in the spanned string.
        val spanStart = getSpanStart(annotation).takeIf { it >= 0 } ?: continue
        val argIndex = Integer.parseInt(annotation.value)
        // if no string is available just replace it with an empty string.
        val replacementString = args.getOrNull(index = argIndex) ?: ""
        this.replace(
            spanStart,
            this.getSpanEnd(annotation),
            replacementString,
        )
    }
}

/**
 * Enumerated values representing the valid <annotation> keys that can be processed
 * by [Int.toAnnotatedString]
 */
private enum class ValidAnnotationType {
    ARG,
    LINK,
    EMPHASIS,
}

private fun Annotation.isArgAnnotation(): Boolean =
    this.key.uppercase() == ValidAnnotationType.ARG.name
