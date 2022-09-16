#addin nuget:?package=Cake.FileHelpers&version=5.0.0
#addin nuget:?package=Cake.AndroidAppManifest&version=1.1.2
#addin nuget:?package=Cake.Plist&version=0.7.0
#addin nuget:?package=Cake.Incubator&version=7.0.0
#tool dotnet:?package=GitVersion.Tool&version=5.8.1
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

record Dev(): VariantConfig("Bitwarden Dev", "com.x8bit.bitwarden-dev", "com.8bit.bitwarden-dev", "development");
record QA(): VariantConfig("Bitwarden QA", "com.x8bit.bitwarden-qa", "com.8bit.bitwarden-qa", "development");
record Beta(): VariantConfig("Bitwarden Beta", "com.x8bit.bitwarden-beta", "com.8bit.bitwarden-beta", "production");
record Prod(): VariantConfig("Bitwarden", "com.x8bit.bitwarden", "com.8bit.bitwarden", "production");

VariantConfig GetVariant() => variant switch{
    "qa" => new QA(),
    "beta" => new Beta(),
    "prod" => new Prod(),
    _ => new Dev()
};

GitVersion _gitVersion; //will be set by GetGitInfo task
var _slnPath = Path.Combine(""); //base path used to access files
string _iOSMainBundleId = string.Empty; //will be set by UpdateiOSPlist task
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

//Android
Task("UpdateAndroidAppIcon")
	.Does(()=>{
		//TODO
		//manifest.ApplicationIcon = "@mipmap/ic_launcher";
		Information($"Updated Androix App Icon with success");
	});
	
Task("UpdateAndroidGoogleServices")
	.Does(()=>{
		//TODO
		Information($"Updated Androix App Icon with success");
	});
	

Task("UpdateAndroidManifest")
	.IsDependentOn("GetGitInfo")
	.Does(()=> 
	{
		var buildVariant = GetVariant();
		var manifestPath = Path.Combine(_slnPath, "src", "Android", "Properties", "AndroidManifest.xml");
 		var manifest = DeserializeAppManifest(manifestPath);

		var prevVersionCode = manifest.VersionCode;
		var prevVersionName = manifest.VersionName;
        _androidPackageName = manifest.PackageName;

		manifest.VersionCode = CreateBuildNumber(prevVersionCode);
		manifest.VersionName = GetVersionName(prevVersionName, buildVariant, _gitVersion);
		manifest.PackageName = buildVariant.AndroidPackageName;
		manifest.ApplicationLabel = buildVariant.AppName;

		Information($"AndroidManigest.xml VersionCode from {prevVersionCode} to {manifest.VersionCode}");
		Information($"AndroidManigest.xml VersionName from {prevVersionName} to {manifest.VersionName}");
		Information($"AndroidManigest.xml PackageName from {_androidPackageName} to {buildVariant.AndroidPackageName}");
		Information($"AndroidManigest.xml ApplicationLabel to {buildVariant.AppName}");
	
    	SerializeAppManifest(manifestPath, manifest);
		Information("AndroidManifest updated with success!");
	});

Task("UpdateAndroidCodeFiles")
	.IsDependentOn("UpdateAndroidManifest")
	.Does(()=> {
        var buildVariant = GetVariant();
        var filePath = Path.Combine(_slnPath, "src", "Android", "Services", "BiometricService.cs");

        var fileText = FileReadText(filePath);

        //We're not using _androidPackageName here because the codefile is currently slightly different string than the one in AndroidManifest.xml
        var keyName = "com.8bit.bitwarden";

        if(string.IsNullOrEmpty(fileText) || !fileText.Contains(keyName))
        {
            throw new Exception($"Couldn't find {filePath} or it didn't contain: {keyName}");
        }

        fileText = fileText.Replace(keyName, buildVariant.AndroidPackageName);

        FileWriteText(filePath, fileText);
		Information($"BiometricService.cs modified successfully.");		
	});

