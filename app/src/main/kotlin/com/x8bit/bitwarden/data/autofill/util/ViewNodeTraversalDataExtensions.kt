package com.x8bit.bitwarden.data.autofill.util

import android.app.assist.AssistStructure
import com.bitwarden.ui.platform.base.util.orNullIfBlank
import com.x8bit.bitwarden.data.autofill.model.ViewNodeTraversalData

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
