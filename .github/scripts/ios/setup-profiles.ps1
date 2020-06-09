$homePath = Resolve-Path "~" | Select-Object -ExpandProperty Path;
$secretsPath = $homePath + "/secrets"

$autofillProfilePath = $($secretsPath + "/dist_autofill.mobileprovision");
$bitwardenProfilePath = $($secretsPath + "/dist_bitwarden.mobileprovision");
$extensionProfilePath = $($secretsPath + "/dist_extension.mobileprovision");
$profilesDirPath = "~/Library/MobileDevice/Provisioning Profiles"

if (!(Test-Path -Path $profilesDirPath))  
{
    New-Item -ItemType Directory -Path $profilesDirPath 
}

$autofill_uuid = grep UUID -A1 -a $autofillProfilePath | grep -io "[-A-F0-9]\{36\}"
Copy-Item $autofillProfilePath -destination "$profilesDirPath/$autofill_uuid.mobileprovision"

$bitwarden_uuid = grep UUID -A1 -a $bitwardenProfilePath | grep -io "[-A-F0-9]\{36\}"
Copy-Item $bitwardenProfilePath -destination "$profilesDirPath/$bitwarden_uuid.mobileprovision"

$extension_uuid = grep UUID -A1 -a $extensionProfilePath | grep -io "[-A-F0-9]\{36\}"
Copy-Item $extensionProfilePath -destination "$profilesDirPath/$extension_uuid.mobileprovision"
