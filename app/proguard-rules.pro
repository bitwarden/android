################################################################################
# Bitwarden SDK
################################################################################

# We need to access the SDK using JNA and this makes it very easy to obfuscate away the SDK unless
# we keep it here.
-keep class com.bitwarden.** { *; }

################################################################################
# Bitwarden Models
################################################################################

# Keep all enums
-keepclassmembers enum * { *; }

################################################################################
# Credential Manager
################################################################################

-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
    *;
}

################################################################################
# Firebase Crashlytics
################################################################################

# Keep file names and line numbers.
-keepattributes SourceFile,LineNumberTable

# Keep custom exceptions.
-keep public class * extends java.lang.Exception

################################################################################
# kotlinx.serialization
################################################################################

-keepattributes *Annotation*, InnerClasses

# kotlinx-serialization-json specific.
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

################################################################################
# Glide
################################################################################

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule

################################################################################
# Google Protobuf generated files
################################################################################

-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }

################################################################################
# JNA
################################################################################

# See https://github.com/java-native-access/jna/blob/fdb8695fb9b05fba467dadfe5735282f8bcc053d/www/FrequentlyAskedQuestions.md#jna-on-android
-dontwarn java.awt.*
-keep class com.sun.jna.* { *; }
-keepclassmembers class * extends com.sun.jna.* { public *; }

# Keep annotated classes
-keep @com.sun.jna.* class *
-keepclassmembers class * {
    @com.sun.jna.* *;
}

################################################################################
# ZXing
################################################################################

# Suppress zxing missing class error due to circular references
-dontwarn com.google.zxing.BarcodeFormat
-dontwarn com.google.zxing.EncodeHintType
-dontwarn com.google.zxing.MultiFormatWriter
-dontwarn com.google.zxing.common.BitMatrix
