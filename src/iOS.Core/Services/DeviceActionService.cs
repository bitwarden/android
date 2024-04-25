using System;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.Core.Resources.Localization;
using Bit.App.Utilities.Prompts;
using Bit.Core.Enums;
using Bit.iOS.Core.Utilities;
using Bit.iOS.Core.Views;
using CoreGraphics;
using Foundation;
using LocalAuthentication;
using UIKit;
using Bit.Core.Utilities;

namespace Bit.iOS.Core.Services
{
    public class DeviceActionService : IDeviceActionService
    {
        private Toast _toast;
        private UIAlertController _progressAlert;
        private string _userAgent;

        public string DeviceUserAgent
        {
            get
            {
                if (string.IsNullOrWhiteSpace(_userAgent))
                {
                    _userAgent = $"Bitwarden_Mobile/{AppInfo.VersionString} " +
                        $"(iOS {UIDevice.CurrentDevice.SystemVersion}; Model {UIDevice.CurrentDevice.Model})";
                }
                return _userAgent;
            }
        }

        public Bit.Core.Enums.DeviceType DeviceType => Bit.Core.Enums.DeviceType.iOS;

        public bool LaunchApp(string appName)
        {
            throw new NotImplementedException();
        }

        public void Toast(string text, bool longDuration = false)
        {
            if (!_toast?.Dismissed ?? false)
            {
                _toast.Dismiss(false);
            }
            _toast = new Toast(text)
            {
                Duration = TimeSpan.FromSeconds(longDuration ? 5 : 3)
            };
            _toast.BottomMargin = 110;
            _toast.LeftMargin = 20;
            _toast.RightMargin = 20;
            _toast.Show();
            _toast.DismissCallback = () =>
            {
                _toast?.Dispose();
                _toast = null;
            };
        }

        public Task ShowLoadingAsync(string text)
        {
            if (_progressAlert != null)
            {
                HideLoadingAsync().GetAwaiter().GetResult();
            }
            var vc = GetPresentedViewController();
            if (vc is null)
            {
                return Task.CompletedTask;
            }

            var result = new TaskCompletionSource<int>();

            var loadingIndicator = new UIActivityIndicatorView(new CGRect(10, 5, 50, 50));
            loadingIndicator.HidesWhenStopped = true;
            loadingIndicator.ActivityIndicatorViewStyle = ThemeHelpers.LightTheme ? UIActivityIndicatorViewStyle.Gray :
                UIActivityIndicatorViewStyle.White;
            loadingIndicator.StartAnimating();

            _progressAlert = UIAlertController.Create(null, text, UIAlertControllerStyle.Alert);
            _progressAlert.View.TintColor = UIColor.Black;
            _progressAlert.View.Add(loadingIndicator);

            vc.PresentViewController(_progressAlert, false, () => result.TrySetResult(0));
            return result.Task;
        }

        public Task HideLoadingAsync()
        {
            var result = new TaskCompletionSource<int>();
            if (_progressAlert == null)
            {
                result.TrySetResult(0);
                return result.Task;
            }

            _progressAlert.DismissViewController(false, () => result.TrySetResult(0));
            _progressAlert.Dispose();
            _progressAlert = null;
            return result.Task;
        }

        public Task<string> DisplayPromptAync(string title = null, string description = null,
            string text = null, string okButtonText = null, string cancelButtonText = null,
            bool numericKeyboard = false, bool autofocus = true, bool password = false)
        {
            var vc = GetPresentedViewController();
            if (vc is null)
            {
                return null;
            }

            var result = new TaskCompletionSource<string>();
            var alert = UIAlertController.Create(title ?? string.Empty, description, UIAlertControllerStyle.Alert);
            UITextField input = null;
            okButtonText = okButtonText ?? AppResources.Ok;
            cancelButtonText = cancelButtonText ?? AppResources.Cancel;
            alert.AddAction(UIAlertAction.Create(cancelButtonText, UIAlertActionStyle.Cancel, x =>
            {
                result.TrySetResult(null);
            }));
            alert.AddAction(UIAlertAction.Create(okButtonText, UIAlertActionStyle.Default, x =>
            {
                result.TrySetResult(input.Text ?? string.Empty);
            }));
            alert.AddTextField(x =>
            {
                input = x;
                input.Text = text ?? string.Empty;
                if (numericKeyboard)
                {
                    input.KeyboardType = UIKeyboardType.NumberPad;
                }
                if (password) {
                    input.SecureTextEntry = true;
                }
                if (!ThemeHelpers.LightTheme)
                {
                    input.KeyboardAppearance = UIKeyboardAppearance.Dark;
                }
            });
            vc.PresentViewController(alert, true, null);
            return result.Task;
        }

