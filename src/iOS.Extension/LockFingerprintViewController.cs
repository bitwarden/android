using System;
using Bit.iOS.Extension.Models;
using UIKit;
using XLabs.Ioc;
using Plugin.Settings.Abstractions;
using Foundation;
using MobileCoreServices;

namespace Bit.iOS.Extension
{
    public partial class LockFingerprintViewController : UIViewController
    {
        private ISettings _settings;

        public LockFingerprintViewController(IntPtr handle) : base(handle)
        { }

        public Context Context { get; set; }

        public override void ViewWillAppear(bool animated)
        {
            UINavigationBar.Appearance.ShadowImage = new UIImage();
            UINavigationBar.Appearance.SetBackgroundImage(new UIImage(), UIBarMetrics.Default);
            base.ViewWillAppear(animated);
        }

        public override void ViewDidLoad()
        {
            _settings = Resolver.Resolve<ISettings>();

            View.BackgroundColor = new UIColor(red: 0.94f, green: 0.94f, blue: 0.96f, alpha: 1.0f);

            var descriptor = UIFontDescriptor.PreferredBody;

            base.ViewDidLoad();
        }

        partial void CancelButton_Activated(UIBarButtonItem sender)
        {
            CompleteRequest();
        }

        private void CompleteRequest()
        {
            var resultsProvider = new NSItemProvider(null, UTType.PropertyList);
            var resultsItem = new NSExtensionItem { Attachments = new NSItemProvider[] { resultsProvider } };
            var returningItems = new NSExtensionItem[] { resultsItem };

            Context.ExtContext.CompleteRequest(returningItems, null);
        }
    }
}
