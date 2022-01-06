using System;
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

        public Task<string> GetTokenAsync()
        {
            return Task.FromResult(NSUserDefaults.StandardUserDefaults.StringForKey(TokenSetting));
        }

        public bool IsRegisteredForPush => UIApplication.SharedApplication.IsRegisteredForRemoteNotifications;

        public async Task RegisterAsync()
        {
            var tcs = new TaskCompletionSource<bool>();

            var authOptions = UNAuthorizationOptions.Alert | UNAuthorizationOptions.Badge | UNAuthorizationOptions.Sound;
            UNUserNotificationCenter.Current.RequestAuthorization(authOptions, (granted, error) =>
            {
                if (error != null)
                {
                    Console.WriteLine($"Push Notifications {error}");
                }
                else
                {
                    Console.WriteLine($"Push Notifications {granted}");
                }

                tcs.SetResult(granted);
            });

            if (await tcs.Task)
            {
                UIApplication.SharedApplication.RegisterForRemoteNotifications();
            }
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