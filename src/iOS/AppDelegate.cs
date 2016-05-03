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
            global::Xamarin.Forms.Forms.Init();

            if(!Resolver.IsSet)
            {
                SetIoc();
            }

            LoadApplication(new App.App(Resolver.Resolve<IAuthService>(), Resolver.Resolve<IDatabaseService>()));

            return base.FinishedLaunching(app, options);
        }

        private void SetIoc()
        {
            var container = new UnityContainer();

            container
                .RegisterType<ISqlService, SqlService>(new ContainerControlledLifetimeManager())
                .RegisterType<IDatabaseService, DatabaseService>(new ContainerControlledLifetimeManager())
                .RegisterType<ISecureStorageService, KeyChainStorageService>(new ContainerControlledLifetimeManager())
                .RegisterInstance(CrossSettings.Current, new ContainerControlledLifetimeManager())
                .RegisterType<IApiService, ApiService>(new ContainerControlledLifetimeManager())
                .RegisterType<ICryptoService, CryptoService>(new ContainerControlledLifetimeManager())
                .RegisterType<IAuthService, AuthService>(new ContainerControlledLifetimeManager())
                .RegisterType<IFolderService, FolderService>(new ContainerControlledLifetimeManager())
                .RegisterType<ISiteService, SiteService>(new ContainerControlledLifetimeManager())
                .RegisterInstance(CrossConnectivity.Current, new ContainerControlledLifetimeManager())
                .RegisterInstance(UserDialogs.Instance, new ContainerControlledLifetimeManager());

            Resolver.SetResolver(new UnityResolver(container));
        }
    }
}
