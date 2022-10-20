#addin nuget:?package=Cake.FileHelpers&version=5.0.0
#addin nuget:?package=Cake.AndroidAppManifest&version=1.1.2
#addin nuget:?package=Cake.Plist&version=0.7.0
#addin nuget:?package=Cake.Incubator&version=7.0.0
#tool dotnet:?package=GitVersion.Tool&version=5.10.3
using Path = System.IO.Path;

var debugScript = Argument<bool>("debugScript", false);
var target = Argument("target", "Default");
var configuration = Argument("configuration", "Release");
var variant = Argument("variant", "dev");

abstract record VariantConfig(
    string AppName, 
    string AndroidPackageName,
    string iOSBundleId,
    string ApsEnvironment
    );

record Dev(): VariantConfig("Bitwarden Dev", "com.x8bit.bitwarden.dev", "com.8bit.bitwarden.dev", "development");
record QA(): VariantConfig("Bitwarden QA", "com.x8bit.bitwarden.qa", "com.8bit.bitwarden.qa", "development");
record Beta(): VariantConfig("Bitwarden Beta", "com.x8bit.bitwarden.beta", "com.8bit.bitwarden.beta", "production");
record Prod(): VariantConfig("Bitwarden", "com.x8bit.bitwarden", "com.8bit.bitwarden", "production");

VariantConfig GetVariant() => variant.ToLower() switch{
    "qa" => new QA(),
    "beta" => new Beta(),
    "prod" => new Prod(),
    _ => new Dev()
};

GitVersion _gitVersion; //will be set by GetGitInfo task
var _slnPath = Path.Combine(""); //base path used to access files. If build.cake file is moved, just update this
string _androidPackageName = string.Empty; //will be set by UpdateAndroidManifest task
string CreateFeatureBranch(string prevVersionName, GitVersion git) => $"{prevVersionName}-{git.BranchName.Replace("/","-")}";
string GetVersionName(string prevVersionName, VariantConfig buildVariant, GitVersion git) => buildVariant is Prod? prevVersionName : CreateFeatureBranch(prevVersionName, git); 
int CreateBuildNumber(int previousNumber) => ++previousNumber; //TODO

Task("GetGitInfo")
    .Does(()=> {
        _gitVersion = GitVersion(new GitVersionSettings());

        if(debugScript)
        {
            Information($"GitVersion Dump:\n{_gitVersion.Dump()}");
        }

         Information("Git data Load successfully.");
    });

#region Android
Task("UpdateAndroidAppIcon")
    .Does(()=>{
        //TODO
        //manifest.ApplicationIcon = "@mipmap/ic_launcher";
        Information($"Updated Androix App Icon with success");
    });


Task("UpdateAndroidManifest")
    .IsDependentOn("GetGitInfo")
    .Does(()=> 
    {
        var buildVariant = GetVariant();
        var manifestPath = Path.Combine(_slnPath, "src", "Android", "Properties", "AndroidManifest.xml");

        // Cake.AndroidAppManifest doesn't currently enable us to access nested items so, quick (not ideal) fix:
        var manifestText = FileReadText(manifestPath);
        manifestText = manifestText.Replace("com.x8bit.bitwarden.", buildVariant.AndroidPackageName + ".");
        manifestText = manifestText.Replace("android:label=\"Bitwarden\"", $"android:label=\"{buildVariant.AppName}\"");
        FileWriteText(manifestPath, manifestText);

        var manifest = DeserializeAppManifest(manifestPath);

        var prevVersionCode = manifest.VersionCode;
        var prevVersionName = manifest.VersionName;
        _androidPackageName = manifest.PackageName;

        //manifest.VersionCode = CreateBuildNumber(prevVersionCode);
        manifest.VersionName = GetVersionName(prevVersionName, buildVariant, _gitVersion);
        manifest.PackageName = buildVariant.AndroidPackageName;
        manifest.ApplicationLabel = buildVariant.AppName;

        //Information($"AndroidManigest.xml VersionCode from {prevVersionCode} to {manifest.VersionCode}");
        Information($"AndroidManigest.xml VersionName from {prevVersionName} to {manifest.VersionName}");
        Information($"AndroidManigest.xml PackageName from {_androidPackageName} to {buildVariant.AndroidPackageName}");
        Information($"AndroidManigest.xml ApplicationLabel to {buildVariant.AppName}");
    
        SerializeAppManifest(manifestPath, manifest);

        Information("AndroidManifest updated with success!");
    });

