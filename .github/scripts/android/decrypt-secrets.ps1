$rootPath = $env:GITHUB_WORKSPACE;

$decryptSecretPath = $($rootPath + "/.github/scripts/decrypt-secret.ps1");

$appKeystorePlayFilename = "app_play-keystore.jks";
$appKeystorePlayPath = $($rootPath + "/src/Android/$appKeystorePlayFilename");
$appKeystoreUploadFilename = "app_upload-keystore.jks";
$appKeystoreUploadPath = $($rootPath + "/src/Android/$appKeystoreUploadFilename");
$appKeystoreFdroidFilename = "app_fdroid-keystore.jks";
$appKeystoreFdroidPath = $($rootPath + "/src/Android/$appKeystoreFdroidFilename");
$googleServicesFilename = "google-services.json";
$googleServicesPath = $($rootPath + "/src/Android/$googleServicesFilename");

Invoke-Expression `
    "& `"$decryptSecretPath`" -filename $($appKeystorePlayFilename + ".gpg") -output $($appKeystorePlayPath)"
Invoke-Expression `
    "& `"$decryptSecretPath`" -filename $($appKeystoreUploadFilename + ".gpg") -output $($appKeystoreUploadPath)"
Invoke-Expression `
    "& `"$decryptSecretPath`" -filename $($appKeystoreFdroidFilename + ".gpg") -output $($appKeystoreFdroidPath)"
Invoke-Expression `
    "& `"$decryptSecretPath`" -filename $($googleServicesFilename + ".gpg") -output $($googleServicesPath)"
Invoke-Expression "& `"$decryptSecretPath`" -filename play_creds.json.gpg"
