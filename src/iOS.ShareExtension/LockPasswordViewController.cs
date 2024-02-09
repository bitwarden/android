using System;
using Bit.App.Controls;
using Bit.Core.Utilities;
using Bit.iOS.Core.Utilities;
using UIKit;

namespace Bit.iOS.ShareExtension
{
    public partial class LockPasswordViewController : Core.Controllers.BaseLockPasswordViewController
    {
        UIBarButtonItem _cancelButton;
        UIControl _accountSwitchButton;
        AccountSwitchingOverlayView _accountSwitchingOverlayView;
        private Lazy<AccountSwitchingOverlayHelper> _accountSwitchingOverlayHelper = new Lazy<AccountSwitchingOverlayHelper>(() => new AccountSwitchingOverlayHelper());

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

        public override UITableView TableView => _mainTableView;

        public override async void ViewDidLoad()
        {
            _cancelButton = new UIBarButtonItem(UIBarButtonSystemItem.Cancel, CancelButton_TouchUpInside);

            base.ViewDidLoad();

            _cancelButton.TintColor = ThemeHelpers.NavBarTextColor;
            _submitButton.TintColor = ThemeHelpers.NavBarTextColor;

            _accountSwitchButton = await _accountSwitchingOverlayHelper.Value.CreateAccountSwitchToolbarButtonItemCustomViewAsync();
            _accountSwitchButton.TouchUpInside += AccountSwitchedButton_TouchUpInside;

            _navItem.SetLeftBarButtonItems(new UIBarButtonItem[]
            {
                _cancelButton,
                new UIBarButtonItem(_accountSwitchButton)
            }, false);

            _accountSwitchingOverlayView = _accountSwitchingOverlayHelper.Value.CreateAccountSwitchingOverlayView(_overlayView);
        }

        private void CancelButton_TouchUpInside(object sender, EventArgs e)
        {
            Cancel();
        }

        private void AccountSwitchedButton_TouchUpInside(object sender, EventArgs e)
        {
            _accountSwitchingOverlayHelper.Value.OnToolbarItemActivated(_accountSwitchingOverlayView, _overlayView);
        }

        protected override void UpdateNavigationBarTheme()
        {
            UpdateNavigationBarTheme(_navBar);
        }

        partial void SubmitButton_Activated(UIBarButtonItem sender)
        {
            CheckPasswordAsync().FireAndForget();
        }

        protected override void Dispose(bool disposing)
        {
            if (disposing)
            {
                if (TableView != null)
                {
                    TableView.Source?.Dispose();
                }
                if (_accountSwitchButton != null)
                {
                    _accountSwitchingOverlayHelper.Value.DisposeAccountSwitchToolbarButtonItemImage(_accountSwitchButton);
                
                    _accountSwitchButton.TouchUpInside -= AccountSwitchedButton_TouchUpInside;
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
