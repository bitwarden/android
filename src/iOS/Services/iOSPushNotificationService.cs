using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.Core.Services;
using Foundation;
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

        public void SendLocalNotification(string title, string message, string notificationId, string notificationType, Dictionary<string, string> notificationData = null, int notificationTimeoutMinutes = 0)
        {
            if (string.IsNullOrEmpty(notificationId))
            {
                throw new ArgumentNullException("notificationId cannot be null or empty.");
            }

            var content = new UNMutableNotificationContent()
            {
                Title = title,
                Body = message
            };

            if (notificationData != null)
            {
                notificationData.Add("type", notificationType);
                content.UserInfo = NSDictionary.FromObjectsAndKeys(notificationData.Values.ToArray(), notificationData.Keys.ToArray());
            }

            var request = UNNotificationRequest.FromIdentifier(notificationId, content, null);
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