#if !FDROID
using System;
using Android.App;
using Bit.App.Abstractions;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Firebase.Messaging;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace Bit.Droid.Push
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

                JObject obj = null;
                if (message.Data.TryGetValue("data", out var data))
                {
                    // Legacy GCM format
                    obj = JObject.Parse(data);
                }
                else if (message.Data.TryGetValue("type", out var typeData) &&
                    Enum.TryParse(typeData, out NotificationType type))
                {
                    // New FCMv1 format
                    obj = new JObject
                    {
                        { "type", (int)type }
                    };

                    if (message.Data.TryGetValue("payload", out var payloadData))
                    {
                        obj.Add("payload", payloadData);
                    }
                }

                if (obj == null)
                {
                    return;
                }

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
