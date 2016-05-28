using System;
using System.Diagnostics;
using System.Linq;
using Bit.App.Abstractions;
using Bit.App.Repositories;
using Bit.App.Services;
using Bit.iOS.Core.Services;
using CoreGraphics;
using Foundation;
using Microsoft.Practices.Unity;
using MobileCoreServices;
using UIKit;
using XLabs.Ioc;
using XLabs.Ioc.Unity;

namespace Bit.iOS.Extension
{
    public partial class ActionViewController : UIViewController
    {
        public ActionViewController() : base("ActionViewController", null)
        {
            if(!Resolver.IsSet)
            {
                SetIoc();
            }
        }

        public string HtmlContent { get; set; }
        public Uri BaseUri { get; set; }
        public Uri Url { get; set; }

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

        public override void DidReceiveMemoryWarning()
        {
            base.DidReceiveMemoryWarning();
        }

        public async override void LoadView()
        {
            View = new UIView(new CGRect(x: 0.0, y: 0, width: 320.0, height: 200.0));
            var button = new UIButton(new CGRect(x: 10.0, y: 50.0, width: 200.0, height: 30.0));
            button.SetTitle("Done", UIControlState.Normal);
            button.TouchUpInside += Button_TouchUpInside;
            View.AddSubview(button);
        }

        private void Button_TouchUpInside(object sender, EventArgs e)
        {
            var itemData = new NSDictionary(
                "username", "me@example.com",
                "password", "mypassword",
                "autoSubmit", true);

            var resultsProvider = new NSItemProvider(
                new NSDictionary(NSJavaScriptExtension.FinalizeArgumentKey, itemData), UTType.PropertyList);

            var resultsItem = new NSExtensionItem { Attachments = new NSItemProvider[] { resultsProvider } };
            var returningItems = new NSExtensionItem[] { resultsItem };

            ExtensionContext.CompleteRequest(returningItems, null);
        }

        public override void ViewDidLoad()
        {
            base.ViewDidLoad();

            foreach(var item in ExtensionContext.InputItems)
            {
                foreach(var itemProvider in item.Attachments)
                {
                    if(!itemProvider.HasItemConformingTo(UTType.PropertyList))
                    {
                        continue;
                    }

                    itemProvider.LoadItem(UTType.PropertyList, null, (NSObject list, NSError error) =>
                    {
                        if(list == null)
                        {
                            return;
                        }

                        var dict = list as NSDictionary;
                        var result = dict[NSJavaScriptExtension.PreprocessingResultsKey];
                        if(result == null)
                        {
                            return;
                        }

                        HtmlContent = result.ValueForKey(new NSString("htmlContent")) as NSString;
                        BaseUri = new Uri(result.ValueForKey(new NSString("baseUri")) as NSString);
                        Url = new Uri(result.ValueForKey(new NSString("url")) as NSString);
                    });

                    break;
                }
            }
        }

    }
}