void ReplaceInFile(string filePath, string oldtext, string newtext)
{
    var fileText = FileReadText(filePath);

    if(string.IsNullOrEmpty(fileText) || !fileText.Contains(oldtext))
    {
        throw new Exception($"Couldn't find {filePath} or it didn't contain: {oldtext}");
    }

    fileText = fileText.Replace(oldtext, newtext);

    FileWriteText(filePath, fileText);
    Information($"{filePath} modified successfully.");		
}

Task("UpdateAndroidCodeFiles")
    .IsDependentOn("UpdateAndroidManifest")
    .Does(()=> {
        var buildVariant = GetVariant();

        //We're not using _androidPackageName here because the codefile is currently slightly different string than the one in AndroidManifest.xml
        var keyName = "com.8bit.bitwarden";
        var fixedPackageName = buildVariant.AndroidPackageName.Replace("x8bit", "8bit");
        var filePath = Path.Combine(_slnPath, "src", "Android", "Services", "BiometricService.cs");
        ReplaceInFile(filePath, keyName, fixedPackageName);

        var packageFileList = new string[] {
            Path.Combine(_slnPath, "src", "Android", "MainActivity.cs"),
            Path.Combine(_slnPath, "src", "Android", "MainApplication.cs"),
            Path.Combine(_slnPath, "src", "Android", "Constants.cs"),
            Path.Combine(_slnPath, "src", "Android", "Accessibility", "AccessibilityService.cs"),
            Path.Combine(_slnPath, "src", "Android", "Autofill", "AutofillHelpers.cs"),
            Path.Combine(_slnPath, "src", "Android", "Autofill", "AutofillService.cs"),
            Path.Combine(_slnPath, "src", "Android", "Receivers", "ClearClipboardAlarmReceiver.cs"),
            Path.Combine(_slnPath, "src", "Android", "Receivers", "EventUploadReceiver.cs"),
            Path.Combine(_slnPath, "src", "Android", "Receivers", "PackageReplacedReceiver.cs"),
            Path.Combine(_slnPath, "src", "Android", "Receivers", "RestrictionsChangedReceiver.cs"),
            Path.Combine(_slnPath, "src", "Android", "Services", "DeviceActionService.cs"),
            Path.Combine(_slnPath, "src", "Android", "Tiles", "AutofillTileService.cs"),
            Path.Combine(_slnPath, "src", "Android", "Tiles", "GeneratorTileService.cs"),
            Path.Combine(_slnPath, "src", "Android", "Tiles", "MyVaultTileService.cs"),
            Path.Combine(_slnPath, "src", "Android", "google-services.json"),
            Path.Combine(_slnPath, "store", "google", "Publisher", "Program.cs"),
        };

        foreach(string path in packageFileList)
        {
            ReplaceInFile(path, "com.x8bit.bitwarden", buildVariant.AndroidPackageName);
        }

        var labelFileList = new string[] {
            Path.Combine(_slnPath, "src", "Android", "Autofill", "AutofillService.cs"),
        };

        foreach(string path in labelFileList)
        {
            ReplaceInFile(path, "Bitwarden\"", $"{buildVariant.AppName}\"");
        }
    });
#endregion Android

#region iOS
enum iOSProjectType
{
    Null,
    MainApp,
    Autofill,
    Extension,
    ShareExtension
}

string GetiOSBundleId(VariantConfig buildVariant, iOSProjectType projectType) => projectType switch
{
    iOSProjectType.Autofill => $"{buildVariant.iOSBundleId}.autofill",
    iOSProjectType.Extension => $"{buildVariant.iOSBundleId}.find-login-action-extension",
    iOSProjectType.ShareExtension => $"{buildVariant.iOSBundleId}.share-extension",
    _ => buildVariant.iOSBundleId
};

