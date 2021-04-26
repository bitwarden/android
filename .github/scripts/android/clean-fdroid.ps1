$rootPath = $env:GITHUB_WORKSPACE;

$androidPath = $($rootPath + "/src/Android/Android.csproj");
$appPath = $($rootPath + "/src/App/App.csproj");

$androidManifest = $($rootPath + "/src/Android/Properties/AndroidManifest.xml");

Write-Output "########################################"
Write-Output "##### Clean Android and App"
Write-Output "########################################"

msbuild "$($androidPath)" "/t:Clean" "/p:Configuration=FDroid"
msbuild "$($appPath)" "/t:Clean" "/p:Configuration=FDroid"

Write-Output "########################################"
Write-Output "##### Backup project files"
Write-Output "########################################"

Copy-Item $androidManifest $($androidManifest + ".original");
Copy-Item $androidPath $($androidPath + ".original");
Copy-Item $appPath $($appPath + ".original");

Write-Output "########################################"
Write-Output "##### Cleanup Android Manifest"
Write-Output "########################################"

$xml=New-Object XML;
$xml.Load($androidManifest);

$nsAndroid=New-Object System.Xml.XmlNamespaceManager($xml.NameTable);
$nsAndroid.AddNamespace("android", "http://schemas.android.com/apk/res/android");

$xml.Save($androidManifest);

Write-Output "########################################"
Write-Output "##### Uninstall from Android.csproj"
Write-Output "########################################"

$xml=New-Object XML;
$xml.Load($androidPath);

$ns=New-Object System.Xml.XmlNamespaceManager($xml.NameTable);
$ns.AddNamespace("ns", $xml.DocumentElement.NamespaceURI);

$firebaseNode=$xml.SelectSingleNode(`
    "/ns:Project/ns:ItemGroup/ns:PackageReference[@Include='Xamarin.Firebase.Messaging']", $ns);
$firebaseNode.ParentNode.RemoveChild($firebaseNode);

$daggerNode=$xml.SelectSingleNode(`
    "/ns:Project/ns:ItemGroup/ns:PackageReference[@Include='Xamarin.Google.Dagger']", $ns);
$daggerNode.ParentNode.RemoveChild($daggerNode);

$safetyNetNode=$xml.SelectSingleNode(`
    "/ns:Project/ns:ItemGroup/ns:PackageReference[@Include='Xamarin.GooglePlayServices.SafetyNet']", $ns);
$safetyNetNode.ParentNode.RemoveChild($safetyNetNode);

$xml.Save($androidPath);

Write-Output "########################################"
Write-Output "##### Uninstall from App.csproj"
Write-Output "########################################"

$xml=New-Object XML;
$xml.Load($appPath);

$appCenterNode=$xml.SelectSingleNode("/Project/ItemGroup/PackageReference[@Include='Microsoft.AppCenter.Crashes']");
$appCenterNode.ParentNode.RemoveChild($appCenterNode);

$xml.Save($appPath);
