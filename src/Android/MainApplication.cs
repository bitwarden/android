using System;
using Acr.UserDialogs;
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
using PushNotification.Plugin;
using PushNotification.Plugin.Abstractions;
using XLabs.Ioc;
using System.Threading.Tasks;
using Plugin.Settings.Abstractions;
using FFImageLoading.Forms.Droid;
using XLabs.Ioc.SimpleInjectorContainer;
using SimpleInjector;

namespace Bit.Android
{
#if DEBUG
    [Application(Debuggable = true)]
#else
    [Application(Debuggable = false)]
#endif
    public class MainApplication : Application, Application.IActivityLifecycleCallbacks
    {
        private const string FirstLaunchKey = "firstLaunch";
        private const string LastVersionCodeKey = "lastVersionCode";

        public static Context AppContext;

        public MainApplication(IntPtr handle, JniHandleOwnership transer)
          : base(handle, transer)
        {
            //AndroidEnvironment.UnhandledExceptionRaiser += AndroidEnvironment_UnhandledExceptionRaiser;

            if(!Resolver.IsSet)
            {
                SetIoc(this);
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

            RegisterActivityLifecycleCallbacks(this);
            AppContext = ApplicationContext;
            StartPushService();
            HandlePushReregistration();
        }

        private void HandlePushReregistration()
        {
            var pushNotification = Resolver.Resolve<IPushNotification>();
            var settings = Resolver.Resolve<ISettings>();

            // Reregister for push token based on certain conditions
            // ref https://github.com/rdelrosario/xamarin-plugins/issues/65

            var reregister = false;

            // 1. First time starting the app after a new install
            if(settings.GetValueOrDefault(FirstLaunchKey, true))
            {
                settings.AddOrUpdateValue(FirstLaunchKey, false);
                reregister = true;
            }

            // 2. App version changed (installed update)
            var versionCode = Context.ApplicationContext.PackageManager.GetPackageInfo(Context.PackageName, 0).VersionCode;
            if(settings.GetValueOrDefault(LastVersionCodeKey, -1) != versionCode)
            {
                settings.AddOrUpdateValue(LastVersionCodeKey, versionCode);
                reregister = true;
            }

            // 3. In debug mode
            if(App.Utilities.Helpers.InDebugMode())
            {
                reregister = true;
            }

            // 4. Doesn't have a push token currently
            if(string.IsNullOrWhiteSpace(pushNotification.Token))
            {
                reregister = true;
            }

            if(reregister)
            {
                pushNotification.Unregister();
                if(Resolver.Resolve<IAuthService>().IsAuthenticated)
                {
                    pushNotification.Register();
                }
            }
        }

        public override void OnTerminate()
        {
            base.OnTerminate();
            UnregisterActivityLifecycleCallbacks(this);
        }

        public void OnActivityCreated(Activity activity, Bundle savedInstanceState)
        {
            CrossCurrentActivity.Current.Activity = activity;
        }

        public void OnActivityDestroyed(Activity activity)
        {
        }

        public void OnActivityPaused(Activity activity)
        {
        }

        public void OnActivityResumed(Activity activity)
        {
            CrossCurrentActivity.Current.Activity = activity;
        }

        public void OnActivitySaveInstanceState(Activity activity, Bundle outState)
        {
        }

        public void OnActivityStarted(Activity activity)
        {
            CrossCurrentActivity.Current.Activity = activity;
        }

        public void OnActivityStopped(Activity activity)
        {
        }

        public static void StartPushService()
        {
            AppContext.StartService(new Intent(AppContext, typeof(PushNotificationService)));
            if(Build.VERSION.SdkInt >= BuildVersionCodes.Kitkat)
            {
                PendingIntent pintent = PendingIntent.GetService(AppContext, 0, new Intent(AppContext,
                    typeof(PushNotificationService)), 0);
                AlarmManager alarm = (AlarmManager)AppContext.GetSystemService(AlarmService);
                alarm.Cancel(pintent);
            }
        }

        public static void StopPushService()
        {
            AppContext.StopService(new Intent(AppContext, typeof(PushNotificationService)));
            if(Build.VERSION.SdkInt >= BuildVersionCodes.Kitkat)
            {
                PendingIntent pintent = PendingIntent.GetService(AppContext, 0, new Intent(AppContext,
                    typeof(PushNotificationService)), 0);
                AlarmManager alarm = (AlarmManager)AppContext.GetSystemService(AlarmService);
                alarm.Cancel(pintent);
            }
        }

        public static void SetIoc(Application application)
        {
            UserDialogs.Init(application);
            CachedImageRenderer.Init();
            ZXing.Net.Mobile.Forms.Android.Platform.Init();
            CrossFingerprint.SetCurrentActivityResolver(() => CrossCurrentActivity.Current.Activity);

            //var container = new UnityContainer();
            var container = new Container();

            // Android Stuff
            container.RegisterSingleton(application.ApplicationContext);
            container.RegisterSingleton<Application>(application);

            // Services
            container.RegisterSingleton<IDatabaseService, DatabaseService>();
            container.RegisterSingleton<ISqlService, SqlService>();
            container.RegisterSingleton<ISecureStorageService, AndroidKeyStoreStorageService>();
            container.RegisterSingleton<ICryptoService, CryptoService>();
            container.RegisterSingleton<IKeyDerivationService, BouncyCastleKeyDerivationService>();
            container.RegisterSingleton<IAuthService, AuthService>();
            container.RegisterSingleton<IFolderService, FolderService>();
            container.RegisterSingleton<ILoginService, LoginService>();
            container.RegisterSingleton<ISyncService, SyncService>();
            container.RegisterSingleton<IDeviceActionService, DeviceActionService>();
            container.RegisterSingleton<IAppIdService, AppIdService>();
            container.RegisterSingleton<IPasswordGenerationService, PasswordGenerationService>();
            container.RegisterSingleton<IReflectionService, ReflectionService>();
            container.RegisterSingleton<ILockService, LockService>();
            container.RegisterSingleton<IAppInfoService, AppInfoService>();
            container.RegisterSingleton<IGoogleAnalyticsService, GoogleAnalyticsService>();
            container.RegisterSingleton<IDeviceInfoService, DeviceInfoService>();
            container.RegisterSingleton<ILocalizeService, LocalizeService>();
            container.RegisterSingleton<ILogService, LogService>();
            container.RegisterSingleton<IHttpService, HttpService>();
            container.RegisterSingleton<ITokenService, TokenService>();
            container.RegisterSingleton<ISettingsService, SettingsService>();
            container.RegisterSingleton<IMemoryService, MemoryService>();
            container.RegisterSingleton<IAppSettingsService, AppSettingsService>();

            // Repositories
            container.RegisterSingleton<IFolderRepository, FolderRepository>();
            container.RegisterSingleton<IFolderApiRepository, FolderApiRepository>();
            container.RegisterSingleton<ILoginRepository, LoginRepository>();
            container.RegisterSingleton<IAttachmentRepository, AttachmentRepository>();
            container.RegisterSingleton<ILoginApiRepository, LoginApiRepository>();
            container.RegisterSingleton<IConnectApiRepository, ConnectApiRepository>();
            container.RegisterSingleton<IDeviceApiRepository, DeviceApiRepository>();
            container.RegisterSingleton<IAccountsApiRepository, AccountsApiRepository>();
            container.RegisterSingleton<ICipherApiRepository, CipherApiRepository>();
            container.RegisterSingleton<ISettingsRepository, SettingsRepository>();
            container.RegisterSingleton<ISettingsApiRepository, SettingsApiRepository>();
            container.RegisterSingleton<ITwoFactorApiRepository, TwoFactorApiRepository>();

            // Other
            container.RegisterSingleton(CrossSettings.Current);
            container.RegisterSingleton(CrossConnectivity.Current);
            container.RegisterSingleton(UserDialogs.Instance);
            container.RegisterSingleton(CrossFingerprint.Current);

            // Push
            var pushListener = new PushNotificationListener();
            container.RegisterSingleton<IPushNotificationListener>(pushListener);
            CrossPushNotification.Initialize(pushListener, "962181367620");
            container.RegisterSingleton(CrossPushNotification.Current);

            container.Verify();
            Resolver.SetResolver(new SimpleInjectorResolver(container));
        }
    }
}