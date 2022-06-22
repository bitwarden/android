using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class ScanPageViewModel: BaseViewModel
    {
        private bool _showScanner;
        private string _totpAuthenticationKey;

        public ScanPageViewModel()
        {
            ShowScanner = true;
            ToggleScanModeCommand = new Command(ToggleScanAsync);
        }

        public Command ToggleScanModeCommand { get; set; }
        public string ScanQrPageTitle => ShowScanner ? AppResources.ScanQrTitle : AppResources.AuthenticatorKeyScanner;
        public string CameraInstructionTop => ShowScanner ? AppResources.CameraInstructionTop : AppResources.OnceTheKeyIsSuccessfullyEntered;
        public string CameraInstructionBottom => ShowScanner ? AppResources.CameraInstructionBottom : AppResources.SelectAddTotpToStoreTheKeySafely;
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
                    nameof(CameraInstructionTop),
                    nameof(CameraInstructionBottom)
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
                    TextColor = ThemeManager.GetResourceColor("BackgroundColor")
                });
                fs.Spans.Add(new Span
                {
                    Text = ShowScanner ? AppResources.EnterCodeManually : AppResources.ScanQRCode,
                    TextColor = ThemeManager.GetResourceColor("PrimaryColor")
                });
                return fs;
            }
        }
        
        private void ToggleScanAsync()
        {
            ShowScanner = !ShowScanner;
        }
    }
}