        public Task<ValidatablePromptResponse?> DisplayValidatablePromptAsync(ValidatablePromptConfig config)
        {
            throw new NotImplementedException();
        }

        public void RateApp()
        {
            string uri = null;
            if (SystemMajorVersion() < 11)
            {
                uri = "itms-apps://itunes.apple.com/WebObjects/MZStore.woa/wa/viewContentsUserReviews" +
                    "?id=1137397744&onlyLatestVersion=true&pageNumber=0&sortOrdering=1&type=Purple+Software";
            }
            else
            {
                uri = "itms-apps://itunes.apple.com/us/app/id1137397744?action=write-review";
            }
            Launcher.OpenAsync(uri).FireAndForget();
        }

        public bool SupportsFaceBiometric()
        {
            if (SystemMajorVersion() < 11)
            {
                return false;
            }
            using (var context = new LAContext())
            {
                if (!context.CanEvaluatePolicy(LAPolicy.DeviceOwnerAuthenticationWithBiometrics, out var e))
                {
                    return false;
                }
                return context.BiometryType == LABiometryType.FaceId;
            }
        }

        public Task<bool> SupportsFaceBiometricAsync()
        {
            return Task.FromResult(SupportsFaceBiometric());
        }

        public bool SupportsNfc()
        {
            if(Application.Current is App.App currentApp && !currentApp.Options.IosExtension)
            {
                return CoreNFC.NFCNdefReaderSession.ReadingAvailable;
            }
            return false;
        }

        public bool SupportsCamera()
        {
            return true;
        }

        public int SystemMajorVersion()
        {
            var versionParts = UIDevice.CurrentDevice.SystemVersion.Split('.');
            if (versionParts.Length > 0 && int.TryParse(versionParts[0], out var version))
            {
                return version;
            }
            // unable to determine version
            return -1;
        }

        public string SystemModel()
        {
            return UIDevice.CurrentDevice.Model;
        }

        public Task<string> DisplayAlertAsync(string title, string message, string cancel, params string[] buttons)
        {
            var vc = GetPresentedViewController();
            if (vc is null)
            {
                return null;
            }

            var result = new TaskCompletionSource<string>();
            var alert = UIAlertController.Create(title ?? string.Empty, message, UIAlertControllerStyle.Alert);
            if (!string.IsNullOrWhiteSpace(cancel))
            {
                alert.AddAction(UIAlertAction.Create(cancel, UIAlertActionStyle.Cancel, x =>
                {
                    result.TrySetResult(cancel);
                }));
            }
            foreach (var button in buttons)
            {
                alert.AddAction(UIAlertAction.Create(button, UIAlertActionStyle.Default, x =>
                {
                    result.TrySetResult(button);
                }));
            }
            vc.PresentViewController(alert, true, null);
            return result.Task;
        }

        public Task<string> DisplayActionSheetAsync(string title, string cancel, string destruction,
            params string[] buttons)
        {
            if (Application.Current is App.App app && app.Options != null && !app.Options.IosExtension)
            {
                return Bit.App.App.MainPage.DisplayActionSheet(title, cancel, destruction, buttons);
            }
            var vc = GetPresentedViewController();
            if (vc is null)
            {
                return null;
            }

            var result = new TaskCompletionSource<string>();
            var sheet = UIAlertController.Create(title, null, UIAlertControllerStyle.ActionSheet);
            if (UIDevice.CurrentDevice.UserInterfaceIdiom == UIUserInterfaceIdiom.Pad)
            {
                var x = vc.View.Bounds.Width / 2;
                var y = vc.View.Bounds.Bottom;
                var rect = new CGRect(x, y, 0, 0);

                sheet.PopoverPresentationController.SourceView = vc.View;
                sheet.PopoverPresentationController.SourceRect = rect;
                sheet.PopoverPresentationController.PermittedArrowDirections = UIPopoverArrowDirection.Unknown;
            }
            foreach (var button in buttons)
            {
                sheet.AddAction(UIAlertAction.Create(button, UIAlertActionStyle.Default,
                    x => result.TrySetResult(button)));
            }
            if (!string.IsNullOrWhiteSpace(destruction))
            {
                sheet.AddAction(UIAlertAction.Create(destruction, UIAlertActionStyle.Destructive,
                    x => result.TrySetResult(destruction)));
            }
            if (!string.IsNullOrWhiteSpace(cancel))
            {
                sheet.AddAction(UIAlertAction.Create(cancel, UIAlertActionStyle.Cancel,
                    x => result.TrySetResult(cancel)));
            }
            vc.PresentViewController(sheet, true, null);
            return result.Task;
        }

