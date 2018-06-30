using System;
using XLabs.Ioc;
using Foundation;
using UIKit;
using Bit.App.Abstractions;
using Bit.App.Services;
using Bit.iOS.Services;
using Plugin.Connectivity;
using Bit.App.Repositories;
using Plugin.Fingerprint;
using Plugin.Settings.Abstractions;
using System.Diagnostics;
using Xamarin.Forms;
using Bit.iOS.Core.Services;
using Plugin.Connectivity.Abstractions;
using Bit.App.Pages;
using HockeyApp.iOS;
using Bit.iOS.Core;
using Google.Analytics;
using SimpleInjector;
using XLabs.Ioc.SimpleInjectorContainer;
using CoreNFC;
using Bit.App.Resources;

namespace Bit.iOS
{
    [Register("AppDelegate")]
    public partial class AppDelegate : global::Xamarin.Forms.Platform.iOS.FormsApplicationDelegate
    {
        private GaiCompletionHandler _dispatchHandler = null;
        private NFCNdefReaderSession _nfcSession = null;
        private ILockService _lockService;
        private IDeviceInfoService _deviceInfoService;
        private iOSPushNotificationHandler _pushHandler = null;
        private NFCReaderDelegate _nfcDelegate = null;

        public ISettings Settings { get; set; }

        public override bool FinishedLaunching(UIApplication app, NSDictionary options)
        {
            Forms.Init();

            if(!Resolver.IsSet)
            {
                SetIoc();
            }

            _lockService = Resolver.Resolve<ILockService>();
            _deviceInfoService = Resolver.Resolve<IDeviceInfoService>();
            _pushHandler = new iOSPushNotificationHandler(Resolver.Resolve<IPushNotificationListener>());
            _nfcDelegate = new NFCReaderDelegate((success, message) => ProcessYubikey(success, message));
            var appIdService = Resolver.Resolve<IAppIdService>();

            var crashManagerDelegate = new HockeyAppCrashManagerDelegate(
                appIdService, Resolver.Resolve<IAuthService>());
            var manager = BITHockeyManager.SharedHockeyManager;
            manager.Configure("51f96ae568ba45f699a18ad9f63046c3", crashManagerDelegate);
            manager.CrashManager.CrashManagerStatus = BITCrashManagerStatus.AutoSend;
            manager.UserId = appIdService.AppId;
            manager.StartManager();
            manager.Authenticator.AuthenticateInstallation();
            manager.DisableMetricsManager = manager.DisableFeedbackManager = manager.DisableUpdateManager = true;

            LoadApplication(new App.App(
                null,
                Resolver.Resolve<IAuthService>(),
                Resolver.Resolve<IConnectivity>(),
                Resolver.Resolve<IDatabaseService>(),
                Resolver.Resolve<ISyncService>(),
                Resolver.Resolve<ISettings>(),
                _lockService,
                Resolver.Resolve<ILocalizeService>(),
                Resolver.Resolve<IAppInfoService>(),
                Resolver.Resolve<IAppSettingsService>(),
                Resolver.Resolve<IDeviceActionService>()));

            // Appearance stuff

            var primaryColor = new UIColor(red: 0.24f, green: 0.55f, blue: 0.74f, alpha: 1.0f);
            var grayLight = new UIColor(red: 0.47f, green: 0.47f, blue: 0.47f, alpha: 1.0f);

            UINavigationBar.Appearance.ShadowImage = new UIImage();
            UINavigationBar.Appearance.SetBackgroundImage(new UIImage(), UIBarMetrics.Default);
            UIBarButtonItem.AppearanceWhenContainedIn(new Type[] { typeof(UISearchBar) }).TintColor = primaryColor;
            UIButton.AppearanceWhenContainedIn(new Type[] { typeof(UISearchBar) }).SetTitleColor(primaryColor,
                UIControlState.Normal);
            UIButton.AppearanceWhenContainedIn(new Type[] { typeof(UISearchBar) }).TintColor = primaryColor;
            UIStepper.Appearance.TintColor = grayLight;
            UISlider.Appearance.TintColor = primaryColor;

            MessagingCenter.Subscribe<Xamarin.Forms.Application, ToolsExtensionPage>(
                Xamarin.Forms.Application.Current, "ShowAppExtension", (sender, page) =>
            {
                var itemProvider = new NSItemProvider(new NSDictionary(), Core.Constants.UTTypeAppExtensionSetup);
                var extensionItem = new NSExtensionItem();
                extensionItem.Attachments = new NSItemProvider[] { itemProvider };
                var activityViewController = new UIActivityViewController(new NSExtensionItem[] { extensionItem }, null);
                activityViewController.CompletionHandler = (activityType, completed) =>
                {
                    page.EnabledExtension(completed && activityType == "com.8bit.bitwarden.find-login-action-extension");
                };

                var modal = UIApplication.SharedApplication.KeyWindow.RootViewController.ModalViewController;
                if(activityViewController.PopoverPresentationController != null)
                {
                    activityViewController.PopoverPresentationController.SourceView = modal.View;
                    var frame = UIScreen.MainScreen.Bounds;
                    frame.Height /= 2;
                    activityViewController.PopoverPresentationController.SourceRect = frame;
                }

                modal.PresentViewController(activityViewController, true, null);
            });

            MessagingCenter.Subscribe<Xamarin.Forms.Application, bool>(Xamarin.Forms.Application.Current,
                    "ListenYubiKeyOTP", (sender, listen) =>
                {
                    if(_deviceInfoService.NfcEnabled)
                    {
                        _nfcSession?.InvalidateSession();
                        _nfcSession?.Dispose();
                        _nfcSession = null;
                        if(listen)
                        {
                            _nfcSession = new NFCNdefReaderSession(_nfcDelegate, null, true);
                            _nfcSession.AlertMessage = AppResources.HoldYubikeyNearTop;
                            _nfcSession.BeginSession();
                        }
                    }
                });

            UIApplication.SharedApplication.StatusBarHidden = false;
            UIApplication.SharedApplication.StatusBarStyle = UIStatusBarStyle.LightContent;

            MessagingCenter.Subscribe<Xamarin.Forms.Application, bool>(Xamarin.Forms.Application.Current,
                "ShowStatusBar", (sender, show) =>
            {
                UIApplication.SharedApplication.SetStatusBarHidden(!show, false);
            });

            ZXing.Net.Mobile.Forms.iOS.Platform.Init();
            return base.FinishedLaunching(app, options);
        }

