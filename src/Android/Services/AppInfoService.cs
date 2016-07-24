using Bit.App.Abstractions;
using AndrodApp = Android.App.Application;

namespace Bit.Android.Services
{
    public class AppInfoService : IAppInfoService
    {
        public string Version => AndrodApp.Context.ApplicationContext.PackageManager
            .GetPackageInfo(AndrodApp.Context.PackageName, 0).VersionName;

        public string Build => AndrodApp.Context.ApplicationContext.PackageManager
            .GetPackageInfo(AndrodApp.Context.PackageName, 0).VersionCode.ToString();
    }
}
