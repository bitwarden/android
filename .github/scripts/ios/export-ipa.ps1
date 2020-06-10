param (
  [Parameter(Mandatory=$true)]
  [string] $method
)

$rootPath = $env:GITHUB_WORKSPACE;
$homePath = Resolve-Path "~" | Select-Object -ExpandProperty Path

$exportOptionsPath = "$rootPath/.github/resources/export-options-$method.plist";
$archivePath = "$homePath/Library/Developer/Xcode/Archives/*/*.xcarchive";
$exportPath = "$rootPath/com.8bit.bitwarden.ipa"

xcodebuild -exportArchive -archivePath $archivePath -exportPath $exportPath -exportOptionsPlist $exportOptionsPath
