using System.Threading.Tasks;
using Bit.App.Abstractions;
using Foundation;
using UIKit;

namespace Bit.iOS.Services
{
    public class iOSPushNotificationService : IPushNotificationService
    {
        private const string TokenSetting = "token";

        public Task<string> GetTokenAsync()
        {
            return Task.FromResult(NSUserDefaults.StandardUserDefaults.StringForKey(TokenSetting));
        }

        public Task RegisterAsync()
        {
            var userNotificationTypes = UIUserNotificationType.Alert | UIUserNotificationType.Badge |
                UIUserNotificationType.Sound;
            var settings = UIUserNotificationSettings.GetSettingsForTypes(userNotificationTypes, null);
            UIApplication.SharedApplication.RegisterUserNotificationSettings(settings);
            return Task.FromResult(0);
        }

        public Task UnregisterAsync()
        {
            UIApplication.SharedApplication.UnregisterForRemoteNotifications();
            // TODO: unregister call
            // _pushNotificationListener.OnUnregistered(Device.iOS);
            NSUserDefaults.StandardUserDefaults.SetString(string.Empty, TokenSetting);
            NSUserDefaults.StandardUserDefaults.Synchronize();
            return Task.FromResult(0);
        }
    }
}