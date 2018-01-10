$rootPath = $args[0];

# Android.csproj

$xml=New-Object XML;
$xml.Load($rootPath + "\src\Android\Android.csproj");

$ns=New-Object System.Xml.XmlNamespaceManager($xml.NameTable);
$ns.AddNamespace("ns", $xml.DocumentElement.NamespaceURI);

$firebaseNode=$xml.SelectSingleNode("/ns:Project/ns:ItemGroup/ns:PackageReference[@Include='Xamarin.Firebase.Messaging']", $ns);
$firebaseNode.ParentNode.RemoveChild($firebaseNode);

$playServiceNode=$xml.SelectSingleNode("/ns:Project/ns:ItemGroup/ns:PackageReference[@Include='Xamarin.GooglePlayServices.Analytics']", $ns);
$playServiceNode.ParentNode.RemoveChild($playServiceNode);

$xml.Save($rootPath + "\src\Android\Android.csproj");

# App.csproj

$xml=New-Object XML;
$xml.Load($rootPath + "\src\App\App.csproj");

$hockeyNode=$xml.SelectSingleNode("/Project/ItemGroup/PackageReference[@Include='HockeySDK.Xamarin']");
$hockeyNode.ParentNode.RemoveChild($hockeyNode);

$xml.Save($rootPath + "\src\App\App.csproj");
