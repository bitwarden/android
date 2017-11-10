using Android.App;
using Android.Content;
using Bit.App.Abstractions;
using Bit.App.Utilities;
using Plugin.Settings.Abstractions;
using System;
using XLabs.Ioc;

namespace Bit.Android
{
    [BroadcastReceiver(Name = "com.x8bit.bitwarden.PackageReplacedReceiver", Exported = false)]
    [IntentFilter(new[] { Intent.ActionMyPackageReplaced })]
    public class PackageReplacedReceiver : BroadcastReceiver
    {
        public override void OnReceive(Context context, Intent intent)
        {
            Console.WriteLine("Bitwarden App Updated!!");
            Helpers.PerformUpdateTasks(Resolver.Resolve<ISettings>(),
                Resolver.Resolve<IAppInfoService>(), Resolver.Resolve<IDatabaseService>(),
                Resolver.Resolve<ISyncService>());
        }
    }
}
