dir "${env:ProgramFiles(x86)}\Android\android-sdk\platforms"
dir "${env:ProgramFiles(x86)}\Android\android-sdk\build-tools"

$pwd = Get-Location
cd "${env:ProgramFiles(x86)}\Android\android-sdk\tools\bin"
.\sdkmanager --list
Echo 'y' | .\sdkmanager "platforms;android-28" "build-tools;28.0.2" | Out-Null
Echo 'y' | .\sdkmanager --update | Out-Null

dir "${env:ProgramFiles(x86)}\Android\android-sdk\platforms"
dir "${env:ProgramFiles(x86)}\Android\android-sdk\build-tools"

cd $pwd