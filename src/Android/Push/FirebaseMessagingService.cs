#if !FDROID
using Android.App;
using Android.Content;
using Bit.App.Abstractions;
using Bit.Core.Utilities;
using Firebase.Messaging;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using Xamarin.Forms;

namespace Bit.Droid.Push
{
    [Service]
    [IntentFilter(new[] { "com.google.firebase.MESSAGING_EVENT" })]
    public class FirebaseMessagingService : Firebase.Messaging.FirebaseMessagingService
    {
        public async override void OnMessageReceived(RemoteMessage message)
        {
            if (message?.Data == null)
            {
                return;
            }
            var data = message.Data.ContainsKey("data") ? message.Data["data"] : null;
            if (data == null)
            {
                return;
            }
            try
            {
                var obj = JObject.Parse(data);
                var listener = ServiceContainer.Resolve<IPushNotificationListenerService>(
                    "pushNotificationListenerService");
                await listener.OnMessageAsync(obj, Device.Android);
            }
            catch (JsonReaderException ex)
            {
                System.Diagnostics.Debug.WriteLine(ex.ToString());
            }
        }
    }
}
#endif
