using Android.App;
using Bit.App.Abstractions;
using System.Linq;
using AndroidApp = Android.App.Application;

namespace Bit.Android.Services
{
    public class AppInfoService : IAppInfoService
    {
        public string Version => AndroidApp.Context.ApplicationContext.PackageManager
            .GetPackageInfo(AndroidApp.Context.PackageName, 0).VersionName;

        public string Build => AndroidApp.Context.ApplicationContext.PackageManager
            .GetPackageInfo(AndroidApp.Context.PackageName, 0).VersionCode.ToString();

        public bool AutofillServiceEnabled => AutofillRunning();

        private bool AutofillRunning()
        {
            var manager = ((ActivityManager)Xamarin.Forms.Forms.Context.GetSystemService("activity"));
            var services = manager.GetRunningServices(int.MaxValue);
            return services.Any(s => s.Process.ToLowerInvariant().Contains("bitwarden") &&
                s.Service.ClassName.ToLowerInvariant().Contains("autofill"));
        }
    }
}
