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
using Plugin.Fingerprint;
using Xamarin.Android.Net;
using System.Net.Http;
using System.Net;
using Bit.App.Utilities;
using Bit.App.Pages;
using Bit.App.Utilities.AccountManagement;
using Bit.App.Controls;
using Bit.App.Platforms.Android.Autofill;
using Bit.Core.Enums;
using Bit.Core.Services.UserVerification;

#if !FDROID
using Android.Gms.Security;
#endif
using DeviceType = Bit.Core.Enums.DeviceType;

namespace Bit.Droid
{
#if DEBUG
    [Application(Debuggable = true)]
#else
    [Application(Debuggable = false)]
#endif
    [Register("com.x8bit.bitwarden.MainApplication")]
#if FDROID
    public class MainApplication : MauiApplication
#else
    public class MainApplication : MauiApplication, ProviderInstaller.IProviderInstallListener
#endif
    {
        public MainApplication(IntPtr handle, JniHandleOwnership transer)
          : base(handle, transer)
        {
            if (ServiceContainer.RegisteredServices.Count == 0)
            {
                RegisterLocalServices();

                var deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
                ServiceContainer.Init(deviceActionService.DeviceUserAgent, Core.Constants.ClearCiphersCacheKey,
                    Core.Constants.AndroidAllClearCipherCacheKeys);

                ServiceContainer.Register<IWatchDeviceService>(new WatchDeviceService(ServiceContainer.Resolve<ICipherService>(),
                    ServiceContainer.Resolve<IEnvironmentService>(),
                    ServiceContainer.Resolve<IStateService>(),
                    ServiceContainer.Resolve<IVaultTimeoutService>()));

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

                var userPinService = new UserPinService(
                    ServiceContainer.Resolve<IStateService>(),
                    ServiceContainer.Resolve<ICryptoService>(),
                    ServiceContainer.Resolve<IVaultTimeoutService>());
                ServiceContainer.Register<IUserPinService>(userPinService);

                var userVerificationMediatorService = new UserVerificationMediatorService(
                    ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService"),
                    ServiceContainer.Resolve<IPasswordRepromptService>("passwordRepromptService"),
                    userPinService,
                    deviceActionService,
                    ServiceContainer.Resolve<IUserVerificationService>());
                ServiceContainer.Register<IUserVerificationMediatorService>(userVerificationMediatorService);

                var fido2AuthenticatorService = new Fido2AuthenticatorService(
                    ServiceContainer.Resolve<ICipherService>(),
                    ServiceContainer.Resolve<ISyncService>(),
                    ServiceContainer.Resolve<ICryptoFunctionService>(),
                    userVerificationMediatorService);
                ServiceContainer.Register<IFido2AuthenticatorService>(fido2AuthenticatorService);

                var fido2GetAssertionUserInterface = new Fido2GetAssertionUserInterface(
                    ServiceContainer.Resolve<IStateService>(),
                    ServiceContainer.Resolve<IVaultTimeoutService>(),
                    ServiceContainer.Resolve<ICipherService>(),
                    ServiceContainer.Resolve<IUserVerificationMediatorService>());
                ServiceContainer.Register<IFido2AndroidGetAssertionUserInterface>(fido2GetAssertionUserInterface);                

                var fido2MakeCredentialUserInterface = new Fido2MakeCredentialUserInterface(
                    ServiceContainer.Resolve<IStateService>(),
                    ServiceContainer.Resolve<IVaultTimeoutService>(),
                    ServiceContainer.Resolve<ICipherService>(),
                    ServiceContainer.Resolve<IUserVerificationMediatorService>(),
                    ServiceContainer.Resolve<IDeviceActionService>(),
                    ServiceContainer.Resolve<IPlatformUtilsService>());
                ServiceContainer.Register<IFido2MakeCredentialConfirmationUserInterface>(fido2MakeCredentialUserInterface);

                var fido2ClientService = new Fido2ClientService(
                    ServiceContainer.Resolve<IStateService>(),
                    ServiceContainer.Resolve<IEnvironmentService>(),
                    ServiceContainer.Resolve<ICryptoFunctionService>(),
                    ServiceContainer.Resolve<IFido2AuthenticatorService>(),
                    fido2GetAssertionUserInterface,
                    fido2MakeCredentialUserInterface);
                ServiceContainer.Register<IFido2ClientService>(fido2ClientService);

                ServiceContainer.Register<IFido2MediatorService>(new Fido2MediatorService(
                    fido2AuthenticatorService,
                    fido2ClientService,
                    ServiceContainer.Resolve<ICipherService>()));
            }
#if !FDROID
            if (Build.VERSION.SdkInt <= BuildVersionCodes.Kitkat)
            {
                ProviderInstaller.InstallIfNeededAsync(ApplicationContext, this);
            }
#endif
        }

        protected override MauiApp CreateMauiApp() => App.MauiProgram.CreateMauiApp();

        public override void OnCreate()
        {
            base.OnCreate();
            Bootstrap();
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
            //Task.Run(() =>
            //{
            //    FFImageLoading.Forms.Platform.CachedImageRenderer.Init(true);
            //    FFImageLoading.ImageService.Instance.Initialize(new FFImageLoading.Config.Configuration
            //    {
            //        FadeAnimationEnabled = false,
            //        FadeAnimationForCachedImages = false,
            //        HttpClient = new HttpClient(new AndroidClientHandler() { AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate })
            //    });
            //    ZXing.Net.Mobile.Forms.Android.Platform.Init();
            //});
            CrossFingerprint.SetCurrentActivityResolver(() => Microsoft.Maui.ApplicationModel.Platform.CurrentActivity);

            var preferencesStorage = new PreferencesStorageService(null);
            var localAppDataFolderPath = System.Environment.GetFolderPath(System.Environment.SpecialFolder.LocalApplicationData);
            var liteDbStorage = new LiteDbStorageService(Path.Combine(localAppDataFolderPath, "bitwarden.db"));
            var localizeService = new LocalizeService();
            var broadcasterService = new BroadcasterService(logger);
            var messagingService = new MobileBroadcasterMessagingService(broadcasterService);
            var i18nService = new MobileI18nService(localizeService.GetCurrentCultureInfo());
            var secureStorageService = new SecureStorageService();
            var cryptoPrimitiveService = new CryptoPrimitiveService();
            var mobileStorageService = new MobileStorageService(preferencesStorage, liteDbStorage);
            var storageMediatorService = new StorageMediatorService(mobileStorageService, secureStorageService, preferencesStorage);
            var stateService = new StateService(mobileStorageService, secureStorageService, storageMediatorService, messagingService);
            var stateMigrationService =
                new StateMigrationService(Bit.Core.Enums.DeviceType.Android, liteDbStorage, preferencesStorage, secureStorageService);
            var clipboardService = new ClipboardService(stateService);
            var deviceActionService = new DeviceActionService(stateService, messagingService);
            var fileService = new FileService(stateService, broadcasterService);
            var platformUtilsService = new MobilePlatformUtilsService(deviceActionService, clipboardService,
                messagingService, broadcasterService);
            var autofillHandler = new AutofillHandler(stateService, messagingService, clipboardService,
                platformUtilsService, new LazyResolve<IEventService>());
            var cryptoFunctionService = new PclCryptoFunctionService(cryptoPrimitiveService);
            var cryptoService = new CryptoService(stateService, cryptoFunctionService, logger);
            var biometricService = new BiometricService(stateService, cryptoService);
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
            ServiceContainer.Register<IClipboardService>("clipboardService", clipboardService);
            ServiceContainer.Register<IDeviceActionService>("deviceActionService", deviceActionService);
            ServiceContainer.Register<IFileService>(fileService);
            ServiceContainer.Register<IAutofillHandler>(autofillHandler);            
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
            var locale = ServiceContainer.Resolve<IStateService>().GetLocale();
            (ServiceContainer.Resolve<II18nService>("i18nService") as MobileI18nService)
                .Init(locale != null ? new System.Globalization.CultureInfo(locale) : null);
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
