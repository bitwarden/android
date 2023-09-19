################################################################################
# Bitwarden SDK
################################################################################

# We need to access the SDK using JNA and this makes it very easy to obfuscate away the SDK unless
# we keep it here.
-keep class com.bitwarden.sdk.** { *; }

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

# JsonElement serializers
-keep,includedescriptorclasses class kotlinx.serialization.json.**$$serializer { *; }
-keep,includedescriptorclasses class com.x8bit.bitwarden.**$$serializer { *; }
-keepclassmembers class com.x8bit.bitwarden.** {
    *** Companion;
}
-keepclasseswithmembers class om.x8bit.bitwarden.** {
    kotlinx.serialization.KSerializer serializer(...);
}

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
# Okhttp/Retrofit https://square.github.io/okhttp/ & https://square.github.io/retrofit/
################################################################################

# https://github.com/square/okhttp/blob/339732e3a1b78be5d792860109047f68a011b5eb/okhttp/src/jvmMain/resources/META-INF/proguard/okhttp3.pro#L11-L14
-dontwarn okhttp3.internal.platform.**
-dontwarn org.bouncycastle.**
# Related to this issue on https://github.com/square/retrofit/issues/3880
# Check https://github.com/square/retrofit/tags for new versions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
# This solves this issue https://github.com/square/retrofit/issues/3880
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

################################################################################
# ZXing
################################################################################

# Suppress zxing missing class error due to circular references
-dontwarn com.google.zxing.BarcodeFormat
-dontwarn com.google.zxing.EncodeHintType
-dontwarn com.google.zxing.MultiFormatWriter
-dontwarn com.google.zxing.common.BitMatrix
