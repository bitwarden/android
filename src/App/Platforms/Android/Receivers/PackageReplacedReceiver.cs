using Android.App;
using Android.Content;
using Bit.App.Abstractions;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;

namespace Bit.Droid.Receivers
{
    [BroadcastReceiver(Name = "com.x8bit.bitwarden.PackageReplacedReceiver", Exported = false)]
    [IntentFilter(new[] { Intent.ActionMyPackageReplaced })]
    public class PackageReplacedReceiver : BroadcastReceiver
    {
        public override async void OnReceive(Context context, Intent intent)
        {
            await AppHelpers.PerformUpdateTasksAsync(
                ServiceContainer.Resolve<ISyncService>("syncService"),
                ServiceContainer.Resolve<IDeviceActionService>("deviceActionService"), 
                ServiceContainer.Resolve<IStateService>("stateService"));
        }
    }
}
