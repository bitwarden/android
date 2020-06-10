$homePath = Resolve-Path "~" | Select-Object -ExpandProperty Path;
$secretsPath = $homePath + "/secrets"

$mobileKeyPath = $($secretsPath + "/bitwarden-mobile-key.p12");
$distCertPath = $($secretsPath + "/iphone-distribution-cert.p12");

security create-keychain -p $env:KEYCHAIN_PASSWORD build.keychain
security default-keychain -s build.keychain
security unlock-keychain -p $env:KEYCHAIN_PASSWORD build.keychain
security set-keychain-settings -lut 1200 build.keychain
security import $mobileKeyPath -k build.keychain -P $env:MOBILE_KEY_PASSWORD -T /usr/bin/codesign -T /usr/bin/security
security import $distCertPath -k build.keychain -P $env:DIST_CERT_PASSWORD -T /usr/bin/codesign -T /usr/bin/security
security set-key-partition-list -S apple-tool:,apple:,codesign: -s -k $env:KEYCHAIN_PASSWORD build.keychain