        public override void DidEnterBackground(UIApplication uiApplication)
        {
            var view = new UIView(UIApplication.SharedApplication.KeyWindow.Frame)
            {
                Tag = 4321
            };

            var backgroundView = new UIView(UIApplication.SharedApplication.KeyWindow.Frame)
            {
                BackgroundColor = new UIColor(red: 0.93f, green: 0.94f, blue: 0.96f, alpha: 1.0f)
            };

            var imageView = new UIImageView(new UIImage("logo.png"))
            {
                Center = new CoreGraphics.CGPoint(view.Center.X, view.Center.Y - 30)
            };

            view.AddSubview(backgroundView);
            view.AddSubview(imageView);

            UIApplication.SharedApplication.KeyWindow.AddSubview(view);
            UIApplication.SharedApplication.KeyWindow.BringSubviewToFront(view);
            UIApplication.SharedApplication.KeyWindow.EndEditing(true);
            UIApplication.SharedApplication.SetStatusBarHidden(true, false);

            // Log the date/time we last backgrounded
            _lockService.UpdateLastActivity();

            // Dispatch Google Analytics
            SendGoogleAnalyticsHitsInBackground();

            base.DidEnterBackground(uiApplication);
            Debug.WriteLine("DidEnterBackground");
        }

        public override void OnResignActivation(UIApplication uiApplication)
        {
            base.OnResignActivation(uiApplication);
            Debug.WriteLine("OnResignActivation");
        }

        public override void WillTerminate(UIApplication uiApplication)
        {
            base.WillTerminate(uiApplication);
            Debug.WriteLine("WillTerminate");
        }

