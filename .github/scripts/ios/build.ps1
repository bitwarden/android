param (
  [Parameter(Mandatory=$true)]
  [string] $configuration,
  [string] $platform = "iPhone",
  [switch] $archive
)

$rootPath = $env:GITHUB_WORKSPACE;
$iosPath = $($rootPath + "/src/iOS/iOS.csproj");

if ($archive)
{
  Write-Output "########################################"
  Write-Output "##### Archive $configuration Configuration for $platform Platform"
  Write-Output "########################################"
  msbuild "$($iosPath)" "/p:Platform=$platform" "/p:Configuration=$configuration" `
    "/p:ArchiveOnBuild=true" "/t:`"Build`""

  Write-Output "########################################"
  Write-Output "##### Done"
  Write-Output "########################################"
  ls ~/Library/Developer/Xcode/Archives
} else
{
  Write-Output "########################################"
  Write-Output "##### Build $configuration Configuration for $platform Platform"
  Write-Output "########################################"
  msbuild "$($iosPath)" "/p:Platform=$platform" "/p:Configuration=$configuration" "/t:`"Build`""
}
