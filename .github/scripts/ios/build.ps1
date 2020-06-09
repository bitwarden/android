param (
  [Parameter(Mandatory=$true)]
  [string] $configuration,
  [string] $platform = "iPhone"
)

security default-keychain -s build.keychain
security unlock-keychain -p $env:KEYCHAIN_PASSWORD build.keychain
security set-key-partition-list -S apple-tool:,apple: -s -k $env:KEYCHAIN_PASSWORD build.keychain

$rootPath = $env:GITHUB_WORKSPACE;
$iosPath = $($rootPath + "/src/iOS/iOS.csproj");

Write-Output "########################################"
Write-Output "##### Build $configuration Configuration for $platform Platform"
Write-Output "########################################"

msbuild "$($iosPath)" "/p:Platform=$platform" "/p:Configuration=$configuration"