        public override void OnActivated(UIApplication uiApplication)
        {
            base.OnActivated(uiApplication);
            Debug.WriteLine("OnActivated");

            UIApplication.SharedApplication.ApplicationIconBadgeNumber = 0;

            var view = UIApplication.SharedApplication.KeyWindow.ViewWithTag(4321);
            if(view != null)
            {
                view.RemoveFromSuperview();
                UIApplication.SharedApplication.SetStatusBarHidden(false, false);
            }
        }

        public override void WillEnterForeground(UIApplication uiApplication)
        {
            SendResumedMessage();

            // Restores the dispatch interval because dispatchWithCompletionHandler
            // has disabled automatic dispatching.
            Gai.SharedInstance.DispatchInterval = 10;

            base.WillEnterForeground(uiApplication);
            Debug.WriteLine("WillEnterForeground");
        }

        public override bool OpenUrl(UIApplication application, NSUrl url, string sourceApplication,
            NSObject annotation)
        {
            return true;
        }

        public override void FailedToRegisterForRemoteNotifications(UIApplication application, NSError error)
        {
            _pushHandler?.OnErrorReceived(error);
        }

        public override void RegisteredForRemoteNotifications(UIApplication application, NSData deviceToken)
        {
            _pushHandler?.OnRegisteredSuccess(deviceToken);
        }

        public override void DidRegisterUserNotificationSettings(UIApplication application,
            UIUserNotificationSettings notificationSettings)
        {
            application.RegisterForRemoteNotifications();
        }

        public override void DidReceiveRemoteNotification(UIApplication application, NSDictionary userInfo,
            Action<UIBackgroundFetchResult> completionHandler)
        {
            _pushHandler?.OnMessageReceived(userInfo);
        }

        public override void ReceivedRemoteNotification(UIApplication application, NSDictionary userInfo)
        {
            _pushHandler?.OnMessageReceived(userInfo);
        }

        private void SendResumedMessage()
        {
            MessagingCenter.Send(Xamarin.Forms.Application.Current, "Resumed", false);
        }

        private void SetIoc()
        {
            var container = new Container();

            // Services
            container.RegisterSingleton<IDatabaseService, DatabaseService>();
            container.RegisterSingleton<ISqlService, SqlService>();
            container.RegisterSingleton<ISecureStorageService, KeyChainStorageService>();
            container.RegisterSingleton<ICryptoService, CryptoService>();
            container.RegisterSingleton<IKeyDerivationService, CommonCryptoKeyDerivationService>();
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
            container.RegisterInstance(CrossConnectivity.Current);
            container.RegisterInstance(CrossFingerprint.Current);

            Settings = new Settings("group.com.8bit.bitwarden");
            container.RegisterInstance(Settings);

            // Push
            container.RegisterSingleton<IPushNotificationListener, PushNotificationListener>();
            container.RegisterSingleton<IPushNotificationService, iOSPushNotificationService>();

            FFImageLoading.Forms.Platform.CachedImageRenderer.Init();
            Resolver.SetResolver(new SimpleInjectorResolver(container));
        }

        /// <summary>
        /// This method sends any queued hits when the app enters the background.
        /// ref: https://developers.google.com/analytics/devguides/collection/ios/v3/dispatch
        /// </summary>
        private void SendGoogleAnalyticsHitsInBackground()
        {
            var taskExpired = false;
            var taskId = UIApplication.SharedApplication.BeginBackgroundTask(() =>
            {
                taskExpired = true;
            });

            if(taskId == UIApplication.BackgroundTaskInvalid)
            {
                return;
            }

            _dispatchHandler = (result) =>
            {
                // Send hits until no hits are left, a dispatch error occurs, or the background task expires.
                if(_dispatchHandler != null && result == DispatchResult.Good && !taskExpired)
                {
                    Gai.SharedInstance.Dispatch(_dispatchHandler);
                }
                else
                {
                    UIApplication.SharedApplication.EndBackgroundTask(taskId);
                }
            };

            Gai.SharedInstance.Dispatch(_dispatchHandler);
        }

        private void ProcessYubikey(bool success, string message)
        {
            if(success)
            {
                Device.BeginInvokeOnMainThread(() =>
                {
                    MessagingCenter.Send(Xamarin.Forms.Application.Current, "GotYubiKeyOTP", message);
                });
            }
        }
    }
}
