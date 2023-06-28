using System;
using Bit.App.Controls;
using Bit.Core.Utilities;
using Bit.iOS.Core.Utilities;
using UIKit;

namespace Bit.iOS.Autofill
{
    public partial class LockPasswordViewController : Core.Controllers.BaseLockPasswordViewController
    {
        AccountSwitchingOverlayView _accountSwitchingOverlayView;
        AccountSwitchingOverlayHelper _accountSwitchingOverlayHelper;

        public override UITableView TableView => MainTableView;

        public LockPasswordViewController(IntPtr handle)
            : base(handle)
        {
            BiometricIntegritySourceKey = Bit.Core.Constants.iOSAutoFillBiometricIntegritySourceKey;
            DismissModalAction = Cancel;
            autofillExtension = true;
        }

        public CredentialProviderViewController CPViewController { get; set; }
        public override UINavigationItem BaseNavItem => NavItem;
        public override UIBarButtonItem BaseCancelButton => CancelButton;
        public override UIBarButtonItem BaseSubmitButton => SubmitButton;
        public override Action Success => () => CPViewController.DismissLockAndContinue();
        public override Action Cancel => () => CPViewController.CompleteRequest();

        public override async void ViewDidLoad()
        {
            base.ViewDidLoad();

            _accountSwitchingOverlayHelper = new AccountSwitchingOverlayHelper();
            AccountSwitchingBarButton.Image = await _accountSwitchingOverlayHelper.CreateAvatarImageAsync();

            _accountSwitchingOverlayView = _accountSwitchingOverlayHelper.CreateAccountSwitchingOverlayView(OverlayView);
        }

        partial void AccountSwitchingBarButton_Activated(UIBarButtonItem sender)
        {
            _accountSwitchingOverlayHelper.OnToolbarItemActivated(_accountSwitchingOverlayView, OverlayView);
        }

        partial void SubmitButton_Activated(UIBarButtonItem sender)
        {
            CheckPasswordAsync().FireAndForget();
        }

        partial void CancelButton_Activated(UIBarButtonItem sender)
        {
            Cancel();
        }
    }
}
