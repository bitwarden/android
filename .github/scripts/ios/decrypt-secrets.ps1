$rootPath = $env:GITHUB_WORKSPACE;

$decryptSecretPath = $($rootPath + ".\.github\scripts\decrypt-secret.ps1");

Invoke-Expression "& `"$decryptSecretPath`" -filename bitwarden-mobile-key.p12.gpg"
Invoke-Expression "& `"$decryptSecretPath`" -filename iphone-distribution-cert.p12.gpg"
