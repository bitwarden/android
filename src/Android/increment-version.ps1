$rootPath = $args[0];
$newVersionCode = $args[1];

$xml=New-Object XML;
$xml.Load($rootPath + "\src\Android\Properties\AndroidManifest.xml");
$node=$xml.SelectNodes("/manifest");
$node.SetAttribute("android:versionCode", $newVersionCode);
$xml.Save($rootPath + "\src\Android\Properties\AndroidManifest.xml");