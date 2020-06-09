$rootPath = $env:GITHUB_WORKSPACE;

$androidPath = $($rootPath + "/src/Android/Android.csproj");

$appKeystorePlayFilename = "app_play-keystore.jks";
$appKeystoreUploadFilename = "app_upload-keystore.jks";

Write-Output "########################################"
Write-Output "##### Sign Google Play Bundle Release Configuration"
Write-Output "########################################"

msbuild "$($androidPath)" "/t:SignAndroidPackage" "/p:Configuration=Release" "/p:AndroidKeyStore=true" `
    "/p:AndroidSigningKeyAlias=upload" "/p:AndroidSigningKeyPass=$($env:UPLOAD_KEYSTORE_PASSWORD)" `
    "/p:AndroidSigningKeyStore=$($appKeystoreUploadFilename)" `
    "/p:AndroidSigningStorePass=$($env:UPLOAD_KEYSTORE_PASSWORD)" "/p:AndroidPackageFormat=aab" "/v:quiet"

Write-Output "########################################"
Write-Output "##### Copy Google Play Bundle to project root"
Write-Output "########################################"

$signedAabPath = $($rootPath + "/src/Android/bin/Release/com.x8bit.bitwarden-Signed.aab");
$signedAabDestPath = $($rootPath + "/com.x8bit.bitwarden.aab");

Copy-Item $signedAabPath $signedAabDestPath

Write-Output "########################################"
Write-Output "##### Sign APK Release Configuration"
Write-Output "########################################"

msbuild "$($androidPath)" "/t:SignAndroidPackage" "/p:Configuration=Release" "/p:AndroidKeyStore=true" `
    "/p:AndroidSigningKeyAlias=bitwarden" "/p:AndroidSigningKeyPass=$($env:PLAY_KEYSTORE_PASSWORD)" `
    "/p:AndroidSigningKeyStore=$($appKeystorePlayFilename)" `
    "/p:AndroidSigningStorePass=$($env:PLAY_KEYSTORE_PASSWORD)" "/v:quiet"
	
Write-Output "########################################"
Write-Output "##### Copy Release APK to project root"
Write-Output "########################################"

$signedApkPath = $($rootPath + "/src/Android/bin/Release/com.x8bit.bitwarden-Signed.apk");
$signedApkDestPath = $($rootPath + "/com.x8bit.bitwarden.apk");

Copy-Item $signedApkPath $signedApkDestPath
