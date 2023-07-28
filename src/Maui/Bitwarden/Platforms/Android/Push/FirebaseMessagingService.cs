#if !FDROID
using System;
using Android.App;
using Bit.App.Abstractions;
using Bit.Core.Abstractions;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Firebase.Messaging;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using Xamarin.Forms;

namespace Bit.App.Droid.Push
{
    [Service(Exported=false)]
    [IntentFilter(new[] { "com.google.firebase.MESSAGING_EVENT" })]
    public class FirebaseMessagingService : Firebase.Messaging.FirebaseMessagingService
    {
        public async override void OnNewToken(string token)
        {
            try { 
                var stateService = ServiceContainer.Resolve<IStateService>("stateService");
                var pushNotificationService = ServiceContainer.Resolve<IPushNotificationService>("pushNotificationService");
            
                await stateService.SetPushRegisteredTokenAsync(token);
                await pushNotificationService.RegisterAsync();
            }
            catch (Exception ex)
            {
                Logger.Instance.Exception(ex);
            }
        }
        
        public async override void OnMessageReceived(RemoteMessage message)
        {
            try
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

                var obj = JObject.Parse(data);
                var listener = ServiceContainer.Resolve<IPushNotificationListenerService>(
                    "pushNotificationListenerService");
                await listener.OnMessageAsync(obj, Device.Android);
            }
            catch (Exception ex)
            {
                Logger.Instance.Exception(ex);
            }
        }
    }
}
#endif
