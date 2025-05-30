@file:OmitFromCoverage

package com.bitwarden.ui.platform.base.util

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import com.bitwarden.annotation.OmitFromCoverage

/**
 * Creates an [AnnotatedString] from a string from a resource and allows for optional arguments
 * to be applied.
 *
 * @see Int.toAnnotatedString
 */
@Composable
fun annotatedStringResource(
    @StringRes id: Int,
    vararg args: String,
    style: SpanStyle = bitwardenDefaultSpanStyle,
    emphasisHighlightStyle: SpanStyle = bitwardenBoldSpanStyle,
    linkHighlightStyle: SpanStyle = bitwardenClickableTextSpanStyle,
    resources: Resources = LocalContext.current.resources,
    onAnnotationClick: ((annotationKey: String) -> Unit)? = null,
): AnnotatedString =
    id.toAnnotatedString(
        args = args,
        style = style,
        emphasisHighlightStyle = emphasisHighlightStyle,
        linkHighlightStyle = linkHighlightStyle,
        resources = resources,
        onAnnotationClick = onAnnotationClick,
    )
