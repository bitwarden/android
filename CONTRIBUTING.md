# How to Contribute

Contributions of all kinds are welcome!

Please visit our [Community Forums](https://community.bitwarden.com/) for general community discussion and the development roadmap.

Here is how you can get involved:

* **Request a new feature:** Go to the [Feature Requests category](https://community.bitwarden.com/c/feature-requests/) of the Community Forums. Please search existing feature requests before making a new one
  
* **Write code for a new feature:** Make a new post in the [Github Contributions category](https://community.bitwarden.com/c/github-contributions/) of the Community Forums. Include a description of your proposed contribution, screeshots, and links to any relevant feature requests. This helps get feedback from the community and Bitwarden team members before you start writing code
  
* **Report a bug or submit a bugfix:** Use Github issues and pull requests
  
* **Write documentation:** Submit a pull request to the [Bitwarden help repository](https://github.com/bitwarden/help)
  
* **Help other users:** Go to the [Ask the Bitwarden Community category](https://community.bitwarden.com/c/support/) on the Community Forums
  
* **Translate:** See the localization (i10n) section below

## Contributor Agreement

Please sign the [Contributor Agreement](https://cla-assistant.io/bitwarden/mobile) if you intend on contributing to any Github repository. Pull requests cannot be accepted and merged unless the author has signed the Contributor Agreement.

## Pull Request Guidelines

* commit any pull requests against the `master` branch
* include a link to your Community Forums post

# Localization (l10n)

[![Crowdin](https://d322cqt584bo4o.cloudfront.net/bitwarden-mobile/localized.svg)](https://crowdin.com/project/bitwarden-mobile)

We use a translation tool called [Crowdin](https://crowdin.com) to help manage our localization efforts across many different languages.

If you are interested in helping translate the Bitwarden mobile app into another language (or make a translation correction), please register an account at Crowdin and join our project here: https://crowdin.com/project/bitwarden-mobile

If the language that you are interested in translating is not already listed, create a new account on Crowdin, join the project, and contact the project owner (https://crowdin.com/profile/dwbit).

You can read Crowdin's getting started guide for translators here: https://support.crowdin.com/crowdin-intro/

# Set Up Local Development Enviornment

## iOS App

Overall, there are several steps required to create an iOS build on your local machine. One important thing you need to change to make the project runable on local device is to change all the `com.8bit` to something else, for example, `com.example`. The reason that we need to make this change is: we cannnot create App Ids that contain the same domain as `com.8bit`. And we will use `com.example` as an example in the following steps. 

otherwise, you cannot create the **App Ids** and **App Group**.

1. Create an **Apple Development Certificate**. This can be done either through:
   - Xcode: Xcode -> Preference -> Accounts -> Add Apple Id Account -> Manage Certificates -> Create **Apple Development** Certificate
   - [Apple Developer Portal](https://developer.apple.com/account/resources/certificates/list)
2. Create an App Group:
   - Create an App Group by using [Apple Developer Portal](https://developer.apple.com/account/resources/identifiers/list/applicationGroup)
   - Set the Identifier to `group.com.example.bitwarden`
3. Create an iCloud Container:
   - Create an iCloud Container by using [Apple Developer Portal](https://developer.apple.com/account/resources/identifiers/list/cloudContainer)
   - Set the Identifier to `iCloud.com.example.bitwarden`
4. Create 4 App Ids
   - Create App Ids by using [Apple Developer Portal](https://developer.apple.com/account/resources/identifiers/list)
   - `com.example.bitwarden` with capabilities:
        - App Groups: seleect the one we created in ***Step 2***
        - Associated Domains: select the one we created in ***Step 3***
        - AutoFill Credential Provider
        - iCloud 
        - NFC Tag Reading
        - Push Notifications (no need to configure certificates)
   - `com.example.bitwarden.autofill` with capabilities:
        - App Groups: seleect the one we created in step 2
        - AutoFill Credential Provider
        - Push Notifications (no need to configure certificates)
   - `com.example.bitwarden.find-login-action-extension`
        - App Groups: seleect the one we created in step 2
        - Push Notifications (no need to configure certificates)
   - `com.example.bitwarden.share-extension`
        - App Groups: seleect the one we created in step 2
        - Push Notifications (no need to configure certificates)
5. Create, Download, and Install 4 Profiles
   - We also need to create 4 profiles that match with the 4 App Ids. All of those profiles should use the same Development Certificate that you created in ***Step 1***.
   - Download and Install the Profiles:
        - Manually: Download the profile and double click it
        - Xcode: Xcode -> Preference -> Accounts -> Select Apple Id Account -> Download Manual Profiles
   - Make sure `~/Library/MobileDevice/Provisioning Profiles` contains all the newly downloaded **Povisioning Profiles** 
6. Replace all the `com.8bit` by `com.example` and change the `LTZ2PFU5D6` Team Id to your own Team Id.
   - Here is a patch that you can modify and apply it to your branch.
```diff
diff --git a/src/iOS.Autofill/Entitlements.plist b/src/iOS.Autofill/Entitlements.plist
--- a/src/iOS.Autofill/Entitlements.plist
+++ b/src/iOS.Autofill/Entitlements.plist
@@ -6,11 +6,11 @@
 	<true/>
 	<key>com.apple.security.application-groups</key>
 	<array>
-		<string>group.com.8bit.bitwarden</string>
+		<string>group.com.{domain}.bitwarden</string>
 	</array>
 	<key>keychain-access-groups</key>
 	<array>
-		<string>$(AppIdentifierPrefix)com.8bit.bitwarden</string>
+		<string>$(AppIdentifierPrefix)com.{domain}.bitwarden</string>
 	</array>
 </dict>
 </plist>
diff --git a/src/iOS.Autofill/Info.plist b/src/iOS.Autofill/Info.plist
--- a/src/iOS.Autofill/Info.plist
+++ b/src/iOS.Autofill/Info.plist
@@ -9,7 +9,7 @@
 	<key>CFBundleName</key>
 	<string>Bitwarden Autofill</string>
 	<key>CFBundleIdentifier</key>
-	<string>com.8bit.bitwarden.autofill</string>
+	<string>com.{domain}.bitwarden.autofill</string>
 	<key>CFBundleShortVersionString</key>
 	<string>2022.05.1</string>
 	<key>CFBundleVersion</key>
diff --git a/src/iOS.Core/Constants.cs b/src/iOS.Core/Constants.cs
--- a/src/iOS.Core/Constants.cs
+++ b/src/iOS.Core/Constants.cs
@@ -27,7 +27,7 @@
         public const string UTTypeAppExtensionChangePasswordAction = "org.appextension.change-password-action";
         public const string UTTypeAppExtensionFillWebViewAction = "org.appextension.fill-webview-action";
         public const string UTTypeAppExtensionFillBrowserAction = "org.appextension.fill-browser-action";
-        public const string UTTypeAppExtensionSetup = "com.8bit.bitwarden.extension-setup";
+        public const string UTTypeAppExtensionSetup = "com.{domain}.bitwarden.extension-setup";
         public const string UTTypeAppExtensionUrl = "public.url";
         public const string UTTypeAppExtensionImage = "public.image";
 
diff --git a/src/iOS.Core/Utilities/iOSCoreHelpers.cs b/src/iOS.Core/Utilities/iOSCoreHelpers.cs
--- a/src/iOS.Core/Utilities/iOSCoreHelpers.cs
+++ b/src/iOS.Core/Utilities/iOSCoreHelpers.cs
@@ -19,11 +19,11 @@ namespace Bit.iOS.Core.Utilities
 {
     public static class iOSCoreHelpers
     {
-        public static string AppId = "com.8bit.bitwarden";
-        public static string AppAutofillId = "com.8bit.bitwarden.autofill";
-        public static string AppExtensionId = "com.8bit.bitwarden.find-login-action-extension";
-        public static string AppGroupId = "group.com.8bit.bitwarden";
-        public static string AccessGroup = "LTZ2PFU5D6.com.8bit.bitwarden";
+        public static string AppId = "com.{domain}.bitwarden";
+        public static string AppAutofillId = "com.{domain}.bitwarden.autofill";
+        public static string AppExtensionId = "com.{domain}.bitwarden.find-login-action-extension";
+        public static string AppGroupId = "group.com.{domain}.bitwarden";
+        public static string AccessGroup = "{team_id}.com.{domain}.bitwarden";
 
         public static void InitLogger()
         {
diff --git a/src/iOS.Extension/Entitlements.plist b/src/iOS.Extension/Entitlements.plist
--- a/src/iOS.Extension/Entitlements.plist
+++ b/src/iOS.Extension/Entitlements.plist
@@ -4,11 +4,11 @@
 <dict>
 	<key>com.apple.security.application-groups</key>
 	<array>
-		<string>group.com.8bit.bitwarden</string>
+		<string>group.com.{domain}.bitwarden</string>
 	</array>
 	<key>keychain-access-groups</key>
 	<array>
-		<string>$(AppIdentifierPrefix)com.8bit.bitwarden</string>
+		<string>$(AppIdentifierPrefix)com.{domain}.bitwarden</string>
 	</array>
 </dict>
 </plist>
diff --git a/src/iOS.Extension/Info.plist b/src/iOS.Extension/Info.plist
--- a/src/iOS.Extension/Info.plist
+++ b/src/iOS.Extension/Info.plist
@@ -9,7 +9,7 @@
 	<key>CFBundleName</key>
 	<string>Bitwarden Extension</string>
 	<key>CFBundleIdentifier</key>
-	<string>com.8bit.bitwarden.find-login-action-extension</string>
+	<string>com.{domain}.bitwarden.find-login-action-extension</string>
 	<key>CFBundleShortVersionString</key>
 	<string>2022.05.1</string>
 	<key>CFBundleLocalizations</key>
@@ -103,7 +103,7 @@
 			||  ANY $attachment.registeredTypeIdentifiers UTI-CONFORMS-TO "org.appextension.change-password-action"
 			||  ANY $attachment.registeredTypeIdentifiers UTI-CONFORMS-TO "org.appextension.fill-webview-action"
 			||  ANY $attachment.registeredTypeIdentifiers UTI-CONFORMS-TO "org.appextension.fill-browser-action"
-            ||  ANY $attachment.registeredTypeIdentifiers UTI-CONFORMS-TO "com.8bit.bitwarden.extension-setup"
+            ||  ANY $attachment.registeredTypeIdentifiers UTI-CONFORMS-TO "com.{domain}.bitwarden.extension-setup"
 		).@count == $extensionItem.attachments.@count
 	).@count == 1</string>
 		</dict>
diff --git a/src/iOS.ShareExtension/Entitlements.plist b/src/iOS.ShareExtension/Entitlements.plist
--- a/src/iOS.ShareExtension/Entitlements.plist
+++ b/src/iOS.ShareExtension/Entitlements.plist
@@ -4,11 +4,11 @@
 <dict>
 	<key>com.apple.security.application-groups</key>
 	<array>
-		<string>group.com.8bit.bitwarden</string>
+		<string>group.com.{domain}.bitwarden</string>
 	</array>
 	<key>keychain-access-groups</key>
 	<array>
-		<string>$(AppIdentifierPrefix)com.8bit.bitwarden</string>
+		<string>$(AppIdentifierPrefix)com.{domain}.bitwarden</string>
 	</array>
 </dict>
 </plist>
diff --git a/src/iOS.ShareExtension/Info.plist b/src/iOS.ShareExtension/Info.plist
--- a/src/iOS.ShareExtension/Info.plist
+++ b/src/iOS.ShareExtension/Info.plist
@@ -7,7 +7,7 @@
 	<key>CFBundleName</key>
 	<string>Bitwarden Share Extension</string>
 	<key>CFBundleIdentifier</key>
-	<string>com.8bit.bitwarden.share-extension</string>
+	<string>com.{domain}.bitwarden.share-extension</string>
 	<key>CFBundleDevelopmentRegion</key>
 	<string>en</string>
 	<key>CFBundleInfoDictionaryVersion</key>
diff --git a/src/iOS/Entitlements.plist b/src/iOS/Entitlements.plist
--- a/src/iOS/Entitlements.plist
+++ b/src/iOS/Entitlements.plist
@@ -6,11 +6,11 @@
 	<true/>
 	<key>com.apple.security.application-groups</key>
 	<array>
-		<string>group.com.8bit.bitwarden</string>
+		<string>group.com.{domain}.bitwarden</string>
 	</array>
 	<key>keychain-access-groups</key>
 	<array>
-		<string>$(AppIdentifierPrefix)com.8bit.bitwarden</string>
+		<string>$(AppIdentifierPrefix)com.{domain}.bitwarden</string>
 	</array>
 	<key>com.apple.developer.ubiquity-container-identifiers</key>
 	<array>
diff --git a/src/iOS/Info.plist b/src/iOS/Info.plist
--- a/src/iOS/Info.plist
+++ b/src/iOS/Info.plist
@@ -9,7 +9,7 @@
 	<key>CFBundleName</key>
 	<string>Bitwarden</string>
 	<key>CFBundleIdentifier</key>
-	<string>com.8bit.bitwarden</string>
+	<string>com.{domain}.bitwarden</string>
 	<key>CFBundleShortVersionString</key>
 	<string>2022.05.1</string>
 	<key>CFBundleVersion</key>
@@ -27,7 +27,7 @@
 			<key>CFBundleTypeRole</key>
 			<string>Editor</string>
 			<key>CFBundleURLName</key>
-			<string>com.8bit.bitwarden.url</string>
+			<string>com.{domain}.bitwarden.url</string>
 		</dict>
 	</array>
 	<key>CFBundleLocalizations</key>

```
7. you can use `Debug iPhone` or `Debug iPhone Simulator` target to build and test the App now