        public string GetBuildNumber()
        {
            return NSBundle.MainBundle.InfoDictionary["CFBundleVersion"].ToString();
        }

        public void OpenAccessibilitySettings()
        {
            throw new NotImplementedException();
        }

        public void OpenCredentialProviderSettings() => throw new NotImplementedException();

        public void OpenAutofillSettings()
        {
            throw new NotImplementedException();
        }

        public long GetActiveTime()
        {
            // Fall back to UnixTimeMilliseconds in case this approach stops working. We'll lose clock-change
            // protection but the lock functionality will continue to work.
            return iOSHelpers.GetSystemUpTimeMilliseconds() ?? DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        }

        public Task ExecuteFido2CredentialActionAsync(AppOptions appOptions)
        {
            // only used by Android
            throw new NotImplementedException();
        }

        public void CloseMainApp()
        {
            throw new NotImplementedException();
        }

        public bool SupportsFido2()
        {
            // FIDO2 WebAuthn supported on 13.3+
            var versionParts = UIDevice.CurrentDevice.SystemVersion.Split('.');
            if (versionParts.Length > 0 && int.TryParse(versionParts[0], out var version))
            {
                if (version == 13)
                {
                    if (versionParts.Length > 1 && int.TryParse(versionParts[1], out var minorVersion))
                    {
                        return minorVersion >= 3;
                    }
                }
                else if (version > 13)
                {
                    return true;
                }
            }
            return false;
        }

        public bool SupportsCredentialProviderService() => throw new NotImplementedException();

        public bool SupportsAutofillServices() => UIDevice.CurrentDevice.CheckSystemVersion(12, 0);
        public bool SupportsInlineAutofill() => false;
        public bool SupportsDrawOver() => false;

        private UIViewController GetPresentedViewController()
        {
            var window = UIApplication.SharedApplication.KeyWindow;
            var vc = window.RootViewController;
            while (vc.PresentedViewController != null)
            {
                vc = vc.PresentedViewController;
            }
            return vc;
        }

        private bool TabBarVisible()
        {
            var vc = GetPresentedViewController();
            return vc != null && (vc is UITabBarController ||
                (vc.ChildViewControllers?.Any(c => c is UITabBarController) ?? false));
        }

        public void OpenAccessibilityOverlayPermissionSettings()
        {
            throw new NotImplementedException();
        }

        public float GetSystemFontSizeScale()
        {
            var tempHeight = 20f;
            var scaledHeight = (float)new UIFontMetrics(UIFontTextStyle.Body.GetConstant()).GetScaledValue(tempHeight);
            return scaledHeight / tempHeight;
        }

        public async Task OnAccountSwitchCompleteAsync()
        {
            await ASHelpers.ReplaceAllIdentitiesAsync();
        }

        public Task SetScreenCaptureAllowedAsync()
        {
            // only used by Android. Not possible in iOS
            return Task.CompletedTask;
        }

        public void OpenAppSettings()
        {
            var url = new NSUrl(UIApplication.OpenSettingsUrlString);
            UIApplication.SharedApplication.OpenUrl(url);
        }

        public void CloseExtensionPopUp()
        {
            GetPresentedViewController().DismissViewController(true, null);
        }

        public string GetAutofillAccessibilityDescription() => null;
        public string GetAutofillDrawOverDescription() => null;
    }
}
