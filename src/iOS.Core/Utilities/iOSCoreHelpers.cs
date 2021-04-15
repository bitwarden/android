using System;
using System.IO;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Resources;
using Bit.App.Services;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.iOS.Core.Services;
using CoreNFC;
using Foundation;
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

        public static void RegisterAppCenter()
        {
            var appCenterHelper = new AppCenterHelper(
                ServiceContainer.Resolve<IAppIdService>("appIdService"),
                ServiceContainer.Resolve<IUserService>("userService"));
            var appCenterTask = appCenterHelper.InitAsync();
        }

        public static void RegisterLocalServices()
        {
            if (ServiceContainer.Resolve<ILogService>("logService", true) == null)
            {
                ServiceContainer.Register<ILogService>("logService", new ConsoleLogService());
            }

            var preferencesStorage = new PreferencesStorageService(AppGroupId);
            var appGroupContainer = new NSFileManager().GetContainerUrl(AppGroupId);
            var liteDbStorage = new LiteDbStorageService(
                Path.Combine(appGroupContainer.Path, "Library", "bitwarden.db"));
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
            var biometricService = new BiometricService(mobileStorageService);
            var cryptoFunctionService = new PclCryptoFunctionService(cryptoPrimitiveService);
            var cryptoService = new CryptoService(mobileStorageService, secureStorageService, cryptoFunctionService);
            var passwordRepromptService = new MobilePasswordRepromptService(platformUtilsService, cryptoService);

            ServiceContainer.Register<IBroadcasterService>("broadcasterService", broadcasterService);
            ServiceContainer.Register<IMessagingService>("messagingService", messagingService);
            ServiceContainer.Register<ILocalizeService>("localizeService", localizeService);
            ServiceContainer.Register<II18nService>("i18nService", i18nService);
            ServiceContainer.Register<ICryptoPrimitiveService>("cryptoPrimitiveService", cryptoPrimitiveService);
            ServiceContainer.Register<IStorageService>("storageService", mobileStorageService);
            ServiceContainer.Register<IStorageService>("secureStorageService", secureStorageService);
            ServiceContainer.Register<IDeviceActionService>("deviceActionService", deviceActionService);
            ServiceContainer.Register<IPlatformUtilsService>("platformUtilsService", platformUtilsService);
            ServiceContainer.Register<IBiometricService>("biometricService", biometricService);
            ServiceContainer.Register<ICryptoFunctionService>("cryptoFunctionService", cryptoFunctionService);
            ServiceContainer.Register<ICryptoService>("cryptoService", cryptoService);
            ServiceContainer.Register<IPasswordRepromptService>("passwordRepromptService", passwordRepromptService);
        }

        public static void Bootstrap(Func<Task> postBootstrapFunc = null)
        {
            (ServiceContainer.Resolve<II18nService>("i18nService") as MobileI18nService).Init();
            ServiceContainer.Resolve<IAuthService>("authService").Init();
            (ServiceContainer.
                Resolve<IPlatformUtilsService>("platformUtilsService") as MobilePlatformUtilsService).Init();
            // Note: This is not awaited
            var bootstrapTask = BootstrapAsync(postBootstrapFunc);
        }

        public static void AppearanceAdjustments(IDeviceActionService deviceActionService)
        {
            ThemeHelpers.SetAppearance(ThemeManager.GetTheme(false), deviceActionService.UsingDarkTheme());
            UIApplication.SharedApplication.StatusBarHidden = false;
            UIApplication.SharedApplication.StatusBarStyle = UIStatusBarStyle.LightContent;
        }

        public static void SubscribeBroadcastReceiver(UIViewController controller, NFCNdefReaderSession nfcSession,
            NFCReaderDelegate nfcDelegate)
        {
            var broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            var messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            var deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            broadcasterService.Subscribe(nameof(controller), (message) =>
            {
                if (message.Command == "showDialog")
                {
                    var details = message.Data as DialogDetails;
                    var confirmText = string.IsNullOrWhiteSpace(details.ConfirmText) ?
                        AppResources.Ok : details.ConfirmText;

                    NSRunLoop.Main.BeginInvokeOnMainThread(async () =>
                    {
                        var result = await deviceActionService.DisplayAlertAsync(details.Title, details.Text,
                           details.CancelText, details.ConfirmText);
                        var confirmed = result == details.ConfirmText;
                        messagingService.Send("showDialogResolve", new Tuple<int, bool>(details.DialogId, confirmed));
                    });
                }
                else if (message.Command == "listenYubiKeyOTP")
                {
                    ListenYubiKey((bool)message.Data, deviceActionService, nfcSession, nfcDelegate);
                }
            });
        }

        public static void ListenYubiKey(bool listen, IDeviceActionService deviceActionService,
            NFCNdefReaderSession nfcSession, NFCReaderDelegate nfcDelegate)
        {
            if (deviceActionService.SupportsNfc())
            {
                nfcSession?.InvalidateSession();
                nfcSession?.Dispose();
                nfcSession = null;
                if (listen)
                {
                    nfcSession = new NFCNdefReaderSession(nfcDelegate, null, true)
                    {
                        AlertMessage = AppResources.HoldYubikeyNearTop
                    };
                    nfcSession.BeginSession();
                }
            }
        }

        private static async Task BootstrapAsync(Func<Task> postBootstrapFunc = null)
        {
            var disableFavicon = await ServiceContainer.Resolve<IStorageService>("storageService").GetAsync<bool?>(
                Bit.Core.Constants.DisableFaviconKey);
            await ServiceContainer.Resolve<IStateService>("stateService").SaveAsync(
                Bit.Core.Constants.DisableFaviconKey, disableFavicon);
            await ServiceContainer.Resolve<IEnvironmentService>("environmentService").SetUrlsFromStorageAsync();
            if (postBootstrapFunc != null)
            {
                await postBootstrapFunc.Invoke();
            }
        }
    }
}
