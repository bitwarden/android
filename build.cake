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

GitVersion _gitVersion;
var _slnPath = Path.Combine("");
var _iosPath = Path.Combine(_slnPath, "src", "iOS");
var _droidPath = Path.Combine(_slnPath, "src", "Android");

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
		var manifestPath = Path.Combine(_droidPath, "Properties", "AndroidManifest.xml");
 		var manifest = DeserializeAppManifest(manifestPath);

		var prevVersionCode = manifest.VersionCode;
		var prevVersionName = manifest.VersionName;

		manifest.VersionCode = CreateBuildNumber(prevVersionCode);
		manifest.VersionName = GetVersionName(prevVersionName, buildVariant, _gitVersion);
		manifest.PackageName = buildVariant.AndroidPackageName;
		manifest.ApplicationLabel = buildVariant.AppName;

		Information($"AndroidManigest.xml VersionCode from {prevVersionCode} to {manifest.VersionCode}");
		Information($"AndroidManigest.xml VersionName from {prevVersionName} to {manifest.VersionName}");
		Information($"AndroidManigest.xml PackageName {buildVariant.AndroidPackageName}");
		Information($"AndroidManigest.xml ApplicationLabel to {buildVariant.AppName}");
	
    	SerializeAppManifest(manifestPath, manifest);
		Information("AndroidManifest updated with success!");
	});

//iOS
Task("UpdateiOSIcon")
	.Does(()=>{
		//TODO
		Information($"Updating IOS App Icon");
	});

Task("UpdateiOSEntitlements")
	.IsDependentOn("GetGitInfo")
	.Does(()=> {
		
        var buildVariant = GetVariant();
        var entitlementsPath = Path.Combine(_iosPath, "Entitlements.plist");
		var EntitlementlistFile = File(entitlementsPath);
		dynamic Entitlements = DeserializePlist(EntitlementlistFile);

		Entitlements["aps-environment"] = buildVariant.ApsEnvironment;
		Entitlements["keychain-access-groups"] = new List<string>() { "$(AppIdentifierPrefix)" + buildVariant.iOSBundleId };

		Information($"Changed ApsEnvironment name to {buildVariant.ApsEnvironment}");
		Information($"Changed keychain-access-groups bundleID to {buildVariant.iOSBundleId}");

		SerializePlist(EntitlementlistFile, Entitlements);

        Information("iOS Entitlements.plist updated with success!");
	});


Task("UpdateiOSInfoPlist")
	.IsDependentOn("GetGitInfo")
	.Does(()=> {
		
        var buildVariant = GetVariant();
        var infoPath = Path.Combine(_iosPath, "Info.plist");

		var plistFile = File(infoPath);
		dynamic plist = DeserializePlist(plistFile);


		var prevVersionString = plist["CFBundleVersion"];
		var prevVersion = int.Parse(plist["CFBundleVersion"]);
		var newVersion = CreateBuildNumber(prevVersion).ToString();
		var versionName = GetVersionName(prevVersionString, buildVariant, _gitVersion);
		plist["CFBundleName"] = buildVariant.AppName;
		plist["CFBundleDisplayName"] = buildVariant.AppName;

		
		plist["CFBundleVersion"] = newVersion;
		plist["CFBundleShortVersionString"] = versionName;
		plist["CFBundleIdentifier"] = buildVariant.iOSBundleId;

		SerializePlist(plistFile, plist);

		Information($"Changed app name to {buildVariant.AppName}");
		Information($"Changed Bundle version to {versionName}");
		Information($"Changed Bundle Identifier to {buildVariant.iOSBundleId}");
		Information($"App Version Number updated from {prevVersion} to {newVersion}");
        Information("iOS Info.plist updated with success!");
	});


Task("Android")
	.IsDependentOn("UpdateAndroidAppIcon")
	.IsDependentOn("UpdateAndroidGoogleServices")
	.IsDependentOn("UpdateAndroidManifest")
	.Does(()=>
	{
		Information("Android app updated");
	});

Task("iOS")
	.IsDependentOn("UpdateiOSIcon")
    .IsDependentOn("UpdateiOSEntitlements")
    .IsDependentOn("UpdateiOSInfoPlist")
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