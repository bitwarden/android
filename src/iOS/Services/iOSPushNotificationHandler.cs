using System;
using System.Diagnostics;
using Bit.App.Abstractions;
using Foundation;
using Microsoft.AppCenter.Crashes;
using Newtonsoft.Json.Linq;
using UserNotifications;
using Xamarin.Forms;

namespace Bit.iOS.Services
{
    public class iOSPushNotificationHandler : NSObject, IUNUserNotificationCenterDelegate
    {
        private const string TokenSetting = "token";
        //private const string DomainName = "iOSPushNotificationService";

        const string TAG = "##PUSH NOTIFICATIONS";

        private readonly IPushNotificationListenerService _pushNotificationListenerService;

        public iOSPushNotificationHandler(
            IPushNotificationListenerService pushNotificationListenerService)
        {
            _pushNotificationListenerService = pushNotificationListenerService;
        }

        public void OnMessageReceived(NSDictionary userInfo)
        {
            try
            {
                Console.WriteLine($"{TAG} - OnMessageReceived.");

                var json = DictionaryToJson(userInfo);
                var values = JObject.Parse(json);
                var keyAps = new NSString("aps");
                if (userInfo.ContainsKey(keyAps) && userInfo.ValueForKey(keyAps) is NSDictionary aps)
                {
                    foreach (var apsKey in aps)
                    {
                        if (!values.TryGetValue(apsKey.Key.ToString(), out JToken temp))
                        {
                            values.Add(apsKey.Key.ToString(), apsKey.Value.ToString());
                        }
                    }
                }
                _pushNotificationListenerService.OnMessageAsync(values, Device.iOS);
            }
            catch (Exception ex)
            {
                Crashes.TrackError(ex);
            }
        }

        public void OnErrorReceived(NSError error)
        {
            Console.WriteLine($"{TAG} - Registration Failed.");
            _pushNotificationListenerService.OnError(error.LocalizedDescription, Device.iOS);
        }

        public void OnRegisteredSuccess(NSData token)
        {
            Console.WriteLine($"{TAG} - Successfully Registered.");

            var hexDeviceToken = BitConverter.ToString(token.ToArray())
                                             .Replace("-", string.Empty)
                                             .ToLowerInvariant();

            Console.WriteLine($"{TAG} - Token: {hexDeviceToken}");

            UNUserNotificationCenter.Current.Delegate = this;

            _pushNotificationListenerService.OnRegisteredAsync(hexDeviceToken, Device.iOS);
            NSUserDefaults.StandardUserDefaults.SetString(hexDeviceToken, TokenSetting);
            NSUserDefaults.StandardUserDefaults.Synchronize();
        }

        private static string DictionaryToJson(NSDictionary dictionary)
        {
            var json = NSJsonSerialization.Serialize(dictionary, NSJsonWritingOptions.PrettyPrinted, out NSError error);
            return json.ToString(NSStringEncoding.UTF8);
        }

        // To receive notifications in foreground on iOS 10 devices.
        [Export("userNotificationCenter:willPresentNotification:withCompletionHandler:")]
        public void WillPresentNotification(UNUserNotificationCenter center, UNNotification notification, Action<UNNotificationPresentationOptions> completionHandler)
        {
            Console.WriteLine($"{TAG} WillPresentNotification {notification?.Request?.Content?.UserInfo}");

            OnMessageReceived(notification?.Request?.Content?.UserInfo);
            completionHandler(UNNotificationPresentationOptions.Alert);
        }

        [Export("userNotificationCenter:didReceiveNotificationResponse:withCompletionHandler:")]
        public void DidReceiveNotificationResponse(UNUserNotificationCenter center, UNNotificationResponse response, Action completionHandler)
        {
            Console.WriteLine($"{TAG} DidReceiveNotificationResponse {response?.Notification?.Request?.Content?.UserInfo}");

            if (response.IsDefaultAction)
            {
                OnMessageReceived(response?.Notification?.Request?.Content?.UserInfo);
            }

            // Inform caller it has been handled
            completionHandler();
        }
    }
}
