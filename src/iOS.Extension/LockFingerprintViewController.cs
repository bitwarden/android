using System;
using Bit.iOS.Extension.Models;
using UIKit;
using XLabs.Ioc;
using Plugin.Fingerprint.Abstractions;
using System.Threading.Tasks;
using Bit.iOS.Core.Controllers;
using Bit.App.Resources;
using Bit.App.Abstractions;

namespace Bit.iOS.Extension
{
    public partial class LockFingerprintViewController : ExtendedUIViewController
    {
        private IAppSettingsService _appSettingsService;
        private IFingerprint _fingerprint;

        public LockFingerprintViewController(IntPtr handle) : base(handle)
        { }

        public Context Context { get; set; }
        public LoadingViewController LoadingController { get; set; }

        public override void ViewWillAppear(bool animated)
        {
            UINavigationBar.Appearance.ShadowImage = new UIImage();
            UINavigationBar.Appearance.SetBackgroundImage(new UIImage(), UIBarMetrics.Default);
            base.ViewWillAppear(animated);
        }

        public override void ViewDidLoad()
        {
            _appSettingsService = Resolver.Resolve<IAppSettingsService>();
            _fingerprint = Resolver.Resolve<IFingerprint>();

            NavItem.Title = AppResources.VerifyFingerprint;
            CancelButton.Title = AppResources.Cancel;
            View.BackgroundColor = new UIColor(red: 0.94f, green: 0.94f, blue: 0.96f, alpha: 1.0f);

            UseButton.SetTitle(AppResources.UseFingerprintToUnlock, UIControlState.Normal);
            var descriptor = UIFontDescriptor.PreferredBody;
            UseButton.Font = UIFont.FromDescriptor(descriptor, descriptor.PointSize);
            UseButton.BackgroundColor = new UIColor(red: 0.24f, green: 0.55f, blue: 0.74f, alpha: 1.0f);
            UseButton.TintColor = UIColor.White;
            UseButton.TouchUpInside += UseButton_TouchUpInside;

            base.ViewDidLoad();
        }

        private void UseButton_TouchUpInside(object sender, EventArgs e)
        {
            var task = CheckFingerprintAsync();
        }

        public override void ViewDidAppear(bool animated)
        {
            base.ViewDidAppear(animated);
            var task = CheckFingerprintAsync();
        }

        partial void CancelButton_Activated(UIBarButtonItem sender)
        {
            LoadingController.CompleteRequest(null);
        }

        partial void FingerprintButton_TouchUpInside(UIButton sender)
        {
            var task = CheckFingerprintAsync();
        }

        public async Task CheckFingerprintAsync()
        {
            var fingerprintRequest = new AuthenticationRequestConfiguration(AppResources.FingerprintDirection)
            {
                AllowAlternativeAuthentication = true,
                CancelTitle = AppResources.Cancel,
                FallbackTitle = AppResources.LogOut
            };
            var result = await _fingerprint.AuthenticateAsync(fingerprintRequest);
            if(result.Authenticated)
            {
                _appSettingsService.Locked = false;
                LoadingController.DismissLockAndContinue();
            }
        }
    }
}
