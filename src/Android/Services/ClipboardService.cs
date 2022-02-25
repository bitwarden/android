using System;
using System.Threading.Tasks;
using Android.App;
using Android.Content;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Droid.Receivers;
using Plugin.CurrentActivity;
using Xamarin.Essentials;

namespace Bit.Droid.Services
{
    public class ClipboardService : IClipboardService
    {
        private readonly IStateService _stateService;
        private readonly Lazy<PendingIntent> _clearClipboardPendingIntent;

        public ClipboardService(IStateService stateService)
        {
            _stateService = stateService;

            _clearClipboardPendingIntent = new Lazy<PendingIntent>(() =>
                PendingIntent.GetBroadcast(CrossCurrentActivity.Current.Activity,
                                           0,
                                           new Intent(CrossCurrentActivity.Current.Activity, typeof(ClearClipboardAlarmReceiver)),
                                           PendingIntentFlags.UpdateCurrent));
        }

        public async Task CopyTextAsync(string text, int expiresInMs = -1)
        {
            await Clipboard.SetTextAsync(text);

            await ClearClipboardAlarmAsync(expiresInMs);
        }

        private async Task ClearClipboardAlarmAsync(int expiresInMs = -1)
        {
            var clearMs = expiresInMs;
            if (clearMs < 0)
            {
                // if not set then we need to check if the user set this config
                var clearSeconds = await _stateService.GetClearClipboardAsync();
                if (clearSeconds != null)
                {
                    clearMs = clearSeconds.Value * 1000;
                }
            }
            if (clearMs < 0)
            {
                return;
            }
            var triggerMs = Java.Lang.JavaSystem.CurrentTimeMillis() + clearMs;
            var alarmManager = CrossCurrentActivity.Current.Activity.GetSystemService(Context.AlarmService) as AlarmManager;
            alarmManager.Set(AlarmType.Rtc, triggerMs, _clearClipboardPendingIntent.Value);
        }
    }
}
