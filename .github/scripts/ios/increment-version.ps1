$rootPath = $env:GITHUB_WORKSPACE;
$buildNumber = 100 + [int]$env:GITHUB_RUN_NUMBER;

$bitwardenInfo = $($rootPath + "/src/iOS/Info.plist");
$extensionInfo = $($rootPath + "/src/iOS.Extension/Info.plist");
$autofillInfo = $($rootPath + "/src/iOS.Autofill/Info.plist");

Write-Output "########################################"
Write-Output "##### Setting CFBundleVersion $buildNumber"
Write-Output "########################################"

function Update-Version($file) {
    $xml=New-Object XML;
    $xml.Load($file);

    Select-Xml -xml $xml -XPath "//dict/key[. = 'CFBundleVersion']/following-sibling::string[1]" |
    %{ 
        $_.Node.InnerXml = $buildNumber
    }

    $xml.Save($file);
}

Update-Version $bitwardenInfo
Update-Version $extensionInfo
Update-Version $autofillInfo
