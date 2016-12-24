using System;
using System.Collections.Generic;
using System.Linq;
using XLabs.Ioc;
using XLabs.Ioc.Unity;

using Foundation;
using UIKit;
using Bit.App.Abstractions;
using Bit.App.Services;
using Microsoft.Practices.Unity;
using Bit.iOS.Services;
using Plugin.Connectivity;
using Acr.UserDialogs;
using Bit.App.Repositories;
using Plugin.Fingerprint;
using Plugin.Fingerprint.Abstractions;
using Plugin.Settings.Abstractions;
using System.Diagnostics;
using Xamarin.Forms;
using Bit.iOS.Core.Services;
using PushNotification.Plugin;
using Plugin.Connectivity.Abstractions;
using Bit.App.Pages;
using HockeyApp.iOS;
using Bit.iOS.Core;
using Google.Analytics;

namespace Bit.iOS
{
    [Register("AppDelegate")]
    public partial class AppDelegate : global::Xamarin.Forms.Platform.iOS.FormsApplicationDelegate
    {
        private GaiCompletionHandler _dispatchHandler = null;

        public ISettings Settings { get; set; }

        public override bool FinishedLaunching(UIApplication app, NSDictionary options)
        {
            global::Xamarin.Forms.Forms.Init();

            if(!Resolver.IsSet)
            {
                SetIoc();
            }

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
                Resolver.Resolve<IAuthService>(),
                Resolver.Resolve<IConnectivity>(),
                Resolver.Resolve<IUserDialogs>(),
                Resolver.Resolve<IDatabaseService>(),
                Resolver.Resolve<ISyncService>(),
                Resolver.Resolve<IFingerprint>(),
                Resolver.Resolve<ISettings>(),
                Resolver.Resolve<ILockService>(),
                Resolver.Resolve<IGoogleAnalyticsService>(),
                Resolver.Resolve<ILocalizeService>()));

            // Appearance stuff

            var primaryColor = new UIColor(red: 0.24f, green: 0.55f, blue: 0.74f, alpha: 1.0f);
            var grayLight = new UIColor(red: 0.47f, green: 0.47f, blue: 0.47f, alpha: 1.0f);

            UINavigationBar.Appearance.ShadowImage = new UIImage();
            UINavigationBar.Appearance.SetBackgroundImage(new UIImage(), UIBarMetrics.Default);
            UIBarButtonItem.AppearanceWhenContainedIn(new Type[] { typeof(UISearchBar) }).TintColor = primaryColor;
            UIButton.AppearanceWhenContainedIn(new Type[] { typeof(UISearchBar) }).SetTitleColor(primaryColor, UIControlState.Normal);
            UIButton.AppearanceWhenContainedIn(new Type[] { typeof(UISearchBar) }).TintColor = primaryColor;
            UIStepper.Appearance.TintColor = grayLight;
            UISlider.Appearance.TintColor = primaryColor;

            MessagingCenter.Subscribe<Xamarin.Forms.Application, ToolsExtensionPage>(Xamarin.Forms.Application.Current, "ShowAppExtension", (sender, page) =>
            {
                var itemProvider = new NSItemProvider(new NSDictionary(), iOS.Core.Constants.UTTypeAppExtensionSetup);
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

            UIApplication.SharedApplication.StatusBarHidden = false;
            UIApplication.SharedApplication.StatusBarStyle = UIStatusBarStyle.LightContent;

            MessagingCenter.Subscribe<Xamarin.Forms.Application, bool>(Xamarin.Forms.Application.Current, "ShowStatusBar", (sender, show) =>
            {
                UIApplication.SharedApplication.SetStatusBarHidden(!show, false);
            });

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
            Settings.AddOrUpdateValue(App.Constants.LastActivityDate, DateTime.UtcNow);

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

        public override bool OpenUrl(UIApplication application, NSUrl url, string sourceApplication, NSObject annotation)
        {
            return true;
        }

        public override void FailedToRegisterForRemoteNotifications(UIApplication application, NSError error)
        {
            if(CrossPushNotification.Current is IPushNotificationHandler)
            {
                ((IPushNotificationHandler)CrossPushNotification.Current).OnErrorReceived(error);
            }
        }

        public override void RegisteredForRemoteNotifications(UIApplication application, NSData deviceToken)
        {
            if(CrossPushNotification.Current is IPushNotificationHandler)
            {
                ((IPushNotificationHandler)CrossPushNotification.Current).OnRegisteredSuccess(deviceToken);
            }
        }

        public override void DidRegisterUserNotificationSettings(UIApplication application, UIUserNotificationSettings notificationSettings)
        {
            application.RegisterForRemoteNotifications();
        }

        public override void DidReceiveRemoteNotification(UIApplication application, NSDictionary userInfo, Action<UIBackgroundFetchResult> completionHandler)
        {
            if(CrossPushNotification.Current is IPushNotificationHandler)
            {
                ((IPushNotificationHandler)CrossPushNotification.Current).OnMessageReceived(userInfo);
            }
        }

        public override void ReceivedRemoteNotification(UIApplication application, NSDictionary userInfo)
        {
            if(CrossPushNotification.Current is IPushNotificationHandler)
            {
                ((IPushNotificationHandler)CrossPushNotification.Current).OnMessageReceived(userInfo);
            }
        }

        private void SendResumedMessage()
        {
            MessagingCenter.Send(Xamarin.Forms.Application.Current, "Resumed", false);
        }

        private void SetIoc()
        {
            var container = new UnityContainer();

            container
                // Services
                .RegisterType<IDatabaseService, DatabaseService>(new ContainerControlledLifetimeManager())
                .RegisterType<ISqlService, SqlService>(new ContainerControlledLifetimeManager())
                .RegisterType<ISecureStorageService, KeyChainStorageService>(new ContainerControlledLifetimeManager())
                .RegisterType<ICryptoService, CryptoService>(new ContainerControlledLifetimeManager())
                .RegisterType<IKeyDerivationService, CommonCryptoKeyDerivationService>(new ContainerControlledLifetimeManager())
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
                .RegisterType<ILocalizeService, LocalizeService>(new ContainerControlledLifetimeManager())
                .RegisterType<ILogService, LogService>(new ContainerControlledLifetimeManager())
                .RegisterType<IHttpService, HttpService>(new ContainerControlledLifetimeManager())
                // Repositories
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
                .RegisterInstance(CrossConnectivity.Current, new ContainerControlledLifetimeManager())
                .RegisterInstance(UserDialogs.Instance, new ContainerControlledLifetimeManager())
                .RegisterInstance(CrossFingerprint.Current, new ContainerControlledLifetimeManager());

            Settings = new Settings("group.com.8bit.bitwarden");
            container.RegisterInstance(Settings, new ContainerControlledLifetimeManager());

            CrossPushNotification.Initialize(container.Resolve<IPushNotificationListener>());
            container.RegisterInstance(CrossPushNotification.Current, new ContainerControlledLifetimeManager());

            Resolver.SetResolver(new UnityResolver(container));
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
    }
}
