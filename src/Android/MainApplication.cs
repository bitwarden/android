using System;
using System.IO;
using System.Threading.Tasks;
using Android.App;
using Android.Content;
using Android.OS;
using Android.Runtime;
using Bit.App.Abstractions;
using Bit.App.Services;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.Droid.Services;
using Plugin.CurrentActivity;
using Plugin.Fingerprint;
using Xamarin.Android.Net;
using System.Net.Http;
using System.Net;
using Bit.App.Utilities;
using Bit.App.Pages;
using Bit.App.Utilities.AccountManagement;
using Bit.App.Utilities.Helpers;
using Bit.App.Controls;
#if !FDROID
using Android.Gms.Security;
#endif

namespace Bit.Droid
{
#if DEBUG
    [Application(Debuggable = true)]
#else
    [Application(Debuggable = false)]
#endif
    [Register("com.x8bit.bitwarden.MainApplication")]
#if FDROID
    public class MainApplication : Application
#else
    public class MainApplication : Application, ProviderInstaller.IProviderInstallListener
#endif
    {
        public MainApplication(IntPtr handle, JniHandleOwnership transer)
          : base(handle, transer)
        {
            if (ServiceContainer.RegisteredServices.Count == 0)
            {
                RegisterLocalServices();
                var deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
                ServiceContainer.Init(deviceActionService.DeviceUserAgent, Constants.ClearCiphersCacheKey,
                    Constants.AndroidAllClearCipherCacheKeys);
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
                    ServiceContainer.Resolve<IKeyConnectorService>("keyConnectorService"),
                    ServiceContainer.Resolve<IPasswordRepromptService>("passwordRepromptService"),
                    ServiceContainer.Resolve<ICryptoService>("cryptoService"));
                ServiceContainer.Register<IVerificationActionsFlowHelper>("verificationActionsFlowHelper", verificationActionsFlowHelper);

                var accountsManager = new AccountsManager(
                    ServiceContainer.Resolve<IBroadcasterService>("broadcasterService"),
                    ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService"),
                    ServiceContainer.Resolve<IStorageService>("secureStorageService"),
                    ServiceContainer.Resolve<IStateService>("stateService"),
                    ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService"),
                    ServiceContainer.Resolve<IAuthService>("authService"),
                    ServiceContainer.Resolve<ILogger>("logger"));
                ServiceContainer.Register<IAccountsManager>("accountsManager", accountsManager);

                var cipherHelper = new CipherHelper(
                    ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService"),
                    ServiceContainer.Resolve<IEventService>("eventService"),
                    ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService"),
                    ServiceContainer.Resolve<IClipboardService>("clipboardService"),
                    ServiceContainer.Resolve<IPasswordRepromptService>("passwordRepromptService")
                );
                ServiceContainer.Register<ICipherHelper>("cipherHelper", cipherHelper);
            }
#if !FDROID
            if (Build.VERSION.SdkInt <= BuildVersionCodes.Kitkat)
            {
                ProviderInstaller.InstallIfNeededAsync(ApplicationContext, this);
            }
#endif
        }

        public override void OnCreate()
        {
            base.OnCreate();
            Bootstrap();
            CrossCurrentActivity.Current.Init(this);
        }

        public void OnProviderInstallFailed(int errorCode, Intent recoveryIntent)
        {
        }

        public void OnProviderInstalled()
        {
        }

