param (
  [Parameter(Mandatory=$true)]
  [string] $method
)

$rootPath = $env:GITHUB_WORKSPACE;
$homePath = Resolve-Path "~" | Select-Object -ExpandProperty Path

$exportOptionsPath = "$rootPath/.github/resources/export-options-$method.plist";
$archiveBasePath = "$homePath/Library/Developer/Xcode/Archives/*";
$archivePath = "$archiveBasePath/*.xcarchive";
$exportPath = "$archiveBasePath/ipa-export";
$ipaPath = "$exportPath/Bitwarden.ipa";
$destIpaPath = "$rootPath/Bitwarden.ipa";

xcodebuild -exportArchive -archivePath $archivePath -exportPath $exportPath -exportOptionsPlist $exportOptionsPath

Copy-Item $ipaPath $destIpaPath
ls