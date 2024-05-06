using System;
using UIKit;
using Bit.iOS.Core.Controllers;
using Bit.Core.Resources.Localization;
using Bit.iOS.Core.Utilities;

namespace Bit.iOS.Autofill
{
    public partial class SetupViewController : ExtendedUIViewController
    {
        public SetupViewController(IntPtr handle)
            : base(handle)
        {
            DismissModalAction = Cancel;
        }

        public CredentialProviderViewController CPViewController { get; set; }

        public override void ViewDidLoad()
        {
            var descriptor = UIFontDescriptor.PreferredBody;
            DescriptionLabel.Text = $@"{AppResources.AutofillSetup}

{AppResources.AutofillSetup2}";
            DescriptionLabel.Font = UIFont.FromDescriptor(descriptor, descriptor.PointSize);
            DescriptionLabel.TextColor = ThemeHelpers.MutedColor;

            ActivatedLabel.Text = AppResources.AutofillActivated;
            ActivatedLabel.Font = UIFont.FromDescriptor(descriptor, descriptor.PointSize * 1.3f);
            ActivatedLabel.TextColor = ThemeHelpers.SuccessColor;

            BackButton.Title = AppResources.Back;
            base.ViewDidLoad();
            var task = ASHelpers.ReplaceAllIdentitiesAsync();
        }

        partial void BackButton_Activated(UIBarButtonItem sender)
        {
            Cancel();
        }

        private void Cancel()
        {
            CPViewController.CompleteRequest();
        }
    }
}