        private void RegisterLocalServices()
        {
            ServiceContainer.Register<INativeLogService>("nativeLogService", new AndroidLogService());
#if FDROID
            var logger = new StubLogger();
#elif DEBUG
            var logger = DebugLogger.Instance;
#else
            var logger = Logger.Instance;
#endif
            ServiceContainer.Register("logger", logger);

            // Note: This might cause a race condition. Investigate more.
            Task.Run(() =>
            {
                FFImageLoading.Forms.Platform.CachedImageRenderer.Init(true);
                FFImageLoading.ImageService.Instance.Initialize(new FFImageLoading.Config.Configuration
                {
                    FadeAnimationEnabled = false,
                    FadeAnimationForCachedImages = false,
                    HttpClient = new HttpClient(new AndroidClientHandler() { AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate })
                });
                ZXing.Net.Mobile.Forms.Android.Platform.Init();
            });
            CrossFingerprint.SetCurrentActivityResolver(() => CrossCurrentActivity.Current.Activity);

            var preferencesStorage = new PreferencesStorageService(null);
            var documentsPath = System.Environment.GetFolderPath(System.Environment.SpecialFolder.Personal);
            var liteDbStorage = new LiteDbStorageService(Path.Combine(documentsPath, "bitwarden.db"));
            var localizeService = new LocalizeService();
            var broadcasterService = new BroadcasterService(logger);
            var messagingService = new MobileBroadcasterMessagingService(broadcasterService);
            var i18nService = new MobileI18nService(localizeService.GetCurrentCultureInfo());
            var secureStorageService = new SecureStorageService();
            var cryptoPrimitiveService = new CryptoPrimitiveService();
            var mobileStorageService = new MobileStorageService(preferencesStorage, liteDbStorage);
            var stateService = new StateService(mobileStorageService, secureStorageService, messagingService);
            var stateMigrationService =
                new StateMigrationService(liteDbStorage, preferencesStorage, secureStorageService);
            var clipboardService = new ClipboardService(stateService);
            var deviceActionService = new DeviceActionService(clipboardService, stateService, messagingService,
                broadcasterService, () => ServiceContainer.Resolve<IEventService>("eventService"));
            var platformUtilsService = new MobilePlatformUtilsService(deviceActionService, clipboardService,
                messagingService, broadcasterService);
            var biometricService = new BiometricService();
            var cryptoFunctionService = new PclCryptoFunctionService(cryptoPrimitiveService);
            var cryptoService = new CryptoService(stateService, cryptoFunctionService);
            var passwordRepromptService = new MobilePasswordRepromptService(platformUtilsService, cryptoService);

            ServiceContainer.Register<IBroadcasterService>("broadcasterService", broadcasterService);
            ServiceContainer.Register<IMessagingService>("messagingService", messagingService);
            ServiceContainer.Register<ILocalizeService>("localizeService", localizeService);
            ServiceContainer.Register<II18nService>("i18nService", i18nService);
            ServiceContainer.Register<ICryptoPrimitiveService>("cryptoPrimitiveService", cryptoPrimitiveService);
            ServiceContainer.Register<IStorageService>("storageService", mobileStorageService);
            ServiceContainer.Register<IStorageService>("secureStorageService", secureStorageService);
            ServiceContainer.Register<IStateService>("stateService", stateService);
            ServiceContainer.Register<IStateMigrationService>("stateMigrationService", stateMigrationService);
            ServiceContainer.Register<IClipboardService>("clipboardService", clipboardService);
            ServiceContainer.Register<IDeviceActionService>("deviceActionService", deviceActionService);
            ServiceContainer.Register<IPlatformUtilsService>("platformUtilsService", platformUtilsService);
            ServiceContainer.Register<IBiometricService>("biometricService", biometricService);
            ServiceContainer.Register<ICryptoFunctionService>("cryptoFunctionService", cryptoFunctionService);
            ServiceContainer.Register<ICryptoService>("cryptoService", cryptoService);
            ServiceContainer.Register<IPasswordRepromptService>("passwordRepromptService", passwordRepromptService);
            ServiceContainer.Register<IAvatarImageSourcePool>("avatarImageSourcePool", new AvatarImageSourcePool());

            // Push
#if FDROID
            ServiceContainer.Register<IPushNotificationListenerService>(
                "pushNotificationListenerService", new NoopPushNotificationListenerService());
            ServiceContainer.Register<IPushNotificationService>(
                "pushNotificationService", new NoopPushNotificationService());
#else
            var notificationListenerService = new PushNotificationListenerService();
            ServiceContainer.Register<IPushNotificationListenerService>(
                "pushNotificationListenerService", notificationListenerService);
            var androidPushNotificationService = new AndroidPushNotificationService(
                stateService, notificationListenerService);
            ServiceContainer.Register<IPushNotificationService>(
                "pushNotificationService", androidPushNotificationService);
#endif
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
            await ServiceContainer.Resolve<IEnvironmentService>("environmentService").SetUrlsFromStorageAsync();
        }

        private void InitializeAppSetup()
        {
            var appSetup = new AppSetup();
            appSetup.InitializeServicesLastChance();
            ServiceContainer.Register<IAppSetup>("appSetup", appSetup);
        }
    }
}
