using System;
using Foundation;
using UIKit;

namespace Bit.iOS.Autofill
{
    public partial class LoginAddViewController : Core.Controllers.LoginAddViewController
    {
        public LoginAddViewController(IntPtr handle) : base(handle)
        { }

        public LoginListViewController LoginListController { get; set; }

        public override UINavigationItem BaseNavItem => NavItem;
        public override UIBarButtonItem BaseCancelButton => CancelBarButton;
        public override UIBarButtonItem BaseSaveButton => SaveBarButton;

        public override Action Success => () =>
        {
            _googleAnalyticsService.TrackAutofillExtensionEvent("CreatedLogin");
            LoginListController?.DismissModal();
        };

        partial void CancelBarButton_Activated(UIBarButtonItem sender)
        {
            DismissViewController(true, null);
        }

        async partial void SaveBarButton_Activated(UIBarButtonItem sender)
        {
            await this.SaveAsync();
        }

        public override void PrepareForSegue(UIStoryboardSegue segue, NSObject sender)
        {
            var navController = segue.DestinationViewController as UINavigationController;
            if(navController != null)
            {
                var passwordGeneratorController = navController.TopViewController as PasswordGeneratorViewController;
                if(passwordGeneratorController != null)
                {
                    passwordGeneratorController.PasswordOptions = Context.PasswordOptions;
                    passwordGeneratorController.Parent = this;
                }
            }
        }
    }
}
