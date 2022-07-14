using System;
using System.Threading.Tasks;
using Android.App;
using Android.Content;
using Android.OS;
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
                // #1962 Just ignore, the content is copied either way but there is some app interfiering in the process
                // that the OS catches and just throws this exception.
            }
        }

        public bool IsCopyNotificationHandledByPlatform()
        {
            // Android 13+ provides built-in notification when text is copied to the clipboard
            return (int)Build.VERSION.SdkInt >= 33;
        }

        private void CopyToClipboard(string text, bool isSensitive = true)
        {
            var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
            var clipboardManager = activity.GetSystemService(
                Context.ClipboardService) as Android.Content.ClipboardManager;
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
            var alarmManager = CrossCurrentActivity.Current.Activity.GetSystemService(Context.AlarmService) as AlarmManager;
            alarmManager.Set(AlarmType.Rtc, triggerMs, _clearClipboardPendingIntent.Value);
        }
    }
}
