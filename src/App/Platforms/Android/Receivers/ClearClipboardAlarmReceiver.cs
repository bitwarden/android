using Android.Content;
using Android.OS;

namespace Bit.Droid.Receivers
{
    [BroadcastReceiver(Name = "com.x8bit.bitwarden.ClearClipboardAlarmReceiver", Exported = false)]
    public class ClearClipboardAlarmReceiver : BroadcastReceiver
    {
        public override void OnReceive(Context context, Intent intent)
        {
            var clipboardManager = context.GetSystemService(Context.ClipboardService) as ClipboardManager;
            if (clipboardManager == null)
            {
                return;
            }
            // ClearPrimaryClip is supported down to API 28 with mixed results, so we're requiring 33+ instead
            if ((int)Build.VERSION.SdkInt < 33)
            {
                clipboardManager.PrimaryClip = ClipData.NewPlainText("bitwarden", " ");
                return;
            }
            clipboardManager.ClearPrimaryClip();
        }
    }
}
