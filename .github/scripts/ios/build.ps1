param (
  [Parameter(Mandatory=$true)]
  [string] $configuration,
  [string] $platform = "iPhone"
)

$rootPath = $env:GITHUB_WORKSPACE;
$iosPath = $($rootPath + "\src\iOS\iOS.csproj");

Write-Output "########################################"
Write-Output "##### Build $configuration Configuration for $platform Platform"
Write-Output "########################################"

msbuild "$($iosPath)" "/p:Platform=$platform" "/p:Configuration=$configuration"
