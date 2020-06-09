$rootPath = $env:GITHUB_WORKSPACE;
$homePath = $env:HOME;

$publisherPath = $($rootPath + "\store\google\Publisher\bin\Release\netcoreapp2.0\Publisher.dll");
$credsPath = $($homePath + "\secrets\play_creds.json");
$aabPath = $($rootPath + "\com.x8bit.bitwarden.aab");
$track = "alpha";

dotnet $publisherPath $credsPath $aabPath $track
