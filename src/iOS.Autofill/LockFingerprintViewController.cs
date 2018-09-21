using System;
using UIKit;

namespace Bit.iOS.Extension
{
    public partial class LockFingerprintViewController : Core.Controllers.LockFingerprintViewController
    {
        public LockFingerprintViewController(IntPtr handle) : base(handle)
        { }

        public LoadingViewController LoadingController { get; set; }
        public override UINavigationItem BaseNavItem => NavItem;
        public override UIBarButtonItem BaseCancelButton => CancelButton;
        public override UIButton BaseUseButton => UseButton;
        public override UIButton BaseFingerprintButton => FingerprintButton;
        public override Action Success => () => LoadingController.DismissLockAndContinue();

        partial void CancelButton_Activated(UIBarButtonItem sender)
        {
            LoadingController.CompleteRequest(null);
        }

        partial void FingerprintButton_TouchUpInside(UIButton sender)
        {
            var task = CheckFingerprintAsync();
        }
    }
}
