using System;
using Android.App;
using Android.Content;

namespace Bit.Droid.Receivers
{
    [BroadcastReceiver(Name = "com.x8bit.bitwarden.PackageReplacedReceiver", Exported = false)]
    [IntentFilter(new[] { Intent.ActionMyPackageReplaced })]
    public class PackageReplacedReceiver : BroadcastReceiver
    {
        public override void OnReceive(Context context, Intent intent)
        {
            System.Diagnostics.Debug.WriteLine("PackageReplacedReceiver OnReceive");
        }
    }
}
