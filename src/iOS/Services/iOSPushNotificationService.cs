using Bit.App.Abstractions;
using Foundation;
using UIKit;
using Xamarin.Forms;

namespace Bit.iOS.Services
{
    public class iOSPushNotificationService : IPushNotificationService
    {
        private const string TokenSetting = "token";

        private readonly IPushNotificationListener _pushNotificationListener;

        public iOSPushNotificationService(
            IPushNotificationListener pushNotificationListener)
        {
            _pushNotificationListener = pushNotificationListener;
        }

        public string Token => NSUserDefaults.StandardUserDefaults.StringForKey(TokenSetting);

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
            _pushNotificationListener.OnUnregistered(Device.iOS);
            NSUserDefaults.StandardUserDefaults.SetString(string.Empty, TokenSetting);
            NSUserDefaults.StandardUserDefaults.Synchronize();
        }
    }
}
