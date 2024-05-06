using System;
using Bit.Core.Resources.Localization;
using Bit.iOS.Core.Controllers;
using Bit.iOS.Core.Utilities;
using Bit.iOS.Extension.Models;
using UIKit;

namespace Bit.iOS.Extension
{
    public partial class SetupViewController : ExtendedUIViewController
    {
        public SetupViewController(IntPtr handle)
            : base(handle)
        {
            ModalPresentationStyle = UIModalPresentationStyle.FullScreen;
            DismissModalAction = Cancel;
        }

        public Context Context { get; set; }
        public LoadingViewController LoadingController { get; set; }

        public override void ViewDidLoad()
        {
            base.ViewDidLoad();
            var descriptor = UIFontDescriptor.PreferredBody;
            DescriptionLabel.Text = $@"{AppResources.ExtensionSetup}

{AppResources.ExtensionSetup2}";
            DescriptionLabel.Font = UIFont.FromDescriptor(descriptor, descriptor.PointSize);
            DescriptionLabel.TextColor = ThemeHelpers.MutedColor;

            ActivatedLabel.Text = AppResources.ExtensionActivated;
            ActivatedLabel.Font = UIFont.FromDescriptor(descriptor, descriptor.PointSize * 1.3f);
            ActivatedLabel.TextColor = ThemeHelpers.SuccessColor;

            BackButton.TintColor = ThemeHelpers.NavBarTextColor;
            BackButton.Title = AppResources.Back;
        }

        partial void BackButton_Activated(UIBarButtonItem sender)
        {
            Cancel();
        }
        
        private void Cancel()
        {
            LoadingController.CompleteRequest(null, null);
        }
    }
}
