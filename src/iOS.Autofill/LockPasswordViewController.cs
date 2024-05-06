using System;
using Bit.App.Controls;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.iOS.Core.Utilities;
using UIKit;

namespace Bit.iOS.Autofill
{
    public partial class LockPasswordViewController : Core.Controllers.BaseLockPasswordViewController
    {
        UIBarButtonItem _cancelButton;
        UIControl _accountSwitchButton;
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
        public override UIBarButtonItem BaseCancelButton => _cancelButton;
        public override UIBarButtonItem BaseSubmitButton => SubmitButton;
        public override Action Success => () => CPViewController.DismissLockAndContinue();
        public override Action Cancel => () => CPViewController.CompleteRequest();

        public override async void ViewDidLoad()
        {
            try
            {
                _cancelButton = new UIBarButtonItem(UIBarButtonSystemItem.Cancel, CancelButton_TouchUpInside);

                base.ViewDidLoad();

                _accountSwitchingOverlayHelper = new AccountSwitchingOverlayHelper();

                _accountSwitchButton = await _accountSwitchingOverlayHelper.CreateAccountSwitchToolbarButtonItemCustomViewAsync();
                _accountSwitchButton.TouchUpInside += AccountSwitchedButton_TouchUpInside;

                NavItem.SetLeftBarButtonItems(new UIBarButtonItem[]
                {
                    _cancelButton,
                    new UIBarButtonItem(_accountSwitchButton)
                }, false);

                _accountSwitchingOverlayView = _accountSwitchingOverlayHelper.CreateAccountSwitchingOverlayView(OverlayView);
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
        }

        private void CancelButton_TouchUpInside(object sender, EventArgs e)
        {
            Cancel();
        }

        private void AccountSwitchedButton_TouchUpInside(object sender, EventArgs e)
        {
            _accountSwitchingOverlayHelper.OnToolbarItemActivated(_accountSwitchingOverlayView, OverlayView);
        }

        partial void SubmitButton_Activated(UIBarButtonItem sender)
        {
            CheckPasswordAsync().FireAndForget();
        }

        protected override void Dispose(bool disposing)
        {
            if (disposing)
            {
                if (_accountSwitchButton != null)
                {
                    _accountSwitchingOverlayHelper.DisposeAccountSwitchToolbarButtonItemImage(_accountSwitchButton);

                    _accountSwitchButton.TouchUpInside -= AccountSwitchedButton_TouchUpInside;
                }
            }

            base.Dispose(disposing);
        }
    }
}
