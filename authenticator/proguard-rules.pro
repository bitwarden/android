################################################################################
# Bitwarden SDK
################################################################################

# We need to access the SDK using JNA and this makes it very easy to obfuscate away the SDK unless
# we keep it here. The SDK's packages are flat, so `.*` matches only SDK classes and not the app's
# own modules (e.g. `com.bitwarden.core.data`). Add new SDK packages here when the SDK adds them.
-keep class com.bitwarden.api.base.* { *; }
-keep class com.bitwarden.auth.* { *; }
-keep class com.bitwarden.collections.* { *; }
-keep class com.bitwarden.core.* { *; }
-keep class com.bitwarden.crypto.* { *; }
-keep class com.bitwarden.cryptosynchandler.* { *; }
-keep class com.bitwarden.encoding.* { *; }
-keep class com.bitwarden.exporters.* { *; }
-keep class com.bitwarden.fido.* { *; }
-keep class com.bitwarden.generators.* { *; }
-keep class com.bitwarden.importers.* { *; }
-keep class com.bitwarden.managedsettings.* { *; }
-keep class com.bitwarden.organizations.* { *; }
-keep class com.bitwarden.pm.* { *; }
-keep class com.bitwarden.policies.* { *; }
-keep class com.bitwarden.random.* { *; }
-keep class com.bitwarden.sdk.* { *; }
-keep class com.bitwarden.send.* { *; }
-keep class com.bitwarden.servercommunicationconfig.* { *; }
-keep class com.bitwarden.ssh.* { *; }
-keep class com.bitwarden.state.* { *; }
-keep class com.bitwarden.unlock.* { *; }
-keep class com.bitwarden.usercryptomanagement.* { *; }
-keep class com.bitwarden.vault.* { *; }

################################################################################
# Bitwarden Models
################################################################################

# Network layer models live in the `:network` module.
-keepclassmembers class com.bitwarden.network.model.** { *; }

# `BaseEnumeratedIntSerializer` reads each enum constant's `@SerialName` by field name via
# reflection, so the annotated constants must keep their names.
-keepclassmembers enum * {
    @kotlinx.serialization.SerialName <fields>;
}

################################################################################
# Firebase Crashlytics
################################################################################

# Keep file names and line numbers.
-keepattributes SourceFile,LineNumberTable

################################################################################
# kotlinx.serialization
################################################################################

-keepattributes *Annotation*, InnerClasses

################################################################################
# Google Protobuf generated files
################################################################################

-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { <fields>; }

################################################################################
# JNA
################################################################################

# See https://github.com/java-native-access/jna/blob/fdb8695fb9b05fba467dadfe5735282f8bcc053d/www/FrequentlyAskedQuestions.md#jna-on-android
-dontwarn java.awt.*
-keep class com.sun.jna.* { *; }
-keep @com.sun.jna.* class *

################################################################################
# Okhttp/Retrofit https://square.github.io/okhttp/ & https://square.github.io/retrofit/
################################################################################

# Retrofit and OkHttp bundle their own consumer rules; only the BouncyCastle warning is needed here.
-dontwarn org.bouncycastle.**

################################################################################
# ZXing
################################################################################

# Suppress zxing missing class error due to circular references
-dontwarn com.google.zxing.BarcodeFormat
-dontwarn com.google.zxing.EncodeHintType
-dontwarn com.google.zxing.MultiFormatWriter
-dontwarn com.google.zxing.common.BitMatrix