string GetiOSBundleName(VariantConfig buildVariant, iOSProjectType projectType) => projectType switch
{
    iOSProjectType.Autofill => $"{buildVariant.AppName} Autofill",
    iOSProjectType.Extension => $"{buildVariant.AppName} Extension",
    iOSProjectType.ShareExtension => $"{buildVariant.AppName} Share Extension",
    _ => buildVariant.AppName
};

private void UpdateiOSInfoPlist(string plistPath, VariantConfig buildVariant, GitVersion git, iOSProjectType projectType = iOSProjectType.MainApp)
{
    var plistFile = File(plistPath);
    dynamic plist = DeserializePlist(plistFile);

    var prevVersionName = plist["CFBundleShortVersionString"];
    var prevVersionString = plist["CFBundleVersion"];
    var prevVersion = int.Parse(plist["CFBundleVersion"]);
    var prevBundleId = plist["CFBundleIdentifier"];
    var prevBundleName = plist["CFBundleName"];
    var newVersion = CreateBuildNumber(prevVersion).ToString();
    var newVersionName = GetVersionName(prevVersionName, buildVariant, git);
    var newBundleId = GetiOSBundleId(buildVariant, projectType);
    var newBundleName = GetiOSBundleName(buildVariant, projectType);

    plist["CFBundleName"] = newBundleName;
    plist["CFBundleDisplayName"] = newBundleName;
    //plist["CFBundleVersion"] = newVersion;
    plist["CFBundleShortVersionString"] = newVersionName;
    plist["CFBundleIdentifier"] = newBundleId;

    if(projectType == iOSProjectType.MainApp)
    {
        plist["CFBundleURLTypes"][0]["CFBundleURLName"] = $"{buildVariant.iOSBundleId}.url";
    }

    if(projectType == iOSProjectType.Extension)
    {
        var keyText = plist["NSExtension"]["NSExtensionAttributes"]["NSExtensionActivationRule"];
        plist["NSExtension"]["NSExtensionAttributes"]["NSExtensionActivationRule"] = keyText.Replace("com.8bit.bitwarden", buildVariant.iOSBundleId);
    }

    SerializePlist(plistFile, plist);

    Information($"Changed app name from {prevBundleName} to {newBundleName}");
    //Information($"Changed Bundle Version from {prevVersion} to {newVersion}");
    Information($"Changed Bundle Short Version name from {prevVersionName} to {newVersionName}");
    Information($"Changed Bundle Identifier from {prevBundleId} to {newBundleId}");
    Information($"{plistPath} updated with success!");
}

private void UpdateiOSEntitlementsPlist(string entitlementsPath, VariantConfig buildVariant)
{
    var EntitlementlistFile = File(entitlementsPath);
    dynamic Entitlements = DeserializePlist(EntitlementlistFile);

    Entitlements["aps-environment"] = buildVariant.ApsEnvironment;
    Entitlements["keychain-access-groups"] = new List<string>() { "$(AppIdentifierPrefix)" + buildVariant.iOSBundleId };
    Entitlements["com.apple.security.application-groups"] = new List<string>() { $"group.{buildVariant.iOSBundleId}" };;

    Information($"Changed ApsEnvironment name to {buildVariant.ApsEnvironment}");
    Information($"Changed keychain-access-groups bundleID to {buildVariant.iOSBundleId}");

    SerializePlist(EntitlementlistFile, Entitlements);

    Information($"{entitlementsPath} updated with success!");
}

Task("UpdateiOSIcon")
    .Does(()=>{
        //TODO
        Information($"Updating IOS App Icon");
    });

Task("UpdateiOSPlist")
    .IsDependentOn("GetGitInfo")
    .Does(()=> {
        var buildVariant = GetVariant();
        var infoPath = Path.Combine(_slnPath, "src", "iOS", "Info.plist");
        var entitlementsPath = Path.Combine(_slnPath, "src", "iOS", "Entitlements.plist");
        UpdateiOSInfoPlist(infoPath, buildVariant, _gitVersion, iOSProjectType.MainApp);
        UpdateiOSEntitlementsPlist(entitlementsPath, buildVariant);
    });

