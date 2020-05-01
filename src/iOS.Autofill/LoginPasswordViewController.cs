using System;

using Foundation;
using UIKit;

namespace Bit.iOS.Autofill
{
	public partial class LoginPasswordViewController : Core.Controllers.LoginPasswordViewController
	{
		public LoginPasswordViewController(IntPtr handle) : base(handle)
		{
		}
        public CredentialProviderViewController CPViewController { get; set; }
        public override UINavigationItem BaseNavItem => NavItem;
        public override UIBarButtonItem BaseCancelButton => CancelButton;
        public override UIBarButtonItem BaseSubmitButton => SubmitButton;
        public override Action Success => () => CPViewController.DismissAccessControlAndContinue();
        public override Action Cancel => () => CPViewController.CompleteRequest();

        partial void SubmitButton_Activated(UIBarButtonItem sender)
        {
            var task = LogInAsync();
        }
        
        partial void CancelButton_Activated(UIBarButtonItem sender)
        {
            Cancel();
        }
    }
}
