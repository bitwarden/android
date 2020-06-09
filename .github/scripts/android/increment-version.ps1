$rootPath = $env:GITHUB_WORKSPACE;
$buildNumber = 3000 + [int]$env:GITHUB_RUN_NUMBER;

Write-Output "########################################"
Write-Output "##### Setting Version Code $buildNumber"
Write-Output "########################################"

$androidManifest = $($rootPath + "/src/Android/Properties/AndroidManifest.xml");

$xml=New-Object XML;
$xml.Load($androidManifest);

$node=$xml.SelectNodes("/manifest");
$node.SetAttribute("android:versionCode", [string]$buildNumber);

$xml.Save($androidManifest);
