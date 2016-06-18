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
    [Application]
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
            Resolver.Resolve<IPushNotification>().Unregister();
            Resolver.Resolve<IPushNotification>().Register();
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

            if(global::Android.OS.Build.VERSION.SdkInt >= global::Android.OS.BuildVersionCodes.Kitkat)
            {
                PendingIntent pintent = PendingIntent.GetService(AppContext, 0, new Intent(AppContext, typeof(PushNotificationService)), 0);
                AlarmManager alarm = (AlarmManager)AppContext.GetSystemService(AlarmService);
                alarm.Cancel(pintent);
            }
        }

        public static void StopPushService()
        {
            AppContext.StopService(new Intent(AppContext, typeof(PushNotificationService)));
            if(global::Android.OS.Build.VERSION.SdkInt >= global::Android.OS.BuildVersionCodes.Kitkat)
            {
                PendingIntent pintent = PendingIntent.GetService(AppContext, 0, new Intent(AppContext, typeof(PushNotificationService)), 0);
                AlarmManager alarm = (AlarmManager)AppContext.GetSystemService(AlarmService);
                alarm.Cancel(pintent);
            }
        }

        private void SetIoc()
        {
            var container = new UnityContainer();

            container
                // Services
                .RegisterType<IDatabaseService, DatabaseService>(new ContainerControlledLifetimeManager())
                .RegisterType<ISqlService, SqlService>(new ContainerControlledLifetimeManager())
                .RegisterType<ISecureStorageService, KeyStoreStorageService>(new ContainerControlledLifetimeManager())
                .RegisterType<ICryptoService, CryptoService>(new ContainerControlledLifetimeManager())
                .RegisterType<IAuthService, AuthService>(new ContainerControlledLifetimeManager())
                .RegisterType<IFolderService, FolderService>(new ContainerControlledLifetimeManager())
                .RegisterType<ISiteService, SiteService>(new ContainerControlledLifetimeManager())
                .RegisterType<ISyncService, SyncService>(new ContainerControlledLifetimeManager())
                .RegisterType<IClipboardService, ClipboardService>(new ContainerControlledLifetimeManager())
                .RegisterType<IPushNotificationListener, PushNotificationListener>(new ContainerControlledLifetimeManager())
                // Repositories
                .RegisterType<IFolderRepository, FolderRepository>(new ContainerControlledLifetimeManager())
                .RegisterType<IFolderApiRepository, FolderApiRepository>(new ContainerControlledLifetimeManager())
                .RegisterType<ISiteRepository, SiteRepository>(new ContainerControlledLifetimeManager())
                .RegisterType<ISiteApiRepository, SiteApiRepository>(new ContainerControlledLifetimeManager())
                .RegisterType<IAuthApiRepository, AuthApiRepository>(new ContainerControlledLifetimeManager())
                // Other
                .RegisterInstance(CrossSettings.Current, new ContainerControlledLifetimeManager())
                .RegisterInstance(CrossConnectivity.Current, new ContainerControlledLifetimeManager())
                .RegisterInstance(UserDialogs.Instance, new ContainerControlledLifetimeManager())
                .RegisterInstance(CrossFingerprint.Current, new ContainerControlledLifetimeManager());

            CrossPushNotification.Initialize(container.Resolve<IPushNotificationListener>(), "SENDERID");
            container.RegisterInstance(CrossPushNotification.Current, new ContainerControlledLifetimeManager());

            Resolver.SetResolver(new UnityResolver(container));
            CrossFingerprint.SetCurrentActivityResolver(() => CrossCurrentActivity.Current.Activity);

            UserDialogs.Init(this);
        }
    }
}