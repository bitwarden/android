using System;
using Android.App;
using Android.Content;
using Android.OS;
using Android.Runtime;
using Bit.Android.Services;
using Bit.App.Abstractions;
using Bit.App.Repositories;
using Bit.App.Services;
using Plugin.Connectivity;
using Plugin.CurrentActivity;
using Plugin.Fingerprint;
using Plugin.Settings;
using XLabs.Ioc;
using System.Threading.Tasks;
using XLabs.Ioc.SimpleInjectorContainer;
using SimpleInjector;
using Android.Gms.Security;

namespace Bit.Android
{
#if DEBUG
    [Application(Debuggable = true)]
#else
    [Application(Debuggable = false)]
#endif
    public class MainApplication : Application, ProviderInstaller.IProviderInstallListener
    {
        private const string FirstLaunchKey = "firstLaunch";
        private const string LastVersionCodeKey = "lastVersionCode";

        public MainApplication(IntPtr handle, JniHandleOwnership transer)
          : base(handle, transer)
        {
            //AndroidEnvironment.UnhandledExceptionRaiser += AndroidEnvironment_UnhandledExceptionRaiser;

            if(!Resolver.IsSet)
            {
                SetIoc(this);
            }

            if(Build.VERSION.SdkInt <= BuildVersionCodes.Kitkat)
            {
                ProviderInstaller.InstallIfNeededAsync(ApplicationContext, this);
            }
        }

        private void AndroidEnvironment_UnhandledExceptionRaiser(object sender, RaiseThrowableEventArgs e)
        {
            var message = Utilities.AppendExceptionToMessage("", e.Exception);
            //Utilities.SaveCrashFile(message, true);
            Utilities.SendCrashEmail(message, false);
        }

        public override void OnCreate()
        {
            base.OnCreate();

            // workaround for app compat bug
            // ref https://forums.xamarin.com/discussion/62414/app-resuming-results-in-crash-with-formsappcompatactivity
            Task.Delay(10).Wait();
            CrossCurrentActivity.Current.Init(this);
        }

        public static void SetIoc(Application application)
        {
            Refractored.FabControl.Droid.FloatingActionButtonViewRenderer.Init();
            FFImageLoading.Forms.Platform.CachedImageRenderer.Init(true);
            ZXing.Net.Mobile.Forms.Android.Platform.Init();
            CrossFingerprint.SetCurrentActivityResolver(() => CrossCurrentActivity.Current.Activity);

            //var container = new UnityContainer();
            var container = new Container();

            // Android Stuff
            container.RegisterInstance(application.ApplicationContext);
            container.RegisterInstance<Application>(application);

            // Services
            container.RegisterSingleton<IDatabaseService, DatabaseService>();
            container.RegisterSingleton<ISqlService, SqlService>();
            container.RegisterSingleton<ISecureStorageService, AndroidKeyStoreStorageService>();
            container.RegisterSingleton<ICryptoService, CryptoService>();
            container.RegisterSingleton<IKeyDerivationService, BouncyCastleKeyDerivationService>();
            container.RegisterSingleton<IAuthService, AuthService>();
            container.RegisterSingleton<IFolderService, FolderService>();
            container.RegisterSingleton<ICollectionService, CollectionService>();
            container.RegisterSingleton<ICipherService, CipherService>();
            container.RegisterSingleton<ISyncService, SyncService>();
            container.RegisterSingleton<IDeviceActionService, DeviceActionService>();
            container.RegisterSingleton<IAppIdService, AppIdService>();
            container.RegisterSingleton<IPasswordGenerationService, PasswordGenerationService>();
            container.RegisterSingleton<ILockService, LockService>();
            container.RegisterSingleton<IAppInfoService, AppInfoService>();
#if FDROID
            container.RegisterSingleton<IGoogleAnalyticsService, NoopGoogleAnalyticsService>();
#else
            container.RegisterSingleton<IGoogleAnalyticsService, GoogleAnalyticsService>();
#endif
            container.RegisterSingleton<IDeviceInfoService, DeviceInfoService>();
            container.RegisterSingleton<ILocalizeService, LocalizeService>();
            container.RegisterSingleton<ILogService, LogService>();
            container.RegisterSingleton<IHttpService, HttpService>();
            container.RegisterSingleton<ITokenService, TokenService>();
            container.RegisterSingleton<ISettingsService, SettingsService>();
            container.RegisterSingleton<IAppSettingsService, AppSettingsService>();

            // Repositories
            container.RegisterSingleton<IFolderRepository, FolderRepository>();
            container.RegisterSingleton<IFolderApiRepository, FolderApiRepository>();
            container.RegisterSingleton<ICipherRepository, CipherRepository>();
            container.RegisterSingleton<IAttachmentRepository, AttachmentRepository>();
            container.RegisterSingleton<IConnectApiRepository, ConnectApiRepository>();
            container.RegisterSingleton<IDeviceApiRepository, DeviceApiRepository>();
            container.RegisterSingleton<IAccountsApiRepository, AccountsApiRepository>();
            container.RegisterSingleton<ICipherApiRepository, CipherApiRepository>();
            container.RegisterSingleton<ISettingsRepository, SettingsRepository>();
            container.RegisterSingleton<ISettingsApiRepository, SettingsApiRepository>();
            container.RegisterSingleton<ITwoFactorApiRepository, TwoFactorApiRepository>();
            container.RegisterSingleton<ISyncApiRepository, SyncApiRepository>();
            container.RegisterSingleton<ICollectionRepository, CollectionRepository>();
            container.RegisterSingleton<ICipherCollectionRepository, CipherCollectionRepository>();

            // Other
            container.RegisterInstance(CrossSettings.Current);
            container.RegisterInstance(CrossConnectivity.Current);
            container.RegisterInstance(CrossFingerprint.Current);

            // Push
#if FDROID
            container.RegisterSingleton<IPushNotificationListener, NoopPushNotificationListener>();
            container.RegisterSingleton<IPushNotificationService, NoopPushNotificationService>();
#else
            container.RegisterSingleton<IPushNotificationListener, PushNotificationListener>();
            container.RegisterSingleton<IPushNotificationService, AndroidPushNotificationService>();
#endif

            container.Verify();
            Resolver.SetResolver(new SimpleInjectorResolver(container));
        }

        public void OnProviderInstallFailed(int errorCode, Intent recoveryIntent)
        {
        }

        public void OnProviderInstalled()
        {
        }
    }
}