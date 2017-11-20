$AndroidToolPath = "${env:ProgramFiles(x86)}\Android\android-sdk\tools\android"

Function Get-AndroidSDKs() { 
    $output = & $AndroidToolPath list sdk --all 
    $sdks = $output |% { 
        if ($_ -match '(?<index>\d+)- (?<sdk>.+), revision (?<revision>[\d\.]+)') { 
            $sdk = New-Object PSObject 
            Add-Member -InputObject $sdk -MemberType NoteProperty -Name Index -Value $Matches.index 
            Add-Member -InputObject $sdk -MemberType NoteProperty -Name Name -Value $Matches.sdk 
            Add-Member -InputObject $sdk -MemberType NoteProperty -Name Revision -Value $Matches.revision 
            $sdk 
        } 
    } 
    $sdks 
}

Function Install-AndroidSDK() { 
    [CmdletBinding()] 
    Param( 
        [Parameter(Mandatory=$true, Position=0)] 
        [PSObject[]]$sdks 
    )
    $sdkIndexes = $sdks |% { $_.Index } 
    $sdkIndexArgument = [string]::Join(',', $sdkIndexes) 
    Echo 'y' | & $AndroidToolPath update sdk -u -a -t $sdkIndexArgument 
}

# install android 26
$sdks = Get-AndroidSDKs |? { $_.name -like 'sdk platform*API 26*' } 
Install-AndroidSDK -sdks $sdks
