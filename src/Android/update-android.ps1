cd "${env:ProgramFiles(x86)}\Android\android-sdk\tools\bin"
.\sdkmanager --list
Echo 'y' | .\sdkmanager "platforms;android-28" "build-tools;28.0.2"
Echo 'y' | .\sdkmanager --update
dir "${env:ProgramFiles(x86)}\Android\android-sdk\platforms"
