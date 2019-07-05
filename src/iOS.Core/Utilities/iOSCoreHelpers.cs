using System.IO;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Services;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.iOS.Core.Services;
using Foundation;
using HockeyApp.iOS;
using UIKit;

namespace Bit.iOS.Core.Utilities
{
    public static class iOSCoreHelpers
    {
        public static string AppId = "com.8bit.bitwarden";
        public static string AppAutofillId = "com.8bit.bitwarden.autofill";
        public static string AppExtensionId = "com.8bit.bitwarden.find-login-action-extension";
        public static string AppGroupId = "group.com.8bit.bitwarden";
        public static string AccessGroup = "LTZ2PFU5D6.com.8bit.bitwarden";

        public static void RegisterHockeyApp()
        {
            var crashManagerDelegate = new HockeyAppCrashManagerDelegate(
                ServiceContainer.Resolve<IAppIdService>("appIdService"),
                ServiceContainer.Resolve<IUserService>("userService"));
            var manager = BITHockeyManager.SharedHockeyManager;
            manager.Configure("51f96ae568ba45f699a18ad9f63046c3", crashManagerDelegate);
            manager.CrashManager.CrashManagerStatus = BITCrashManagerStatus.AutoSend;
            manager.StartManager();
            manager.Authenticator.AuthenticateInstallation();
            manager.DisableMetricsManager = manager.DisableFeedbackManager = manager.DisableUpdateManager = true;
            var task = crashManagerDelegate.InitAsync(manager);
        }

        public static void RegisterLocalServices()
        {
            if(ServiceContainer.Resolve<ILogService>("logService", true) == null)
            {
                ServiceContainer.Register<ILogService>("logService", new ConsoleLogService());
            }

            var preferencesStorage = new PreferencesStorageService(AppGroupId);
            var appGroupContainer = new NSFileManager().GetContainerUrl(AppGroupId);
            var liteDbStorage = new LiteDbStorageService(
                Path.Combine(appGroupContainer.Path, "Library", "bitwarden.db"));
            liteDbStorage.InitAsync();
            var localizeService = new LocalizeService();
            var broadcasterService = new BroadcasterService();
            var messagingService = new MobileBroadcasterMessagingService(broadcasterService);
            var i18nService = new MobileI18nService(localizeService.GetCurrentCultureInfo());
            var secureStorageService = new KeyChainStorageService(AppId, AccessGroup,
                () => ServiceContainer.Resolve<IAppIdService>("appIdService").GetAppIdAsync());
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
        }

        public static void Bootstrap()
        {
            (ServiceContainer.Resolve<II18nService>("i18nService") as MobileI18nService).Init();
            ServiceContainer.Resolve<IAuthService>("authService").Init();
            // Note: This is not awaited
            var bootstrapTask = BootstrapAsync();
        }

        public static void AppearanceAdjustments()
        {
            ThemeHelpers.SetAppearance(ThemeManager.GetTheme(false));
            UIApplication.SharedApplication.StatusBarHidden = false;
            UIApplication.SharedApplication.StatusBarStyle = UIStatusBarStyle.LightContent;
        }

        private static async Task BootstrapAsync()
        {
            var disableFavicon = await ServiceContainer.Resolve<IStorageService>("storageService").GetAsync<bool?>(
                Bit.Core.Constants.DisableFaviconKey);
            await ServiceContainer.Resolve<IStateService>("stateService").SaveAsync(
                Bit.Core.Constants.DisableFaviconKey, disableFavicon);
            await ServiceContainer.Resolve<IEnvironmentService>("environmentService").SetUrlsFromStorageAsync();
        }
    }
}