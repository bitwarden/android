using System;
using System.IO;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Models;
using Bit.App.Pages;
using Bit.App.Resources;
using Bit.App.Services;
using Bit.App.Utilities;
using Bit.App.Utilities.AccountManagement;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.iOS.Core.Services;
using CoreNFC;
using Foundation;
using UIKit;
using Xamarin.Forms;

namespace Bit.iOS.Core.Utilities
{
    public static class iOSCoreHelpers
    {
        public static string AppId = "com.8bit.bitwarden";
        public static string AppAutofillId = "com.8bit.bitwarden.autofill";
        public static string AppExtensionId = "com.8bit.bitwarden.find-login-action-extension";
        public static string AppGroupId = "group.com.8bit.bitwarden";
        public static string AccessGroup = "LTZ2PFU5D6.com.8bit.bitwarden";

        public static void InitApp<T>(T rootController,
            string clearCipherCacheKey,
            NFCNdefReaderSession nfcSession,
            out NFCReaderDelegate nfcDelegate,
            out IAccountsManager accountsManager)
            where T : UIViewController, IAccountsManagerHost
        {
            Forms.Init();

            if (ServiceContainer.RegisteredServices.Count > 0)
            {
                ServiceContainer.Reset();
            }
            RegisterLocalServices();
            var deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            var messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            ServiceContainer.Init(deviceActionService.DeviceUserAgent,
                                  clearCipherCacheKey,
                                  Bit.Core.Constants.iOSAllClearCipherCacheKeys);   
            InitLogger();

            RegisterFinallyBeforeBootstrap();

            Bootstrap();

            var appOptions = new AppOptions { IosExtension = true };
            var app = new App.App(appOptions);
            ThemeManager.SetTheme(app.Resources);

            AppearanceAdjustments();

            nfcDelegate = new Core.NFCReaderDelegate((success, message) =>
                messagingService.Send("gotYubiKeyOTP", message));
            SubscribeBroadcastReceiver(rootController, nfcSession, nfcDelegate);

            accountsManager = ServiceContainer.Resolve<IAccountsManager>("accountsManager");
            accountsManager.Init(() => appOptions, rootController);
        }

        public static void InitLogger()
        {
            ServiceContainer.Resolve<ILogger>("logger").InitAsync();
        }

        public static void RegisterLocalServices()
        {
            if (ServiceContainer.Resolve<INativeLogService>("nativeLogService", true) == null)
            {
                ServiceContainer.Register<INativeLogService>("nativeLogService", new ConsoleLogService());
            }

            ILogger logger = null;
            if (ServiceContainer.Resolve<ILogger>("logger", true) == null)
            {
#if DEBUG
                logger = DebugLogger.Instance;
#else
                logger = Logger.Instance;
#endif
                ServiceContainer.Register("logger", logger);
            }

            var preferencesStorage = new PreferencesStorageService(AppGroupId);
            var appGroupContainer = new NSFileManager().GetContainerUrl(AppGroupId);
            var liteDbStorage = new LiteDbStorageService(
                Path.Combine(appGroupContainer.Path, "Library", "bitwarden.db"));
            var localizeService = new LocalizeService();
            var broadcasterService = new BroadcasterService(logger);
            var messagingService = new MobileBroadcasterMessagingService(broadcasterService);
            var i18nService = new MobileI18nService(localizeService.GetCurrentCultureInfo());
            var secureStorageService = new KeyChainStorageService(AppId, AccessGroup,
                () => ServiceContainer.Resolve<IAppIdService>("appIdService").GetAppIdAsync());
            var cryptoPrimitiveService = new CryptoPrimitiveService();
            var mobileStorageService = new MobileStorageService(preferencesStorage, liteDbStorage);
            var storageMediatorService = new StorageMediatorService(mobileStorageService, secureStorageService, preferencesStorage);
            var stateService = new StateService(mobileStorageService, secureStorageService, storageMediatorService, messagingService);
            var stateMigrationService =
                new StateMigrationService(DeviceType.iOS, liteDbStorage, preferencesStorage, secureStorageService);
            var deviceActionService = new DeviceActionService();
            var fileService = new FileService(stateService, messagingService);
            var clipboardService = new ClipboardService(stateService);
            var platformUtilsService = new MobilePlatformUtilsService(deviceActionService, clipboardService,
                messagingService, broadcasterService);
            var cryptoFunctionService = new PclCryptoFunctionService(cryptoPrimitiveService);
            var cryptoService = new CryptoService(stateService, cryptoFunctionService);
            var biometricService = new BiometricService(stateService, cryptoService);
            var userPinService = new UserPinService(stateService, cryptoService);
            var passwordRepromptService = new MobilePasswordRepromptService(platformUtilsService, cryptoService, stateService);

            ServiceContainer.Register<ISynchronousStorageService>(preferencesStorage);
            ServiceContainer.Register<IBroadcasterService>("broadcasterService", broadcasterService);
            ServiceContainer.Register<IMessagingService>("messagingService", messagingService);
            ServiceContainer.Register<ILocalizeService>("localizeService", localizeService);
            ServiceContainer.Register<II18nService>("i18nService", i18nService);
            ServiceContainer.Register<ICryptoPrimitiveService>("cryptoPrimitiveService", cryptoPrimitiveService);
            ServiceContainer.Register<IStorageService>("storageService", mobileStorageService);
            ServiceContainer.Register<IStorageService>("secureStorageService", secureStorageService);
            ServiceContainer.Register<IStorageMediatorService>(storageMediatorService);
            ServiceContainer.Register<IStateService>("stateService", stateService);
            ServiceContainer.Register<IStateMigrationService>("stateMigrationService", stateMigrationService);
            ServiceContainer.Register<IDeviceActionService>("deviceActionService", deviceActionService);
            ServiceContainer.Register<IFileService>(fileService);
            ServiceContainer.Register<IAutofillHandler>(new AutofillHandler());            
            ServiceContainer.Register<IClipboardService>("clipboardService", clipboardService);
            ServiceContainer.Register<IPlatformUtilsService>("platformUtilsService", platformUtilsService);
            ServiceContainer.Register<IBiometricService>("biometricService", biometricService);
            ServiceContainer.Register<ICryptoFunctionService>("cryptoFunctionService", cryptoFunctionService);
            ServiceContainer.Register<ICryptoService>("cryptoService", cryptoService);
            ServiceContainer.Register<IPasswordRepromptService>("passwordRepromptService", passwordRepromptService);
            ServiceContainer.Register<IAvatarImageSourcePool>("avatarImageSourcePool", new AvatarImageSourcePool());
            ServiceContainer.Register<IUserPinService>(userPinService);
        }

