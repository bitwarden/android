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
using Bit.Droid.Utilities;
using Plugin.CurrentActivity;
using Plugin.Fingerprint;
using Xamarin.Android.Net;
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
            ServiceContainer.Register<ILogService>("logService", new AndroidLogService());

            // Note: This might cause a race condition. Investigate more.
            Task.Run(() =>
            {
                FFImageLoading.Forms.Platform.CachedImageRenderer.Init(true);
                FFImageLoading.ImageService.Instance.Initialize(new FFImageLoading.Config.Configuration
                {
                    FadeAnimationEnabled = false,
                    FadeAnimationForCachedImages = false
                });
                ZXing.Net.Mobile.Forms.Android.Platform.Init();
            });
            CrossFingerprint.SetCurrentActivityResolver(() => CrossCurrentActivity.Current.Activity);

            var preferencesStorage = new PreferencesStorageService(null);
            var documentsPath = System.Environment.GetFolderPath(System.Environment.SpecialFolder.Personal);
            var liteDbStorage = new LiteDbStorageService(Path.Combine(documentsPath, "bitwarden.db"));
            var localizeService = new LocalizeService();
            var broadcasterService = new BroadcasterService();
            var messagingService = new MobileBroadcasterMessagingService(broadcasterService);
            var i18nService = new MobileI18nService(localizeService.GetCurrentCultureInfo());
            var secureStorageService = new SecureStorageService();
            var cryptoPrimitiveService = new CryptoPrimitiveService();
            var mobileStorageService = new MobileStorageService(preferencesStorage, liteDbStorage);
            var deviceActionService = new DeviceActionService(mobileStorageService, messagingService,
                broadcasterService, () => ServiceContainer.Resolve<IEventService>("eventService"));
            var platformUtilsService = new MobilePlatformUtilsService(deviceActionService, messagingService,
                broadcasterService);
            var biometricService = new BiometricService();
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
                mobileStorageService, notificationListenerService);
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
            var disableFavicon = await ServiceContainer.Resolve<IStorageService>("storageService")
                .GetAsync<bool?>(Constants.DisableFaviconKey);
            await ServiceContainer.Resolve<IStateService>("stateService").SaveAsync(
                Constants.DisableFaviconKey, disableFavicon);
            await ServiceContainer.Resolve<IEnvironmentService>("environmentService").SetUrlsFromStorageAsync();
        }
    }
}
