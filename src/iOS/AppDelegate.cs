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
using Plugin.Settings;
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

namespace Bit.iOS
{
    // The UIApplicationDelegate for the application. This class is responsible for launching the 
    // User Interface of the application, as well as listening (and optionally responding) to 
    // application events from iOS.
    [Register("AppDelegate")]
    public partial class AppDelegate : global::Xamarin.Forms.Platform.iOS.FormsApplicationDelegate
    {
        //
        // This method is invoked when the application has loaded and is ready to run. In this 
        // method you should instantiate the window, load the UI into it and then make the window
        // visible.
        //
        // You have 17 seconds to return from this method, or iOS will terminate your application.
        //
        public override bool FinishedLaunching(UIApplication app, NSDictionary options)
        {
            CrossFingerprint.AllowReuse = false;

            global::Xamarin.Forms.Forms.Init();

            if(!Resolver.IsSet)
            {
                SetIoc();
            }

            LoadApplication(new App.App(
                Resolver.Resolve<IAuthService>(),
                Resolver.Resolve<IDatabaseService>(),
                Resolver.Resolve<IFingerprint>(),
                Resolver.Resolve<ISettings>()));

            return base.FinishedLaunching(app, options);
        }

        public override void DidEnterBackground(UIApplication uiApplication)
        {
            // TODO: Make this an image view
            var colorView = new UIView(UIApplication.SharedApplication.KeyWindow.Frame)
            {
                BackgroundColor = UIColor.Black,
                Tag = 4321
            };
            UIApplication.SharedApplication.KeyWindow.AddSubview(colorView);
            UIApplication.SharedApplication.KeyWindow.BringSubviewToFront(colorView);

            // Log the date/time we last backgrounded
            CrossSettings.Current.AddOrUpdateValue(Constants.SettingLastBackgroundedDate, DateTime.UtcNow);

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
            SendLockMessage();
            base.WillEnterForeground(uiApplication);
            Debug.WriteLine("WillEnterForeground");
        }

        private void SendLockMessage()
        {
            MessagingCenter.Send(Xamarin.Forms.Application.Current, "Lock", false);
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

            Resolver.SetResolver(new UnityResolver(container));
        }
    }
}
