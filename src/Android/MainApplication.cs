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
using Microsoft.Practices.Unity;
using Plugin.Connectivity;
using Plugin.CurrentActivity;
using Plugin.Fingerprint;
using Plugin.Settings;
using PushNotification.Plugin;
using PushNotification.Plugin.Abstractions;
using XLabs.Ioc;
using XLabs.Ioc.Unity;

namespace Bit.Android
{
#if DEBUG
    [Application(Debuggable = true)]
#else
    [Application(Debuggable=false)]
#endif
    public class MainApplication : Application, Application.IActivityLifecycleCallbacks
    {
        public static Context AppContext;

        public MainApplication(IntPtr handle, JniHandleOwnership transer)
          : base(handle, transer)
        {
            if(!Resolver.IsSet)
            {
                SetIoc();
            }
        }

        public override void OnCreate()
        {
            base.OnCreate();
            RegisterActivityLifecycleCallbacks(this);
            AppContext = ApplicationContext;
            StartPushService();

            var pushNotification = Resolver.Resolve<IPushNotification>();
#if DEBUG
            // When running in debug mode you must unregister the previous instance first or else things wont work
            // ref https://github.com/rdelrosario/xamarin-plugins/issues/65
            pushNotification.Unregister();
#endif
            if(Resolver.Resolve<IAuthService>().IsAuthenticated)
            {
                pushNotification.Register();
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

        private void SetIoc()
        {
            UserDialogs.Init(this);

            var container = new UnityContainer();

            container
                // Android Stuff
                .RegisterInstance(ApplicationContext)
                .RegisterInstance<Application>(this)
                // Services
                .RegisterType<IDatabaseService, DatabaseService>(new ContainerControlledLifetimeManager())
                .RegisterType<ISqlService, SqlService>(new ContainerControlledLifetimeManager())
                .RegisterType<ISecureStorageService, KeyStoreStorageService>(new ContainerControlledLifetimeManager())
                .RegisterType<ICryptoService, CryptoService>(new ContainerControlledLifetimeManager())
                .RegisterType<IKeyDerivationService, BouncyCastleKeyDerivationService>(new ContainerControlledLifetimeManager())
                .RegisterType<IAuthService, AuthService>(new ContainerControlledLifetimeManager())
                .RegisterType<IFolderService, FolderService>(new ContainerControlledLifetimeManager())
                .RegisterType<ISiteService, SiteService>(new ContainerControlledLifetimeManager())
                .RegisterType<ISyncService, SyncService>(new ContainerControlledLifetimeManager())
                .RegisterType<IClipboardService, ClipboardService>(new ContainerControlledLifetimeManager())
                .RegisterType<IPushNotificationListener, PushNotificationListener>(new ContainerControlledLifetimeManager())
                .RegisterType<IAppIdService, AppIdService>(new ContainerControlledLifetimeManager())
                .RegisterType<IPasswordGenerationService, PasswordGenerationService>(new ContainerControlledLifetimeManager())
                .RegisterType<IReflectionService, ReflectionService>(new ContainerControlledLifetimeManager())
                .RegisterType<ILockService, LockService>(new ContainerControlledLifetimeManager())
                .RegisterType<IAppInfoService, AppInfoService>(new ContainerControlledLifetimeManager())
                .RegisterType<IGoogleAnalyticsService, GoogleAnalyticsService>(new ContainerControlledLifetimeManager())
                .RegisterType<IDeviceInfoService, DeviceInfoService>(new ContainerControlledLifetimeManager())
                // Repositories
                .RegisterType<IFolderRepository, FolderRepository>(new ContainerControlledLifetimeManager())
                .RegisterType<IFolderApiRepository, FolderApiRepository>(new ContainerControlledLifetimeManager())
                .RegisterType<ISiteRepository, SiteRepository>(new ContainerControlledLifetimeManager())
                .RegisterType<ISiteApiRepository, SiteApiRepository>(new ContainerControlledLifetimeManager())
                .RegisterType<IAuthApiRepository, AuthApiRepository>(new ContainerControlledLifetimeManager())
                .RegisterType<IDeviceApiRepository, DeviceApiRepository>(new ContainerControlledLifetimeManager())
                .RegisterType<IAccountsApiRepository, AccountsApiRepository>(new ContainerControlledLifetimeManager())
                .RegisterType<ICipherApiRepository, CipherApiRepository>(new ContainerControlledLifetimeManager())
                // Other
                .RegisterInstance(CrossSettings.Current, new ContainerControlledLifetimeManager())
                .RegisterInstance(CrossConnectivity.Current, new ContainerControlledLifetimeManager())
                .RegisterInstance(UserDialogs.Instance, new ContainerControlledLifetimeManager())
                .RegisterInstance(CrossFingerprint.Current, new ContainerControlledLifetimeManager());

            CrossPushNotification.Initialize(container.Resolve<IPushNotificationListener>(), "962181367620");
            container.RegisterInstance(CrossPushNotification.Current, new ContainerControlledLifetimeManager());

            Resolver.SetResolver(new UnityResolver(container));
            CrossFingerprint.SetCurrentActivityResolver(() => CrossCurrentActivity.Current.Activity);
        }
    }
}