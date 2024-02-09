using System.Collections.ObjectModel;
using System.Windows.Input;
using Bit.App.Abstractions;
using Bit.Core.Resources.Localization;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Camera.MAUI;
using Camera.MAUI.ZXingHelper;

namespace Bit.App.Pages
{
    public class ScanPageViewModel : BaseViewModel
    {
        private bool _showScanner = true;
        private string _totpAuthenticationKey;
        private IPlatformUtilsService _platformUtilsService;
        private IDeviceActionService _deviceActionService;
        private ILogger _logger;

        public ScanPageViewModel()
        {
            ToggleScanModeCommand = CreateDefaultAsyncRelayCommand(ToggleScanMode, onException: HandleException);
            StartCameraCommand = new Command(StartCamera);
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _logger = ServiceContainer.Resolve<ILogger>();
            InitAsync().FireAndForget();
        }

        public async Task InitAsync()
        {
            try
            {
                await MainThread.InvokeOnMainThreadAsync(async () =>
                {
                    var hasCameraPermission = await PermissionManager.CheckAndRequestPermissionAsync(new Permissions.Camera());
                    HasCameraPermission = hasCameraPermission == PermissionStatus.Granted;
                    ShowScanner = hasCameraPermission == PermissionStatus.Granted;
                });

                BarCodeOptions = new BarcodeDecodeOptions
                {
                    AutoRotate = false, //shouldn't be needed for QRCodes
                    PossibleFormats = { ZXing.BarcodeFormat.QR_CODE },
                    ReadMultipleCodes = false, //runs slower when true and we only need one
                    TryHarder = false, //runs slower when true
                    TryInverted = true
                };
                TriggerPropertyChanged(nameof(BarCodeOptions));
            }
            catch (Exception ex)
            {
                HandleException(ex);
            }
        }

        private CameraInfo _camera = null;
        public CameraInfo Camera 
        {
            get => _camera;
            set
            {
                _camera = value;
                TriggerPropertyChanged(nameof(Camera));

                StartCameraCommand?.Execute(this);
            }
        }
        private ObservableCollection<CameraInfo> _cameras = new();
        public ObservableCollection<CameraInfo> Cameras
        {
            get => _cameras;
            set
            {
                _cameras = value;
                TriggerPropertyChanged(nameof(Cameras));
            }
        }
        public int NumCameras
        {
            set
            {
                if (value > 0)
                {
                    Camera = Cameras.First();
                }
            }
        }

        public BarcodeDecodeOptions BarCodeOptions { get; set; }
        public bool AutoStartPreview { get; set; } = false;

        public ICommand StartCameraCommand { get; set; }
        public ICommand ToggleScanModeCommand { get; set; }

        private bool _hasCameraPermission = false;
        public bool HasCameraPermission
        {
            get => _hasCameraPermission;
            set
            {
                _hasCameraPermission = value;

                StartCameraCommand?.Execute(this);
            }
        }

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

        private void StartCamera()
        {
            if (HasCameraPermission && Camera != null)
            {
                // Note: If we need to improve performance on Android we can use _cameraView.StartCamera() directly on ScanPage.xaml.cs
                // this allows us to set a specific smaller resolution that should help performance and time to scan.
                // (The supported resolutions are available in the Camera object)
                // This solution would likely replace the "AutoStartPreview" logic in this Command.

                //Setting AutoStartPreview to false and then true should trigger the CameraView to start
                AutoStartPreview = false;
                TriggerPropertyChanged(nameof(AutoStartPreview));
                AutoStartPreview = true;
                TriggerPropertyChanged(nameof(AutoStartPreview));
            }
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
                }
                return;
            }
            ShowScanner = !ShowScanner;
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

        private void HandleException(Exception ex)
        {
            MainThread.InvokeOnMainThreadAsync(async () =>
            {
                await _deviceActionService.HideLoadingAsync();
                await _platformUtilsService.ShowDialogAsync(AppResources.GenericErrorMessage);
            }).FireAndForget();
            _logger.Exception(ex);
        }
    }
}
