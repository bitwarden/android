$rootPath = "%APPVEYOR_BUILD_FOLDER%";
$newVersionCode = "%APPVEYOR_BUILD_NUMBER%";

$xml=New-Object XML;
$xml.Load($rootPath + "src\Android\Properties\AndroidManifest.xml");
$node=$xml.SelectNodes("/manifest");
$node.SetAttribute("android:versionCode", $newVersionCode);
$xml.Save($rootPath + "src\Android\Properties\AndroidManifest.xml");