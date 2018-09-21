using System;
using UIKit;

namespace Bit.iOS.Extension
{
    public partial class LockPinViewController : Core.Controllers.LockPinViewController
    {
        public LockPinViewController(IntPtr handle) : base(handle)
        { }

        public LoadingViewController LoadingController { get; set; }
        public override UINavigationItem BaseNavItem => NavItem;
        public override UIBarButtonItem BaseCancelButton => CancelButton;
        public override UILabel BasePinLabel => PinLabel;
        public override UILabel BaseInstructionLabel => InstructionLabel;
        public override UITextField BasePinTextField => PinTextField;
        public override Action Success => () => LoadingController.DismissLockAndContinue();

        partial void CancelButton_Activated(UIBarButtonItem sender)
        {
            LoadingController.CompleteRequest(null);
        }
    }
}
