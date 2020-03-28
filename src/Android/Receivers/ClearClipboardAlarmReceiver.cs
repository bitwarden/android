using Android.Content;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Bit.Droid.Utilities;

namespace Bit.Droid.Receivers
{
    [BroadcastReceiver(Name = "com.x8bit.bitwarden.ClearClipboardAlarmReceiver", Exported = false)]
    public class ClearClipboardAlarmReceiver : BroadcastReceiver
    {
        public override void OnReceive(Context context, Intent intent)
        {
            var clipboardManager = context.GetSystemService(Context.ClipboardService) as ClipboardManager;
            if (StaticStore.LastClipboardValue != null && StaticStore.LastClipboardValue == clipboardManager.Text)
            {
                clipboardManager.Text = string.Empty;
            }
            StaticStore.LastClipboardValue = null;
        }
    }
}
