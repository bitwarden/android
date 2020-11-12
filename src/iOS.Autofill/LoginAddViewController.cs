using System;
using Bit.iOS.Core.Views;
using Foundation;
using UIKit;

namespace Bit.iOS.Autofill
{
    public partial class LoginAddViewController : Core.Controllers.LoginAddViewController
    {
        public LoginAddViewController(IntPtr handle)
            : base(handle)
        {
            DismissModalAction = Cancel;
        }

        public LoginListViewController LoginListController { get; set; }
        public LoginSearchViewController LoginSearchController { get; set; }

        public override UINavigationItem BaseNavItem => NavItem;
        public override UIBarButtonItem BaseCancelButton => CancelBarButton;
        public override UIBarButtonItem BaseSaveButton => SaveBarButton;

        public override Action<string> Success => id =>
        {
            LoginListController?.DismissModal();
            LoginSearchController?.DismissModal();
        };

        partial void CancelBarButton_Activated(UIBarButtonItem sender)
        {
            Cancel();
        }
        
        private void Cancel()
        {
            DismissViewController(true, null);
        }

        async partial void SaveBarButton_Activated(UIBarButtonItem sender)
        {
            await SaveAsync();
        }

        public override void PrepareForSegue(UIStoryboardSegue segue, NSObject sender)
        {
            if (segue.DestinationViewController is UINavigationController navController)
            {
                if (navController.TopViewController is PasswordGeneratorViewController passwordGeneratorController)
                {
                    passwordGeneratorController.PasswordOptions = Context.PasswordOptions;
                    passwordGeneratorController.Parent = this;
                    segue.DestinationViewController.PresentationController.Delegate =
                        new CustomPresentationControllerDelegate(passwordGeneratorController.DismissModalAction);
                }
            }
        }
    }
}
