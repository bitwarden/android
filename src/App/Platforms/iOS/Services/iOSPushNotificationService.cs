using System.Diagnostics;
using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.Core;
using Bit.Core.Resources.Localization;
using Bit.Core.Services;
using Foundation;
using Newtonsoft.Json;
using UIKit;
using UserNotifications;

namespace Bit.iOS.Services
{
    public class iOSPushNotificationService : NSObject, IPushNotificationService, IUNUserNotificationCenterDelegate
    {
        private const string TokenSetting = "token";
        const string TAG = "##PUSH NOTIFICATIONS";

        public Task<string> GetTokenAsync()
        {
            return Task.FromResult(NSUserDefaults.StandardUserDefaults.StringForKey(TokenSetting));
        }

        public bool IsRegisteredForPush => UIApplication.SharedApplication.IsRegisteredForRemoteNotifications;

        public async Task<bool> AreNotificationsSettingsEnabledAsync()
        {
            var settings = await UNUserNotificationCenter.Current.GetNotificationSettingsAsync();
            return settings.AlertSetting == UNNotificationSetting.Enabled;
        }

        public async Task RegisterAsync()
        {
            Debug.WriteLine($"{TAG} RegisterAsync");

            var tcs = new TaskCompletionSource<bool>();

            var authOptions = UNAuthorizationOptions.Alert | UNAuthorizationOptions.Badge | UNAuthorizationOptions.Sound;
            UNUserNotificationCenter.Current.RequestAuthorization(authOptions, (granted, error) =>
            {
                if (error != null)
                {
                    Debug.WriteLine($"{TAG} {error}");
                }
                else
                {
                    Debug.WriteLine($"{TAG} {granted}");
                }

                tcs.SetResult(granted);
            });

            if (await tcs.Task)
            {
                Debug.WriteLine($"{TAG} RegisterForRemoteNotifications");
                UIApplication.SharedApplication.RegisterForRemoteNotifications();
            }
        }

        public Task UnregisterAsync()
        {
            Debug.WriteLine($"{TAG} UnregisterAsync");

            UIApplication.SharedApplication.UnregisterForRemoteNotifications();
            // TODO: unregister call
            // _pushNotificationListener.OnUnregistered(Device.iOS);
            NSUserDefaults.StandardUserDefaults.SetString(string.Empty, TokenSetting);
            NSUserDefaults.StandardUserDefaults.Synchronize();
            return Task.FromResult(0);
        }

        public void SendLocalNotification(string title, string message, BaseNotificationData data)
        {
            if (string.IsNullOrEmpty(data.Id))
            {
                throw new ArgumentNullException("notificationId cannot be null or empty.");
            }

            var content = new UNMutableNotificationContent()
            {
                Title = title,
                Body = message,
                CategoryIdentifier = Constants.iOSNotificationCategoryId
            };

            if (data != null)
            {
                content.UserInfo = NSDictionary.FromObjectAndKey(NSData.FromString(JsonConvert.SerializeObject(data), NSStringEncoding.UTF8), new NSString(Constants.NotificationData));
            }

            var actions = new UNNotificationAction[] { UNNotificationAction.FromIdentifier(Constants.iOSNotificationClearActionId, AppResources.Clear, UNNotificationActionOptions.Foreground) };
            var category = UNNotificationCategory.FromIdentifier(Constants.iOSNotificationCategoryId, actions, new string[] { }, UNNotificationCategoryOptions.CustomDismissAction);
            UNUserNotificationCenter.Current.SetNotificationCategories(new NSSet<UNNotificationCategory>(category));

            var request = UNNotificationRequest.FromIdentifier(data.Id, content, null);
            UNUserNotificationCenter.Current.AddNotificationRequest(request, (err) =>
            {
                if (err != null)
                {
                    Logger.Instance.Exception(new Exception($"Failed to schedule notification: {err}"));
                }
            });
        }

        public void DismissLocalNotification(string notificationId)
        {
            if (string.IsNullOrEmpty(notificationId))
            {
                return;
            }

            UNUserNotificationCenter.Current.RemovePendingNotificationRequests(new string[] { notificationId });
            UNUserNotificationCenter.Current.RemoveDeliveredNotifications(new string[] { notificationId });
        }
    }
}