using System;
using UIKit;

namespace Bit.iOS.Autofill
{
    public partial class LockFingerprintViewController : Core.Controllers.LockFingerprintViewController
    {
        public LockFingerprintViewController(IntPtr handle) : base(handle)
        { }

        public CredentialProviderViewController CPViewController { get; set; }
        public override UINavigationItem BaseNavItem => NavItem;
        public override UIBarButtonItem BaseCancelButton => CancelButton;
        public override UIButton BaseUseButton => UseButton;
        public override UIButton BaseFingerprintButton => FingerprintButton;
        public override Action Success => () => CPViewController.DismissLockAndContinue();

        partial void CancelButton_Activated(UIBarButtonItem sender)
        {
            CPViewController.CompleteRequest();
        }

        partial void FingerprintButton_TouchUpInside(UIButton sender)
        {
            var task = CheckFingerprintAsync();
        }
    }
}
