msbuild "Android.csproj" "/t:Clean" "/p:Configuration=FDroid"
msbuild "..\App\App.csproj" "/t:Clean" "/p:Configuration=FDroid"

Uninstall-Package Xamarin.Firebase.Messaging
Uninstall-Package Xamarin.GooglePlayServices.Analytics
Uninstall-Package HockeySDK.Xamarin

msbuild "Android.csproj" "/logger:C:\Program Files\AppVeyor\BuildAgent\Appveyor.MSBuildLogger.dll" "/p:Configuration=FDroid"
msbuild "Android.csproj" "/t:SignAndroidPackage" "/p:Configuration=FDroid" "/p:AndroidKeyStore=true" "/p:AndroidSigningKeyAlias=bitwarden" "/p:AndroidSigningKeyPass=$($env:keystore_password)" "/p:AndroidSigningKeyStore=8bit.keystore" "/p:AndroidSigningStorePass=$($env:keystore_password)"

Copy-Item .\bin\FDroid\com.x8bit.bitwarden-Signed.apk ..\com.x8bit.bitwarden-fdroid-$($env:APPVEYOR_BUILD_NUMBER).apk
