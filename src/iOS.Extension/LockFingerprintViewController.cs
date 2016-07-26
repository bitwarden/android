using System;
using Bit.iOS.Extension.Models;
using UIKit;
using XLabs.Ioc;
using Plugin.Settings.Abstractions;
using Plugin.Fingerprint.Abstractions;
using System.Threading.Tasks;
using Bit.App;

namespace Bit.iOS.Extension
{
    public partial class LockFingerprintViewController : UIViewController
    {
        private ISettings _settings;
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
            _settings = Resolver.Resolve<ISettings>();
            _fingerprint = Resolver.Resolve<IFingerprint>();

            View.BackgroundColor = new UIColor(red: 0.94f, green: 0.94f, blue: 0.96f, alpha: 1.0f);

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

        public async Task CheckFingerprintAsync()
        {
            var result = await _fingerprint.AuthenticateAsync("Use your fingerprint to verify.");
            if(result.Authenticated)
            {
                _settings.AddOrUpdateValue(Constants.SettingLocked, false);
                LoadingController.DismissLockAndContinue();
            }
        }
    }
}
