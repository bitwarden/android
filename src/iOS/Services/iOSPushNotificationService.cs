using System.Diagnostics;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Foundation;
using UIKit;
using UserNotifications;

namespace Bit.iOS.Services
{
    public class iOSPushNotificationService : NSObject, IPushNotificationService, IUNUserNotificationCenterDelegate
    {
        private const string TokenSetting = "token";
        const string TAG = "##PUSH NOTIFICATIONS";

        public Task<string> GetTokenAsync()
        {
            return Task.FromResult(NSUserDefaults.StandardUserDefaults.StringForKey(TokenSetting));
        }

        public bool IsRegisteredForPush => UIApplication.SharedApplication.IsRegisteredForRemoteNotifications;

        public async Task RegisterAsync()
        {
            Debug.WriteLine($"{TAG} RegisterAsync");

            var tcs = new TaskCompletionSource<bool>();

            var authOptions = UNAuthorizationOptions.Alert | UNAuthorizationOptions.Badge | UNAuthorizationOptions.Sound;
            UNUserNotificationCenter.Current.RequestAuthorization(authOptions, (granted, error) =>
            {
                if (error != null)
                {
                    Debug.WriteLine($"{TAG} {error}");
                }
                else
                {
                    Debug.WriteLine($"{TAG} {granted}");
                }

                tcs.SetResult(granted);
            });

            if (await tcs.Task)
            {
                Debug.WriteLine($"{TAG} RegisterForRemoteNotifications");
                UIApplication.SharedApplication.RegisterForRemoteNotifications();
            }
        }

        public Task UnregisterAsync()
        {
            Debug.WriteLine($"{TAG} UnregisterAsync");

            UIApplication.SharedApplication.UnregisterForRemoteNotifications();
            // TODO: unregister call
            // _pushNotificationListener.OnUnregistered(Device.iOS);
            NSUserDefaults.StandardUserDefaults.SetString(string.Empty, TokenSetting);
            NSUserDefaults.StandardUserDefaults.Synchronize();
            return Task.FromResult(0);
        }
    }
}