//iOS
private void UpdateiOSInfoPlist(string plistPath, VariantConfig buildVariant, GitVersion git, bool mainApp = false)
{
    var plistFile = File(plistPath);
    dynamic plist = DeserializePlist(plistFile);

    var prevVersionName = plist["CFBundleShortVersionString"];
    var prevVersionString = plist["CFBundleVersion"];
    var prevVersion = int.Parse(plist["CFBundleVersion"]);
    var prevBundleId = plist["CFBundleIdentifier"];
    var newVersion = CreateBuildNumber(prevVersion).ToString();
    var versionName = GetVersionName(prevVersionName, buildVariant, git);

    if(mainApp)
    {
        _iOSMainBundleId = plist["CFBundleIdentifier"];
    }

    if(string.IsNullOrEmpty(_iOSMainBundleId))
    {
        throw new Exception("iOS Main Bundle ID wasn't set, UpdateiOSPlist task needs to run first");
    }

    var newBundleId = prevBundleId.Replace(_iOSMainBundleId, buildVariant.iOSBundleId);
    plist["CFBundleName"] = buildVariant.AppName;
    plist["CFBundleDisplayName"] = buildVariant.AppName;

    
    plist["CFBundleVersion"] = newVersion;
    plist["CFBundleShortVersionString"] = versionName;
    plist["CFBundleIdentifier"] = newBundleId;

    SerializePlist(plistFile, plist);

    Information($"Changed app name to {buildVariant.AppName}");
    Information($"Changed Bundle Version from {prevVersion} to {newVersion}");
    Information($"Changed Bundle Short Version name to {versionName}");
    Information($"Changed Bundle Identifier from {prevBundleId} to {newBundleId}");
    Information($"{plistPath} updated with success!");
}

private void UpdateiOSEntitlementsPlist(string entitlementsPath, VariantConfig buildVariant)
{
		var EntitlementlistFile = File(entitlementsPath);
		dynamic Entitlements = DeserializePlist(EntitlementlistFile);

		Entitlements["aps-environment"] = buildVariant.ApsEnvironment;
		Entitlements["keychain-access-groups"] = new List<string>() { "$(AppIdentifierPrefix)" + buildVariant.iOSBundleId };

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
		UpdateiOSInfoPlist(infoPath, buildVariant, _gitVersion, true);
        UpdateiOSEntitlementsPlist(entitlementsPath, buildVariant);
	});

Task("UpdateiOSAutofillPlist")
	.IsDependentOn("GetGitInfo")
	.IsDependentOn("UpdateiOSPlist")
	.Does(()=> {
        var buildVariant = GetVariant();
        var infoPath = Path.Combine(_slnPath, "src", "iOS.Autofill", "Info.plist");
        var entitlementsPath = Path.Combine(_slnPath, "src", "iOS.Autofill", "Entitlements.plist");
		UpdateiOSInfoPlist(infoPath, buildVariant, _gitVersion);
        UpdateiOSEntitlementsPlist(entitlementsPath, buildVariant);
	});

Task("UpdateiOSExtensionPlist")
	.IsDependentOn("GetGitInfo")
	.IsDependentOn("UpdateiOSPlist")
	.Does(()=> {
        var buildVariant = GetVariant();
        var infoPath = Path.Combine(_slnPath, "src", "iOS.Extension", "Info.plist");
        var entitlementsPath = Path.Combine(_slnPath, "src", "iOS.Extension", "Entitlements.plist");
		UpdateiOSInfoPlist(infoPath, buildVariant, _gitVersion);
        UpdateiOSEntitlementsPlist(entitlementsPath, buildVariant);
	});

Task("UpdateiOSShareExtensionPlist")
	.IsDependentOn("GetGitInfo")
	.IsDependentOn("UpdateiOSPlist")
	.Does(()=> {
        var buildVariant = GetVariant();
        var infoPath = Path.Combine(_slnPath, "src", "iOS.ShareExtension", "Info.plist");
        var entitlementsPath = Path.Combine(_slnPath, "src", "iOS.ShareExtension", "Entitlements.plist");
		UpdateiOSInfoPlist(infoPath, buildVariant, _gitVersion);
        UpdateiOSEntitlementsPlist(entitlementsPath, buildVariant);
	});

Task("UpdateiOSCodeFiles")
	.IsDependentOn("UpdateiOSPlist")
	.Does(()=> {
        var buildVariant = GetVariant();
        var filePath = Path.Combine(_slnPath, "src", "iOS.Core", "Utilities", "iOSCoreHelpers.cs");

        var fileText = FileReadText(filePath);

        fileText = fileText.Replace(_iOSMainBundleId, buildVariant.iOSBundleId);

        FileWriteText(filePath, fileText);
		Information($"iOSCoreHelpers.cs modified successfully.");		
	});

/// Main Tasks
Task("Android")
	.IsDependentOn("UpdateAndroidAppIcon")
	.IsDependentOn("UpdateAndroidGoogleServices")
	.IsDependentOn("UpdateAndroidManifest")
	.IsDependentOn("UpdateAndroidCodeFiles")
	.Does(()=>
	{
		Information("Android app updated");
	});

Task("iOS")
	.IsDependentOn("UpdateiOSIcon")
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

RunTarget(target);