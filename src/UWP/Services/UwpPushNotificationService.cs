using Bit.App.Abstractions;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System;
using System.Diagnostics;
using Windows.Networking.PushNotifications;
using Xamarin.Forms;

namespace Bit.UWP.Services
{
    public class UwpPushNotificationService : IPushNotificationService
    {
        private PushNotificationChannel _channel;
        private JsonSerializer _serializer = new JsonSerializer
        {
            ReferenceLoopHandling = ReferenceLoopHandling.Ignore
        };
        private readonly IPushNotificationListener _pushNotificationListener;

        public UwpPushNotificationService(IPushNotificationListener pushNotificationListener)
        {
            _pushNotificationListener = pushNotificationListener;
        }

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

            _pushNotificationListener.OnRegistered(Token, Device.UWP);
        }

        private void Channel_PushNotificationReceived(PushNotificationChannel sender, PushNotificationReceivedEventArgs args)
        {
            Debug.WriteLine("Push Notification Received " + args.NotificationType);
            JObject jobject = null;

            switch(args.NotificationType)
            {
                case PushNotificationType.Badge:
                    jobject = JObject.FromObject(args.BadgeNotification, _serializer);
                    break;
                case PushNotificationType.Raw:
                    jobject = JObject.FromObject(args.RawNotification, _serializer);
                    break;
                case PushNotificationType.Tile:
                    jobject = JObject.FromObject(args.TileNotification, _serializer);
                    break;
                case PushNotificationType.TileFlyout:
                    jobject = JObject.FromObject(args.TileNotification, _serializer);
                    break;
                case PushNotificationType.Toast:
                    jobject = JObject.FromObject(args.ToastNotification, _serializer);
                    break;
            }

            Debug.WriteLine("Sending JObject to PushNotificationListener " + args.NotificationType);
            _pushNotificationListener.OnMessage(jobject, Device.UWP);
        }

        public void Unregister()
        {
            if(_channel != null)
            {
                _channel.PushNotificationReceived -= Channel_PushNotificationReceived;
                _channel = null;
            }

            _pushNotificationListener.OnUnregistered(Device.UWP);
        }
    }
}
