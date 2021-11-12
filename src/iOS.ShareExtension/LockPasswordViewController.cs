using Bit.iOS.Core.Utilities;
using System;
using UIKit;

namespace Bit.iOS.ShareExtension
{
    public partial class LockPasswordViewController : Core.Controllers.LockPasswordViewController
    {
        public LockPasswordViewController(IntPtr handle)
            : base(handle)
        {
            BiometricIntegrityKey = Bit.Core.Constants.iOSExtensionBiometricIntegrityKey;
            DismissModalAction = Cancel;
        }

        public LoadingViewController LoadingController { get; set; }
        public override UINavigationItem BaseNavItem => _navItem;
        public override UIBarButtonItem BaseCancelButton => _cancelButton;
        public override UIBarButtonItem BaseSubmitButton => _submitButton;
        public override Action Success => () => LoadingController.DismissLockAndContinue();
        public override Action Cancel => () => LoadingController.CompleteRequest(null, null);

        public override void ViewDidLoad()
        {
            base.ViewDidLoad();
            _cancelButton.TintColor = ThemeHelpers.NavBarTextColor;
            _submitButton.TintColor = ThemeHelpers.NavBarTextColor;
        }

        partial void SubmitButton_Activated(UIBarButtonItem sender)
        {
            var task = CheckPasswordAsync();
        }

        partial void CancelButton_Activated(UIBarButtonItem sender)
        {
            Cancel();
        }
    }
}
