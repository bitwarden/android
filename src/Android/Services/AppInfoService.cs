using Android.App;
using Android.Views.Autofill;
using Bit.App.Abstractions;
using Plugin.CurrentActivity;
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

        public bool AutofillAccessibilityServiceEnabled => AutofillAccessibilityRunning();
        public bool AutofillServiceEnabled => AutofillEnabled();

        private bool AutofillAccessibilityRunning()
        {
            var manager = ((ActivityManager)CrossCurrentActivity.Current.Activity.GetSystemService("activity"));
            var services = manager.GetRunningServices(int.MaxValue);
            return services.Any(s => s.Process.ToLowerInvariant().Contains("bitwarden") &&
                s.Service.ClassName.ToLowerInvariant().Contains("autofill"));
        }

        private bool AutofillEnabled()
        {
            if(global::Android.OS.Build.VERSION.SdkInt < global::Android.OS.BuildVersionCodes.O)
            {
                return false;
            }

            var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
            var afm = (AutofillManager)activity.GetSystemService(Java.Lang.Class.FromType(typeof(AutofillManager)));
            return afm.IsEnabled;
        }
    }
}
