using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Repositories;
using Bit.App.Services;
using Bit.UWP.Services;
using FFImageLoading.Forms.WinUWP;
using Plugin.Connectivity;
using Plugin.Fingerprint;
using Plugin.Settings.Abstractions;
using SimpleInjector;
using System;
using System.Collections.Generic;
using System.Reflection;
using Windows.ApplicationModel;
using Windows.ApplicationModel.Activation;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Navigation;
using XLabs.Ioc;
using XLabs.Ioc.SimpleInjectorContainer;
using FFImageLoading.Forms;

namespace Bit.UWP
{
    sealed partial class App : Application
    {
        public App()
        {
            InitializeComponent();
            Suspending += OnSuspending;
            if(!Resolver.IsSet)
            {
                SetIoc();
            }
        }

        public ISettings Settings { get; set; }

        protected override void OnLaunched(LaunchActivatedEventArgs e)
        {
            ZXing.Net.Mobile.Forms.WindowsUniversal.ZXingScannerViewRenderer.Init();

            var rootFrame = Window.Current.Content as Frame;
            if(rootFrame == null)
            {
                rootFrame = new Frame();
                rootFrame.NavigationFailed += OnNavigationFailed;

                var assembliesToInclude = new List<Assembly>()
                {
                    typeof(CachedImage).GetTypeInfo().Assembly,
                    typeof(CachedImageRenderer).GetTypeInfo().Assembly
                };
                Xamarin.Forms.Forms.Init(e, assembliesToInclude);

                ((Style)Resources["TabbedPageStyle"]).Setters[0] = ((Style)Resources["TabbedPageStyle2"]).Setters[0];

                if(e.PreviousExecutionState == ApplicationExecutionState.Terminated)
                {
                    //TODO: Load state from previously suspended application
                }

                Window.Current.Content = rootFrame;
            }

            if(e.PrelaunchActivated == false)
            {
                if(rootFrame.Content == null)
                {
                    rootFrame.Navigate(typeof(MainPage), e.Arguments);
                }

                Window.Current.Activate();
            }
        }

        void OnNavigationFailed(object sender, NavigationFailedEventArgs e)
        {
            throw new Exception("Failed to load Page " + e.SourcePageType.FullName);
        }

        private void OnSuspending(object sender, SuspendingEventArgs e)
        {
            var deferral = e.SuspendingOperation.GetDeferral();
            //TODO: Save application state and stop any background activity
            deferral.Complete();
        }

        private void SetIoc()
        {
            var container = new Container();

            // Services
            container.RegisterSingleton<IDatabaseService, DatabaseService>();
            container.RegisterSingleton<ISqlService, SqlService>();
            container.RegisterSingleton<ISecureStorageService, SecureStorageService>();
            container.RegisterSingleton<ICryptoService, CryptoService>();
            container.RegisterSingleton<IKeyDerivationService, KeyDerivationService>();
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
            container.RegisterSingleton<IGoogleAnalyticsService, GoogleAnalyticsService>();
            container.RegisterSingleton<IDeviceInfoService, DeviceInfoService>();
            container.RegisterSingleton<ILocalizeService, LocalizeService>();
            container.RegisterSingleton<ILogService, LogService>();
            container.RegisterSingleton<IHttpService, HttpService>();
            container.RegisterSingleton<ITokenService, TokenService>();
            container.RegisterSingleton<ISettingsService, SettingsService>();
            container.RegisterSingleton<IAppSettingsService, AppSettingsService>();
            container.RegisterSingleton<IReflectionService, ReflectionService>();

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
            container.RegisterSingleton(CrossConnectivity.Current);
            container.RegisterSingleton(UserDialogs.Instance);
            container.RegisterSingleton(CrossFingerprint.Current);

            container.RegisterSingleton(Plugin.Settings.CrossSettings.Current);

            // Push
            var pushListener = new PushNotificationListener();
            container.RegisterSingleton<IPushNotificationListener>(pushListener);
            CrossPushNotification.Initialize(pushListener);
            container.RegisterSingleton(CrossPushNotification.Current);
            CachedImageRenderer.Init();

            Resolver.SetResolver(new SimpleInjectorResolver(container));
        }
    }
}
