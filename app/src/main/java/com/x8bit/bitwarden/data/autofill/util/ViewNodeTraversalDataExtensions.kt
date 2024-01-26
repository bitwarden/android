package com.x8bit.bitwarden.data.autofill.util

import android.app.assist.AssistStructure
import com.x8bit.bitwarden.data.autofill.model.ViewNodeTraversalData
import com.x8bit.bitwarden.ui.platform.base.util.orNullIfBlank

/**
 * The android app URI scheme. Example: androidapp://com.x8bit.bitwarden
 */
private const val ANDROID_APP_SCHEME: String = "androidapp"

/**
 * Try and build a URI. The try progression looks like this:
 * 1. Try searching traversal data for website URIs.
 * 2. Try searching traversal data for package names, if one is found, convert it into a URI.
 * 3. Try extracting a package name from [assistStructure], if one is found, convert it into a URI.
 */
@Suppress("ReturnCount")
fun List<ViewNodeTraversalData>.buildUriOrNull(
    assistStructure: AssistStructure,
): String? {
    // Search list of [ViewNodeTraversalData] for a website URI.
    this
        .firstOrNull { it.website != null }
        ?.website
        ?.let { websiteUri ->
            return websiteUri
        }

    // Search list of [ViewNodeTraversalData] for a valid package name.
    this
        .firstOrNull { it.idPackage != null }
        ?.idPackage
        ?.let { packageName ->
            return buildUri(
                domain = packageName,
                scheme = ANDROID_APP_SCHEME,
            )
        }

    // Try getting the package name from the [AssistStructure] as a last ditch effort.
    return assistStructure
        .buildPackageNameOrNull()
        ?.let { packageName ->
            buildUri(
                domain = packageName,
                scheme = ANDROID_APP_SCHEME,
            )
        }
}

/**
 * Combine [domain] and [scheme] into a URI.
 */
fun buildUri(
    domain: String,
    scheme: String,
): String = "$scheme://$domain"

/**
 * Attempt to extract the package name from the title of the [AssistStructure].
 *
 * As an example, the title might look like this: com.facebook.katana/com.facebook.bloks.facebook...
 * Then this function would return: com.facebook.katana
 */
private fun AssistStructure.buildPackageNameOrNull(): String? = if (windowNodeCount > 0) {
    getWindowNodeAt(0)
        .title
        ?.toString()
        ?.orNullIfBlank()
        ?.split('/')
        ?.firstOrNull()
} else {
    null
}
