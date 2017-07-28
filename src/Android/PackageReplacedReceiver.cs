using Android.App;
using Android.Content;
using Bit.App.Abstractions;
using Bit.App.Utilities;
using Plugin.Settings.Abstractions;
using System.Diagnostics;
using XLabs.Ioc;

namespace Bit.Android
{
    [BroadcastReceiver(Name = "com.x8bit.bitwarden.PackageReplacedReceiver", Exported = true)]
    [IntentFilter(new[] { Intent.ActionMyPackageReplaced })]
    public class PackageReplacedReceiver : BroadcastReceiver
    {
        public override void OnReceive(Context context, Intent intent)
        {
            Debug.WriteLine("App updated!");
            Helpers.PerformUpdateTasks(Resolver.Resolve<ISettings>(), Resolver.Resolve<IAppInfoService>(),
                Resolver.Resolve<IDatabaseService>());
        }
    }
}