        public static void RegisterFinallyBeforeBootstrap()
        {
            ServiceContainer.Register<IWatchDeviceService>(new WatchDeviceService(ServiceContainer.Resolve<ICipherService>(),
                ServiceContainer.Resolve<IEnvironmentService>(),
                ServiceContainer.Resolve<IStateService>(),
                ServiceContainer.Resolve<IVaultTimeoutService>(),
                ServiceContainer.Resolve<ILogger>()));
        }

        public static void Bootstrap(Func<Task> postBootstrapFunc = null)
        {
            var locale = ServiceContainer.Resolve<IStateService>().GetLocale();
            (ServiceContainer.Resolve<II18nService>("i18nService") as MobileI18nService)
                .Init(locale != null ? new System.Globalization.CultureInfo(locale) : null);
            ServiceContainer.Resolve<IAuthService>("authService").Init();
            (ServiceContainer.
                Resolve<IPlatformUtilsService>("platformUtilsService") as MobilePlatformUtilsService).Init();

            var accountsManager = new AccountsManager(
                ServiceContainer.Resolve<IBroadcasterService>("broadcasterService"),
                ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService"),
                ServiceContainer.Resolve<IStorageService>("secureStorageService"),
                ServiceContainer.Resolve<IStateService>("stateService"),
                ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService"),
                ServiceContainer.Resolve<IAuthService>("authService"),
                ServiceContainer.Resolve<ILogger>("logger"),
                ServiceContainer.Resolve<IMessagingService>("messagingService"),
                ServiceContainer.Resolve<IWatchDeviceService>(),
                ServiceContainer.Resolve<IConditionedAwaiterManager>());
            ServiceContainer.Register<IAccountsManager>("accountsManager", accountsManager);

            // Note: This is not awaited
            var bootstrapTask = BootstrapAsync(postBootstrapFunc);
        }

        public static void AppearanceAdjustments()
        {
            ThemeHelpers.SetAppearance(ThemeManager.GetTheme(), ThemeManager.OsDarkModeEnabled());
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
                           details.CancelText, confirmText);
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
            await ServiceContainer.Resolve<IEnvironmentService>("environmentService").SetUrlsFromStorageAsync();

            InitializeAppSetup();
            // TODO: Update when https://github.com/bitwarden/mobile/pull/1662 gets merged
            var deleteAccountActionFlowExecutioner = new DeleteAccountActionFlowExecutioner(
                ServiceContainer.Resolve<IApiService>("apiService"),
                ServiceContainer.Resolve<IMessagingService>("messagingService"),
                ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService"),
                ServiceContainer.Resolve<IDeviceActionService>("deviceActionService"),
                ServiceContainer.Resolve<ILogger>("logger"));
            ServiceContainer.Register<IDeleteAccountActionFlowExecutioner>("deleteAccountActionFlowExecutioner", deleteAccountActionFlowExecutioner);

            var verificationActionsFlowHelper = new VerificationActionsFlowHelper(
                ServiceContainer.Resolve<IPasswordRepromptService>("passwordRepromptService"),
                ServiceContainer.Resolve<ICryptoService>("cryptoService"),
                ServiceContainer.Resolve<IUserVerificationService>());
            ServiceContainer.Register<IVerificationActionsFlowHelper>("verificationActionsFlowHelper", verificationActionsFlowHelper);

            if (postBootstrapFunc != null)
            {
                await postBootstrapFunc.Invoke();
            }
        }

        private static void InitializeAppSetup()
        {
            var appSetup = new AppSetup();
            appSetup.InitializeServicesLastChance();
            ServiceContainer.Register<IAppSetup>("appSetup", appSetup);
        }
    }
}
