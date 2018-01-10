#if !FDROID
using System;
using Bit.App;
using Bit.App.Abstractions;
using Plugin.Settings.Abstractions;
using Xamarin.Forms;

namespace Bit.Android.Services
{
    public class AndroidPushNotificationService : IPushNotificationService
    {
        private readonly IPushNotificationListener _pushNotificationListener;
        private readonly ISettings _settings;

        public AndroidPushNotificationService(
            IPushNotificationListener pushNotificationListener,
            ISettings settings)
        {
            _pushNotificationListener = pushNotificationListener;
            _settings = settings;
        }

        public string Token => _settings.GetValueOrDefault(Constants.PushCurrentToken, null);

        public void Register()
        {
            var registeredToken = _settings.GetValueOrDefault(Constants.PushRegisteredToken, null);
            if(!string.IsNullOrWhiteSpace(registeredToken) && registeredToken != Token)
            {
                _pushNotificationListener.OnRegistered(registeredToken, Device.Android);
            }
            else
            {
                _settings.AddOrUpdateValue(Constants.PushLastRegistrationDate, DateTime.UtcNow);
            }
        }

        public void Unregister()
        {
            // Do we ever need to unregister?
        }
    }
}
#endif
