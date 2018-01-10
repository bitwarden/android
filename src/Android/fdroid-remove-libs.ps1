$rootPath = $args[0];

$androidPath = $($rootPath + "\src\Android\Android.csproj");
$appPath = $($rootPath + "\src\App\App.csproj");

# Backup files

Copy-Item $androidPath $($androidPath + ".original");
Copy-Item $appPath $($appPath + ".original");

# Android.csproj

$xml=New-Object XML;
$xml.Load($androidPath);

$ns=New-Object System.Xml.XmlNamespaceManager($xml.NameTable);
$ns.AddNamespace("ns", $xml.DocumentElement.NamespaceURI);

$firebaseNode=$xml.SelectSingleNode("/ns:Project/ns:ItemGroup/ns:PackageReference[@Include='Xamarin.Firebase.Messaging']", $ns);
$firebaseNode.ParentNode.RemoveChild($firebaseNode);

$playServiceNode=$xml.SelectSingleNode("/ns:Project/ns:ItemGroup/ns:PackageReference[@Include='Xamarin.GooglePlayServices.Analytics']", $ns);
$playServiceNode.ParentNode.RemoveChild($playServiceNode);

$xml.Save($androidPath);

# App.csproj

$xml=New-Object XML;
$xml.Load($appPath);

$hockeyNode=$xml.SelectSingleNode("/Project/ItemGroup/PackageReference[@Include='HockeySDK.Xamarin']");
$hockeyNode.ParentNode.RemoveChild($hockeyNode);

$xml.Save($appPath);
