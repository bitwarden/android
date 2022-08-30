#if !FDROID
using System;
using System.Threading.Tasks;
using Android.App;
using Android.Content;
using AndroidX.Core.App;
using Bit.App.Abstractions;
using Bit.Core;
using Bit.Core.Abstractions;
using Xamarin.Forms;

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
            var notificationManager = NotificationManagerCompat.From(Android.App.Application.Context);
            notificationManager.Cancel(int.Parse(notificationId));
        }

        public void SendLocalNotification(string title, string message, string notificationId = null)
        {
            notificationId = notificationId ?? new Random().Next(1000, 999999).ToString();
            var context = Android.App.Application.Context;
            var intent = new Intent(context, typeof(MainActivity));
            var pendingIntent = PendingIntent.GetActivity(context, new Random().Next(1000, 999999), intent, PendingIntentFlags.OneShot);
            var builder = new NotificationCompat.Builder(context, Constants.AndroidNotificationChannelId)
               .SetContentIntent(pendingIntent)
               .SetContentTitle(title)
               .SetContentText(message)
               .SetSmallIcon(Resource.Mipmap.ic_launcher)
               .SetAutoCancel(true);

            // Build the notification:
            var notification = builder.Build();
            var notificationManager = NotificationManagerCompat.From(context);
            notificationManager.Notify(int.Parse(notificationId), notification);
        }
    }
}
#endif
