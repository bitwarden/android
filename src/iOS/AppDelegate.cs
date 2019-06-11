using System;
using System.IO;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Services;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.iOS.Core.Services;
using Bit.iOS.Services;
using Foundation;
using UIKit;

namespace Bit.iOS
{
    [Register("AppDelegate")]
    public partial class AppDelegate : Xamarin.Forms.Platform.iOS.FormsApplicationDelegate
    {
        private const string AppId = "com.8bit.bitwarden";
        private const string AppGroupId = "group.com.8bit.bitwarden";
        private const string AccessGroup = "LTZ2PFU5D6.com.8bit.bitwarden";

        private iOSPushNotificationHandler _pushHandler;

        public override bool FinishedLaunching(UIApplication app, NSDictionary options)
        {
            Xamarin.Forms.Forms.Init();
            InitApp();
            Bootstrap();

            LoadApplication(new App.App(null));

            ZXing.Net.Mobile.Forms.iOS.Platform.Init();
            return base.FinishedLaunching(app, options);
        }

        public override void FailedToRegisterForRemoteNotifications(UIApplication application, NSError error)
        {
            _pushHandler?.OnErrorReceived(error);
        }

        public override void RegisteredForRemoteNotifications(UIApplication application, NSData deviceToken)
        {
            _pushHandler?.OnRegisteredSuccess(deviceToken);
        }

        public override void DidRegisterUserNotificationSettings(UIApplication application,
            UIUserNotificationSettings notificationSettings)
        {
            application.RegisterForRemoteNotifications();
        }

        public override void DidReceiveRemoteNotification(UIApplication application, NSDictionary userInfo,
            Action<UIBackgroundFetchResult> completionHandler)
        {
            _pushHandler?.OnMessageReceived(userInfo);
        }

        public override void ReceivedRemoteNotification(UIApplication application, NSDictionary userInfo)
        {
            _pushHandler?.OnMessageReceived(userInfo);
        }

        private void InitApp()
        {
            if(ServiceContainer.RegisteredServices.Count > 0)
            {
                return;
            }
            RegisterLocalServices();
            ServiceContainer.Init();
            _pushHandler = new iOSPushNotificationHandler(
                ServiceContainer.Resolve<IPushNotificationListenerService>("pushNotificationListenerService"));
            // TODO: HockeyApp Init
        }

        private void RegisterLocalServices()
        {
            ServiceContainer.Register<ILogService>("logService", new ConsoleLogService());
            ServiceContainer.Register("settingsShim", new App.Migration.SettingsShim());
            if(false && App.Migration.MigrationHelpers.NeedsMigration())
            {
                ServiceContainer.Register<App.Migration.Abstractions.IOldSecureStorageService>(
                    "oldSecureStorageService", new Migration.KeyChainStorageService());
            }

            // Note: This might cause a race condition. Investigate more.
            Task.Run(() =>
            {
                FFImageLoading.Forms.Platform.CachedImageRenderer.Init();
                FFImageLoading.ImageService.Instance.Initialize(new FFImageLoading.Config.Configuration
                {
                    FadeAnimationEnabled = false,
                    FadeAnimationForCachedImages = false
                });
            });

            var preferencesStorage = new PreferencesStorageService(AppGroupId);
            var appGroupContainer = new NSFileManager().GetContainerUrl(AppGroupId);
            var liteDbStorage = new LiteDbStorageService(
                Path.Combine(appGroupContainer.Path, "Library", "bitwarden.db"));
            liteDbStorage.InitAsync();
            var localizeService = new LocalizeService();
            var broadcasterService = new BroadcasterService();
            var messagingService = new MobileBroadcasterMessagingService(broadcasterService);
            var i18nService = new MobileI18nService(localizeService.GetCurrentCultureInfo());
            var secureStorageService = new KeyChainStorageService(AppId, AccessGroup);
            var cryptoPrimitiveService = new CryptoPrimitiveService();
            var mobileStorageService = new MobileStorageService(preferencesStorage, liteDbStorage);
            var deviceActionService = new DeviceActionService(mobileStorageService, messagingService);
            var platformUtilsService = new MobilePlatformUtilsService(deviceActionService, messagingService,
                broadcasterService);

            ServiceContainer.Register<IBroadcasterService>("broadcasterService", broadcasterService);
            ServiceContainer.Register<IMessagingService>("messagingService", messagingService);
            ServiceContainer.Register<ILocalizeService>("localizeService", localizeService);
            ServiceContainer.Register<II18nService>("i18nService", i18nService);
            ServiceContainer.Register<ICryptoPrimitiveService>("cryptoPrimitiveService", cryptoPrimitiveService);
            ServiceContainer.Register<IStorageService>("storageService", mobileStorageService);
            ServiceContainer.Register<IStorageService>("secureStorageService", secureStorageService);
            ServiceContainer.Register<IDeviceActionService>("deviceActionService", deviceActionService);
            ServiceContainer.Register<IPlatformUtilsService>("platformUtilsService", platformUtilsService);

            // Push
            var notificationListenerService = new PushNotificationListenerService();
            ServiceContainer.Register<IPushNotificationListenerService>(
                "pushNotificationListenerService", notificationListenerService);
            var iosPushNotificationService = new iOSPushNotificationService();
            ServiceContainer.Register<IPushNotificationService>(
                "pushNotificationService", iosPushNotificationService);
        }

        private void Bootstrap()
        {
            (ServiceContainer.Resolve<II18nService>("i18nService") as MobileI18nService).Init();
            ServiceContainer.Resolve<IAuthService>("authService").Init();
            // Note: This is not awaited
            var bootstrapTask = BootstrapAsync();
        }

        private async Task BootstrapAsync()
        {
            var disableFavicon = await ServiceContainer.Resolve<IStorageService>("storageService").GetAsync<bool?>(
                Constants.DisableFaviconKey);
            await ServiceContainer.Resolve<IStateService>("stateService").SaveAsync(Constants.DisableFaviconKey,
                disableFavicon);
            await ServiceContainer.Resolve<IEnvironmentService>("environmentService").SetUrlsFromStorageAsync();
        }
    }
}
