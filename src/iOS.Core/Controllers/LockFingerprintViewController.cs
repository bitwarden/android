using System;
using UIKit;
using XLabs.Ioc;
using Plugin.Fingerprint.Abstractions;
using System.Threading.Tasks;
using Bit.iOS.Core.Controllers;
using Bit.App.Resources;
using Bit.App.Abstractions;

namespace Bit.iOS.Core.Controllers
{
    public abstract class LockFingerprintViewController : ExtendedUIViewController
    {
        private IAppSettingsService _appSettingsService;
        private IFingerprint _fingerprint;
        private IDeviceInfoService _deviceInfo;

        public LockFingerprintViewController(IntPtr handle) : base(handle)
        { }

        public abstract UINavigationItem BaseNavItem { get; }
        public abstract UIBarButtonItem BaseCancelButton { get; }
        public abstract UIButton BaseUseButton { get; }
        public abstract UIButton BaseFingerprintButton { get; }
        public abstract Action Success { get; }

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
            _deviceInfo = Resolver.Resolve<IDeviceInfoService>();

            BaseNavItem.Title = _deviceInfo.HasFaceIdSupport ? AppResources.VerifyFaceID : AppResources.VerifyFingerprint;
            BaseCancelButton.Title = AppResources.Cancel;
            View.BackgroundColor = new UIColor(red: 0.94f, green: 0.94f, blue: 0.96f, alpha: 1.0f);

            BaseUseButton.SetTitle(_deviceInfo.HasFaceIdSupport ? AppResources.UseFaceIDToUnlock :
                AppResources.UseFingerprintToUnlock, UIControlState.Normal);
            var descriptor = UIFontDescriptor.PreferredBody;
            BaseUseButton.Font = UIFont.FromDescriptor(descriptor, descriptor.PointSize);
            BaseUseButton.BackgroundColor = new UIColor(red: 0.24f, green: 0.55f, blue: 0.74f, alpha: 1.0f);
            BaseUseButton.TintColor = UIColor.White;
            BaseUseButton.TouchUpInside += UseButton_TouchUpInside;

            BaseFingerprintButton.SetImage(new UIImage(_deviceInfo.HasFaceIdSupport ? "smile.png" : "fingerprint.png"),
                UIControlState.Normal);

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

        public async Task CheckFingerprintAsync()
        {
            var fingerprintRequest = new AuthenticationRequestConfiguration(
                _deviceInfo.HasFaceIdSupport ? AppResources.FaceIDDirection : AppResources.FingerprintDirection)
            {
                AllowAlternativeAuthentication = true,
                CancelTitle = AppResources.Cancel,
                FallbackTitle = AppResources.LogOut
            };
            var result = await _fingerprint.AuthenticateAsync(fingerprintRequest);
            if(result.Authenticated)
            {
                _appSettingsService.Locked = false;
                Success();
            }
        }
    }
}