Task("UpdateiOSAutofillPlist")
    .IsDependentOn("GetGitInfo")
    .IsDependentOn("UpdateiOSPlist")
    .Does(()=> {
        var buildVariant = GetVariant();
        var infoPath = Path.Combine(_slnPath, "src", "iOS.Autofill", "Info.plist");
        var entitlementsPath = Path.Combine(_slnPath, "src", "iOS.Autofill", "Entitlements.plist");
        UpdateiOSInfoPlist(infoPath, buildVariant, _gitVersion, iOSProjectType.Autofill);
        UpdateiOSEntitlementsPlist(entitlementsPath, buildVariant);
    });

Task("UpdateiOSExtensionPlist")
    .IsDependentOn("GetGitInfo")
    .IsDependentOn("UpdateiOSPlist")
    .Does(()=> {
        var buildVariant = GetVariant();
        var infoPath = Path.Combine(_slnPath, "src", "iOS.Extension", "Info.plist");
        var entitlementsPath = Path.Combine(_slnPath, "src", "iOS.Extension", "Entitlements.plist");
        UpdateiOSInfoPlist(infoPath, buildVariant, _gitVersion, iOSProjectType.Extension);
        UpdateiOSEntitlementsPlist(entitlementsPath, buildVariant);
    });

Task("UpdateiOSShareExtensionPlist")
    .IsDependentOn("GetGitInfo")
    .IsDependentOn("UpdateiOSPlist")
    .Does(()=> {
        var buildVariant = GetVariant();
        var infoPath = Path.Combine(_slnPath, "src", "iOS.ShareExtension", "Info.plist");
        var entitlementsPath = Path.Combine(_slnPath, "src", "iOS.ShareExtension", "Entitlements.plist");
        UpdateiOSInfoPlist(infoPath, buildVariant, _gitVersion, iOSProjectType.ShareExtension);
        UpdateiOSEntitlementsPlist(entitlementsPath, buildVariant);
    });

Task("UpdateiOSCodeFiles")
    .IsDependentOn("UpdateiOSPlist")
    .Does(()=> {
        var buildVariant = GetVariant();
        var fileList = new string[] {
            Path.Combine(_slnPath, "src", "iOS.Core", "Utilities", "iOSCoreHelpers.cs"),
            Path.Combine(_slnPath, "src", "iOS.Core", "Constants.cs"),
            Path.Combine(".github", "resources", "export-options-ad-hoc.plist"),
            Path.Combine(".github", "resources", "export-options-app-store.plist"),
        };

        foreach(string path in fileList)
        {
            ReplaceInFile(path, "com.8bit.bitwarden", buildVariant.iOSBundleId);
        }
    });
#endregion iOS

#region Main Tasks
Task("Android")
    //.IsDependentOn("UpdateAndroidAppIcon")
    .IsDependentOn("UpdateAndroidManifest")
    .IsDependentOn("UpdateAndroidCodeFiles")
    .Does(()=>
    {
        Information("Android app updated");
    });

Task("iOS")
    //.IsDependentOn("UpdateiOSIcon")
    .IsDependentOn("UpdateiOSPlist")
    .IsDependentOn("UpdateiOSAutofillPlist")
    .IsDependentOn("UpdateiOSExtensionPlist")
    .IsDependentOn("UpdateiOSShareExtensionPlist")
    .IsDependentOn("UpdateiOSCodeFiles")
    .Does(()=>
    {
        Information("iOS app updated");
    });

Task("Default")
    .Does(() => {
        var usage = @"Missing target.

Usage:
  dotnet cake build.cake --target (Android | iOS) --variant (dev | qa | beta | prod)

Options:
 --debugScript=<bool> Script debug mode.	
";
        Information(usage);
    });
#endregion Main Tasks

RunTarget(target);