using Android.Content;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;

namespace Bit.Droid.Receivers
{
    [BroadcastReceiver(Name = "com.x8bit.bitwarden.LockAlarmReceiver", Exported = false)]
    public class LockAlarmReceiver : BroadcastReceiver
    {
        public async override void OnReceive(Context context, Intent intent)
        {
            System.Diagnostics.Debug.WriteLine("LockAlarmReceiver OnReceive");
            var lockService = ServiceContainer.Resolve<ILockService>("lockService");
            await lockService.CheckLockAsync();
        }
    }
}
