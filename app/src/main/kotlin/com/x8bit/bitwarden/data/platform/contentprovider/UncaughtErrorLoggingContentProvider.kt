package com.x8bit.bitwarden.data.platform.contentprovider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.bitwarden.annotation.OmitFromCoverage
import timber.log.Timber

/**
 * [ContentProvider] for setting up uncaught error logging.
 *
 * This allows us to play nice with Crashlytics since it is also instantiated as
 * a content provider and has it's own uncaught exception handler.
 */
@OmitFromCoverage
class UncaughtErrorLoggingContentProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        val defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Timber.e(exception, "Uncaught exception")
            defaultUncaughtExceptionHandler?.uncaughtException(thread, exception)
        }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
}
