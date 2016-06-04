using System;
using System.Drawing;
using Bit.App.Abstractions;
using Bit.App.Repositories;
using Bit.App.Services;
using Bit.iOS.Core.Services;
using Foundation;
using Microsoft.Practices.Unity;
using UIKit;
using XLabs.Ioc;
using XLabs.Ioc.Unity;

namespace Bit.iOS.Extension
{
    public partial class SplashViewController : UIViewController
    {
        public SplashViewController(IntPtr handle) : base(handle)
        {
        }

        public override void DidReceiveMemoryWarning()
        {
            // Releases the view if it doesn't have a superview.
            base.DidReceiveMemoryWarning();

            // Release any cached data, images, etc that aren't in use.
        }

        #region View lifecycle

        public override void ViewDidLoad()
        {
            base.ViewDidLoad();
            View.BackgroundColor = UIColor.FromPatternImage(new UIImage("boxed-bg.png"));
            NavigationController.SetNavigationBarHidden(true, false);
        }

        public override void ViewWillAppear(bool animated)
        {
            base.ViewWillAppear(animated);
        }

        public override void ViewDidAppear(bool animated)
        {
            base.ViewDidAppear(animated);

            if(!Resolver.IsSet)
            {
                SetIoc();
            }

            PerformSegue("seque", this);
        }

        private void SetIoc()
        {
            var container = new UnityContainer();

            container
                // Services
                .RegisterType<IDatabaseService, DatabaseService>(new ContainerControlledLifetimeManager())
                .RegisterType<ISqlService, SqlService>(new ContainerControlledLifetimeManager())
                //.RegisterType<ISecureStorageService, KeyChainStorageService>(new ContainerControlledLifetimeManager())
                .RegisterType<ICryptoService, CryptoService>(new ContainerControlledLifetimeManager())
                .RegisterType<IAuthService, AuthService>(new ContainerControlledLifetimeManager())
                .RegisterType<IFolderService, FolderService>(new ContainerControlledLifetimeManager())
                .RegisterType<ISiteService, SiteService>(new ContainerControlledLifetimeManager())
                .RegisterType<ISyncService, SyncService>(new ContainerControlledLifetimeManager())
                //.RegisterType<IClipboardService, ClipboardService>(new ContainerControlledLifetimeManager())
                // Repositories
                .RegisterType<IFolderRepository, FolderRepository>(new ContainerControlledLifetimeManager())
                .RegisterType<IFolderApiRepository, FolderApiRepository>(new ContainerControlledLifetimeManager())
                .RegisterType<ISiteRepository, SiteRepository>(new ContainerControlledLifetimeManager())
                .RegisterType<ISiteApiRepository, SiteApiRepository>(new ContainerControlledLifetimeManager())
                .RegisterType<IAuthApiRepository, AuthApiRepository>(new ContainerControlledLifetimeManager());
            // Other
            //.RegisterInstance(CrossSettings.Current, new ContainerControlledLifetimeManager())
            //.RegisterInstance(CrossConnectivity.Current, new ContainerControlledLifetimeManager())
            //.RegisterInstance(UserDialogs.Instance, new ContainerControlledLifetimeManager())
            //.RegisterInstance(CrossFingerprint.Current, new ContainerControlledLifetimeManager());

            Resolver.SetResolver(new UnityResolver(container));
        }

        public override void ViewWillDisappear(bool animated)
        {
            base.ViewWillDisappear(animated);
        }

        public override void ViewDidDisappear(bool animated)
        {
            base.ViewDidDisappear(animated);
        }

        #endregion
    }
}