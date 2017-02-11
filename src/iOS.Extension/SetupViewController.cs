using System;
using Bit.iOS.Extension.Models;
using UIKit;
using Plugin.Settings.Abstractions;
using Bit.iOS.Core.Controllers;
using Bit.App.Resources;

namespace Bit.iOS.Extension
{
    public partial class SetupViewController : ExtendedUIViewController
    {
        public SetupViewController(IntPtr handle) : base(handle)
        { }

        public Context Context { get; set; }
        public LoadingViewController LoadingController { get; set; }

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
            DescriptionLabel.Text = $@"{AppResources.ExtensionSetup}

{AppResources.ExtensionSetup2}";
            DescriptionLabel.Font = UIFont.FromDescriptor(descriptor, descriptor.PointSize);
            DescriptionLabel.TextColor = new UIColor(red: 0.47f, green: 0.47f, blue: 0.47f, alpha: 1.0f);

            ActivatedLabel.Text = AppResources.ExtensionActivated;
            ActivatedLabel.Font = UIFont.FromDescriptor(descriptor, descriptor.PointSize * 1.3f);

            BackButton.Title = AppResources.Back;
            base.ViewDidLoad();
        }

        partial void BackButton_Activated(UIBarButtonItem sender)
        {
            LoadingController.CompleteRequest(null);
        }
    }
}
