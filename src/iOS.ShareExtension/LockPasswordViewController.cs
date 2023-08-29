using Bit.App.Controls;
using Bit.Core.Utilities;
using Bit.iOS.Core.Utilities;
using System;
using UIKit;

namespace Bit.iOS.ShareExtension
{
    public partial class LockPasswordViewController : Core.Controllers.BaseLockPasswordViewController
    {
        AccountSwitchingOverlayView _accountSwitchingOverlayView;
        AccountSwitchingOverlayHelper _accountSwitchingOverlayHelper;

        public LockPasswordViewController()
        {
            BiometricIntegritySourceKey = Bit.Core.Constants.iOSShareExtensionBiometricIntegritySourceKey;
            DismissModalAction = Cancel;
        }

        public LockPasswordViewController(IntPtr handle)
            : base(handle)
        {
            BiometricIntegritySourceKey = Bit.Core.Constants.iOSShareExtensionBiometricIntegritySourceKey;
            DismissModalAction = Cancel;
        }

        public LoadingViewController LoadingController { get; set; }
        public override UINavigationItem BaseNavItem => _navItem;
        public override UIBarButtonItem BaseCancelButton => _cancelButton;
        public override UIBarButtonItem BaseSubmitButton => _submitButton;
        public override Action Success => () =>
        {
            LoadingController?.Navigate(Bit.Core.Enums.NavigationTarget.Home);
            LoadingController = null;
        };
        public override Action Cancel => () =>
        {
            LoadingController?.CompleteRequest();
            LoadingController = null;
        };
        public Action LaunchHomePage;

        public override UITableView TableView => _mainTableView;

        public override async void ViewDidLoad()
        {
            base.ViewDidLoad();

            _cancelButton.TintColor = ThemeHelpers.NavBarTextColor;
            _submitButton.TintColor = ThemeHelpers.NavBarTextColor;

            _accountSwitchingOverlayHelper = new AccountSwitchingOverlayHelper();
            _accountSwitchingButton.Image = await _accountSwitchingOverlayHelper.CreateAvatarImageAsync();

            _accountSwitchingOverlayView = _accountSwitchingOverlayHelper.CreateAccountSwitchingOverlayView(_overlayView);

            if (!HasLoginOrUnlockMethod)
            {
                // user doesn't have a login method
                // needs to go to homepage and login again
                LaunchHomePage?.Invoke();
            }
        }

        protected override void UpdateNavigationBarTheme()
        {
            UpdateNavigationBarTheme(_navBar);
        }

        partial void AccountSwitchingButton_Activated(UIBarButtonItem sender)
        {
            _accountSwitchingOverlayHelper.OnToolbarItemActivated(_accountSwitchingOverlayView, _overlayView);
        }

        partial void SubmitButton_Activated(UIBarButtonItem sender)
        {
            CheckPasswordAsync().FireAndForget();
        }

        partial void CancelButton_Activated(UIBarButtonItem sender)
        {
            Cancel();
        }

        protected override void Dispose(bool disposing)
        {
            if (disposing)
            {
                if (TableView != null)
                {
                    TableView.Source?.Dispose();
                }
                if (_accountSwitchingButton?.Image != null)
                {
                    var img = _accountSwitchingButton.Image;
                    _accountSwitchingButton.Image = null;
                    img.Dispose();
                }
                if (_accountSwitchingOverlayView != null && _overlayView?.Subviews != null)
                {
                    foreach (var subView in _overlayView.Subviews)
                    {
                        subView.RemoveFromSuperview();
                        subView.Dispose();
                    }
                    _accountSwitchingOverlayView = null;
                    _overlayView.RemoveFromSuperview();
                }
                _accountSwitchingOverlayHelper = null;
            }   

            base.Dispose(disposing);
        }
    }
}
