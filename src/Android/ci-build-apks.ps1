$rootPath = $env:APPVEYOR_BUILD_FOLDER;
$androidPath = $($rootPath + "\src\Android\Android.csproj");
$appPath = $($rootPath + "\src\App\App.csproj");

echo "##### Increment Version"

$androidManifest = $($rootPath + "\src\Android\Properties\AndroidManifest.xml");

$xml=New-Object XML;
$xml.Load($androidManifest);

$node=$xml.SelectNodes("/manifest");
$node.SetAttribute("android:versionCode", $env:APPVEYOR_BUILD_NUMBER);

$xml.Save($androidManifest);

echo "##### Decrypt Keystore"

$encKeystorePath = $($rootPath + "\src\Android\8bit.keystore.enc");
$secureFilePath = $($rootPath + "\secure-file\tools\secure-file.exe");

Invoke-Expression "& `"$secureFilePath`" -decrypt $($encKeystorePath) -secret $($env:keystore_dec_secret)"

echo "##### Sign Release Configuration"

msbuild "$($androidPath)" "/t:SignAndroidPackage" "/p:Configuration=Release" "/p:AndroidKeyStore=true" "/p:AndroidSigningKeyAlias=bitwarden" "/p:AndroidSigningKeyPass=$($env:keystore_password)" "/p:AndroidSigningKeyStore=8bit.keystore" "/p:AndroidSigningStorePass=$($env:keystore_password)" "/v:quiet"

echo "##### Copy Release apk to project root"

$signedApkPath = $($rootPath + "\src\Android\bin\Release\com.x8bit.bitwarden-Signed.apk");
$signedApkDestPath = $($rootPath + "\com.x8bit.bitwarden-" + $env:APPVEYOR_BUILD_NUMBER + ".apk");

Copy-Item $signedApkPath $signedApkDestPath

echo "##### Clean Android and App"

msbuild "$($androidPath)" "/t:Clean" "/p:Configuration=FDroid"
msbuild "$($appPath)" "/t:Clean" "/p:Configuration=FDroid"

echo "##### Build and Sign FDroid Configuration"

msbuild "$($androidPath)" "/logger:C:\Program Files\AppVeyor\BuildAgent\Appveyor.MSBuildLogger.dll" "/p:Configuration=FDroid"
msbuild "$($androidPath)" "/t:SignAndroidPackage" "/p:Configuration=FDroid" "/p:AndroidKeyStore=true" "/p:AndroidSigningKeyAlias=bitwarden" "/p:AndroidSigningKeyPass=$($env:keystore_password)" "/p:AndroidSigningKeyStore=8bit.keystore" "/p:AndroidSigningStorePass=$($env:keystore_password)" "/v:quiet"

echo "##### Copy FDroid apk to project root"

$signedApkPath = $($rootPath + "\src\Android\bin\FDroid\com.x8bit.bitwarden-Signed.apk");
$signedApkDestPath = $($rootPath + "\com.x8bit.bitwarden-fdroid-" + $env:APPVEYOR_BUILD_NUMBER + ".apk");

Copy-Item $signedApkPath $signedApkDestPath

echo "##### Done"
