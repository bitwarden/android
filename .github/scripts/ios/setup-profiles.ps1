$homePath = $env:HOME;
$secretsPath = $homePath + "/secrets"

$autofillProfilePath = $($secretsPath + "/dist_autofill.mobileprovision");
$bitwardenProfilePath = $($secretsPath + "/dist_bitwarden.mobileprovision");
$extensionProfilePath = $($secretsPath + "/dist_extension.mobileprovision");

$autofill_uuid = grep UUID -A1 -a $autofillProfilePath | grep -io "[-A-F0-9]\{36\}"
Copy-Item $autofillProfilePath ~/Library/MobileDevice/Provisioning\ Profiles/$autofill_uuid.mobileprovision

$bitwarden_uuid = grep UUID -A1 -a $bitwardenProfilePath | grep -io "[-A-F0-9]\{36\}"
Copy-Item $bitwardenProfilePath ~/Library/MobileDevice/Provisioning\ Profiles/$bitwarden_uuid.mobileprovision

$extension_uuid = grep UUID -A1 -a $extensionProfilePath | grep -io "[-A-F0-9]\{36\}"
Copy-Item $extensionProfilePath ~/Library/MobileDevice/Provisioning\ Profiles/$extension_uuid.mobileprovision
