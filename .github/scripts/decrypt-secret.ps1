param (
  [Parameter(Mandatory=$true)]
  [string] $filename,
  [string] $output
)

$homePath = $env:HOME
$rootPath = $env:GITHUB_WORKSPACE

$secretInputPath = $rootPath + "/.github/secrets"
$input = $secretInputPath + $filename

$passphrase = $env:DECRYPT_FILE_PASSWORD

if ([string]::IsNullOrEmpty($output)) {
  $secretOutputPath = $homePath + "/secrets/"
  if($filename.EndsWith(".gpg")) {
    $output = $secretOutputPath + $filename.TrimEnd(".gpg")
  } else {
    $output = $secretOutputPath + $filename + ".plaintext"
  }
}

if(!(Test-Path -Path $secretPath))  
{
  New-Item -ItemType Directory -Path $secretPath 
}

gpg --quiet --batch --yes --decrypt --passphrase="$passphrase" --output $output $input
