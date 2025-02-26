package com.bitwarden.authenticator.data.platform.annotation

/**
 * Used to omit the annotated class from test coverage reporting. This should be used sparingly and
 * is intended for non-testable classes that are placed in packages along with testable ones.
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FILE,
    AnnotationTarget.FUNCTION,
)
@Retention(AnnotationRetention.BINARY)
annotation class OmitFromCoverage
