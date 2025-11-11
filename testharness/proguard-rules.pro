# CredentialManager classes
-keep class androidx.credentials.** { *; }
-keep interface androidx.credentials.** { *; }

# Hilt
-dontwarn com.google.errorprone.annotations.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.x8bit.bitwarden.testharness.**$$serializer { *; }
-keepclassmembers class com.x8bit.bitwarden.testharness.** {
    *** Companion;
}
-keepclasseswithmembers class com.x8bit.bitwarden.testharness.** {
    kotlinx.serialization.KSerializer serializer(...);
}
