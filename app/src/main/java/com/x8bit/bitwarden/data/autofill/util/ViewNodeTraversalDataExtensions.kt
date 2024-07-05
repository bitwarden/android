package com.x8bit.bitwarden.data.autofill.util

import android.app.assist.AssistStructure
import com.x8bit.bitwarden.data.autofill.model.ViewNodeTraversalData
import com.x8bit.bitwarden.ui.platform.base.util.orNullIfBlank

/**
 * The android app URI scheme. Example: androidapp://com.x8bit.bitwarden
 */
private const val ANDROID_APP_SCHEME: String = "androidapp"

/**
 * Try and build a URI. First, try building a website from the list of [ViewNodeTraversalData]. If
 * that fails, try converting [packageName] into an Android app URI.
 */
fun List<ViewNodeTraversalData>.buildUriOrNull(
    packageName: String?,
): String? {
    // Search list of ViewNodeTraversalData for a website URI.
    this
        .firstOrNull { it.website != null }
        ?.website
        ?.let { websiteUri ->
            return websiteUri
        }

    // If the package name is available, build a URI out of that.
    return packageName
        ?.let { nonNullPackageName ->
            buildUri(
                domain = nonNullPackageName,
                scheme = ANDROID_APP_SCHEME,
            )
        }
}

/**
 * Try and build a package name. First, try searching traversal data for package names. If that
 * fails, try extracting a package name from [assistStructure].
 */
fun List<ViewNodeTraversalData>.buildPackageNameOrNull(
    assistStructure: AssistStructure,
): String? {
    // Search list of ViewNodeTraversalData for a valid package name.
    val traversalDataPackageName = this
        .firstOrNull { it.idPackage != null }
        ?.idPackage

    // Try getting the package name from the AssistStructure as a last ditch effort.
    return traversalDataPackageName
        ?: assistStructure
            .buildPackageNameOrNull()
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
