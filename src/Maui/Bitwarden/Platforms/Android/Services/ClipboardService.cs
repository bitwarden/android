using System;
using System.Threading.Tasks;
using Android.App;
using Android.Content;
using Android.OS;
using Bit.Core.Abstractions;
using Bit.App.Droid.Receivers;
using Bit.App.Droid.Utilities;

namespace Bit.App.Droid.Services
{
    public class ClipboardService : IClipboardService
    {
        private readonly IStateService _stateService;
        private readonly Lazy<PendingIntent> _clearClipboardPendingIntent;

        public ClipboardService(IStateService stateService)
        {
            _stateService = stateService;

            _clearClipboardPendingIntent = new Lazy<PendingIntent>(() =>
                PendingIntent.GetBroadcast(Android.App.Application.Context,
                                           0,
                                           new Intent(Android.App.Application.Context, typeof(ClearClipboardAlarmReceiver)),
                                           AndroidHelpers.AddPendingIntentMutabilityFlag(PendingIntentFlags.UpdateCurrent, false)));
        }

        public async Task CopyTextAsync(string text, int expiresInMs = -1, bool isSensitive = true)
        {
            try
            {
                 // Xamarin.Essentials.Clipboard currently doesn't support the IS_SENSITIVE flag for API 33+
                if ((int)Build.VERSION.SdkInt < 33)
                {
                    await Clipboard.SetTextAsync(text);
                }
                else
                {
                    CopyToClipboard(text, isSensitive);
                }

                await ClearClipboardAlarmAsync(expiresInMs);
            }
            catch (Java.Lang.SecurityException ex) when (ex.Message.Contains("does not belong to"))
            {
                // #1962 Just ignore, the content is copied either way but there is some app interfering in the process
                // that the OS catches and just throws this exception.
            }
        }

        private void CopyToClipboard(string text, bool isSensitive = true)
        {
            var clipboardManager = Android.App.Application.Context.GetSystemService(Context.ClipboardService) as ClipboardManager;
            var clipData = ClipData.NewPlainText("bitwarden", text);
            if (isSensitive)
            {
                clipData.Description.Extras ??= new PersistableBundle();
                clipData.Description.Extras.PutBoolean("android.content.extra.IS_SENSITIVE", true);
            }
            clipboardManager.PrimaryClip = clipData;
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
            var alarmManager = Android.App.Application.Context.GetSystemService(Context.AlarmService) as AlarmManager;
            alarmManager.Set(AlarmType.Rtc, triggerMs, _clearClipboardPendingIntent.Value);
        }
    }
}
