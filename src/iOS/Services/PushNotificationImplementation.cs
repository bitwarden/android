using Bit.App.Abstractions;
using Bit.App.Utilities;
using Foundation;
using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Threading;
using UIKit;
using Xamarin.Forms;

namespace Bit.iOS.Services
{
    public class PushNotificationImplementation : IPushNotification, IPushNotificationHandler
    {
        public string Token => NSUserDefaults.StandardUserDefaults.StringForKey(PushNotificationKey.Token);

        public void Register()
        {
            var userNotificationTypes = UIUserNotificationType.Alert | UIUserNotificationType.Badge | 
                UIUserNotificationType.Sound;
            var settings = UIUserNotificationSettings.GetSettingsForTypes(userNotificationTypes, null);
            UIApplication.SharedApplication.RegisterUserNotificationSettings(settings);
        }

        public void Unregister()
        {
            UIApplication.SharedApplication.UnregisterForRemoteNotifications();
            OnUnregisteredSuccess();
        }

        private static string DictionaryToJson(NSDictionary dictionary)
        {
            NSError error;
            var json = NSJsonSerialization.Serialize(dictionary, NSJsonWritingOptions.PrettyPrinted, out error);
            return json.ToString(NSStringEncoding.UTF8);
        }

        public void OnMessageReceived(NSDictionary userInfo)
        {
            var parameters = new Dictionary<string, object>();
            var json = DictionaryToJson(userInfo);
            var values = JObject.Parse(json);

            var keyAps = new NSString("aps");
            if(userInfo.ContainsKey(keyAps))
            {
                var aps = userInfo.ValueForKey(keyAps) as NSDictionary;
                if(aps != null)
                {
                    foreach(var apsKey in aps)
                    {
                        parameters.Add(apsKey.Key.ToString(), apsKey.Value);
                        JToken temp;
                        if(!values.TryGetValue(apsKey.Key.ToString(), out temp))
                        {
                            values.Add(apsKey.Key.ToString(), apsKey.Value.ToString());
                        }
                    }
                }
            }

            CrossPushNotification.PushNotificationListener.OnMessage(values, Device.iOS);
        }

        public void OnErrorReceived(NSError error)
        {
            Debug.WriteLine("{0} - Registration Failed.", PushNotificationKey.DomainName);
            CrossPushNotification.PushNotificationListener.OnError(error.LocalizedDescription, Device.iOS);
        }

        public void OnRegisteredSuccess(NSData token)
        {
            Debug.WriteLine("{0} - Succesfully Registered.", PushNotificationKey.DomainName);

            var trimmedDeviceToken = token.Description;
            if(!string.IsNullOrWhiteSpace(trimmedDeviceToken))
            {
                trimmedDeviceToken = trimmedDeviceToken.Trim('<');
                trimmedDeviceToken = trimmedDeviceToken.Trim('>');
                trimmedDeviceToken = trimmedDeviceToken.Trim();
                trimmedDeviceToken = trimmedDeviceToken.Replace(" ", "");
            }

            Console.WriteLine("{0} - Token: {1}", PushNotificationKey.DomainName, trimmedDeviceToken);
            CrossPushNotification.PushNotificationListener.OnRegistered(trimmedDeviceToken, Device.iOS);
            NSUserDefaults.StandardUserDefaults.SetString(trimmedDeviceToken, PushNotificationKey.Token);
            NSUserDefaults.StandardUserDefaults.Synchronize();
        }

        public void OnUnregisteredSuccess()
        {
            CrossPushNotification.PushNotificationListener.OnUnregistered(Device.iOS);
            NSUserDefaults.StandardUserDefaults.SetString(string.Empty, PushNotificationKey.Token);
            NSUserDefaults.StandardUserDefaults.Synchronize();
        }
    }

    public interface IPushNotificationHandler
    {
        void OnMessageReceived(NSDictionary parameters);
        void OnErrorReceived(NSError error);
        void OnRegisteredSuccess(NSData token);
        void OnUnregisteredSuccess();
    }

    internal class CrossPushNotification
    {
        private static Lazy<IPushNotification> Implementation = new Lazy<IPushNotification>(
            () => new PushNotificationImplementation(), 
            LazyThreadSafetyMode.PublicationOnly);
        public static bool IsInitialized => PushNotificationListener != null;
        public static IPushNotificationListener PushNotificationListener { get; private set; }

        public static void Initialize<T>(T listener) where T : IPushNotificationListener
        {
            if(PushNotificationListener == null)
            {
                PushNotificationListener = listener;
                Debug.WriteLine("PushNotification plugin initialized.");
            }
            else
            {
                Debug.WriteLine("PushNotification plugin already initialized.");
            }
        }

        public static void Initialize<T>() where T : IPushNotificationListener, new()
        {
            Initialize(new T());
        }

        public static IPushNotification Current
        {
            get
            {
                if(!IsInitialized)
                {
                    throw new Exception("Not initialized.");
                }

                var ret = Implementation.Value;
                if(ret == null)
                {
                    throw new Exception("Not in PCL");
                }

                return ret;
            }
        }
    }
}
