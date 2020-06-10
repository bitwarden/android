$rootPath = $env:GITHUB_WORKSPACE;
$ipaPath = "$rootPath/bitwarden-export/Bitwarden.ipa"

xcrun altool --upload-app --type ios --file "$ipaPath" `
    --username "$env:APPLE_ID_USERNAME" --password "$env:APPLE_ID_PASSWORD"
