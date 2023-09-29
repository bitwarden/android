using System.Diagnostics;
using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.Core;
using Bit.Core.Services;
using Foundation;
using Newtonsoft.Json.Linq;
using UserNotifications;

namespace Bit.iOS.Services
{
    public class iOSPushNotificationHandler : NSObject, IUNUserNotificationCenterDelegate
    {
        private const string TokenSetting = "token";
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
                Debug.WriteLine($"{TAG} - OnMessageReceived.");

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
                Logger.Instance.Exception(ex);
            }
        }

        public void OnErrorReceived(NSError error)
        {
            Debug.WriteLine($"{TAG} - Registration Failed.");
            _pushNotificationListenerService.OnError(error.LocalizedDescription, Device.iOS);
        }

        public void OnRegisteredSuccess(NSData token)
        {
            Debug.WriteLine($"{TAG} - Successfully Registered.");

            var hexDeviceToken = BitConverter.ToString(token.ToArray())
                                             .Replace("-", string.Empty)
                                             .ToLowerInvariant();

            Debug.WriteLine($"{TAG} - Token: {hexDeviceToken}");

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
            Debug.WriteLine($"{TAG} WillPresentNotification {notification?.Request?.Content?.UserInfo}");
            OnMessageReceived(notification?.Request?.Content?.UserInfo);
            completionHandler(UNNotificationPresentationOptions.Alert);
        }

        [Export("userNotificationCenter:didReceiveNotificationResponse:withCompletionHandler:")]
        public void DidReceiveNotificationResponse(UNUserNotificationCenter center, UNNotificationResponse response, Action completionHandler)
        {
            Debug.WriteLine($"{TAG} DidReceiveNotificationResponse {response?.Notification?.Request?.Content?.UserInfo}");
            if ((response?.Notification?.Request?.Content?.UserInfo) == null)
            {
                completionHandler();
                return;
            }

            var userInfo = response?.Notification?.Request?.Content?.UserInfo;
            OnMessageReceived(userInfo);

            if (userInfo.TryGetValue(NSString.FromObject(Constants.NotificationData), out NSObject nsObject))
            {
                var token = JToken.Parse(NSString.FromObject(nsObject).ToString());
                var typeToken = token.SelectToken(Constants.NotificationDataType);
                if (response.IsDefaultAction)
                {
                    if (typeToken.ToString() == PasswordlessNotificationData.TYPE)
                    {
                        _pushNotificationListenerService.OnNotificationTapped(token.ToObject<PasswordlessNotificationData>());
                    }
                }
                else if (response.IsDismissAction)
                {
                    if (typeToken.ToString() == PasswordlessNotificationData.TYPE)
                    {
                        _pushNotificationListenerService.OnNotificationDismissed(token.ToObject<PasswordlessNotificationData>());
                    }
                }
            }
            
            // Inform caller it has been handled
            completionHandler();
        }
    }
}
