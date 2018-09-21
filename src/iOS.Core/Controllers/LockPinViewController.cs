using System;
using UIKit;
using XLabs.Ioc;
using Bit.App.Abstractions;
using Bit.iOS.Core.Utilities;
using Bit.App.Resources;
using System.Diagnostics;
using Bit.iOS.Core.Controllers;

namespace Bit.iOS.Core.Controllers
{
    public abstract class LockPinViewController : ExtendedUIViewController
    {
        private IAppSettingsService _appSettingsService;
        private IAuthService _authService;

        public LockPinViewController(IntPtr handle) : base(handle)
        { }

        public abstract UINavigationItem BaseNavItem { get; }
        public abstract UIBarButtonItem BaseCancelButton { get; }
        public abstract UILabel BasePinLabel { get; }
        public abstract UILabel BaseInstructionLabel { get; }
        public abstract UITextField BasePinTextField { get; }
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
            _authService = Resolver.Resolve<IAuthService>();

            BaseNavItem.Title = AppResources.VerifyPIN;
            BaseCancelButton.Title = AppResources.Cancel;
            View.BackgroundColor = new UIColor(red: 0.94f, green: 0.94f, blue: 0.96f, alpha: 1.0f);

            var descriptor = UIFontDescriptor.PreferredBody;
            BasePinLabel.Font = UIFont.FromName("Menlo-Regular", 35);

            BaseInstructionLabel.Text = AppResources.EnterPIN;
            BaseInstructionLabel.LineBreakMode = UILineBreakMode.WordWrap;
            BaseInstructionLabel.Lines = 0;
            BaseInstructionLabel.Font = UIFont.FromDescriptor(descriptor, descriptor.PointSize * 0.8f);
            BaseInstructionLabel.TextColor = new UIColor(red: 0.47f, green: 0.47f, blue: 0.47f, alpha: 1.0f);

            BasePinTextField.EditingChanged += PinTextField_EditingChanged;

            base.ViewDidLoad();
        }

        public override void ViewDidAppear(bool animated)
        {
            base.ViewDidAppear(animated);
            BasePinTextField.BecomeFirstResponder();
        }

        private void PinTextField_EditingChanged(object sender, EventArgs e)
        {
            SetLabelText();

            if(BasePinTextField.Text.Length >= 4)
            {
                if(BasePinTextField.Text == _authService.PIN)
                {
                    Debug.WriteLine("BW Log, Start Dismiss PIN controller.");
                    _appSettingsService.Locked = false;
                    BasePinTextField.ResignFirstResponder();
                    Success();
                }
                else
                {
                    // TODO: keep track of invalid attempts and logout?

                    var alert = Dialogs.CreateAlert(null, AppResources.InvalidPIN, AppResources.Ok, (a) =>
                    {
                        BasePinTextField.Text = string.Empty;
                        SetLabelText();
                        BasePinTextField.BecomeFirstResponder();
                    });
                    PresentViewController(alert, true, null);
                }
            }
        }

        private void SetLabelText()
        {
            var newText = string.Empty;
            for(int i = 0; i < 4; i++)
            {
                newText += BasePinTextField.Text.Length <= i ? "- " : "• ";
            }

            BasePinLabel.Text = newText.TrimEnd();
        }
    }
}
