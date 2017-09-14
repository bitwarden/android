using System;
using Bit.iOS.Extension.Models;
using UIKit;
using XLabs.Ioc;
using Plugin.Settings.Abstractions;
using Bit.App.Abstractions;
using Bit.iOS.Core.Utilities;
using Bit.App.Resources;
using System.Diagnostics;
using Bit.App;
using Bit.iOS.Core.Controllers;

namespace Bit.iOS.Extension
{
    public partial class LockPinViewController : ExtendedUIViewController
    {
        private IAppSettingsService _appSettingsService;
        private IAuthService _authService;

        public LockPinViewController(IntPtr handle) : base(handle)
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
            _authService = Resolver.Resolve<IAuthService>();

            NavItem.Title = AppResources.VerifyPIN;
            CancelButton.Title = AppResources.Cancel;
            View.BackgroundColor = new UIColor(red: 0.94f, green: 0.94f, blue: 0.96f, alpha: 1.0f);

            var descriptor = UIFontDescriptor.PreferredBody;
            PinLabel.Font = UIFont.FromName("Menlo-Regular", 35);

            InstructionLabel.Text = AppResources.EnterPIN;
            InstructionLabel.LineBreakMode = UILineBreakMode.WordWrap;
            InstructionLabel.Lines = 0;
            InstructionLabel.Font = UIFont.FromDescriptor(descriptor, descriptor.PointSize * 0.8f);
            InstructionLabel.TextColor = new UIColor(red: 0.47f, green: 0.47f, blue: 0.47f, alpha: 1.0f);

            PinTextField.EditingChanged += PinTextField_EditingChanged;

            base.ViewDidLoad();
        }

        public override void ViewDidAppear(bool animated)
        {
            base.ViewDidAppear(animated);
            PinTextField.BecomeFirstResponder();
        }

        private void PinTextField_EditingChanged(object sender, EventArgs e)
        {
            SetLabelText();

            if(PinTextField.Text.Length >= 4)
            {
                if(PinTextField.Text == _authService.PIN)
                {
                    Debug.WriteLine("BW Log, Start Dismiss PIN controller.");
                    _appSettingsService.Locked = false;
                    PinTextField.ResignFirstResponder();
                    LoadingController.DismissLockAndContinue();
                }
                else
                {
                    // TODO: keep track of invalid attempts and logout?

                    var alert = Dialogs.CreateAlert(null, AppResources.InvalidPIN, AppResources.Ok, (a) =>
                    {
                        PinTextField.Text = string.Empty;
                        SetLabelText();
                        PinTextField.BecomeFirstResponder();
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
                newText += PinTextField.Text.Length <= i ? "- " : "● ";
            }

            PinLabel.Text = newText.TrimEnd();
        }

        partial void CancelButton_Activated(UIBarButtonItem sender)
        {
            LoadingController.CompleteRequest(null);
        }
    }
}
