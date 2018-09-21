using System;
using UIKit;
using Bit.iOS.Core.Controllers;
using Bit.App.Resources;
using Bit.iOS.Core.Utilities;
using XLabs.Ioc;
using Bit.App.Abstractions;

namespace Bit.iOS.Autofill
{
    public partial class SetupViewController : ExtendedUIViewController
    {
        public SetupViewController(IntPtr handle) : base(handle)
        { }
        
        public CredentialProviderViewController CPViewController { get; set; }

        public override void ViewWillAppear(bool animated)
        {
            UINavigationBar.Appearance.ShadowImage = new UIImage();
            UINavigationBar.Appearance.SetBackgroundImage(new UIImage(), UIBarMetrics.Default);
            base.ViewWillAppear(animated);
        }

        public override void ViewDidLoad()
        {
            View.BackgroundColor = new UIColor(red: 0.94f, green: 0.94f, blue: 0.96f, alpha: 1.0f);
            var descriptor = UIFontDescriptor.PreferredBody;
            DescriptionLabel.Text = $@"{AppResources.AutofillSetup}

{AppResources.AutofillSetup2}";
            DescriptionLabel.Font = UIFont.FromDescriptor(descriptor, descriptor.PointSize);
            DescriptionLabel.TextColor = new UIColor(red: 0.47f, green: 0.47f, blue: 0.47f, alpha: 1.0f);

            ActivatedLabel.Text = AppResources.AutofillActivated;
            ActivatedLabel.Font = UIFont.FromDescriptor(descriptor, descriptor.PointSize * 1.3f);

            BackButton.Title = AppResources.Back;
            base.ViewDidLoad();

            var tasks = ASHelpers.ReplaceAllIdentities(Resolver.Resolve<ICipherService>());
        }

        partial void BackButton_Activated(UIBarButtonItem sender)
        {
            CPViewController.CompleteRequest();
        }
    }
}
