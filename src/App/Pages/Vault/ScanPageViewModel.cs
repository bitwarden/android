using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class ScanPageViewModel : BaseViewModel
    {
        private bool _showScanner = true;
        private string _totpAuthenticationKey;

        public ScanPageViewModel()
        {
            ToggleScanModeCommand = new Command(() => ShowScanner = !ShowScanner);
        }

        public Command ToggleScanModeCommand { get; set; }
        public string ScanQrPageTitle => ShowScanner ? AppResources.ScanQrTitle : AppResources.AuthenticatorKeyScanner;
        public string CameraInstructionTop => ShowScanner ? AppResources.PointYourCameraAtTheQRCode : AppResources.OnceTheKeyIsSuccessfullyEntered;
        public string TotpAuthenticationKey
        {
            get => _totpAuthenticationKey;
            set => SetProperty(ref _totpAuthenticationKey, value,
                additionalPropertyNames: new string[]
                {
                    nameof(ToggleScanModeLabel)
                });
        }
        public bool ShowScanner
        {
            get => _showScanner;
            set => SetProperty(ref _showScanner, value,
                additionalPropertyNames: new string[]
                {
                    nameof(ToggleScanModeLabel),
                    nameof(ScanQrPageTitle),
                    nameof(CameraInstructionTop)
                });
        }

        public FormattedString ToggleScanModeLabel
        {
            get
            {
                var fs = new FormattedString();
                fs.Spans.Add(new Span
                {
                    Text = ShowScanner ? AppResources.CannotScanQRCode : AppResources.CannotAddAuthenticatorKey,
                    TextColor = ThemeManager.GetResourceColor("TitleTextColor")
                });
                fs.Spans.Add(new Span
                {
                    Text = ShowScanner ? AppResources.EnterKeyManually : AppResources.ScanQRCode,
                    TextColor = ThemeManager.GetResourceColor("ScanningToggleModeTextColor")
                });
                return fs;
            }
        }
    }
}
