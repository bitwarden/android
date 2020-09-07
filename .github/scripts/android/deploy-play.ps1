$rootPath = $env:GITHUB_WORKSPACE;
$homePath = Resolve-Path "~" | Select-Object -ExpandProperty Path;

$publisherPath = $($rootPath + "/store/google/Publisher/bin/Release/netcoreapp2.0/Publisher.dll");
$credsPath = $($homePath + "/secrets/play_creds.json");
$aabPath = $($rootPath + "/com.x8bit.bitwarden.aab");
$track = "internal";

dotnet $publisherPath $credsPath $aabPath $track
