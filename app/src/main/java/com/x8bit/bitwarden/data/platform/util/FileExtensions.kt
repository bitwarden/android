package com.x8bit.bitwarden.data.platform.util

import com.bitwarden.annotation.OmitFromCoverage
import java.io.File

/**
 * A helper function for creating a file from a path.
 */
@OmitFromCoverage
fun fileOf(path: String): File = File(path)
