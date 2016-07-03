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
using Bit.App;
using Bit.iOS.Core.Services;
using PushNotification.Plugin;
using Plugin.DeviceInfo;
using Plugin.Connectivity.Abstractions;

namespace Bit.iOS
{
    [Register("AppDelegate")]
    public partial class AppDelegate : global::Xamarin.Forms.Platform.iOS.FormsApplicationDelegate
    {
        public ISettings Settings { get; set; }

        public override bool FinishedLaunching(UIApplication app, NSDictionary options)
        {
            global::Xamarin.Forms.Forms.Init();

            if(!Resolver.IsSet)
            {
                SetIoc();
            }

            LoadApplication(new App.App(
                Resolver.Resolve<IAuthService>(),
                Resolver.Resolve<IConnectivity>(),
                Resolver.Resolve<IUserDialogs>(),
                Resolver.Resolve<IDatabaseService>(),
                Resolver.Resolve<ISyncService>(),
                Resolver.Resolve<IFingerprint>(),
                Resolver.Resolve<ISettings>()));

            UINavigationBar.Appearance.ShadowImage = new UIImage();
            UINavigationBar.Appearance.SetBackgroundImage(new UIImage(), UIBarMetrics.Default);

            MessagingCenter.Subscribe<Xamarin.Forms.Application>(Xamarin.Forms.Application.Current, "ShowAppExtension", (sender) =>
            {
                var itemProvider = new NSItemProvider(new NSDictionary(), "com.8bit.bitwarden.extension-setup");
                var extensionItem = new NSExtensionItem();
                extensionItem.Attachments = new NSItemProvider[] { itemProvider };
                var activityViewController = new UIActivityViewController(new NSExtensionItem[] { extensionItem }, null);
                activityViewController.CompletionHandler = (activityType, completed) =>
                {
                    MessagingCenter.Send(Xamarin.Forms.Application.Current, "EnabledAppExtension", 
                        completed && activityType == "com.8bit.bitwarden.find-login-action-extension");
                };

                UIApplication.SharedApplication.KeyWindow.RootViewController.ModalViewController
                    .PresentViewController(activityViewController, true, null);
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
                Center = view.Center
            };

            view.AddSubview(backgroundView);
            view.AddSubview(imageView);

            UIApplication.SharedApplication.KeyWindow.AddSubview(view);
            UIApplication.SharedApplication.KeyWindow.BringSubviewToFront(view);
            UIApplication.SharedApplication.KeyWindow.EndEditing(true);

            // Log the date/time we last backgrounded

            Settings.AddOrUpdateValue(Constants.SettingLastBackgroundedDate, DateTime.UtcNow);

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

            var view = UIApplication.SharedApplication.KeyWindow.ViewWithTag(4321);
            if(view != null)
            {
                view.RemoveFromSuperview();
            }
        }

        public override void WillEnterForeground(UIApplication uiApplication)
        {
            SendResumedMessage();
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
                .RegisterType<IAuthService, AuthService>(new ContainerControlledLifetimeManager())
                .RegisterType<IFolderService, FolderService>(new ContainerControlledLifetimeManager())
                .RegisterType<ISiteService, SiteService>(new ContainerControlledLifetimeManager())
                .RegisterType<ISyncService, SyncService>(new ContainerControlledLifetimeManager())
                .RegisterType<IClipboardService, ClipboardService>(new ContainerControlledLifetimeManager())
                .RegisterType<IPushNotificationListener, PushNotificationListener>(new ContainerControlledLifetimeManager())
                .RegisterType<IAppIdService, AppIdService>(new ContainerControlledLifetimeManager())
                .RegisterType<IPasswordGenerationService, PasswordGenerationService>(new ContainerControlledLifetimeManager())
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
                .RegisterInstance(CrossDeviceInfo.Current, new ContainerControlledLifetimeManager())
                .RegisterInstance(CrossConnectivity.Current, new ContainerControlledLifetimeManager())
                .RegisterInstance(UserDialogs.Instance, new ContainerControlledLifetimeManager())
                .RegisterInstance(CrossFingerprint.Current, new ContainerControlledLifetimeManager());

            Settings = new Settings("group.com.8bit.bitwarden");
            container.RegisterInstance(Settings, new ContainerControlledLifetimeManager());

            CrossPushNotification.Initialize(container.Resolve<IPushNotificationListener>());
            container.RegisterInstance(CrossPushNotification.Current, new ContainerControlledLifetimeManager());

            Resolver.SetResolver(new UnityResolver(container));
        }
    }
}
