#if !FDROID
using System;
using Android.App;
using Android.Content;
using Bit.App;
using Bit.App.Abstractions;
using Firebase.Iid;
using Plugin.Settings.Abstractions;
using XLabs.Ioc;

namespace Bit.Android
{
    [Service]
    [IntentFilter(new[] { "com.google.firebase.INSTANCE_ID_EVENT" })]
    public class FirebaseInstanceIdService : Firebase.Iid.FirebaseInstanceIdService
    {
        public override void OnTokenRefresh()
        {
            var settings = Resolver.Resolve<ISettings>();
            settings.AddOrUpdateValue(Constants.PushRegisteredToken, FirebaseInstanceId.Instance.Token);
            Resolver.Resolve<IPushNotificationService>()?.Register();
        }
    }
}
#endif
