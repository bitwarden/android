using Android.Content;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;

namespace Bit.Droid.Receivers
{
    [BroadcastReceiver(Name = "com.x8bit.bitwarden.ClearClipboardAlarmReceiver", Exported = false)]
    public class ClearClipboardAlarmReceiver : BroadcastReceiver
    {
        public async override void OnReceive(Context context, Intent intent)
        {
            var stateService = ServiceContainer.Resolve<IStateService>("stateService");
            var clipboardManager = context.GetSystemService(Context.ClipboardService) as ClipboardManager;
            var lastClipboardValue = await stateService.GetAsync<string>(Constants.LastClipboardValueKey);
            await stateService.RemoveAsync(Constants.LastClipboardValueKey);
            if(lastClipboardValue == clipboardManager.Text)
            {
                clipboardManager.Text = string.Empty;
            }
        }
    }
}
