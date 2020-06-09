$rootPath = $env:GITHUB_WORKSPACE;

$androidPath = $($rootPath + "/src/Android/Android.csproj");

$appKeystoreFdroidFilename = "app_fdroid-keystore.jks";

Write-Output "########################################"
Write-Output "##### Sign FDroid Configuration"
Write-Output "########################################"

msbuild "$($androidPath)" "/t:SignAndroidPackage" "/p:Configuration=FDroid" "/p:AndroidKeyStore=true" `
    "/p:AndroidSigningKeyAlias=bitwarden" "/p:AndroidSigningKeyPass=$($env:FDROID_KEYSTORE_PASSWORD)" `
    "/p:AndroidSigningKeyStore=$($appKeystoreFdroidFilename)" `
    "/p:AndroidSigningStorePass=$($env:FDROID_KEYSTORE_PASSWORD)" "/v:quiet"

Write-Output "########################################"
Write-Output "##### Copy FDroid apk to project root"
Write-Output "########################################"

$signedApkPath = $($rootPath + "/src/Android/bin/FDroid/com.x8bit.bitwarden-Signed.apk");
$signedApkDestPath = $($rootPath + "/com.x8bit.bitwarden-fdroid.apk");

Copy-Item $signedApkPath $signedApkDestPath
