using System;
using Bit.iOS.Core.Utilities;
using Foundation;
using UIKit;

namespace Bit.iOS.Extension
{
    public partial class LoginAddViewController : Core.Controllers.LoginAddViewController
    {
        public LoginAddViewController(IntPtr handle)
            : base(handle)
        { }

        public LoginListViewController LoginListController { get; set; }
        public LoadingViewController LoadingController { get; set; }

        public override UINavigationItem BaseNavItem => NavItem;
        public override UIBarButtonItem BaseCancelButton => CancelBarButton;
        public override UIBarButtonItem BaseSaveButton => SaveBarButton;

        public override void ViewDidLoad()
        {
            base.ViewDidLoad();
            SaveBarButton.TintColor = ThemeHelpers.NavBarTextColor;
            CancelBarButton.TintColor = ThemeHelpers.NavBarTextColor;
        }

        public override Action<string> Success => id =>
        {
            if (LoginListController != null)
            {
                LoginListController.DismissModal();
            }
            else if (LoadingController != null)
            {
                LoadingController.CompleteUsernamePasswordRequest(id, UsernameCell.TextField.Text,
                    PasswordCell.TextField.Text, null, null);
            }
        };

        partial void CancelBarButton_Activated(UIBarButtonItem sender)
        {
            if (LoginListController != null)
            {
                DismissViewController(true, null);
            }
            else
            {
                LoadingController.CompleteRequest(null, null);
            }
        }

        async partial void SaveBarButton_Activated(UIBarButtonItem sender)
        {
            await this.SaveAsync();
        }

        public override void PrepareForSegue(UIStoryboardSegue segue, NSObject sender)
        {
            if (segue.DestinationViewController is UINavigationController navController)
            {
                if (navController.TopViewController is PasswordGeneratorViewController passwordGeneratorController)
                {
                    passwordGeneratorController.PasswordOptions = Context.PasswordOptions;
                    passwordGeneratorController.Parent = this;
                }
            }
        }
    }
}
