using Bit.App.Abstractions;
using AndroidApp = Android.App.Application;

namespace Bit.Android.Services
{
    public class AppInfoService : IAppInfoService
    {
        public string Version => AndroidApp.Context.ApplicationContext.PackageManager
            .GetPackageInfo(AndroidApp.Context.PackageName, 0).VersionName;

        public string Build => AndroidApp.Context.ApplicationContext.PackageManager
            .GetPackageInfo(AndroidApp.Context.PackageName, 0).VersionCode.ToString();
    }
}
