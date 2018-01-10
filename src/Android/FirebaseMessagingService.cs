#if !FDROID
using System;
using Android.App;
using Android.Content;
using Bit.App.Abstractions;
using Firebase.Messaging;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.Android
{
    [Service]
    [IntentFilter(new[] { "com.google.firebase.MESSAGING_EVENT" })]
    public class FirebaseMessagingService : Firebase.Messaging.FirebaseMessagingService
    {
        public override void OnMessageReceived(RemoteMessage message)
        {
            if(message?.Data == null)
            {
                return;
            }

            var data = message.Data.ContainsKey("data") ? message.Data["data"] : null;
            if(data == null)
            {
                return;
            }

            try
            {
                var obj = JObject.Parse(data);
                var listener = Resolver.Resolve<IPushNotificationListener>();
                listener.OnMessage(obj, Device.Android);
            }
            catch(JsonReaderException ex)
            {
                System.Diagnostics.Debug.WriteLine(ex.ToString());
            }
        }
    }
}
#endif
