#if !FDROID
using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Android.App;
using Android.Content;
using Android.OS;
using AndroidX.Core.App;
using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Droid.Receivers;
using Bit.App.Droid.Utilities;
using Newtonsoft.Json;
using Intent = Android.Content.Intent;

namespace Bit.Droid.Services
{
    public class AndroidPushNotificationService : IPushNotificationService
    {
        private readonly IStateService _stateService;
        private readonly IPushNotificationListenerService _pushNotificationListenerService;

        public AndroidPushNotificationService(
            IStateService stateService,
            IPushNotificationListenerService pushNotificationListenerService)
        {
            _stateService = stateService;
            _pushNotificationListenerService = pushNotificationListenerService;
        }

        public bool IsRegisteredForPush => NotificationManagerCompat.From(Android.App.Application.Context)?.AreNotificationsEnabled() ?? false;

        public Task<bool> AreNotificationsSettingsEnabledAsync()
        {
            return Task.FromResult(IsRegisteredForPush);
        }

        public async Task<string> GetTokenAsync()
        {
            return await _stateService.GetPushCurrentTokenAsync();
        }

        public async Task RegisterAsync()
        {
            var registeredToken = await _stateService.GetPushRegisteredTokenAsync();
            var currentToken = await GetTokenAsync();
            if (!string.IsNullOrWhiteSpace(registeredToken) && registeredToken != currentToken)
            {
                await _pushNotificationListenerService.OnRegisteredAsync(registeredToken, Device.Android);
            }
            else
            {
                await _stateService.SetPushLastRegistrationDateAsync(DateTime.UtcNow);
            }
        }

        public Task UnregisterAsync()
        {
            // Do we ever need to unregister?
            return Task.FromResult(0);
        }

        public void DismissLocalNotification(string notificationId)
        {
            if (int.TryParse(notificationId, out int intNotificationId))
            {
                var notificationManager = NotificationManagerCompat.From(Android.App.Application.Context);
                notificationManager.Cancel(intNotificationId);
            }
        }

        public void SendLocalNotification(string title, string message, BaseNotificationData data)
        {
            if (string.IsNullOrEmpty(data.Id))
            {
                throw new ArgumentNullException("notificationId cannot be null or empty.");
            }
            
            var context = Android.App.Application.Context;
            var intent = context.PackageManager?.GetLaunchIntentForPackage(context.PackageName ?? string.Empty);

            var builder = new NotificationCompat.Builder(context, Bit.Core.Constants.AndroidNotificationChannelId);
            if(intent != null && context.PackageManager != null && !string.IsNullOrEmpty(context.PackageName))
            {
                intent.PutExtra(Bit.Core.Constants.NotificationData, JsonConvert.SerializeObject(data));
                var pendingIntentFlags = AndroidHelpers.AddPendingIntentMutabilityFlag(PendingIntentFlags.UpdateCurrent, true);
                var pendingIntent = PendingIntent.GetActivity(context, 20220801, intent, pendingIntentFlags);

                var deleteIntent = new Intent(context, typeof(NotificationDismissReceiver));
                deleteIntent.PutExtra(Bit.Core.Constants.NotificationData, JsonConvert.SerializeObject(data));
                var deletePendingIntent = PendingIntent.GetBroadcast(context, 20220802, deleteIntent, pendingIntentFlags);

                builder.SetContentIntent(pendingIntent)
                    .SetDeleteIntent(deletePendingIntent);
            }
            
            builder.SetContentTitle(title)
               .SetContentText(message)
               .SetSmallIcon(Bit.Core.Resource.Drawable.ic_notification)
               .SetColor((int)Android.Graphics.Color.White)
               .SetAutoCancel(true);
            
            if (data is PasswordlessNotificationData passwordlessNotificationData && passwordlessNotificationData.TimeoutInMinutes > 0)
            {
                builder.SetTimeoutAfter(passwordlessNotificationData.TimeoutInMinutes * 60000);
            }

            var notificationManager = NotificationManagerCompat.From(context);
            notificationManager.Notify(int.Parse(data.Id), builder.Build());
        }
    }
}
#endif
