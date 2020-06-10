param (
  [Parameter(Mandatory=$true)]
  [string] $method
)

$rootPath = $env:GITHUB_WORKSPACE;
$resourcesPath = "$rootPath/.github/resources";
$exportOptionsPath = "$resourcesPath/export-options-$method.plist";
$homePath = Resolve-Path "~" | Select-Object -ExpandProperty Path
$archivesPath = "$homePath/Library/Developer/Xcode/Archives";

cd $archivesPath
cd *
ls

xcodebuild -exportArchive -archivePath *.xcarchive -exportPath com.8bit.bitwarden.ipa -exportOptionsPlist $exportOptionsPath

ls

$destPath = "$rootPath/com.8bit.bitwarden.ipa"
Copy-Item com.8bit.bitwarden.ipa $destPath

cd $rootPath
ls
