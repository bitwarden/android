using Bit.App.Abstractions;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System;
using System.Diagnostics;
using System.Threading;
using Windows.Networking.PushNotifications;
using Xamarin.Forms;

namespace Bit.UWP.Services
{
    public class UwpPushNotificationService : IPushNotificationService
    {
        private PushNotificationChannel _channel;
        private JsonSerializer serializer = new JsonSerializer()
        {
            ReferenceLoopHandling = ReferenceLoopHandling.Ignore
        };

        public string Token => _channel?.Uri.ToString();

        public void Register()
        {
            Debug.WriteLine("Creating Push Notification Channel For Application");
            var channelTask = PushNotificationChannelManager.CreatePushNotificationChannelForApplicationAsync().AsTask();
            channelTask.Wait();

            Debug.WriteLine("Creating Push Notification Channel For Application - Done");
            _channel = channelTask.Result;

            Debug.WriteLine("Registering call back for Push Notification Channel");
            _channel.PushNotificationReceived += Channel_PushNotificationReceived;

            CrossPushNotification.PushNotificationListener.OnRegistered(Token, Device.Windows);
        }

        private void Channel_PushNotificationReceived(PushNotificationChannel sender, PushNotificationReceivedEventArgs args)
        {
            Debug.WriteLine("Push Notification Received " + args.NotificationType);
            JObject jobject = null;

            switch(args.NotificationType)
            {
                case PushNotificationType.Badge:
                    jobject = JObject.FromObject(args.BadgeNotification, serializer);
                    break;
                case PushNotificationType.Raw:
                    jobject = JObject.FromObject(args.RawNotification, serializer);
                    break;
                case PushNotificationType.Tile:
                    jobject = JObject.FromObject(args.TileNotification, serializer);
                    break;
                case PushNotificationType.TileFlyout:
                    jobject = JObject.FromObject(args.TileNotification, serializer);
                    break;
                case PushNotificationType.Toast:
                    jobject = JObject.FromObject(args.ToastNotification, serializer);
                    break;
            }

            Debug.WriteLine("Sending JObject to PushNotificationListener " + args.NotificationType);
            CrossPushNotification.PushNotificationListener.OnMessage(jobject, Device.Windows);
        }

        public void Unregister()
        {
            if(_channel != null)
            {
                _channel.PushNotificationReceived -= Channel_PushNotificationReceived;
                _channel = null;
            }

            CrossPushNotification.PushNotificationListener.OnUnregistered(Device.Windows);
        }
    }

    internal class CrossPushNotification
    {
        private static Lazy<IPushNotificationService> Implementation = new Lazy<IPushNotificationService>(
            () => new UwpPushNotificationService(),
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

        public static IPushNotificationService Current
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
