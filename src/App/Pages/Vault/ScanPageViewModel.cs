using System.Threading.Tasks;
using System.Windows.Input;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;
using Xamarin.Essentials;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class ScanPageViewModel : BaseViewModel
    {
        private bool _showScanner = true;
        private string _totpAuthenticationKey;
        private IPlatformUtilsService _platformUtilsService;
        private IDeviceActionService _deviceActionService;

        public ScanPageViewModel()
        {
            ToggleScanModeCommand = new AsyncCommand(ToggleScanMode);
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
        }

        public ICommand ToggleScanModeCommand { get; set; }
        public ICommand InitScannerCommand { get; set; }

        public bool HasCameraPermission { get; set; }
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

        private async Task ToggleScanMode()
        {
            var cameraPermission = await PermissionManager.CheckAndRequestPermissionAsync(new Permissions.Camera());
            HasCameraPermission = cameraPermission == PermissionStatus.Granted;
            if (!HasCameraPermission)
            {
                var openAppSettingsResult = await _platformUtilsService.ShowDialogAsync(AppResources.EnableCamerPermissionToUseTheScanner, title: string.Empty, confirmText: AppResources.Settings, cancelText: AppResources.NoThanks);
                if (openAppSettingsResult)
                {
                    _deviceActionService.OpenAppSettings();
                    return;
                }
            }
            ShowScanner = !ShowScanner;
            InitScannerCommand.Execute(null);
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
