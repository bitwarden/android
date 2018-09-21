using System;
using UIKit;

namespace Bit.iOS.Extension
{
    public partial class LockPasswordViewController : Core.Controllers.LockPasswordViewController
    {
        public LockPasswordViewController(IntPtr handle) : base(handle)
        { }

        public LoadingViewController LoadingController { get; set; }
        public override UINavigationItem BaseNavItem => NavItem;
        public override UIBarButtonItem BaseCancelButton => CancelButton;
        public override UIBarButtonItem BaseSubmitButton => SubmitButton;
        public override Action Success => () => LoadingController.DismissLockAndContinue();

        partial void SubmitButton_Activated(UIBarButtonItem sender)
        {
            CheckPassword();
        }

        partial void CancelButton_Activated(UIBarButtonItem sender)
        {
            LoadingController.CompleteRequest(null);
        }
    }
}
